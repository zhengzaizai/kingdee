# 金蝶 API 服务封装指南

## 概述

将现有的 `KingdeeApiTool` 改造成独立的 REST API 服务，供 Dify 调用。

## 方案选择

### 方案一：最小改动（推荐）

在现有项目基础上，添加新的 REST 接口：

1. 保留现有代码
2. 新增 `DifyController.java`
3. 暴露两个接口给 Dify

### 方案二：独立服务

创建新的轻量级 Spring Boot 项目，只包含金蝶 API 调用功能。

---

## 实施方案一：添加 Dify 专用接口

### 1. 创建 DifyController

在 `kingdee-qa/src/main/java/com/kingdee/qa/web/` 下创建：

```java
package com.kingdee.qa.web;

import com.kingdee.qa.skill.KingdeeApiTool;
import com.kingdee.qa.skill.SkillLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dify")
@CrossOrigin(origins = "*")
public class DifyController {

    @Autowired
    private SkillLoader skillLoader;

    @Autowired
    private KingdeeApiTool apiTool;

    /**
     * 获取技能文档
     * POST /api/dify/skill/doc
     * Body: {"skillName": "kingdee_query"}
     */
    @PostMapping("/skill/doc")
    public Map<String, Object> getSkillDoc(@RequestBody Map<String, String> request) {
        String skillName = request.get("skillName");
        String doc = skillLoader.activateSkill(skillName);
        
        Map<String, Object> response = new HashMap<>();
        if (doc != null) {
            response.put("success", true);
            response.put("doc", doc);
        } else {
            response.put("success", false);
            response.put("error", "技能不存在: " + skillName);
        }
        return response;
    }

    /**
     * 调用金蝶 API
     * POST /api/dify/kingdee/call
     * Body: {"skillName": "kingdee_query", "requestBody": "{...}"}
     */
    @PostMapping("/kingdee/call")
    public Map<String, Object> callKingdeeApi(@RequestBody Map<String, String> request) {
        String skillName = request.get("skillName");
        String requestBody = request.get("requestBody");
        
        String result = apiTool.callApi(skillName, requestBody);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        return response;
    }

    /**
     * 金蝶登录
     * POST /api/dify/kingdee/login
     * Body: {"acctId": "...", "username": "...", "password": "..."}
     */
    @PostMapping("/kingdee/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        // 实现登录逻辑
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登录成功");
        return response;
    }
}
```

### 2. 修改 pom.xml

确保已添加 Spring Web 依赖（已有则跳过）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 3. 重新编译

```cmd
cd kingdee-qa
mvn clean package -DskipTests
```

### 4. 启动服务

```cmd
java -jar target/kingdee-qa-1.0.0.jar --web
```

### 5. 测试接口

```cmd
# 测试获取技能文档
curl -X POST http://localhost:8080/api/dify/skill/doc ^
  -H "Content-Type: application/json" ^
  -d "{\"skillName\":\"kingdee_query\"}"

# 测试调用金蝶 API
curl -X POST http://localhost:8080/api/dify/kingdee/call ^
  -H "Content-Type: application/json" ^
  -d "{\"skillName\":\"kingdee_query\",\"requestBody\":\"{\\\"FormId\\\":\\\"STK_Inventory\\\"}\"}"
```

---

## 在 Dify 中配置自定义工具

### 工具 1：获取技能文档

```yaml
名称: 获取金蝶技能文档
描述: 获取指定技能的完整接口文档和调用示例
API 端点: http://localhost:8080/api/dify/skill/doc
请求方法: POST
请求头:
  Content-Type: application/json
请求体:
  {
    "skillName": "{{skillName}}"
  }
参数:
  - skillName: 技能名称（kingdee_query 或 kingdee_save）
```

### 工具 2：调用金蝶 API

```yaml
名称: 调用金蝶API
描述: 执行金蝶 WebAPI 查询或保存操作
API 端点: http://localhost:8080/api/dify/kingdee/call
请求方法: POST
请求头:
  Content-Type: application/json
请求体:
  {
    "skillName": "{{skillName}}",
    "requestBody": "{{requestBody}}"
  }
参数:
  - skillName: 技能名称
  - requestBody: 金蝶 API 请求体 JSON 字符串
```

---

## 部署到生产环境

### 使用 Docker

创建 `Dockerfile`：

```dockerfile
FROM openjdk:8-jre-slim
WORKDIR /app
COPY kingdee-qa/target/kingdee-qa-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--web"]
```

构建并运行：

```cmd
docker build -t kingdee-api .
docker run -d -p 8080:8080 --name kingdee-api kingdee-api
```

### 配置反向代理（可选）

如果 Dify 和金蝶 API 服务不在同一台机器，建议使用 Nginx 反向代理：

```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location /api/dify/ {
        proxy_pass http://localhost:8080/api/dify/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 安全建议

1. **添加 API 认证**：使用 API Key 或 JWT Token
2. **限流**：防止 API 被滥用
3. **HTTPS**：生产环境必须使用 HTTPS
4. **IP 白名单**：只允许 Dify 服务器访问

---

## 下一步

阅读 `DIFY_AGENT_CONFIG.md` 了解如何在 Dify 中配置 Agent 应用。
