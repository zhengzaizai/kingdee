package com.kingdee.qa.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 代表一个独立的聊天会话
 */
public class ChatSession {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final String id;
    private String title;
    private final String createdAt;
    private final List<Message> messages = new ArrayList<>();

    public ChatSession() {
        this.id        = UUID.randomUUID().toString();
        this.title     = "新对话";
        this.createdAt = LocalDateTime.now().format(FMT);
    }

    public static class Message {
        public final String role;
        public final String content;
        public final long   timestamp;
        public final long   elapsedMs;
        public final int    toolCallCount;
        // 分段耗时（毫秒）
        public final long   analyzeMs;
        public final long   apiMs;
        public final long   thinkMs;
        // token
        public final int    inputTokens;
        public final int    outputTokens;
        public final int    totalTokens;

        /** 用户消息 */
        public Message(String role, String content) {
            this(role, content, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        /** 完整助手消息 */
        public Message(String role, String content,
                       long elapsedMs, int toolCallCount,
                       long analyzeMs, long apiMs, long thinkMs,
                       int inputTokens, int outputTokens, int totalTokens) {
            this.role          = role;
            this.content       = content;
            this.timestamp     = System.currentTimeMillis();
            this.elapsedMs     = elapsedMs;
            this.toolCallCount = toolCallCount;
            this.analyzeMs     = analyzeMs;
            this.apiMs         = apiMs;
            this.thinkMs       = thinkMs;
            this.inputTokens   = inputTokens;
            this.outputTokens  = outputTokens;
            this.totalTokens   = totalTokens;
        }
    }

    public void addUserMessage(String content) {
        messages.add(new Message("user", content));
        if (messages.size() == 1 && "新对话".equals(title)) {
            title = content.length() > 20 ? content.substring(0, 20) + "\u2026" : content;
        }
    }

    public void addAssistantMessage(String content, long elapsedMs, int toolCallCount,
                                    long analyzeMs, long apiMs, long thinkMs,
                                    int inputTokens, int outputTokens, int totalTokens) {
        messages.add(new Message("assistant", content,
                elapsedMs, toolCallCount,
                analyzeMs, apiMs, thinkMs,
                inputTokens, outputTokens, totalTokens));
    }

    /** 兼容旧调用 */
    public void addAssistantMessage(String content, long elapsedMs, int toolCallCount) {
        addAssistantMessage(content, elapsedMs, toolCallCount, 0, 0, 0, 0, 0, 0);
    }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getCreatedAt()       { return createdAt; }
    public List<Message> getMessages() { return messages; }
}
