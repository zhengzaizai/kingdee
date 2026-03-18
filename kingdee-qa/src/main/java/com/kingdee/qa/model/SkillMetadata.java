package com.kingdee.qa.model;

/**
 * Skill 元数据（Level 1 加载的轻量信息）
 * 对应 SKILL.md 的 YAML frontmatter
 */
public class SkillMetadata {

    private String name;        // 技能唯一标识，如 kingdee_query
    private String description; // 功能描述，AI 据此决定调用哪个 Skill
    private String category;    // 分类，如 "查询"
    private String version;     // 版本号，如 "1.0.0"
    private String path;        // 技能目录的绝对路径

    // ── 接口配置（从 frontmatter endpoint 节点读取）──
    // 金蝶服务路径，如 /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc
    private String endpointPath;
    // 认证类型："session" 表示需要注入 kdservice-sessionid，"none" 表示不需要
    private String authType = "session";

    public SkillMetadata() {}

    public SkillMetadata(String name, String description, String category,
                         String version, String path) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.version = version;
        this.path = path;
    }

    // ── Getters & Setters ──

    public String getName()        { return name; }
    public void setName(String v)  { this.name = v; }

    public String getDescription()       { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getCategory()       { return category; }
    public void setCategory(String v) { this.category = v; }

    public String getVersion()       { return version; }
    public void setVersion(String v) { this.version = v; }

    public String getPath()       { return path; }
    public void setPath(String v) { this.path = v; }

    public String getEndpointPath()       { return endpointPath; }
    public void setEndpointPath(String v) { this.endpointPath = v; }

    public String getAuthType()       { return authType; }
    public void setAuthType(String v) { this.authType = (v != null) ? v : "session"; }

    /**
     * 是否配置了接口路径（即可被 KingdeeApiTool 通用工具直接调用）
     */
    public boolean hasEndpoint() {
        return endpointPath != null && !endpointPath.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "SkillMetadata{name='" + name + "', description='" + description + "'}";
    }
}
