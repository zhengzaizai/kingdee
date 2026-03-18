package com.kingdee.qa.model;

/**
 * 一次对话的结果
 * 包含 AI 回复文本、Token 统计和分段耗时
 */
public class ChatResult {

    private final boolean success;
    private final String response;
    private final String error;
    private final TokenUsage tokenUsage;
    private final long elapsedMs;        // 总耗时
    private final int toolCallCount;

    // 分段耗时（毫秒）
    private final long analyzeMs;        // 大模型分析问题（首次到发出工具调用）
    private final long apiMs;            // 外部 HTTP API 累计耗时
    private final long thinkMs;          // 收到 API 结果后到输出答案

    // 成功时使用（含分段耗时）
    public ChatResult(String response, TokenUsage tokenUsage,
                      long elapsedMs, int toolCallCount,
                      long analyzeMs, long apiMs, long thinkMs) {
        this.success       = true;
        this.response      = response;
        this.error         = null;
        this.tokenUsage    = tokenUsage;
        this.elapsedMs     = elapsedMs;
        this.toolCallCount = toolCallCount;
        this.analyzeMs     = analyzeMs;
        this.apiMs         = apiMs;
        this.thinkMs       = thinkMs;
    }

    // 兼容旧调用（不含分段耗时）
    public ChatResult(String response, TokenUsage tokenUsage, long elapsedMs, int toolCallCount) {
        this(response, tokenUsage, elapsedMs, toolCallCount, 0, 0, 0);
    }

    // 失败时使用
    public ChatResult(String error) {
        this.success       = false;
        this.response      = null;
        this.error         = error;
        this.tokenUsage    = new TokenUsage();
        this.elapsedMs     = 0;
        this.toolCallCount = 0;
        this.analyzeMs     = 0;
        this.apiMs         = 0;
        this.thinkMs       = 0;
    }

    public boolean isSuccess()        { return success; }
    public String getResponse()       { return response; }
    public String getError()          { return error; }
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public long getElapsedMs()        { return elapsedMs; }
    public int getToolCallCount()     { return toolCallCount; }
    public long getAnalyzeMs()        { return analyzeMs; }
    public long getApiMs()            { return apiMs; }
    public long getThinkMs()          { return thinkMs; }
}
