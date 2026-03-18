package com.kingdee.qa.http;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 金蝶登录服务
 * 启动时调用登录接口，自动获取 KDSVCSessionId
 */
public class KingdeeAuthService {

    private static final Logger log = LoggerFactory.getLogger(KingdeeAuthService.class);

    private static final String LOGIN_PATH =
        "/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Gson gson = new Gson();

    public KingdeeAuthService(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl    = baseUrl;
    }

    /**
     * 登录金蝶，返回 KDSVCSessionId
     *
     * @param acctId   账套ID
     * @param username 用户名
     * @param password 密码
     * @return KDSVCSessionId，登录失败时抛出异常
     */
    public String login(String acctId, String username, String password) {
        log.info("[金蝶登录] 账套={}, 用户={}", acctId, username);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("acctID",   acctId);
        body.put("username", username);
        body.put("password", password);
        body.put("lcid",     2052);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        String url = baseUrl + LOGIN_PATH;
        HttpResponse response = httpClient.post(url, headers, body);

        if (!response.isSuccess() || !response.isOk()) {
            String err = response.isSuccess()
                ? "HTTP " + response.getStatusCode() + ": " + response.getBody()
                : response.getError();
            throw new RuntimeException("金蝶登录请求失败: " + err);
        }

        return parseSessionId(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String parseSessionId(String jsonBody) {
        try {
            Map<String, Object> root = gson.fromJson(jsonBody, Map.class);

            // 检查登录结果
            Object loginResult = root.get("LoginResultType");
            if (loginResult != null) {
                int code = ((Number) loginResult).intValue();
                // 1=成功, -5=管理员登录(可接受)
                if (code != 1 && code != -5) {
                    throw new RuntimeException("金蝶登录失败，LoginResultType=" + code
                        + "（0=用户密码错误，-1=登录失败，-2/-3=密码策略问题）");
                }
            }

            // 取 KDSVCSessionId
            String sessionId = (String) root.get("KDSVCSessionId");
            if (sessionId == null || sessionId.trim().isEmpty()) {
                // 尝试从 Context 中取 SessionId
                Object ctxObj = root.get("Context");
                if (ctxObj instanceof Map) {
                    Map<String, Object> ctx = (Map<String, Object>) ctxObj;
                    sessionId = (String) ctx.get("SessionId");
                }
            }

            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new RuntimeException("登录响应中未找到 KDSVCSessionId，响应: " + jsonBody);
            }

            log.info("[金蝶登录] 成功，SessionId={}", sessionId);
            return sessionId.trim();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析登录响应失败: " + e.getMessage() + "\n原始响应: " + jsonBody, e);
        }
    }
}
