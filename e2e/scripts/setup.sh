#!/bin/bash
# E2E Setup — 编译后端 + 前端类型检查 + Docker 环境就绪
# 用法: bash e2e/setup.sh [--skip-build] [--skip-docker]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/.env.e2e"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }
warn() { echo -e "${YELLOW}⚠${NC} $1"; }

SKIP_BUILD=false
SKIP_DOCKER=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --skip-docker) SKIP_DOCKER=true ;;
  esac
done

echo "════════════════════════════════════════"
echo "  E2E Setup — 环境就绪检查"
echo "════════════════════════════════════════"
echo ""

# ── 1. 后端编译 ──
if [ "$SKIP_BUILD" = false ]; then
  echo ">>> 后端编译..."
  cd "$PROJECT_DIR/backend"
  if ./runningerrands-server/mvnw compile -q -DskipTests 2>&1 | tail -3; then
    pass "后端编译通过"
  else
    fail "后端编译失败，请检查错误后重试"
  fi

  # 运行单元测试
  echo ">>> 运行单元测试..."
  if ./runningerrands-server/mvnw test -q 2>&1 | tail -5; then
    pass "单元测试全部通过"
  else
    warn "部分单元测试失败，继续流程..."
  fi
  cd "$PROJECT_DIR"
fi

# ── 2. 前端类型检查 ──
if [ "$SKIP_BUILD" = false ]; then
  echo ">>> 管理端类型检查..."
  cd "$PROJECT_DIR/admin"
  if npx vue-tsc --noEmit 2>&1 | tail -5; then
    pass "管理端类型检查通过"
  else
    warn "管理端类型检查有警告，继续流程..."
  fi
  cd "$PROJECT_DIR"
fi

# ── 3. Docker 环境检查 ──
if [ "$SKIP_DOCKER" = false ]; then
  echo ">>> Docker 服务状态..."
  cd "$PROJECT_DIR/docker"

  if ! docker compose ps 2>/dev/null | grep -q "runningerrands"; then
    warn "Docker 服务未运行，正在启动..."
    docker compose up -d
    echo "等待服务就绪..."
  fi

  # 健康检查轮询
  echo ">>> 等待后端健康检查..."
  for i in $(seq 1 30); do
    if curl -s -o /dev/null -w "%{http_code}" "$API/actuator/health" 2>/dev/null | grep -q "200"; then
      pass "后端健康检查 OK (尝试 $i/30)"
      break
    fi
    if [ "$i" -eq 30 ]; then
      fail "后端启动超时，请检查 docker logs rr-backend"
    fi
    sleep 2
  done

  # 验证 MySQL
  if docker exec "$MYSQL_CONTAINER" mysql -uroot -proot123 -e "SELECT 1" runningerrands >/dev/null 2>&1; then
    pass "MySQL 连接 OK"
  else
    fail "MySQL 连接失败"
  fi

  # 验证 Redis
  if docker exec "$REDIS_CONTAINER" redis-cli -n 1 PING 2>/dev/null | grep -q "PONG"; then
    pass "Redis 连接 OK"
  else
    fail "Redis 连接失败"
  fi

  cd "$PROJECT_DIR"
fi

echo ""
echo "════════════════════════════════════════"
echo -e "  ${GREEN}Setup 完成，环境就绪${NC}"
echo "════════════════════════════════════════"
