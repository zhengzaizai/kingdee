package com.kingdee.qa.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 代表一个独立的聊天会话（对应左侧历史栏中的一条记录）
 */
public class ChatSession {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final String id;
    private String title;            // 会话标题（取第一条用户消息前20字）
    private final String createdAt;
    private final List<Message> messages = new ArrayList<>();

    public ChatSession() {
        this.id        = UUID.randomUUID().toString();
        this.title     = "新对话";
        this.createdAt = LocalDateTime.now().format(FMT);
    }

    // ── 内部消息类 ──
    public static class Message {
        public final String role;     // "user" | "assistant"
        public final String content;
        public final long   timestamp;
        public final long   elapsedMs;
        public final int    toolCallCount;

        public Message(String role, String content, long elapsedMs, int toolCallCount) {
            this.role          = role;
            this.content       = content;
            this.timestamp     = System.currentTimeMillis();
            this.elapsedMs     = elapsedMs;
            this.toolCallCount = toolCallCount;
        }
    }

    public void addUserMessage(String content) {
        messages.add(new Message("user", content, 0, 0));
        // 用第一条用户消息的前 20 个字作为标题
        if (messages.size() == 1 && "新对话".equals(title)) {
            title = content.length() > 20 ? content.substring(0, 20) + "…" : content;
        }
    }

    public void addAssistantMessage(String content, long elapsedMs, int toolCallCount) {
        messages.add(new Message("assistant", content, elapsedMs, toolCallCount));
    }

    // ── Getters ──
    public String getId()                  { return id; }
    public String getTitle()               { return title; }
    public String getCreatedAt()           { return createdAt; }
    public List<Message> getMessages()     { return messages; }
}
