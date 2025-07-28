#!/bin/bash

# 设置错误时退出
set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 配置变量
JAR_PATH="/Users/allen/Code/javaCode/yudao-boot-mini/yudao-server/target/yudao-server.jar"
REMOTE_HOST="server01"
REMOTE_PATH="/data/yudao"
LOG_FILE="/data/yudao/yudao-server.log"

# 检查jar文件是否存在
if [[ ! -f "$JAR_PATH" ]]; then
    log_error "JAR文件不存在: $JAR_PATH"
    log_info "请确保已经编译了Java项目"
    exit 1
fi

log_info "开始部署后端服务..."
log_info "JAR文件路径: $JAR_PATH"
log_info "远程主机: $REMOTE_HOST"
log_info "远程路径: $REMOTE_PATH"

# 步骤1: 上传jar文件到远程服务器
log_info "步骤1: 上传JAR文件到远程服务器..."
scp "$JAR_PATH" "$REMOTE_HOST:$REMOTE_PATH/"
if [ $? -ne 0 ]; then
    log_error "JAR文件上传失败"
    exit 1
fi
log_success "JAR文件上传成功"

# 步骤2: 在远程服务器上执行部署脚本
log_info "步骤2: 在远程服务器上执行部署脚本..."
ssh "$REMOTE_HOST" "cd $REMOTE_PATH && ./deploy.sh"
if [ $? -ne 0 ]; then
    log_error "远程部署脚本执行失败"
    exit 1
fi
log_success "远程部署脚本执行成功"

# 步骤3: 等待服务启动
log_info "步骤3: 等待服务启动..."
sleep 5

# 步骤4: 查看服务日志
log_info "步骤4: 查看服务日志..."
log_info "=== 服务日志内容 ==="
ssh "$REMOTE_HOST" "tail -n 50 $LOG_FILE"

# 步骤5: 检查服务状态
log_info "步骤5: 检查服务状态..."
ssh "$REMOTE_HOST" "ps aux | grep yudao-server.jar | grep -v grep"

log_success "后端服务部署完成！"
log_info "日志文件位置: $LOG_FILE"
log_info "如需查看实时日志，请运行: ssh $REMOTE_HOST 'tail -f $LOG_FILE'"
