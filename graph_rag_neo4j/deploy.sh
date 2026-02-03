#!/bin/bash

# graph-rag-neo4j 服务部署脚本
# 适用于 CentOS 8 Stream + Podman 4.7.x 环境

set -e  # 遇到错误立即退出

echo "开始部署 graph-rag-neo4j 服务..."

# 检查是否安装了 podman
if ! command -v podman &> /dev/null; then
    echo "错误: 未找到 podman，请先安装 podman"
    exit 1
fi

# 检查是否安装了 podman-compose
if ! command -v podman-compose &> /dev/null; then
    echo "警告: 未找到 podman-compose，尝试安装..."
    pip3 install podman-compose || {
        echo "错误: 安装 podman-compose 失败"
        exit 1
    }
fi

# 检查 .env.production 文件是否存在
ENV_FILE=".env.production"
if [ ! -f "$ENV_FILE" ]; then
    echo "错误: $ENV_FILE 文件不存在，请先配置环境变量"
    exit 1
fi

# 复制环境配置到 .env（podman-compose 会使用 .env 文件）
cp "$ENV_FILE" .env

echo "构建并启动服务..."
podman-compose up -d --build

echo "等待服务启动..."
sleep 10

# 检查容器状态
if podman ps | grep -q graph-rag-neo4j; then
    echo "服务部署成功！"
    echo "服务正在运行，端口: 8000"
    echo "可以通过 http://your-server-ip:8000 访问服务"
    
    # 显示容器日志
    echo "最近的日志:"
    podman logs graph-rag-neo4j --tail 10
else
    echo "错误: 服务启动失败"
    echo "查看详细日志:"
    podman logs graph-rag-neo4j
    exit 1
fi

echo "部署完成！"