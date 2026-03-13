package com.kingdee.qa.model;

/**
 * 一次对话的结果
 * 包含 AI 回复文本、Token 统计和耗时
 */
public class ChatResult {

    private final boolean success;      // 是否成功
    private final String response;      // AI 回复的文本
    private final String error;         // 失败时的错误信息
    private final TokenUsage tokenUsage;// Token 使用统计
    private final long elapsedMs;       // 耗时（毫秒）
    private final int toolCallCount;    // 本次对话调用了多少次工具

    // 成功时使用
    public ChatResult(String response, TokenUsage tokenUsage, long elapsedMs, int toolCallCount) {
        this.success = true;
        this.response = response;
        this.error = null;
        this.tokenUsage = tokenUsage;
        this.elapsedMs = elapsedMs;
        this.toolCallCount = toolCallCount;
    }

    // 失败时使用
    public ChatResult(String error) {
        this.success = false;
        this.response = null;
        this.error = error;
        this.tokenUsage = new TokenUsage();
        this.elapsedMs = 0;
        this.toolCallCount = 0;
    }

    public boolean isSuccess()          { return success; }
    public String getResponse()         { return response; }
    public String getError()            { return error; }
    public TokenUsage getTokenUsage()   { return tokenUsage; }
    public long getElapsedMs()          { return elapsedMs; }
    public int getToolCallCount()       { return toolCallCount; }
}
