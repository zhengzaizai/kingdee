package com.kingdee.qa.web;

import com.kingdee.qa.agent.Agent;
import com.kingdee.qa.http.BaiduAsrService;
import com.kingdee.qa.http.BaiduOcrService;
import com.kingdee.qa.http.KingdeeAuthService;
import com.kingdee.qa.model.ChatResult;
import com.kingdee.qa.skill.KingdeeApiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller
 *
 * GET    /api/sessions                   - 会话列表
 * POST   /api/sessions                   - 新建会话
 * DELETE /api/sessions/{id}              - 删除会话
 * GET    /api/sessions/{id}/messages     - 获取消息历史
 * POST   /api/chat/stream                - 流式发送消息（SSE）
 * POST   /api/clear/{sessionId}          - 清空会话记忆
 * POST   /api/login                      - 金蝶登录
 * GET    /api/login/status               - 查询当前登录状态
 * POST   /api/ocr                        - 百度OCR文字识别
 * POST   /api/asr                        - 百度语音识别
 */
@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    private final SessionStore       sessionStore;
    private final KingdeeApiTool     kingdeeApiTool;
    private final KingdeeAuthService kingdeeAuthService;
    private final BaiduOcrService    baiduOcrService;
    private final BaiduAsrService    baiduAsrService;

    public ChatController(SessionStore sessionStore,
                          KingdeeApiTool kingdeeApiTool,
                          KingdeeAuthService kingdeeAuthService,
                          BaiduOcrService baiduOcrService,
                          BaiduAsrService baiduAsrService) {
        this.sessionStore       = sessionStore;
        this.kingdeeApiTool     = kingdeeApiTool;
        this.kingdeeAuthService = kingdeeAuthService;
        this.baiduOcrService    = baiduOcrService;
        this.baiduAsrService    = baiduAsrService;
    }

    @GetMapping("/api/sessions")
    public List<Map<String, Object>> listSessions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession s : sessionStore.listSessions()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        s.getId());
            m.put("title",     s.getTitle());
            m.put("createdAt", s.getCreatedAt());
            m.put("msgCount",  s.getMessages().size());
            result.add(m);
        }
        return result;
    }

    @PostMapping("/api/sessions")
    public Map<String, Object> createSession() {
        String id = sessionStore.createSession();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", id);
        return resp;
    }

    @DeleteMapping("/api/sessions/{id}")
    public Map<String, Object> deleteSession(@PathVariable String id) {
        sessionStore.removeSession(id);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        return resp;
    }

    @GetMapping("/api/sessions/{id}/messages")
    public ResponseEntity<Object> getMessages(@PathVariable String id) {
        ChatSession session = sessionStore.getSession(id);
        if (session == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "会话不存在");
            return ResponseEntity.status(404).body(err);
        }
        List<Map<String, Object>> msgList = new ArrayList<>();
        for (ChatSession.Message m : session.getMessages()) {
            msgList.add(messageToMap(m));
        }
        return ResponseEntity.ok(msgList);
    }
    // POST /api/chat
    @PostMapping("/api/chat")
    public ResponseEntity<Object> chat(@RequestBody Map<String, Object> req) {
        String sessionId = (String) req.get("sessionId");
        String message   = (String) req.get("message");
        boolean noMemory = Boolean.TRUE.equals(req.get("noMemory"));

        if (sessionId == null || message == null || message.trim().isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "参数缺失");
            return ResponseEntity.badRequest().body(err);
        }

        ChatSession session = sessionStore.getSession(sessionId);
        Agent       agent   = sessionStore.getAgent(sessionId);
        if (session == null || agent == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "会话不存在，请先创建会话");
            return ResponseEntity.status(404).body(err);
        }

        session.addUserMessage(message.trim());

        try {
            ChatResult result = agent.chat(message.trim(), noMemory);

            if (result.isSuccess()) {
                session.addAssistantMessage(
                        result.getResponse(),
                        result.getElapsedMs(),
                        result.getToolCallCount(),
                        result.getAnalyzeMs(),
                        result.getApiMs(),
                        result.getThinkMs(),
                        result.getTokenUsage().getInputTokens(),
                        result.getTokenUsage().getOutputTokens(),
                        result.getTokenUsage().getTotalTokens());
                return ResponseEntity.ok(buildChatResponse(result, session));
            } else {
                String errReply = "处理失败: " + result.getError();
                session.addAssistantMessage(errReply, 0, 0);
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("reply", errReply);
                resp.put("elapsedMs", 0); resp.put("toolCalls", 0);
                resp.put("analyzeMs", 0); resp.put("apiMs", 0); resp.put("thinkMs", 0);
                resp.put("inputTokens", 0); resp.put("outputTokens", 0); resp.put("totalTokens", 0);
                resp.put("sessionTitle", session.getTitle());
                return ResponseEntity.ok(resp);
            }
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }



        @PostMapping("/api/clear/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        Agent agent = sessionStore.getAgent(sessionId);
        if (agent != null) agent.clearMemory();
        ChatSession session = sessionStore.getSession(sessionId);
        if (session != null) session.getMessages().clear();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        return resp;
    }

    @PostMapping("/api/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, Object> req) {
        String acctId   = (String) req.get("acctId");
        String username = (String) req.get("username");
        String password = (String) req.get("password");
        if (acctId == null || acctId.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false); err.put("error", "账套ID、用户名、密码均不能为空");
            return ResponseEntity.badRequest().body(err);
        }
        try {
            String sid = kingdeeAuthService.login(acctId.trim(), username.trim(), password.trim());
            kingdeeApiTool.setSessionId(sid);
            log.info("[前端登录] 用户 {} 登录成功", username);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true); resp.put("username", username.trim());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.warn("[前端登录] 登录失败: {}", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false); err.put("error", e.getMessage());
            return ResponseEntity.status(401).body(err);
        }
    }

    @GetMapping("/api/login/status")
    public Map<String, Object> loginStatus() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("loggedIn", kingdeeApiTool.isLoggedIn());
        return resp;
    }

    @PostMapping("/api/ocr")
    public ResponseEntity<Object> ocr(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "未收到图片文件"); return ResponseEntity.badRequest().body(err);
        }
        try {
            byte[] bytes = file.getBytes();
            log.info("[OCR] 收到图片，大小: {} bytes", bytes.length);
            String text = baiduOcrService.recognize(bytes);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true); resp.put("text", text);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false); err.put("error", e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/api/asr")
    public ResponseEntity<Object> asr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "wav") String format,
            @RequestParam(value = "rate", defaultValue = "16000") int rate) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "未收到音频文件"); return ResponseEntity.badRequest().body(err);
        }
        try {
            byte[] bytes = file.getBytes();
            log.info("[ASR] 收到音频，格式: {}, 大小: {} bytes", format, bytes.length);
            String text = baiduAsrService.recognize(bytes, format, rate);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true); resp.put("text", text);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false); err.put("error", e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/api/logs")
    public ResponseEntity<Object> getLogs(
            @RequestParam(value = "lines", defaultValue = "80") int lines) {
        try {
            java.io.File logFile = new java.io.File("logs/kingdee-qa.log");
            if (!logFile.exists()) {
                return ResponseEntity.ok(java.util.Collections.singletonMap("lines", java.util.Collections.emptyList()));
            }
            java.util.List<String> allLines = java.nio.file.Files.readAllLines(
                    logFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            int from = Math.max(0, allLines.size() - lines);
            java.util.List<String> recent = allLines.subList(from, allLines.size());
            return ResponseEntity.ok(java.util.Collections.singletonMap("lines", recent));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.singletonMap("lines",
                    java.util.Collections.singletonList("读取日志失败: " + e.getMessage())));
        }
    }

    private Map<String, Object> buildChatResponse(ChatResult result, ChatSession session) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("reply",        result.getResponse());
        resp.put("elapsedMs",    result.getElapsedMs());
        resp.put("toolCalls",    result.getToolCallCount());
        resp.put("analyzeMs",    result.getAnalyzeMs());
        resp.put("apiMs",        result.getApiMs());
        resp.put("thinkMs",      result.getThinkMs());
        resp.put("inputTokens",  result.getTokenUsage().getInputTokens());
        resp.put("outputTokens", result.getTokenUsage().getOutputTokens());
        resp.put("totalTokens",  result.getTokenUsage().getTotalTokens());
        resp.put("sessionTitle", session.getTitle());
        return resp;
    }

    private Map<String, Object> messageToMap(ChatSession.Message m) {
        Map<String, Object> mm = new LinkedHashMap<>();
        mm.put("role",          m.role);
        mm.put("content",       m.content);
        mm.put("elapsedMs",     m.elapsedMs);
        mm.put("toolCallCount", m.toolCallCount);
        mm.put("analyzeMs",     m.analyzeMs);
        mm.put("apiMs",         m.apiMs);
        mm.put("thinkMs",       m.thinkMs);
        mm.put("inputTokens",   m.inputTokens);
        mm.put("outputTokens",  m.outputTokens);
        mm.put("totalTokens",   m.totalTokens);
        return mm;
    }
}
