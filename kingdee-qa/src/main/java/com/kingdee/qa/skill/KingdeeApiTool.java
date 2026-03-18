package com.kingdee.qa.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.kingdee.qa.http.HttpClient;
import com.kingdee.qa.http.HttpResponse;
import com.kingdee.qa.model.SkillMetadata;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通用金蝶 WebAPI 工具（系统唯一工具类）
 *
 * 只需编写 SKILL.md，无需新增 Java 代码即可扩展新的金蝶接口。
 *
 * 自动识别两种返回格式：
 *   1. ExecuteBillQuery 返回的二维数组 -> 转换为 records 列表
 *   2. 其他接口返回的标准 JSON 对象   -> 直接透传给 LLM
 *
 * 通过 getAccApiMs() 可获取本次 chat 周期内所有 API 调用的累计耗时。
 */
public class KingdeeApiTool {

    private static final Logger log = LoggerFactory.getLogger(KingdeeApiTool.class);

    private final SkillLoader skillLoader;
    private final HttpClient  httpClient;
    private final String      baseUrl;
    private volatile String   sessionId;
    private final Gson        gson = new GsonBuilder().disableHtmlEscaping().create();

    /** 本次 chat 周期内所有 API 调用的累计耗时（毫秒） */
    private final AtomicLong accApiMs = new AtomicLong(0);

    public KingdeeApiTool(SkillLoader skillLoader, HttpClient httpClient,
                          String baseUrl, String sessionId) {
        this.skillLoader = skillLoader;
        this.httpClient  = httpClient;
        this.baseUrl     = baseUrl;
        this.sessionId   = sessionId;
    }

