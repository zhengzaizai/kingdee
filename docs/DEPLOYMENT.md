# 部署指南

## 快速部署

### 1. 环境准备
- Java 8+ 
- Maven 3.6+
- 金蝶云星空账号
- DeepSeek API Key

### 2. 下载项目
```bash
git clone https://github.com/your-username/kingdee-qa.git
cd kingdee-qa
```

### 3. 配置
复制 `.env.example` 为 `.env`，填入你的配置：
```bash
cp .env.example .env
```

### 4. 编译运行
```bash
cd kingdee-qa
mvn clean package
java -jar target/kingdee-qa-1.0.0.jar
```

### 5. Web 模式
```bash
java -jar target/kingdee-qa-1.0.0.jar --web
```
然后访问 http://localhost:8080

## 生产部署

### Docker 部署
```dockerfile
FROM openjdk:8-jre-slim
COPY kingdee-qa/target/kingdee-qa-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--web"]
```

### 服务化部署
创建 systemd 服务文件 `/etc/systemd/system/kingdee-qa.service`：
```ini
[Unit]
Description=Kingdee QA Service
After=network.target

[Service]
Type=simple
User=kingdee
WorkingDirectory=/opt/kingdee-qa
ExecStart=/usr/bin/java -jar kingdee-qa-1.0.0.jar --web
Restart=always

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl enable kingdee-qa
sudo systemctl start kingdee-qa
```