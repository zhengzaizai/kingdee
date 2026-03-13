package com.kingdee.qa.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingdee.qa.http.HttpClient;
import com.kingdee.qa.http.HttpResponse;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 金蝶即时查询 Skill
 *
 * 使用 @Tool 注解，LangChain4j 会自动将此方法暴露给 AI 作为工具。
 * AI 可以直接调用 execute() 查询金蝶 ERP 数据。
 *
 * 金蝶 API 返回格式为二维数组：
 *   第一行 = 字段名列表，后续行 = 数据行
 * 本类将其转换为 List<Map<字段名, 值>> 格式，方便 AI 理解。
 */
public class KingdeeQuerySkill {

    private static final Logger log = LoggerFactory.getLogger(KingdeeQuerySkill.class);

    private static final String QUERY_PATH =
        "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String sessionId;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public KingdeeQuerySkill(HttpClient httpClient, String baseUrl, String sessionId) {
        this.httpClient = httpClient;
        this.baseUrl    = baseUrl;
        this.sessionId  = sessionId;
    }

    @Tool("查询金蝶ERP系统单据数据。" +
          "formId指定表单(如STK_Inventory库存,PUR_PurchaseOrder采购订单,BD_Material物料,BD_StockPlace仓库)。" +
          "fieldKeys指定返回字段，关联资料名称用.FName后缀(如FStockId.FName仓库名,FMATERIALID.FName物料名)。" +
          "filterString为OQL过滤条件，字符串值用单引号，支持like模糊查询(如FMATERIALID.FName like '%螺丝%')。" +
          "返回JSON格式结果，包含success、total、count和records数组。")
    public String execute(
        @P("业务对象表单ID。常用: STK_Inventory(即时库存), PUR_PurchaseOrder(采购订单), BD_Material(物料), " +
           "BD_Supplier(供应商), BD_Customer(客户), BD_StockPlace(仓库), SAL_SaleOrder(销售订单)")
        String formId,
        @P("查询字段集合，多个字段用英文逗号分隔。关联资料名称用.FName(如FStockId.FName), " +
           "编码用.FNumber, 直接写字段名只能得到内码。示例: FMATERIALID.FName,FStockId.FName,FBaseQty")
        String fieldKeys,
        @P("过滤条件(可选)，OQL语法，字符串值用单引号。" +
           "示例: FMATERIALID.FName like '%螺丝%' 或 FBillNo='PO001' 或 FDate>='2026-01-01'。不需要时传空字符串")
        String filterString,
        @P("排序条件(可选)，如 FDate desc。不需要时传空字符串")
        String orderString,
        @P("最大返回行数，默认100，最大10000")
        int limit
    ) {
        log.info("[金蝶查询] FormId={}, Fields={}, Filter={}, Limit={}",
                formId, fieldKeys, filterString, limit);
        // 在终端实时显示工具调用参数，方便调试
        System.out.println("  🔧 调用工具: execute");
        System.out.println("     formId       = \"" + formId + "\"");
        System.out.println("     fieldKeys    = \"" + fieldKeys + "\"");
        System.out.println("     filterString = \"" + filterString + "\"");
        System.out.println("     orderString  = \"" + orderString + "\"");
        System.out.println("     limit        = " + limit);

        if (formId == null || formId.trim().isEmpty()) {
            return errorResult("FormId 不能为空");
        }
        if (fieldKeys == null || fieldKeys.trim().isEmpty()) {
            return errorResult("FieldKeys 不能为空");
        }
        if (limit <= 0 || limit > 10000) limit = 100;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("FormId",       formId.trim());
        data.put("FieldKeys",    fieldKeys.trim());
        data.put("FilterString", filterString != null ? filterString.trim() : "");
        data.put("OrderString",  orderString  != null ? orderString.trim()  : "");
        data.put("TopRowCount",  0);
        data.put("StartRow",     0);
        data.put("Limit",        limit);
        data.put("SubSystemId",  "");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("FormId", formId.trim());
        body.put("Data",   data);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type",        "application/json");
        headers.put("kdservice-sessionid", sessionId);

        String url = baseUrl + QUERY_PATH;
        HttpResponse response = httpClient.post(url, headers, body);

        if (!response.isOk()) {
            String msg = response.isSuccess()
                ? "HTTP " + response.getStatusCode() + ": " + response.getBody()
                : "网络错误: " + response.getError();
            log.error("金蝶 API 调用失败: {}", msg);
            return errorResult(msg);
        }

        return parseResponse(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String parseResponse(String jsonBody) {
        log.info("[金蝶原始响应] {}", jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody);
        try {
            String trimmed = jsonBody.trim();

            // ── 情况1：直接返回二维数组 [[字段名...],[值...], ...] ──
            if (trimmed.startsWith("[")) {
                List<List<Object>> rows = gson.fromJson(trimmed,
                        new com.google.gson.reflect.TypeToken<List<List<Object>>>(){}.getType());
                return parseRows(rows);
            }

            // ── 情况2：包装在 {"Result":{"Result":[...]}} 中 ──
            Map<String, Object> root = gson.fromJson(trimmed, Map.class);
            Object resultObj = root.get("Result");
            if (resultObj == null) {
                // 可能是错误响应，直接返回
                return gson.toJson(root);
            }
            if (resultObj instanceof List) {
                // Result 直接是数组
                List<List<Object>> rows = (List<List<Object>>) resultObj;
                return parseRows(rows);
            }
            // Result 是对象，再取内层 Result
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            Object innerRows = resultMap.get("Result");
            if (innerRows == null) {
                return successResult(new ArrayList<>(), 0);
            }
            List<List<Object>> rows = (List<List<Object>>) innerRows;
            return parseRows(rows);

        } catch (Exception e) {
            log.error("解析金蝶响应失败: {}", e.getMessage());
            return "{\"raw_response\": " + jsonBody + "}";
        }
    }

    /** 将二维数组（首行为字段名，其余行为数据）转换为 records 列表 */
    @SuppressWarnings("unchecked")
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
}
