@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在启动金蝶智能问答 Web 界面...
mvn exec:java -Dexec.mainClass="com.kingdee.qa.Main" -Dexec.args="--web" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8
pause
