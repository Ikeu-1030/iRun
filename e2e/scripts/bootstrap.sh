#!/bin/bash
# E2E Bootstrap — 清理旧数据 + 注入测试数据 + 预获取 Admin Token
# 用法: bash e2e/bootstrap.sh [--keep-data]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/.env.e2e"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }

KEEP_DATA=false
[ "${1:-}" = "--keep-data" ] && KEEP_DATA=true

echo "════════════════════════════════════════"
echo "  E2E Bootstrap — 测试数据准备"
echo "════════════════════════════════════════"
echo ""

# ── 1. 清理历史测试数据 ──
if [ "$KEEP_DATA" = false ]; then
  echo ">>> 清理历史 E2E 测试数据..."

  # 获取测试用户 ID
  TEST_USER_IDS=$($MYSQL_CMD -N -e "SELECT GROUP_CONCAT(id) FROM user WHERE username LIKE '${TEST_USER_PREFIX}%'" 2>/dev/null || echo "")

  if [ -n "$TEST_USER_IDS" ] && [ "$TEST_USER_IDS" != "NULL" ]; then
    $MYSQL_CMD -e "
      DELETE FROM review WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN ($TEST_USER_IDS));
      DELETE FROM task_image WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN ($TEST_USER_IDS));
      DELETE FROM task_order WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN ($TEST_USER_IDS));
      DELETE FROM payment_idempotent WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 DAY);
      DELETE FROM transaction_record WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM credit_log WHERE runner_id IN ($TEST_USER_IDS);
      DELETE FROM notification WHERE title LIKE '%E2E%';
      DELETE FROM task WHERE publisher_id IN ($TEST_USER_IDS);
      DELETE FROM runner_profile WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM user_address WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM user WHERE username LIKE '${TEST_USER_PREFIX}%';
    " 2>/dev/null
    pass "MySQL 历史数据已清理"
  else
    pass "MySQL 无历史测试数据"
  fi

  # 清理 Redis 测试 key
  $REDIS_CMD KEYS "user:code:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  $REDIS_CMD KEYS "user:sms:rate:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  $REDIS_CMD KEYS "user:login:rate:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  $REDIS_CMD KEYS "user:login:fail:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  $REDIS_CMD KEYS "user:refresh:token:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  $REDIS_CMD KEYS "user:reset:*:fail:*" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
  pass "Redis 测试 key 已清理"
fi

# ── 2. 注入 SMS 验证码 ──
echo ">>> 注入 SMS 验证码到 Redis..."

VALID_OPERATIONS="register login change_phone reset_password reset_pay_password"
for op in $VALID_OPERATIONS; do
  for phone in $PHONE_A $PHONE_B $PHONE_C; do
    $REDIS_CMD SET "user:code:$op:$phone" "$TEST_CODE" EX 300 >/dev/null 2>&1
  done
done
pass "SMS 验证码已注入（5 操作 × 3 手机号，TTL 300s）"

# ── 3. 预获取 Admin Token ──
echo ">>> 预获取管理员 Token..."
ADMIN_RESP=$(curl -s -X POST "$API/admin/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")

ADMIN_CODE=$(echo "$ADMIN_RESP" | jq -r '.code // "null"')
if [ "$ADMIN_CODE" = "1" ]; then
  ADMIN_TOKEN=$(echo "$ADMIN_RESP" | jq -r '.data.token')
  echo "export ADMIN_TOKEN=\"$ADMIN_TOKEN\"" > "$SCRIPT_DIR/.admin_token"
  pass "Admin Token 已获取"
else
  fail "管理员登录失败: $(echo "$ADMIN_RESP" | jq -r '.msg // "unknown error"')"
fi

# ── 4. 验证基础数据 ──
echo ">>> 验证基础数据..."
USER_COUNT=$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM user" 2>/dev/null)
ADMIN_COUNT=$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM admin" 2>/dev/null)
CONFIG_COUNT=$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM system_config" 2>/dev/null)
echo "  用户: $USER_COUNT | 管理员: $ADMIN_COUNT | 系统配置: $CONFIG_COUNT"

echo ""
echo "════════════════════════════════════════"
echo -e "  ${GREEN}Bootstrap 完成，测试数据已就绪${NC}"
echo "════════════════════════════════════════"
