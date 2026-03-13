package com.kingdee.qa.agent;

import com.kingdee.qa.config.ConfigManager;
import com.kingdee.qa.model.ChatResult;
import com.kingdee.qa.model.TokenUsage;
import com.kingdee.qa.skill.KingdeeQuerySkill;
import com.kingdee.qa.skill.SkillLoader;
import com.kingdee.qa.state.IES;
import com.kingdee.qa.ui.TerminalUI;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.*;

public class Agent {

    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    private final SkillLoader skillLoader;
    private final IES ies;
    private final TerminalUI ui;
    private final ConfigManager config;

    private final Assistant assistant;
    private final ChatMemory chatMemory;
    private final Map<String, CallInfo> calledTools = new LinkedHashMap<>();
    private final TokenUsage totalTokenUsage = new TokenUsage();

    /**
     * 通用 System Prompt：只描述行为规则，不包含任何具体工具的参数细节。
     * 具体工具的使用方式（字段名、FilterString 格式等）由各技能的 SKILL.md 文档提供。
     */
    interface Assistant {
        @SystemMessage("你是金蝶ERP智能查询助手。\n" +
            "\n" +
            "【工作流程】\n" +
            "1. 收到用户问题后，先检查 IES（已执行工具调用记录）中是否已有可用结果，有则直接利用。\n" +
            "2. 若 IES 中没有相关数据，查阅消息中的【技能文档】，选择合适的工具并按文档说明构造参数后调用。\n" +
            "3. 获得工具返回结果后，用中文整理并清晰展示给用户，不要重复调用已成功的工具。\n" +
            "4. 若查询结果为空，告知用户未找到相关数据。\n" +
            "5. 用户输入均为中文，请按字面意思理解，不存在乱码。")
        String chat(String userMessage);
    }

    public Agent(SkillLoader skillLoader, KingdeeQuerySkill querySkill,
                 ConfigManager config, TerminalUI ui) {
        this.skillLoader = skillLoader;
        this.config = config;
        this.ui = ui;
        this.ies = new IES();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(config.getDeepSeekApiKey())
                .baseUrl(config.getDeepSeekBaseUrl())
                .modelName(config.getDeepSeekModel())
                .temperature(0.1)
                .maxTokens(4096)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(
                config.getMaxConversationHistory());

        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(chatMemory)
                .tools(querySkill)
                .build();

        log.info("Agent 初始化完成，模型: {}", config.getDeepSeekModel());
    }

    public ChatResult chat(String userInput) {
        long startTime = System.currentTimeMillis();
        calledTools.clear();

        try {
            String fullMessage = buildMessageWithContext(userInput);
            log.info("开始处理用户问题: {}", userInput);

            String response = assistant.chat(fullMessage);

            long elapsed = System.currentTimeMillis() - startTime;
            int toolCalls = ies.size();

            log.info("问题处理完成，耗时 {}ms，工具调用 {} 次", elapsed, toolCalls);
            return new ChatResult(response, totalTokenUsage, elapsed, toolCalls);

        } catch (Exception e) {
            log.error("Agent 处理失败: {}", e.getMessage());
            if (config.isDebugMode()) e.printStackTrace();
            return new ChatResult("处理失败: " + e.getMessage());
        }
    }

    /**
     * 构造用户消息：
     *   - 预加载所有技能的完整 SKILL.md 文档（含具体参数说明）
     *   - 附加 IES 历史（供 AI 判断是否已有可用结果）
     *   - 追加用户问题
     */
    private String buildMessageWithContext(String userInput) {
        StringBuilder sb = new StringBuilder();

        // 预加载所有技能完整文档（Level 2）
        for (String skillName : skillLoader.listSkillNames()) {
            String doc = skillLoader.activateSkill(skillName);
            if (doc != null) {
                sb.append("## 技能文档: ").append(skillName).append("\n");
                sb.append(doc).append("\n\n");
            }
        }

        // IES 历史
        if (!ies.isEmpty()) {
            sb.append("## IES（已执行工具调用记录）\n");
            sb.append(ies.getSummary());
            sb.append("\n");
        }

        sb.append("## 用户问题\n").append(userInput);
        return sb.toString();
    }

    public void clearMemory() {
        chatMemory.clear();
        log.info("对话历史已清空");
    }

    public void resetIES() {
        ies.clear();
        calledTools.clear();
        log.info("IES 已重置");
    }

    public String saveSession(String directory) {
        return ies.saveToFile(directory);
    }

    public IES getIES() {
        return ies;
    }

    private static class CallInfo {
        String toolName;
        boolean success;
        int count;
        CallInfo(String toolName, boolean success) {
            this.toolName = toolName;
            this.success  = success;
            this.count    = 1;
        }
    }

    private String buildSignature(String toolName, String paramsJson) {
        try {
            String raw = toolName + ":" + paramsJson;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return toolName + ":" + paramsJson.hashCode();
        }
    }
}
