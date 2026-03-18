@echo off
chcp 65001 >nul
cd /d "%~dp0"
mvn exec:java -Dexec.mainClass="com.kingdee.qa.Main" -Dfile.encoding=UTF-8 -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8
pause
