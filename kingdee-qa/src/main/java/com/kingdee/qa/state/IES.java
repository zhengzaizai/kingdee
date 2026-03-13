package com.kingdee.qa.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * IES - Internal Execution State（内部执行状态）
 *
 * 记录本次对话中所有工具调用的历史，包括：
 *   - 工具名称、参数、返回结果
 *   - 是否成功、错误信息
 *   - AI 生成的摘要
 *
 * Agent 在每次迭代时，会把 IES 摘要放入提示词，
 * 让 AI 知道已经做了什么，避免重复调用。
 */
public class IES {

    private static final Logger log = LoggerFactory.getLogger(IES.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // 所有工具调用记录
    private final List<Map<String, Object>> records = new ArrayList<>();
    // 会话ID，用于生成文件名
    private final String sessionId = LocalDateTime.now().format(FMT);
    // 自增计数器，用于生成唯一 ID
    private int counter = 0;

    // ─────────────────────────────────────────
    // 添加记录
    // ─────────────────────────────────────────

    /**
     * 添加一条工具调用记录
     *
     * @param toolName 工具名称，如 kingdee_query
     * @param params   调用参数
     * @param result   返回结果（完整 JSON 字符串）
     * @param success  是否成功
     * @param error    失败时的错误信息
     * @param summary  AI 生成的摘要（可为 null）
     * @return 本条记录的唯一 ID，如 "ies_record_1"
     */
    public String addRecord(String toolName, Map<String, Object> params, String result,
                            boolean success, String error, String summary) {
        counter++;
        String id = "ies_record_" + counter;

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id",        id);
        record.put("timestamp", LocalDateTime.now().toString());
        record.put("tool_name", toolName);
        record.put("params",    params);
        record.put("result",    result);
        record.put("summary",   summary);
        record.put("success",   success);
        record.put("error",     error);

        records.add(record);
        log.debug("IES 记录 [{}] {} success={}", id, toolName, success);
        return id;
    }

    // ─────────────────────────────────────────
    // 查询
    // ─────────────────────────────────────────

    public List<Map<String, Object>> getAllRecords() {
        return Collections.unmodifiableList(records);
    }

    /** 根据 ID 查找记录 */
    public Map<String, Object> getRecordById(String id) {
        for (Map<String, Object> r : records) {
            if (id.equals(r.get("id"))) return r;
        }
        return null;
    }

    /** 获取最后一条记录 */
    public Map<String, Object> getLatestRecord() {
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    public int size() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    // ─────────────────────────────────────────
    // 生成摘要（供 Agent 提示词使用）
    // ─────────────────────────────────────────

    /**
     * 生成 IES 摘要文本，放入 AI 提示词，让 AI 知道历史调用情况
     */
    public String getSummary() {
        if (records.isEmpty()) {
            return "IES 当前为空，尚未调用任何工具。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("IES 中共有 ").append(records.size()).append(" 条工具调用记录：\n\n");

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> r = records.get(i);
            boolean ok = Boolean.TRUE.equals(r.get("success"));
            String status = ok ? "✓" : "✗";
            String name = (String) r.get("tool_name");
            String id   = (String) r.get("id");

            sb.append(i + 1).append(". ").append(status).append(" ")
              .append(name).append(" (ID: ").append(id).append(")\n");

            // 显示参数
            Object params = r.get("params");
            sb.append("   参数: ").append(GSON.toJson(params)).append("\n");

            // 优先显示摘要，没有摘要就截取结果前 150 字
            String summary = (String) r.get("summary");
            if (summary != null && !summary.isEmpty()) {
                sb.append("   摘要: ").append(summary).append("\n");
            } else {
                String result = (String) r.get("result");
                if (result != null) {
                    String preview = result.length() > 150 ? result.substring(0, 150) + "..." : result;
                    sb.append("   结果: ").append(preview).append("\n");
                }
            }

            if (!ok && r.get("error") != null) {
                sb.append("   错误: ").append(r.get("error")).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────
    // 清空 & 持久化
    // ─────────────────────────────────────────

    /** 清空所有记录（reset 命令时调用） */
    public void clear() {
        records.clear();
        counter = 0;
        log.info("IES 已清空");
    }

    /**
     * 将 IES 保存为 JSON 文件
     *
     * @param directory 保存目录路径（如 "ies_history"）
     * @return 保存的文件路径，保存失败返回 null
     */
    public String saveToFile(String directory) {
        if (records.isEmpty()) {
            log.info("IES 为空，跳过保存");
            return null;
        }

        // 确保目录存在
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "ies_" + sessionId + ".json";
        File file = new File(dir, fileName);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("session_id", sessionId);
        data.put("timestamp",  LocalDateTime.now().toString());
        data.put("records",    records);

        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
            log.info("IES 已保存到: {}", file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            log.error("IES 保存失败: {}", e.getMessage());
            return null;
        }
    }

    public String getSessionId() {
        return sessionId;
    }
}
