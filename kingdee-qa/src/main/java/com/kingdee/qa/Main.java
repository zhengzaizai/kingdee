package com.kingdee.qa;

import com.kingdee.qa.agent.Agent;
import com.kingdee.qa.config.ConfigManager;
import com.kingdee.qa.http.HttpClient;
import com.kingdee.qa.http.KingdeeAuthService;
import com.kingdee.qa.model.ChatResult;
import com.kingdee.qa.model.SkillMetadata;
import com.kingdee.qa.skill.KingdeeQuerySkill;
import com.kingdee.qa.skill.SkillLoader;
import com.kingdee.qa.ui.TerminalUI;
import com.kingdee.qa.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 程序入口
 *
 * 启动模式：
 *   终端模式（默认）: run.bat
 *   Web 模式:        run.bat --web  然后浏览器访问 http://localhost:8080
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String IES_DIR = "ies_history";

    public static void main(String[] args) {
        boolean webMode = args != null && Arrays.asList(args).contains("--web");

        try {
            System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            // ignore
        }

        TerminalUI ui = new TerminalUI();
        if (!webMode) ui.printBanner();

        // Step 1: load config
        ConfigManager config;
        try {
            config = new ConfigManager();
            config.validate();
        } catch (Exception e) {
            System.out.println("\n配置加载失败: " + e.getMessage());
            System.out.println("请检查 src/main/resources/application.properties");
            return;
        }

        // Step 2: discover skills
        SkillLoader skillLoader;
        try {
            skillLoader = new SkillLoader(getSkillsPath());
            List<SkillMetadata> skills = skillLoader.listSkillNames().stream()
                    .map(skillLoader::getMetadata)
                    .collect(Collectors.toList());
            if (!webMode) ui.printSkillDiscovery(skills);
            else System.out.println("发现 " + skills.size() + " 个技能");
        } catch (Exception e) {
            System.out.println("\n技能加载失败: " + e.getMessage());
            return;
        }

        // Step 3: login kingdee
        HttpClient httpClient = new HttpClient(60);
        String sessionId;
        try {
            System.out.println("正在登录金蝶ERP...");
            KingdeeAuthService authService = new KingdeeAuthService(httpClient, config.getKingdeeBaseUrl());
            sessionId = authService.login(
                    config.getKingdeeAcctId(),
                    config.getKingdeeUsername(),
                    config.getKingdeePassword());
            System.out.println("金蝶登录成功");
        } catch (Exception e) {
            System.out.println("\n金蝶登录失败: " + e.getMessage());
            System.out.println("请检查 application.properties 中的账套ID、用户名、密码配置");
            return;
        }

        // Step 4: build shared skill
        KingdeeQuerySkill querySkill = new KingdeeQuerySkill(
                httpClient, config.getKingdeeBaseUrl(), sessionId);

        if (webMode) {
            // ── Web 模式 ──
            try {
                WebServer webServer = new WebServer(config, skillLoader, querySkill);
                webServer.start();
                Thread.currentThread().join(); // keep alive
            } catch (InterruptedException ie) {
                System.out.println("服务已停止");
            } catch (Exception e) {
                System.out.println("\nWeb 服务器启动失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // ── 终端模式 ──
            Agent agent = new Agent(skillLoader, querySkill, config, ui);
            ui.printAgentReady();
            runInteractiveLoop(agent, skillLoader, ui, config);
        }
    }

    private static void runInteractiveLoop(Agent agent, SkillLoader skillLoader,
                                           TerminalUI ui, ConfigManager config) {
        String inputEncoding = System.getProperty("stdin.encoding",
                System.getProperty("file.encoding", "UTF-8"));
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
        } catch (Exception e) {
            reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        while (true) {
            ui.printPrompt();
            String input;
            try {
                input = reader.readLine();
            } catch (Exception e) {
                break;
            }
            if (input == null) break;
            input = input.trim();
            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("正在退出...");
                break;
            }

            if (handleCommand(input, agent, skillLoader, ui)) continue;

            ui.printThinking();
            try {
                ChatResult result = agent.chat(input);
                if (result.isSuccess()) {
                    ui.printResponse(result.getResponse());
                    ui.printStatistics(result.getTokenUsage(),
                            result.getElapsedMs(), result.getToolCallCount());
                } else {
                    ui.printError(result.getError());
                }
            } catch (Exception e) {
                ui.printError("处理失败: " + e.getMessage());
                if (config.isDebugMode()) e.printStackTrace();
            }
        }

        shutdown(agent);
    }

    private static boolean handleCommand(String input, Agent agent,
                                         SkillLoader skillLoader, TerminalUI ui) {
        switch (input.toLowerCase()) {
            case "clear":
                agent.clearMemory();
                System.out.println("对话历史已清空");
                return true;
            case "reset":
                agent.resetIES();
                System.out.println("执行状态 (IES) 已重置");
                return true;
            case "ies":
                System.out.println(agent.getIES().getSummary());
                return true;
            case "skills":
                List<String> activated = skillLoader.getActivatedSkills();
                List<String> all = skillLoader.listSkillNames();
                System.out.println("\n所有技能 (" + all.size() + " 个):");
                for (String name : all) {
                    String mark = activated.contains(name) ? " [已激活]" : "";
                    SkillMetadata m = skillLoader.getMetadata(name);
                    System.out.println("  - " + name + mark
                            + (m != null ? ": " + m.getDescription() : ""));
                }
                return true;
            case "help":
                System.out.println("  clear  - 清空对话历史");
                System.out.println("  reset  - 重置执行状态(IES)");
                System.out.println("  ies    - 查看当前执行状态");
                System.out.println("  skills - 查看所有技能");
                System.out.println("  exit   - 退出系统");
                return true;
            default:
                return false;
        }
    }

    private static void shutdown(Agent agent) {
        System.out.println();
        String savedPath = agent.saveSession(IES_DIR);
        if (savedPath != null) {
            System.out.println("会话已保存: " + savedPath);
        }
        System.out.println("再见！");
    }

    private static String getSkillsPath() {
        java.io.File local = new java.io.File("Skills");
        if (local.exists() && local.isDirectory()) {
            return local.getAbsolutePath();
        }
        java.net.URL url = Main.class.getClassLoader().getResource("Skills");
        if (url != null) {
            return new java.io.File(url.getFile()).getAbsolutePath();
        }
        return "Skills";
    }
}
