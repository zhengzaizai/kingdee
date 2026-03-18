package com.kingdee.qa.model;

/**
 * Token 使用统计
 * 记录每次 LLM 调用消耗的 Token 数量
 */
public class TokenUsage {

    private int inputTokens;   // 输入（提示词）Token 数
    private int outputTokens;  // 输出（回复）Token 数
    private int totalTokens;   // 总 Token 数

    public TokenUsage() {}

    public TokenUsage(int inputTokens, int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = inputTokens + outputTokens;
    }

    /** 将另一个 TokenUsage 的数量累加进来（用于统计全局总量） */
    public void add(TokenUsage other) {
        if (other == null) return;
        this.inputTokens  += other.inputTokens;
        this.outputTokens += other.outputTokens;
        this.totalTokens  += other.totalTokens;
    }

    public int getInputTokens()  { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public int getTotalTokens()  { return totalTokens; }

    @Override
    public String toString() {
        return "输入=" + inputTokens + " | 输出=" + outputTokens + " | 总计=" + totalTokens;
    }
}
