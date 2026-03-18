#!/bin/bash

# 金蝶 QA 系统安装脚本 (Linux/Mac)

set -e

echo "🚀 开始安装金蝶 QA 系统..."

# 检查 Java
echo "📋 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java，请先安装 Java 8 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
echo "✅ 找到 Java 版本: $JAVA_VERSION"

# 检查 Maven
echo "📋 检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 未找到 Maven，请先安装 Maven 3.6 或更高版本"
    exit 1
fi

MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
echo "✅ 找到 Maven 版本: $MVN_VERSION"

# 创建配置文件
echo "📝 创建配置文件..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "✅ 已创建 .env 文件，请编辑填入你的配置"
else
    echo "⚠️  .env 文件已存在，跳过创建"
fi

# 编译项目
echo "🔨 编译项目..."
cd kingdee-qa
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 编译成功！"
else
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi

# 创建启动脚本
echo "📝 创建启动脚本..."
cat > ../run.sh << 'EOF'
#!/bin/bash
cd kingdee-qa
java -Dfile.encoding=UTF-8 -jar target/kingdee-qa-1.0.0.jar "$@"
EOF

cat > ../run-web.sh << 'EOF'
#!/bin/bash
cd kingdee-qa
java -Dfile.encoding=UTF-8 -jar target/kingdee-qa-1.0.0.jar --web
EOF

chmod +x ../run.sh ../run-web.sh

echo ""
echo "🎉 安装完成！"
echo ""
echo "📋 下一步："
echo "1. 编辑 .env 文件，填入你的金蝶和 DeepSeek 配置"
echo "2. 运行 ./run.sh 启动终端模式"
echo "3. 或运行 ./run-web.sh 启动 Web 模式"
echo ""
echo "📚 更多信息请查看 README.md"