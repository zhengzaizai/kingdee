package com.kingdee.qa.http;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 百度 AI 平台 Access Token 获取服务
 * 支持 OCR 和 ASR 两套 API Key / Secret Key
 */
public class BaiduTokenService {

    private static final Logger log = LoggerFactory.getLogger(BaiduTokenService.class);
    private static final String TOKEN_URL =
        "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s";

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    // OCR token 缓存
    private String ocrToken;
    private long   ocrTokenExpireMs;

    // ASR token 缓存
    private String asrToken;
    private long   asrTokenExpireMs;

    private final String ocrApiKey;
    private final String ocrSecretKey;
    private final String asrApiKey;
    private final String asrSecretKey;

    public BaiduTokenService(String ocrApiKey, String ocrSecretKey,
                              String asrApiKey, String asrSecretKey) {
        this.ocrApiKey    = ocrApiKey;
        this.ocrSecretKey = ocrSecretKey;
        this.asrApiKey    = asrApiKey;
        this.asrSecretKey = asrSecretKey;
        this.httpClient   = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /** 获取 OCR Access Token（有缓存，提前5分钟刷新） */
    public synchronized String getOcrToken() {
        if (ocrToken == null || System.currentTimeMillis() > ocrTokenExpireMs - 300_000L) {
            ocrToken = fetchToken(ocrApiKey, ocrSecretKey);
            ocrTokenExpireMs = System.currentTimeMillis() + 29 * 24 * 3600 * 1000L;
            log.info("[百度OCR] Access Token 已刷新");
        }
        return ocrToken;
    }

    /** 获取 ASR Access Token（有缓存，提前5分钟刷新） */
    public synchronized String getAsrToken() {
        if (asrToken == null || System.currentTimeMillis() > asrTokenExpireMs - 300_000L) {
            asrToken = fetchToken(asrApiKey, asrSecretKey);
            asrTokenExpireMs = System.currentTimeMillis() + 29 * 24 * 3600 * 1000L;
            log.info("[百度ASR] Access Token 已刷新");
        }
        return asrToken;
    }

    @SuppressWarnings("unchecked")
    private String fetchToken(String apiKey, String secretKey) {
        String url = String.format(TOKEN_URL, apiKey, secretKey);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            Map<String, Object> map = gson.fromJson(body, Map.class);
            String token = (String) map.get("access_token");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("获取百度 Access Token 失败，响应: " + body);
            }
            return token;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("请求百度 Token 接口失败: " + e.getMessage(), e);
        }
    }
}
