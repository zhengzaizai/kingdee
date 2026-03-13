@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo 🚀 开始安装金蝶 QA 系统...

REM 检查 Java
echo 📋 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到 Java，请先安装 Java 8 或更高版本
    pause
    exit /b 1
)
echo ✅ 找到 Java 环境

REM 检查 Maven
echo 📋 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到 Maven，请先安装 Maven 3.6 或更高版本
    pause
    exit /b 1
)
echo ✅ 找到 Maven 环境

REM 编译项目
echo 🔨 编译项目...
cd kingdee-qa
call mvn clean package -DskipTests

if errorlevel 1 (
    echo ❌ 编译失败，请检查错误信息
    pause
    exit /b 1
)
echo ✅ 编译成功！

cd ..

echo.
echo 🎉 安装完成！
echo.
echo 📋 下一步：
echo 1. 编辑 kingdee-qa\src\main\resources\application.properties，填入你的金蝶和 DeepSeek 配置
echo 2. 运行 kingdee-qa\run.bat 启动终端模式
echo 3. 或运行 kingdee-qa\run-web.bat 启动 Web 模式
echo.
echo 📚 更多信息请查看 README.md
pause