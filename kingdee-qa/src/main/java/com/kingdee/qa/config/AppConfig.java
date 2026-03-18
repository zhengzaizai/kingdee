package com.kingdee.qa.config;

import com.kingdee.qa.http.BaiduAsrService;
import com.kingdee.qa.http.BaiduOcrService;
import com.kingdee.qa.http.BaiduTokenService;
import com.kingdee.qa.http.HttpClient;
import com.kingdee.qa.http.KingdeeAuthService;
import com.kingdee.qa.skill.KingdeeApiTool;
import com.kingdee.qa.skill.SkillLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.URL;

/**
 * Spring 配置类
 *
 * 负责创建所有业务 Bean：
 *   SkillLoader -> HttpClient -> KingdeeAuthService -> (可选)登录获取 sessionId
 *   -> KingdeeApiTool
 *
 * acct_id / username / password 均为可选：
 *   - 三项都填写 -> 启动时自动登录
 *   - 任意一项为空 -> 跳过自动登录，等待前端手动登录
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${kingdee.api.base_url}")
    private String kingdeeBaseUrl;

    @Value("${kingdee.api.acct_id:}")
    private String acctId;

    @Value("${kingdee.api.username:}")
    private String username;

    @Value("${kingdee.api.password:}")
    private String password;

    @Value("${baidu.ocr.api_key}")
    private String baiduOcrApiKey;

    @Value("${baidu.ocr.secret_key}")
    private String baiduOcrSecretKey;

    @Value("${baidu.asr.api_key}")
    private String baiduAsrApiKey;

    @Value("${baidu.asr.secret_key}")
    private String baiduAsrSecretKey;

    @Bean
    public SkillLoader skillLoader() {
        String path = resolveSkillsPath();
        log.info("技能目录路径: {}", path);
        return new SkillLoader(path);
    }

    @Bean
    public HttpClient httpClient() {
        return new HttpClient(60);
    }

    @Bean
    public KingdeeAuthService kingdeeAuthService(HttpClient httpClient) {
        return new KingdeeAuthService(httpClient, kingdeeBaseUrl);
    }

    @Bean
    public KingdeeApiTool kingdeeApiTool(SkillLoader skillLoader,
                                         HttpClient httpClient,
                                         KingdeeAuthService kingdeeAuthService) {
        String sessionId = "";
        boolean hasCredentials = acctId != null && !acctId.trim().isEmpty()
                && username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
        if (hasCredentials) {
            try {
                log.info("检测到配置文件凭据，正在自动登录金蝶ERP...");
                sessionId = kingdeeAuthService.login(acctId, username, password);
                log.info("金蝶自动登录成功");
            } catch (Exception e) {
                log.warn("金蝶自动登录失败（可通过前端手动登录）: {}", e.getMessage());
            }
        } else {
            log.info("未配置金蝶账号凭据，请通过页面右上角登录入口登录");
        }
        return new KingdeeApiTool(skillLoader, httpClient, kingdeeBaseUrl, sessionId);
    }

    @Bean
    public BaiduTokenService baiduTokenService() {
        return new BaiduTokenService(baiduOcrApiKey, baiduOcrSecretKey, baiduAsrApiKey, baiduAsrSecretKey);
    }

    @Bean
    public BaiduOcrService baiduOcrService(BaiduTokenService baiduTokenService) {
        return new BaiduOcrService(baiduTokenService);
    }

    @Bean
    public BaiduAsrService baiduAsrService(BaiduTokenService baiduTokenService) {
        return new BaiduAsrService(baiduTokenService, baiduAsrApiKey);
    }

    // ── 工具方法 ──

    /**
     * 解析 Skills 目录路径：
     *   1. 优先使用工作目录下的 Skills/（开发/jar 运行场景）
     *   2. 从 jar 同级目录查找
     *   3. 从 classpath 读取并解压到临时目录（fat-jar 场景）
     */
    private String resolveSkillsPath() {
        // 1. 工作目录下的 Skills/
        File local = new File("Skills");
        if (local.exists() && local.isDirectory()) {
            return local.getAbsolutePath();
        }

        // 2. jar 同级目录下的 Skills/
        try {
            File jarDir = new File(AppConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            File siblingSkills = new File(jarDir, "Skills");
            if (siblingSkills.exists() && siblingSkills.isDirectory()) {
                return siblingSkills.getAbsolutePath();
            }
        } catch (Exception ignored) {}

        // 3. 从 classpath 解压到临时目录
        try {
            URL url = getClass().getClassLoader().getResource("Skills");
            if (url != null) {
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    // 普通文件系统（mvn spring-boot:run）
                    return new File(url.toURI()).getAbsolutePath();
                } else if ("jar".equals(protocol)) {
                    // jar 包内部，解压到临时目录
                    return extractSkillsFromJar();
                }
            }
        } catch (Exception e) {
            log.warn("解析 Skills 路径失败: {}", e.getMessage());
        }
        return "Skills";
    }

    /**
     * 将 jar 包内的 Skills 目录解压到系统临时目录
     */
    private String extractSkillsFromJar() throws Exception {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("kingdee-skills-");
        tempDir.toFile().deleteOnExit();

        // 扫描 classpath 中所有 Skills/ 下的资源
        String[] knownSkills = {"kingdee_query", "kingdee_save"};
        for (String skill : knownSkills) {
            String resourcePath = "Skills/" + skill + "/SKILL.md";
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    java.nio.file.Path skillDir = tempDir.resolve(skill);
                    java.nio.file.Files.createDirectories(skillDir);
                    java.nio.file.Files.copy(is, skillDir.resolve("SKILL.md"),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log.info("已解压技能: {}", skill);
                }
            }
        }
        log.info("Skills 已解压到临时目录: {}", tempDir);
        return tempDir.toString();
    }
}
