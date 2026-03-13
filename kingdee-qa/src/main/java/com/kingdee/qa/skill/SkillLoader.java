package com.kingdee.qa.skill;

import com.kingdee.qa.model.SkillMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Skill 加载器
 *
 * 实现渐进式三级加载：
 *   Level 1: 启动时扫描目录，只加载元数据（name + description）
 *   Level 2: AI 决定使用某个 Skill 时，加载完整的 SKILL.md 文档
 *   Level 3: 按需加载额外资源文件（references/ 等）
 *
 * 支持格式：
 *   - 标准格式：SKILL.md（YAML frontmatter + Markdown 正文）
 *   - 旧格式：skill.json（元数据）+ README.md（文档）
 */
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    // Level 1：技能元数据缓存
    private final Map<String, SkillMetadata> metadataMap = new LinkedHashMap<>();

    // Level 2：已激活的技能文档缓存
    private final Map<String, String> docCache = new HashMap<>();

    // Skills 目录路径
    private final Path skillsDir;

    public SkillLoader(String skillsDir) {
        this.skillsDir = Paths.get(skillsDir);
        discoverSkills();
    }

    // ─────────────────────────────────────────
    // Level 1: 发现并加载所有技能元数据
    // ─────────────────────────────────────────

    private void discoverSkills() {
        if (!Files.exists(skillsDir)) {
            log.warn("Skills 目录不存在: {}", skillsDir);
            return;
        }

        File[] skillDirs = skillsDir.toFile().listFiles(File::isDirectory);
        if (skillDirs == null || skillDirs.length == 0) {
            log.warn("Skills 目录为空: {}", skillsDir);
            return;
        }

        for (File skillDir : skillDirs) {
            if (skillDir.getName().startsWith(".")) continue;
            try {
                SkillMetadata meta = loadMetadata(skillDir);
                if (meta != null) {
                    metadataMap.put(meta.getName(), meta);
                    log.info("发现技能: {} - {}", meta.getName(), meta.getDescription());
                }
            } catch (Exception e) {
                log.warn("加载技能目录失败 {}: {}", skillDir.getName(), e.getMessage());
            }
        }
        log.info("共发现 {} 个技能", metadataMap.size());
    }

    private SkillMetadata loadMetadata(File skillDir) throws IOException {
        File skillMd   = new File(skillDir, "SKILL.md");
        File skillJson = new File(skillDir, "skill.json");

        if (skillMd.exists())   return loadFromSkillMd(skillMd, skillDir);
        if (skillJson.exists()) return loadFromSkillJson(skillJson, skillDir);
        return null;
    }

    /** 解析 SKILL.md：YAML frontmatter + Markdown 正文 */
    private SkillMetadata loadFromSkillMd(File skillMd, File skillDir) throws IOException {
        String content = new String(Files.readAllBytes(skillMd.toPath()), StandardCharsets.UTF_8);

        if (!content.startsWith("---")) {
            log.warn("{} 格式错误：缺少 YAML frontmatter", skillMd.getPath());
            return null;
        }

        // 分割出 frontmatter 部分
        String[] parts = content.split("---", 3);
        if (parts.length < 2) return null;

        Yaml yaml = new Yaml();
        Map<String, Object> fm = yaml.load(parts[1]);
        if (fm == null) return null;

        String name = (String) fm.get("name");
        if (name == null || name.trim().isEmpty()) return null;

        SkillMetadata meta = new SkillMetadata();
        meta.setName(name.trim());
        meta.setDescription(getStr(fm, "description", ""));
        meta.setCategory(getStr(fm, "category", "未分类"));
        meta.setVersion(getStr(fm, "version", "1.0.0"));
        meta.setPath(skillDir.getAbsolutePath());
        return meta;
    }

    /** 解析 skill.json（旧格式兼容） */
    private SkillMetadata loadFromSkillJson(File skillJson, File skillDir) throws IOException {
        String content = new String(Files.readAllBytes(skillJson.toPath()), StandardCharsets.UTF_8);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        @SuppressWarnings("unchecked")
        Map<String, Object> cfg = gson.fromJson(content, Map.class);

        String name = (String) cfg.get("name");
        if (name == null || name.trim().isEmpty()) return null;

        SkillMetadata meta = new SkillMetadata();
        meta.setName(name.trim());
        meta.setDescription(getStr(cfg, "description", ""));
        meta.setCategory(getStr(cfg, "category", "未分类"));
        meta.setVersion(getStr(cfg, "version", "1.0.0"));
        meta.setPath(skillDir.getAbsolutePath());
        return meta;
    }

    // ─────────────────────────────────────────
    // Level 2: 激活技能，加载完整文档
    // ─────────────────────────────────────────

    /**
     * 激活技能：读取 SKILL.md 中 frontmatter 之后的 Markdown 正文
     * 同一技能只会读取一次，后续使用缓存
     */
    public String activateSkill(String skillName) {
        // 命中缓存，直接返回
        if (docCache.containsKey(skillName)) {
            log.debug("技能 {} 已激活（缓存命中）", skillName);
            return docCache.get(skillName);
        }

        SkillMetadata meta = metadataMap.get(skillName);
        if (meta == null) {
            log.warn("技能不存在: {}", skillName);
            return null;
        }

        Path dir      = Paths.get(meta.getPath());
        Path skillMd  = dir.resolve("SKILL.md");
        Path readmeMd = dir.resolve("README.md");

        String doc = null;
        try {
            if (Files.exists(skillMd)) {
                String content = new String(Files.readAllBytes(skillMd), StandardCharsets.UTF_8);
                if (content.startsWith("---")) {
                    String[] parts = content.split("---", 3);
                    doc = (parts.length >= 3) ? parts[2].trim() : "";
                } else {
                    doc = content;
                }
            } else if (Files.exists(readmeMd)) {
                doc = new String(Files.readAllBytes(readmeMd), StandardCharsets.UTF_8);
            } else {
                // 没有文档，生成基础说明
                doc = "# " + skillName + "\n\n" + meta.getDescription();
            }
        } catch (IOException e) {
            log.error("读取技能文档失败 {}: {}", skillName, e.getMessage());
        }

        if (doc != null) {
            docCache.put(skillName, doc);
            log.info("技能 {} 已激活，文档 {} 字符", skillName, doc.length());
        }
        return doc;
    }

    // ─────────────────────────────────────────
    // Level 3: 按需加载资源文件
    // ─────────────────────────────────────────

    /**
     * 加载技能目录下的额外资源文件
     *
     * @param skillName    技能名称
     * @param relativePath 相对于技能目录的路径，如 "references/schema.md"
     */
    public String loadResource(String skillName, String relativePath) {
        SkillMetadata meta = metadataMap.get(skillName);
        if (meta == null) return null;

        Path resourceFile = Paths.get(meta.getPath(), relativePath);
        if (!Files.exists(resourceFile)) {
            log.warn("资源文件不存在: {}", resourceFile);
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(resourceFile), StandardCharsets.UTF_8);
            log.info("已加载资源文件: {} ({} 字符)", relativePath, content.length());
            return content;
        } catch (IOException e) {
            log.error("读取资源文件失败: {}", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────
    // 查询方法
    // ─────────────────────────────────────────

    public List<String> listSkillNames() {
        return new ArrayList<>(metadataMap.keySet());
    }

    public SkillMetadata getMetadata(String skillName) {
        return metadataMap.get(skillName);
    }

    public Map<String, SkillMetadata> getAllMetadata() {
        return Collections.unmodifiableMap(metadataMap);
    }

    /** 返回已激活（Level 2 加载完成）的技能名称列表 */
    public List<String> getActivatedSkills() {
        return new ArrayList<>(docCache.keySet());
    }

    /** 获取已激活技能的文档正文 */
    public String getActivatedSkillDoc(String skillName) {
        return docCache.get(skillName);
    }

    /**
     * 生成 Level 1 技能列表描述（放入 AI 提示词）
     * 只包含名称和简短描述，不包含完整文档
     */
    public String generateLevel1Description() {
        if (metadataMap.isEmpty()) {
            return "当前没有可用的技能。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 可用技能列表\n\n");
        sb.append("系统共有 ").append(metadataMap.size()).append(" 个技能可用：\n\n");

        for (SkillMetadata meta : metadataMap.values()) {
            sb.append("- **").append(meta.getName()).append("**: ")
              .append(meta.getDescription()).append("\n");
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return (v != null && !v.toString().trim().isEmpty()) ? v.toString().trim() : defaultVal;
    }
}
