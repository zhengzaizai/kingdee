package com.kingdee.qa.agent;

import com.kingdee.qa.config.ConfigManager;
import com.kingdee.qa.model.ChatResult;
import com.kingdee.qa.model.TokenUsage;
import com.kingdee.qa.skill.KingdeeApiTool;
import com.kingdee.qa.skill.SkillLoader;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Agent {

    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    private final SkillLoader    skillLoader;
    private final KingdeeApiTool apiTool;
    private final ConfigManager  config;
    private final Assistant      assistant;
    private final ChatMemory     chatMemory;
    private final TokenUsage     totalTokenUsage = new TokenUsage();
    private volatile long        firstToolCallMs = -1;
    private final AtomicInteger  toolCallCounter  = new AtomicInteger(0);
    private final AtomicInteger  lastInputTokens  = new AtomicInteger(0);
    private final AtomicInteger  lastOutputTokens = new AtomicInteger(0);

    interface Assistant {
        @SystemMessage("你是金蝶ERP智能助手。\n"
            + "\n"
            + "【可用工具】\n"
            + "- getSkillDoc(skillName): 获取指定技能的完整接口文档和调用示例\n"
            + "- callApi(skillName, requestBodyJson): 调用金蝶WebAPI接口\n"
            + "\n"
            + "【工作流程】\n"
            + "1. 收到用户问题后，从消息中的【可用技能列表】选择合适的技能\n"
            + "2. 若消息中已包含【已加载的技能文档】部分，直接使用其中的格式，无需调用 getSkillDoc\n"
            + "3. 若消息中没有该技能的文档，先调用 getSkillDoc 获取接口说明，再调用 callApi\n"
            + "4. 获得结果后用中文回答用户\n"
            + "5. 若查询结果为空，告知用户未找到相关数据\n"
            + "6. 用户输入均为中文，请按字面意思理解\n"
            + "\n"
            + "【回答原则】\n"
            + "- 严格按用户问题作答，用户问什么就回答什么，不要延伸、不要补充额外信息\n"
            + "- 查询物料仓库位置时，必须列出所有仓库，不能只回答一个\n"
            + "- 如果用户只问库存最多的物料，只回答那一个物料，不要列出前三或前五\n"
            + "- 如果用户只问某个仓库是否有某物料，只回答有或没有及数量，不要罗列其他仓库\n"
            + "- 回答尽量简洁，数据直接给出，不加冗余解释和总结段落")
        String chat(String userMessage);
    }

    public Agent(SkillLoader skillLoader, KingdeeApiTool apiTool, ConfigManager config) {
        this.skillLoader = skillLoader;
        this.apiTool     = apiTool;
        this.config      = config;

        ChatModelListener tokenListener = new ChatModelListener() {
            @Override public void onRequest(ChatModelRequestContext ctx) {}
            @Override
            public void onResponse(ChatModelResponseContext ctx) {
                dev.langchain4j.model.output.TokenUsage tu = ctx.response().tokenUsage();
                if (tu != null) {
                    lastInputTokens.addAndGet(tu.inputTokenCount()  != null ? tu.inputTokenCount()  : 0);
                    lastOutputTokens.addAndGet(tu.outputTokenCount() != null ? tu.outputTokenCount() : 0);
                }
            }
            @Override public void onError(ChatModelErrorContext ctx) {}
        };

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(config.getDeepSeekApiKey())
                .baseUrl(config.getDeepSeekBaseUrl())
                .modelName(config.getDeepSeekModel())
                .temperature(0.1)
                .maxTokens(4096)
                .listeners(Collections.singletonList(tokenListener))
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(config.getMaxConversationHistory());
        this.assistant  = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new ToolProvider())
                .build();

        log.info("Agent 初始化完成，模型: {}", config.getDeepSeekModel());
    }

    private class ToolProvider {
        @Tool("获取指定技能的完整接口文档，包含请求体格式、字段说明和调用示例。"
            + "首次使用某技能前必须调用此工具；同一技能在本次对话中只需获取一次。")
        public String getSkillDoc(
            @P("技能名称，必须与可用技能列表中的名称完全一致，例如: kingdee_query、kingdee_save")
            String skillName
        ) {
            log.info("[ToolProvider] getSkillDoc skillName={}", skillName);
            if (firstToolCallMs < 0) firstToolCallMs = System.currentTimeMillis();
            toolCallCounter.incrementAndGet();
            String doc = skillLoader.activateSkill(skillName.trim());
            if (doc == null) {
                log.warn("[ToolProvider] 未找到技能: {}", skillName);
                return "{\"error\": \"未找到技能: " + skillName + "，可用技能: " + skillLoader.listSkillNames() + "\"}";
            }
            log.info("[ToolProvider] getSkillDoc 完成，文档长度: {} 字符", doc.length());
            return doc;
        }

        @Tool("调用金蝶WebAPI接口。skillName 指定技能，requestBodyJson 为接口请求体 JSON。"
            + "调用前须先通过 getSkillDoc 获取该技能的文档。")
        public String callApi(
            @P("技能名称，例如: kingdee_query、kingdee_save")
            String skillName,
            @P("发送给金蝶WebAPI的请求体JSON字符串，格式参照 getSkillDoc 返回的文档示例")
            String requestBodyJson
        ) {
            log.info("[ToolProvider] callApi skillName={}, requestBody 长度: {}", skillName, requestBodyJson.length());
            if (firstToolCallMs < 0) firstToolCallMs = System.currentTimeMillis();
            toolCallCounter.incrementAndGet();
            String result = apiTool.callApi(skillName, requestBodyJson);
            log.info("[ToolProvider] callApi 完成，结果长度: {} 字符", result != null ? result.length() : 0);
            return result;
        }
    }

    public ChatResult chat(String userInput) {
        return chat(userInput, false);
    }

    public ChatResult chat(String userInput, boolean noMemory) {
        long startMs = System.currentTimeMillis();
        firstToolCallMs = -1;
        toolCallCounter.set(0);
        apiTool.resetAccApiMs();
        lastInputTokens.set(0);
        lastOutputTokens.set(0);

        ChatMemory memToUse = noMemory ? MessageWindowChatMemory.withMaxMessages(20) : chatMemory;

        Assistant agentToUse = noMemory
            ? AiServices.builder(Assistant.class)
                .chatLanguageModel(OpenAiChatModel.builder()
                    .apiKey(config.getDeepSeekApiKey())
                    .baseUrl(config.getDeepSeekBaseUrl())
                    .modelName(config.getDeepSeekModel())
                    .temperature(0.1)
                    .maxTokens(4096)
                    .listeners(Collections.singletonList(new ChatModelListener() {
                        @Override public void onRequest(ChatModelRequestContext ctx) {}
                        @Override
                        public void onResponse(ChatModelResponseContext ctx) {
                            dev.langchain4j.model.output.TokenUsage tu = ctx.response().tokenUsage();
                            if (tu != null) {
                                lastInputTokens.addAndGet(tu.inputTokenCount()  != null ? tu.inputTokenCount()  : 0);
                                lastOutputTokens.addAndGet(tu.outputTokenCount() != null ? tu.outputTokenCount() : 0);
                            }
                        }
                        @Override public void onError(ChatModelErrorContext ctx) {}
                    }))
                    .build())
                .chatMemory(memToUse)
                .tools(new ToolProvider())
                .build()
            : this.assistant;

        try {
            String fullMessage = buildMessageWithContext(userInput);
            log.info("开始处理用户问题: {}", userInput);
            String response = agentToUse.chat(fullMessage);
            long totalMs   = System.currentTimeMillis() - startMs;
            long apiMs     = apiTool.getAccApiMs();
            long analyzeMs = (firstToolCallMs > 0) ? (firstToolCallMs - startMs) : totalMs;
            long thinkMs   = Math.max(0, totalMs - analyzeMs - apiMs);
            int  toolCalls = toolCallCounter.get();
            log.info("处理完成 总耗时={}ms 分析={}ms API={}ms 整理={}ms 工具调用={}次",
                    totalMs, analyzeMs, apiMs, thinkMs, toolCalls);
            TokenUsage usage = new TokenUsage(lastInputTokens.get(), lastOutputTokens.get());
            if (!noMemory) totalTokenUsage.add(usage);
            lastInputTokens.set(0);
            lastOutputTokens.set(0);
            TokenUsage reportUsage = noMemory ? usage : totalTokenUsage;
            return new ChatResult(response, reportUsage, totalMs, toolCalls, analyzeMs, apiMs, thinkMs);
        } catch (Exception e) {
            log.error("Agent 处理失败: {}", e.getMessage());
            if (config.isDebugMode()) e.printStackTrace();
            return new ChatResult("处理失败: " + e.getMessage());
        }
    }

    public void clearMemory() {
        chatMemory.clear();
        log.info("对话历史已清空");
    }

    public TokenUsage getTotalTokenUsage() { return totalTokenUsage; }

    private String buildMessageWithContext(String userInput) {
        StringBuilder sb = new StringBuilder();
        sb.append(skillLoader.generateLevel1Description());
        sb.append("\n");

        // 将已激活（已学习）的 Skill 文档直接嵌入上下文
        // 这样无记忆模式下也能直接使用，无需重新调用 getSkillDoc
        List<String> activated = skillLoader.getActivatedSkills();
        if (!activated.isEmpty()) {
            sb.append("## 已加载的技能文档（无需再次调用 getSkillDoc）\n\n");
            for (String skillName : activated) {
                String doc = skillLoader.getActivatedSkillDoc(skillName);
                if (doc != null && !doc.isEmpty()) {
                    sb.append("### 技能: ").append(skillName).append("\n");
                    sb.append(doc).append("\n\n");
                }
            }
        }

        sb.append("## 用户问题\n").append(userInput);
        return sb.toString();
    }
}
