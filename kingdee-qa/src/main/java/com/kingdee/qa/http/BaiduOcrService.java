package com.kingdee.qa.http;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 百度通用文字识别（标准版）服务
 * 文档：https://ai.baidu.com/ai-doc/OCR/zk3h7xz52
 */
public class BaiduOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduOcrService.class);
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    private final BaiduTokenService tokenService;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public BaiduOcrService(BaiduTokenService tokenService) {
        this.tokenService = tokenService;
        this.httpClient   = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 对图片字节数组做 OCR 识别，返回识别出的所有文字（换行拼接）
     *
     * @param imageBytes 图片原始字节（jpg/png/bmp/tiff）
     * @return 识别文字，多行用 \n 连接
     */
    public String recognize(byte[] imageBytes) {
        String token = tokenService.getOcrToken();
        String url   = OCR_URL + "?access_token=" + token;

        // Base64 编码并 URL 编码
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        // 构建 form 请求体
        RequestBody formBody = new FormBody.Builder()
                .add("image", b64)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            log.debug("[百度OCR] 响应: {}", body);
            return parseOcrResult(body);
        } catch (Exception e) {
            throw new RuntimeException("OCR 请求失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String parseOcrResult(String jsonBody) {
        Map<String, Object> root = gson.fromJson(jsonBody, Map.class);
        // 错误码判断
        if (root.containsKey("error_code")) {
            String msg = (String) root.getOrDefault("error_msg", "未知错误");
            throw new RuntimeException("百度OCR识别失败: " + msg + "（error_code=" + root.get("error_code") + "）");
        }
        List<Map<String, Object>> wordResults = (List<Map<String, Object>>) root.get("words_result");
        if (wordResults == null || wordResults.isEmpty()) {
            return "（图片中未识别到文字）";
        }
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> item : wordResults) {
            String word = (String) item.get("words");
            if (word != null && !word.trim().isEmpty()) {
                lines.add(word.trim());
            }
        }
        return lines.isEmpty() ? "（图片中未识别到文字）" : String.join("\n", lines);
    }
}
