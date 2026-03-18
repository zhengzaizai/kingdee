@echo off
chcp 65001 >nul
echo 正在启动金蝶智能问答 Web 界面...
cd /d "%~dp0"
set JAVA_HOME=D:\java\jdk1.8.0_221
set PATH=%JAVA_HOME%\bin;%PATH%

echo 正在停止旧进程...
taskkill /f /im java.exe >nul 2>&1
timeout /t 2 /nobreak >nul

echo 正在编译...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo 编译失败，请检查错误信息
    pause
    exit /b 1
)
echo 编译完成，正在启动服务...
"%JAVA_HOME%\bin\java" -jar target\kingdee-qa-1.0.0.jar
pause
