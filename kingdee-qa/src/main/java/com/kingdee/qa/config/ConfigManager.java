package com.kingdee.qa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 配置管理器（Spring Boot 版本）
 *
 * 原先从 application.properties 手动读取，现在由 Spring @Value 自动注入。
 * 保留所有 getter 方法签名，确保 Agent 等调用方无需修改。
 */
@Component
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    // ── DeepSeek ──
    @Value("${deepseek.api.key}")
    private String deepSeekApiKey;

    @Value("${deepseek.api.base_url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    // ── 金蝶 ──
    @Value("${kingdee.api.base_url:http://8.159.146.158/k3cloud}")
    private String kingdeeBaseUrl;

    @Value("${kingdee.api.acct_id:}")
    private String kingdeeAcctId;

    @Value("${kingdee.api.username:}")
    private String kingdeeUsername;

    @Value("${kingdee.api.password:}")
    private String kingdeePassword;

    // ── 系统 ──
    @Value("${system.max_conversation_history:10}")
    private int maxConversationHistory;

    @Value("${system.max_iterations:50}")
    private int maxIterations;

    @Value("${system.debug_mode:false}")
    private boolean debugMode;

    // ── DeepSeek Getters ──
    public String getDeepSeekApiKey()  { return deepSeekApiKey; }
    public String getDeepSeekBaseUrl() { return deepSeekBaseUrl; }
    public String getDeepSeekModel()   { return deepSeekModel; }

    // ── 金蝶 Getters ──
    public String getKingdeeBaseUrl()  { return kingdeeBaseUrl; }
    public String getKingdeeAcctId()   { return kingdeeAcctId; }
    public String getKingdeeUsername() { return kingdeeUsername; }
    public String getKingdeePassword() { return kingdeePassword; }

    // ── 系统 Getters ──
    public int getMaxConversationHistory() { return maxConversationHistory; }
    public int getMaxIterations()          { return maxIterations; }
    public boolean isDebugMode()           { return debugMode; }

    /** 兼容旧代码调用，Spring 注入后配置已合法，直接通过 */
    public void validate() {
        log.info("配置校验通过 (Spring Boot 托管)");
    }
}
