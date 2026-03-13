package com.kingdee.qa.ui;

import com.kingdee.qa.model.TokenUsage;
import com.kingdee.qa.model.SkillMetadata;

import java.util.List;

/**
 * 终端界面
 * 负责所有控制台输出的格式化
 */
public class TerminalUI {

    private static final String SEP60 = "============================================================";
    private static final String SEP61 = "─────────────────────────────────────────────────────────────";

    // ─────────────────────────────────────────
    // 启动横幅
    // ─────────────────────────────────────────

    public void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║        金蝶 WebAPI 智能问答系统  v1.0.0               ║");
        System.out.println("║        基于 DeepSeek + LangChain4j 构建               ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("支持连续对话和上下文理解");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  - 输入问题进行查询");
        System.out.println("  - 'clear'  - 清空对话历史");
        System.out.println("  - 'reset'  - 重置 IES");
        System.out.println("  - 'ies'    - 查看当前 IES 状态");
        System.out.println("  - 'skills' - 查看已激活的技能");
        System.out.println("  - 'exit' 或 'quit' - 退出系统");
        System.out.println();
    }

    // ─────────────────────────────────────────
    // 技能发现
    // ─────────────────────────────────────────

    public void printSkillDiscovery(List<SkillMetadata> skills) {
        System.out.println(SEP60);
        System.out.println("📦 发现技能 (Level 1 - 元数据加载)");
        System.out.println(SEP60);
        if (skills.isEmpty()) {
            System.out.println("  未发现任何技能，请检查 Skills 目录");
        } else {
            for (SkillMetadata meta : skills) {
                String desc = meta.getDescription();
                if (desc != null && desc.length() > 40) desc = desc.substring(0, 40) + "...";
                System.out.println("  ✓ " + meta.getName() + " (SKILL.md)");
                System.out.println("    " + desc);
            }
        }
        System.out.println(SEP60);
        System.out.println("共发现 " + skills.size() + " 个技能");
        System.out.println(SEP60);
        System.out.println();
        System.out.println("✓ 已发现 " + skills.size() + " 个技能");
        System.out.println();
        System.out.println("正在初始化智能体...");
    }

    public void printAgentReady() {
        System.out.println("✓ 智能体初始化完成");
        System.out.println();
    }

    // ─────────────────────────────────────────
    // 技能激活（Level 2 加载）
    // ─────────────────────────────────────────

    public void printSkillActivation(String skillName, int docLength) {
        System.out.println();
        System.out.println(SEP60);
        System.out.println("📖 激活技能 (Level 2): " + skillName);
        System.out.println(SEP60);
        System.out.println("  ✓ 已加载文档: " + docLength + " 字符 (SKILL.md)");
        System.out.println(SEP60);
    }

    // ─────────────────────────────────────────
    // 迭代过程
    // ─────────────────────────────────────────

    public void printIterationHeader(int iteration) {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.printf( "│ 迭代 %-54d│%n", iteration);
        System.out.println("└─────────────────────────────────────────────────────────┘");
    }

    public void printThinkingComplete(TokenUsage usage) {
        System.out.println("  💭 思考完成 ✓");
        if (usage != null && usage.getTotalTokens() > 0) {
            System.out.printf("     Token: 输入=%,d | 输出=%,d | 总计=%,d%n",
                    usage.getInputTokens(), usage.getOutputTokens(), usage.getTotalTokens());
        }
    }

    public void printToolCall(String toolName, String paramsDesc) {
        System.out.println("  🔧 调用工具: " + toolName);
        if (paramsDesc != null && !paramsDesc.isEmpty()) {
            System.out.println("     参数: " + paramsDesc);
        }
    }

    public void printToolCallSuccess(String summary) {
        System.out.println("  ✅ 工具调用成功");
        if (summary != null && !summary.isEmpty()) {
            for (String line : summary.split("\n")) {
                if (!line.trim().isEmpty()) {
                    System.out.println("     " + line);
                }
            }
        }
    }

    public void printToolCallFailed(String error) {
        System.out.println("  ❌ 工具调用失败: " + error);
    }

    public void printDuplicateWarning(String toolName) {
        System.out.println("  ⚠️  检测到重复调用: " + toolName + "（已使用相同参数调用过，跳过）");
    }

    public void printForcedAnswer() {
        System.out.println("  🛑 重复调用超限，强制基于现有数据生成回答");
    }

    // ─────────────────────────────────────────
    // 最终统计
    // ─────────────────────────────────────────

    public void printStatistics(TokenUsage total, long elapsedMs, int toolCallCount) {
        System.out.println();
        System.out.println(SEP60);
        System.out.println("📊 本次问答统计");
        System.out.println(SEP60);
        System.out.printf( "  ⏱️  耗时:        %.2f 秒%n", elapsedMs / 1000.0);
        System.out.printf( "  🔧 工具调用:    %d 次%n", toolCallCount);
        if (total != null) {
            System.out.printf("  📝 输入 Token:  %,d%n", total.getInputTokens());
            System.out.printf("  💬 输出 Token:  %,d%n", total.getOutputTokens());
            System.out.printf("  📊 总计 Token:  %,d%n", total.getTotalTokens());
        }
        System.out.println(SEP60);
        System.out.println();
    }

    // ─────────────────────────────────────────
    // 错误和提示
    // ─────────────────────────────────────────

    public void printError(String message) {
        System.out.println("  ❌ 错误: " + message);
    }

    public void printWarning(String message) {
        System.out.println("  ⚠️  " + message);
    }

    public void printInfo(String message) {
        System.out.println("  ℹ️  " + message);
    }

    /** 打印用户输入提示符 */
    public void printPrompt() {
        System.out.print("\n👤 用户: ");
    }

    /** 打印 AI 正在分析的提示 */
    public void printThinking() {
        System.out.println();
        System.out.println("🤖 智能体: 正在分析...");
    }

    /** 打印 AI 回复 */
    public void printResponse(String response) {
        System.out.println();
        System.out.println("🤖 智能体: " + response);
    }
}