    /** 动态更新 KDSVCSessionId（前端登录后热替换） */
    public synchronized void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        log.info("[KingdeeApiTool] SessionId 已更新");
    }

    /** 返回当前 SessionId（非空表示已登录） */
    public synchronized String getSessionId() {
        return this.sessionId;
    }

    /** 是否已登录（sessionId 非空） */
    public boolean isLoggedIn() {
        String s = this.sessionId;
        return s != null && !s.trim().isEmpty();
    }

    /** Agent 在每次 chat 开始前调用，重置累计计时器 */
    public void resetAccApiMs() {
        accApiMs.set(0);
    }

    /** 返回本次 chat 周期内所有 API 调用的累计耗时（毫秒） */
    public long getAccApiMs() {
        return accApiMs.get();
    }

    @Tool("调用金蝶WebAPI接口。根据skillName找到对应的接口路径，将requestBodyJson作为请求体发送。"
        + "使用前请先阅读对应SKILL.md文档了解该接口的请求体格式和字段说明。"
        + "skillName必须与SKILL.md中的name字段完全一致。"
        + "requestBodyJson必须是合法的JSON字符串，格式参照各SKILL.md中的示例。")
    public String callApi(
        @P("技能名称，必须与Skills目录下SKILL.md中的name字段完全一致。"
         + "例如：kingdee_query(查询)、kingdee_save(保存)")
        String skillName,

        @P("发送给金蝶WebAPI的请求体JSON字符串。格式参照对应SKILL.md文档中的示例。"
         + "查询接口示例: {\"FormId\":\"STK_Inventory\",\"Data\":{\"FormId\":\"STK_Inventory\","
         + "\"FieldKeys\":\"FMATERIALID.FName,FBaseQty\",\"FilterString\":\"\",\"Limit\":100}}")
        String requestBodyJson
    ) {
        log.info("[KingdeeApiTool] skillName={}", skillName);
        System.out.println("  调用工具: callApi  skillName=\"" + skillName + "\"");
        System.out.println("     body = " + abbreviate(requestBodyJson, 200));

        // 参数校验
        if (skillName == null || skillName.trim().isEmpty()) {
            return errorResult("skillName 不能为空");
        }
        if (requestBodyJson == null || requestBodyJson.trim().isEmpty()) {
            return errorResult("requestBodyJson 不能为空");
        }

        // 未登录保护
        if (!isLoggedIn()) {
            return errorResult("尚未登录金蝶ERP，请先通过页面右上角登录入口完成登录");
        }

        // 查找 skill 元数据
        SkillMetadata meta = skillLoader.getMetadata(skillName.trim());
        if (meta == null) {
            return errorResult("未找到技能: " + skillName + "。可用技能: " + skillLoader.listSkillNames());
        }
        if (!meta.hasEndpoint()) {
            return errorResult("技能 [" + skillName + "] 未配置 endpoint.path");
        }

        // 验证 JSON 合法性
        try {
            gson.fromJson(requestBodyJson, Object.class);
        } catch (JsonSyntaxException e) {
            return errorResult("requestBodyJson 不是合法的 JSON: " + e.getMessage());
        }

        String url = baseUrl + meta.getEndpointPath();
        log.info("[callApi] skill={} url={}", skillName, url);
        log.info("[callApi] 请求体: {}", abbreviate(requestBodyJson, 2000));

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if ("session".equalsIgnoreCase(meta.getAuthType())) {
            headers.put("kdservice-sessionid", sessionId);
        }

        // 发起请求并计时
        long apiStart = System.currentTimeMillis();
        HttpResponse response = httpClient.postRaw(url, headers, requestBodyJson);
        long apiCost = System.currentTimeMillis() - apiStart;
        accApiMs.addAndGet(apiCost);
        System.out.println("     API耗时: " + apiCost + "ms");

        if (!response.isOk()) {
            String msg = response.isSuccess()
                ? "HTTP " + response.getStatusCode() + ": " + response.getBody()
                : "网络错误: " + response.getError();
            log.error("[callApi] 调用失败: {}", msg);
            return errorResult(msg);
        }

        log.info("[callApi] 耗时={}ms 响应长度={}", apiCost, response.getBody().length());
        log.info("[callApi] 原始返回: {}", abbreviate(response.getBody(), 3000));
        String parsed = parseResponse(response.getBody());
        log.info("[callApi] 解析结果: {}", abbreviate(parsed, 2000));
        return parsed;
    }

    // ── 响应解析：自动识别二维数组格式（ExecuteBillQuery）和普通 JSON ──

    @SuppressWarnings("unchecked")
    private String parseResponse(String jsonBody) {
        log.debug("[KingdeeApiTool] 原始响应: {}",
                jsonBody.length() > 300 ? jsonBody.substring(0, 300) + "..." : jsonBody);
        try {
            String trimmed = jsonBody.trim();

            // 情况1：直接返回二维数组 [[字段名,...],[值,...], ...]
            if (trimmed.startsWith("[")) {
                List<List<Object>> rows = gson.fromJson(trimmed,
                        new TypeToken<List<List<Object>>>(){}.getType());
                return parseRows(rows);
            }

            // 情况2：包装对象
            Map<String, Object> root = gson.fromJson(trimmed, Map.class);
            Object resultObj = root.get("Result");
            if (resultObj == null) {
                return jsonBody;
            }
            if (resultObj instanceof List) {
                List<List<Object>> rows = (List<List<Object>>) resultObj;
                return parseRows(rows);
            }
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            Object innerRows = resultMap.get("Result");
            if (innerRows == null) {
                return jsonBody;
            }
            List<List<Object>> rows = (List<List<Object>>) innerRows;
            return parseRows(rows);

        } catch (Exception e) {
            log.error("[KingdeeApiTool] 解析响应失败: {}", e.getMessage());
            return "{\"raw_response\": " + jsonBody + "}";
        }
    }

    /** 将二维数组（首行为字段名，其余行为数据）转换为 records 列表 */
    private String parseRows(List<List<Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return successResult(new ArrayList<>(), 0);
        }
        List<Object> headers = rows.get(0);
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            Map<String, Object> record = new LinkedHashMap<>();
            for (int j = 0; j < headers.size() && j < row.size(); j++) {
                record.put(headers.get(j).toString(), row.get(j));
            }
            records.add(record);
        }
        return successResult(records, records.size());
    }

    private String successResult(List<Map<String, Object>> records, int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("total",   total);
        result.put("count",   records.size());
        result.put("records", records);
        return gson.toJson(result);
    }

    private String errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error",   message);
        return gson.toJson(result);
    }

    private String abbreviate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
