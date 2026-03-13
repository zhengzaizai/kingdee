package com.kingdee.qa.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingdee.qa.agent.Agent;
import com.kingdee.qa.config.ConfigManager;
import com.kingdee.qa.http.HttpClient;
import com.kingdee.qa.http.KingdeeAuthService;
import com.kingdee.qa.model.ChatResult;
import com.kingdee.qa.skill.KingdeeQuerySkill;
import com.kingdee.qa.skill.SkillLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * 轻量级 HTTP 服务器（基于 JDK 内置 com.sun.net.httpserver）
 *
 * 路由：
 *   GET  /                       → index.html
 *   GET  /api/sessions           → 所有会话列表
 *   POST /api/sessions           → 新建会话 → {"id":"..."}
 *   DELETE /api/sessions/{id}    → 删除会话
 *   GET  /api/sessions/{id}/messages → 指定会话的消息列表
 *   POST /api/chat               → {"sessionId":"...","message":"..."}
 *   POST /api/clear/{sessionId}  → 清空指定会话的对话记忆
 */
@SuppressWarnings("restriction")
public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private static final int    DEFAULT_PORT = 8080;

    private final HttpServer server;
    private final Gson       gson = new GsonBuilder().disableHtmlEscaping().create();

    // sessionId → Agent（每个会话独立记忆）
    private final Map<String, Agent>       agents   = new ConcurrentHashMap<>();
    // sessionId → ChatSession（消息历史）
    private final Map<String, ChatSession> sessions = new LinkedHashMap<>();

    // 共享的金蝶 skill（无状态，可复用）
    private final KingdeeQuerySkill querySkill;
    private final SkillLoader       skillLoader;
    private final ConfigManager     config;

    public WebServer(ConfigManager config, SkillLoader skillLoader,
                     KingdeeQuerySkill querySkill) throws IOException {
        this.config      = config;
        this.skillLoader = skillLoader;
        this.querySkill  = querySkill;

        int port = DEFAULT_PORT;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // 静态文件
        server.createContext("/",           this::handleStatic);
        // API
        server.createContext("/api/sessions", this::handleSessions);
        server.createContext("/api/chat",    this::handleChat);
        server.createContext("/api/clear",   this::handleClear);

        log.info("WebServer 初始化完成，端口: {}", port);
    }

    public void start() {
        server.start();
        int port = server.getAddress().getPort();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   金蝶智能问答 Web 界面已启动                      ║");
        System.out.println("║   请用浏览器打开: http://localhost:" + port + "       ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }

    // ─────────────────────────────────────────────
    // 静态文件处理
    // ─────────────────────────────────────────────

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path) || "/index.html".equals(path)) {
            // 从 classpath 读取 index.html
            InputStream is = getClass().getClassLoader().getResourceAsStream("web/index.html");
            if (is == null) {
                sendText(ex, 404, "找不到 index.html");
                return;
            }
            byte[] bytes = readAllBytes(is);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        } else {
            sendText(ex, 404, "Not Found");
        }
    }

    // ─────────────────────────────────────────────
    // /api/sessions
    // ─────────────────────────────────────────────

    private void handleSessions(HttpExchange ex) throws IOException {
        setCorsHeaders(ex);
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath(); // /api/sessions  or  /api/sessions/{id}/messages

        // DELETE /api/sessions/{id}
        if ("DELETE".equals(method)) {
            String id = extractLastSegment(path);
            sessions.remove(id);
            agents.remove(id);
            sendJson(ex, 200, mapOf("ok", true));
            return;
        }

        // GET /api/sessions/{id}/messages
        if ("GET".equals(method) && path.endsWith("/messages")) {
            String[] parts = path.split("/");
            String id = parts[parts.length - 2];
            ChatSession session = sessions.get(id);
            if (session == null) {
                sendJson(ex, 404, mapOf("error", "会话不存在"));
                return;
            }
            sendJson(ex, 200, session.getMessages());
            return;
        }

        // GET /api/sessions → 列表
        if ("GET".equals(method)) {
            List<Map<String, Object>> list = new ArrayList<>();
            // 倒序，最新在前
            List<ChatSession> ordered = new ArrayList<>(sessions.values());
            Collections.reverse(ordered);
            for (ChatSession s : ordered) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        s.getId());
                m.put("title",     s.getTitle());
                m.put("createdAt", s.getCreatedAt());
                m.put("msgCount",  s.getMessages().size());
                list.add(m);
            }
            sendJson(ex, 200, list);
            return;
        }

        // POST /api/sessions → 新建会话
        if ("POST".equals(method)) {
            ChatSession session = new ChatSession();
            sessions.put(session.getId(), session);
            // 为新会话创建独立 Agent
            Agent agent = createAgent();
            agents.put(session.getId(), agent);
            sendJson(ex, 200, mapOf("id", session.getId()));
            return;
        }

        sendText(ex, 405, "Method Not Allowed");
    }

    // ─────────────────────────────────────────────
    // /api/chat  POST {sessionId, message}
    // ─────────────────────────────────────────────

    private void handleChat(HttpExchange ex) throws IOException {
        setCorsHeaders(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { sendText(ex, 200, ""); return; }
        if (!"POST".equals(ex.getRequestMethod()))   { sendText(ex, 405, "Method Not Allowed"); return; }

        String body = readBody(ex);
        @SuppressWarnings("unchecked")
        Map<String, Object> req = gson.fromJson(body, Map.class);
        String sessionId = (String) req.get("sessionId");
        String message   = (String) req.get("message");

        if (sessionId == null || message == null || message.trim().isEmpty()) {
            sendJson(ex, 400, mapOf("error", "参数缺失"));
            return;
        }

        ChatSession session = sessions.get(sessionId);
        Agent       agent   = agents.get(sessionId);

        if (session == null || agent == null) {
            sendJson(ex, 404, mapOf("error", "会话不存在，请先创建会话"));
            return;
        }

        session.addUserMessage(message.trim());

        try {
            ChatResult result = agent.chat(message.trim());
            String reply;
            long   elapsed    = 0;
            int    toolCalls  = 0;
            if (result.isSuccess()) {
                reply     = result.getResponse();
                elapsed   = result.getElapsedMs();
                toolCalls = result.getToolCallCount();
            } else {
                reply = "处理失败: " + result.getError();
            }
            session.addAssistantMessage(reply, elapsed, toolCalls);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("reply",      reply);
            resp.put("elapsedMs",  elapsed);
            resp.put("toolCalls",  toolCalls);
            resp.put("sessionTitle", session.getTitle());
            sendJson(ex, 200, resp);

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage());
            sendJson(ex, 500, mapOf("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // /api/clear/{sessionId}  POST
    // ─────────────────────────────────────────────

    private void handleClear(HttpExchange ex) throws IOException {
        setCorsHeaders(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { sendText(ex, 200, ""); return; }
        String sessionId = extractLastSegment(ex.getRequestURI().getPath());
        Agent agent = agents.get(sessionId);
        if (agent != null) {
            agent.clearMemory();
            agent.resetIES();
        }
        ChatSession session = sessions.get(sessionId);
        if (session != null) session.getMessages().clear();
        sendJson(ex, 200, mapOf("ok", true));
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private Agent createAgent() {
        return new Agent(skillLoader, querySkill, config, null);
    }

    private void sendJson(HttpExchange ex, int code, Object obj) throws IOException {
        byte[] bytes = gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void setCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readBody(HttpExchange ex) throws IOException {
        return new String(readAllBytes(ex.getRequestBody()), StandardCharsets.UTF_8);
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    private String extractLastSegment(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> mapOf(K k, V v) {
        Map<K, V> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }
}
