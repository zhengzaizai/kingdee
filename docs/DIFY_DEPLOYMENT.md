# Dify 部署指南

## 方案一：Docker Desktop 部署（推荐 Windows 用户）

### 1. 安装 Docker Desktop

1. 下载 Docker Desktop for Windows：https://www.docker.com/products/docker-desktop/
2. 安装并启动 Docker Desktop
3. 确认安装成功：
```cmd
docker --version
docker-compose --version
```

### 2. 下载 Dify

打开 PowerShell 或 CMD：

```cmd
git clone https://github.com/langgenius/dify.git
cd dify\docker
```

### 3. 启动 Dify

```cmd
docker-compose up -d
```

首次启动会下载镜像，需要 5-10 分钟。

### 4. 访问 Dify

浏览器打开：http://localhost/install

- 设置管理员账号和密码
- 完成初始化

### 5. 停止/重启

```cmd
# 停止
docker-compose down

# 重启
docker-compose restart

# 查看日志
docker-compose logs -f
```

---

## 方案二：云服务器部署

### 使用阿里云/腾讯云

1. 购买云服务器（推荐配置）：
   - CPU: 2核
   - 内存: 4GB
   - 硬盘: 40GB
   - 系统: Ubuntu 22.04

2. 安装 Docker：
```bash
curl -fsSL https://get.docker.com | bash -s docker
```

3. 部署 Dify：
```bash
git clone https://github.com/langgenius/dify.git
cd dify/docker
docker compose up -d
```

4. 配置防火墙开放 80 端口

5. 访问：http://你的服务器IP

---

## 方案三：Dify Cloud（最简单）

直接使用官方云服务，无需自己部署：

1. 访问：https://cloud.dify.ai/
2. 注册账号
3. 立即开始使用

优点：
- 无需部署维护
- 自动更新
- 稳定可靠

缺点：
- 需要付费（有免费额度）
- 数据在云端

---

## 配置 Dify 连接金蝶系统

### 步骤 1：配置 LLM

1. 进入 Dify 控制台
2. 点击右上角头像 → 设置 → 模型供应商
3. 添加 DeepSeek：
   - API Key: 你的 DeepSeek API Key
   - Base URL: https://api.deepseek.com

### 步骤 2：创建知识库

1. 点击"知识库" → "创建知识库"
2. 上传 SKILL.md 文件：
   - `kingdee-qa/src/main/resources/Skills/kingdee_query/SKILL.md`
   - `kingdee-qa/src/main/resources/Skills/kingdee_save/SKILL.md`
3. 选择分段方式：自动分段
4. 点击"保存并处理"

### 步骤 3：部署金蝶 API 服务

需要将金蝶调用逻辑封装成独立 API，详见 `KINGDEE_API_SERVICE.md`

### 步骤 4：创建 Agent 应用

1. 点击"工作室" → "创建应用" → "Agent"
2. 配置系统提示词（参考原 Agent.java）
3. 添加工具：
   - 自定义工具：金蝶查询 API
   - 自定义工具：金蝶保存 API
4. 关联知识库
5. 发布应用

---

## 常见问题

### Q1: Docker Desktop 启动失败
A: 确保开启 Windows 的 WSL2 功能：
```powershell
wsl --install
```

### Q2: 端口 80 被占用
A: 修改 `docker/.env` 文件中的端口：
```
EXPOSE_NGINX_PORT=8080
```

### Q3: 内存不足
A: Docker Desktop → Settings → Resources → 调整内存至 4GB

### Q4: 无法访问外网
A: 配置 Docker 代理或使用国内镜像源

---

## 下一步

- 阅读 `KINGDEE_API_SERVICE.md` 了解如何封装金蝶 API
- 阅读 `DIFY_AGENT_CONFIG.md` 了解如何配置 Dify Agent
