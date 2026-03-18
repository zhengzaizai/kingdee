# 金蝶 WebAPI 智能问答系统

基于 LangChain4j 和 DeepSeek 的金蝶 ERP 智能问答系统，支持自然语言查询金蝶数据。

## 功能特性

- 自然语言交互：使用 DeepSeek AI 理解用户意图
- 技能系统：可扩展的技能架构，支持动态加载
- 双模式运行：支持终端交互和 Web 界面
- 智能查询：自动生成金蝶 WebAPI 查询语句
- 会话管理：保存和恢复对话历史

## 环境要求

- Java 8 或更高版本
- Maven 3.6+
- 金蝶云星空账号和 WebAPI 访问权限
- DeepSeek API Key

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-username/kingdee-qa.git
cd kingdee-qa
```

### 2. 填写配置

编辑 `kingdee-qa/src/main/resources/application.properties`，填入你的实际配置：

```properties
# DeepSeek API 配置
deepseek.api.key=你的DeepSeek API Key
deepseek.api.base_url=https://api.deepseek.com
deepseek.model=deepseek-chat

# 金蝶 WebAPI 配置
kingdee.api.base_url=http://你的服务器地址/k3cloud
kingdee.api.acct_id=你的账套ID
kingdee.api.username=你的用户名
kingdee.api.password=你的密码

# 系统配置
system.max_conversation_history=10
system.max_iterations=50
system.debug_mode=false
```

> 账套ID 可从金蝶管理中心数据库查询：`SELECT FDATACENTERID FROM T_BAS_DATACENTER`


##### 3.0非编译安装

    可以直接运行kingdee-qa\run-web.bat，不需要编译
##### 3. 编译安装

####    

#### 方式一：一键安装（推荐）

```bat
scripts\setup.bat
```

脚本会自动检查 Java、Maven 环境并编译项目。

#### 方式二：手动安装

**步骤一：验证环境**

```bat
java -version
mvn -version
```

确认 Java 版本 >= 8，Maven 版本 >= 3.6。

**步骤二：进入子目录并编译**

```bat
cd kingdee-qa
mvn clean package -DskipTests
```

编译成功后会在 `kingdee-qa/target/` 下生成 `kingdee-qa-1.0.0.jar`。

**步骤三：直接运行 JAR**

终端模式：

```bat
java -Dfile.encoding=UTF-8 -jar target/kingdee-qa-1.0.0.jar
```

Web 模式：

```bat
java -Dfile.encoding=UTF-8 -jar target/kingdee-qa-1.0.0.jar --web
```

### 4. 运行（使用脚本）

#### 终端模式

```bat
kingdee-qa\run.bat
```

#### Web 模式

```bat
kingdee-qa\run-web.bat
```

然后在浏览器访问 `http://localhost:8080`

## 使用示例

启动后，可以用自然语言提问：

```
> 查询所有客户
> 帮我找出本月的销售订单
> 查看库存数量大于100的物料
```

## 项目结构

```
kingdee-qa/
├── src/main/java/com/kingdee/qa/
│   ├── agent/          # AI Agent 核心
│   ├── config/         # 配置管理
│   ├── http/           # HTTP 客户端和金蝶认证
│   ├── model/          # 数据模型
│   ├── skill/          # 技能系统
│   ├── state/          # 执行状态管理
│   ├── ui/             # 终端界面
│   ├── web/            # Web 服务器
│   └── Main.java       # 程序入口
├── src/main/resources/
│   ├── Skills/         # 技能定义
│   ├── web/            # Web 界面资源
│   └── application.properties  # 配置文件
└── pom.xml
```

## 技能系统

系统采用可扩展的技能架构，技能定义在 `src/main/resources/Skills/` 目录下。

每个技能包含：
- `SKILL.md`：技能描述和使用说明
- 对应的 Java 实现类

当前支持的技能：
- `kingdee_query`：金蝶数据查询

## 常见问题

### 1. 金蝶登录失败

检查 `application.properties` 中的账套ID、用户名、密码是否正确。

### 2. DeepSeek API 调用失败

确认 API Key 是否有效，网络是否可以访问 `api.deepseek.com`。

### 3. 编码问题

确保使用 UTF-8 编码，Windows 用户请使用提供的 `.bat` 脚本启动。

## 许可证

MIT License
