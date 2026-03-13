package com.kingdee.qa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 配置管理器
 * 从 application.properties 读取所有系统配置
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private final Properties props = new Properties();

    public ConfigManager() {
        loadConfig("application.properties");
    }

    public ConfigManager(String fileName) {
        loadConfig(fileName);
    }

    private void loadConfig(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException("找不到配置文件: " + fileName
                        + "\n请将 application.properties 放在 src/main/resources/ 目录下");
            }
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            log.info("配置文件加载成功: {}", fileName);
        } catch (IOException e) {
            throw new RuntimeException("配置文件读取失败: " + e.getMessage(), e);
        }
    }

    // ── DeepSeek 配置 ──
    public String getDeepSeekApiKey() {
        return get("deepseek.api.key");
    }

    public String getDeepSeekBaseUrl() {
        return getOrDefault("deepseek.api.base_url", "https://api.deepseek.com");
    }

    public String getDeepSeekModel() {
        return getOrDefault("deepseek.model", "deepseek-chat");
    }

    // ── 金蝶 WebAPI 配置 ──
    public String getKingdeeBaseUrl() {
        return get("kingdee.api.base_url");
    }

    public String getKingdeeAcctId() {
        return get("kingdee.api.acct_id");
    }

    public String getKingdeeUsername() {
        return get("kingdee.api.username");
    }

    public String getKingdeePassword() {
        return get("kingdee.api.password");
    }

    // ── 系统行为配置 ──
    public int getMaxConversationHistory() {
        return getInt("system.max_conversation_history", 10);
    }

    public int getMaxIterations() {
        return getInt("system.max_iterations", 20);
    }

    public boolean isDebugMode() {
        return Boolean.parseBoolean(getOrDefault("system.debug_mode", "false"));
    }

    // ── 工具方法 ──
    private String get(String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("配置项缺失或为空: " + key
                    + "\n请在 application.properties 中配置该项");
        }
        return value.trim();
    }

    private String getOrDefault(String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    private int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 '{}' 不是有效整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public void validate() {
        StringBuilder errors = new StringBuilder();
        checkRequired(errors, "deepseek.api.key",    "DeepSeek API 密钥");
        checkRequired(errors, "kingdee.api.base_url", "金蝶 API 基础URL");
        checkRequired(errors, "kingdee.api.acct_id",  "金蝶账套ID");
        checkRequired(errors, "kingdee.api.username",  "金蝶用户名");
        checkRequired(errors, "kingdee.api.password",  "金蝶密码");
        if (errors.length() > 0) {
            throw new RuntimeException("配置验证失败，以下配置项缺失:\n" + errors);
        }
        log.info("配置验证通过");
    }

    private void checkRequired(StringBuilder errors, String key, String description) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            errors.append("  - ").append(key).append(" (").append(description).append(")\n");
        }
    }
}
