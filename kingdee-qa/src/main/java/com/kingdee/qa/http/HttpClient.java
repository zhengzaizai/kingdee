package com.kingdee.qa.http;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 客户端
 * 封装 OkHttp，提供简单的 POST/GET 方法
 */
public class HttpClient {

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    /** 默认超时 30 秒 */
    public HttpClient() {
        this(30);
    }

    /** 自定义超时（秒） */
    public HttpClient(int timeoutSeconds) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    // ─────────────────────────────────────────
    // POST 请求
    // ─────────────────────────────────────────

    /**
     * 发送 POST 请求（请求体为 Map，自动序列化为 JSON）
     *
     * @param url     请求地址
     * @param headers 请求头（如 Authorization、kdservice-sessionid）
     * @param body    请求体对象，会被转为 JSON
     * @return HTTP 响应
     */
    public HttpResponse post(String url, Map<String, String> headers, Object body) {
        String jsonBody = gson.toJson(body);
        log.debug("POST {} 请求体: {}", url, jsonBody);

        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return execute(builder.build());
    }

    // ─────────────────────────────────────────
    // GET 请求
    // ─────────────────────────────────────────

    public HttpResponse get(String url, Map<String, String> headers) {
        log.debug("GET {}", url);

        Request.Builder builder = new Request.Builder().url(url).get();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return execute(builder.build());
    }

    // ─────────────────────────────────────────
    // 内部执行
    // ─────────────────────────────────────────

    private HttpResponse execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            String bodyStr = response.body() != null ? response.body().string() : "";

            if (code != 200) {
                log.warn("HTTP 请求返回非200状态码: {} {}", code, bodyStr);
            } else {
                log.debug("HTTP 请求成功: {}", code);
            }

            return new HttpResponse(code, bodyStr, true, null);
        } catch (IOException e) {
            log.error("HTTP 请求失败: {}", e.getMessage());
            return new HttpResponse(0, null, false, e.getMessage());
        }
    }
}
