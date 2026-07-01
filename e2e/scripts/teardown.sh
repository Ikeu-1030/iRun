#!/bin/bash
# E2E Teardown — 清理测试产生的所有数据
# 用法: bash e2e/teardown.sh [--keep-tokens] [--keep-users]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/.env.e2e"

GREEN='\033[0;32m'
NC='\033[0m'
pass() { echo -e "${GREEN}✓${NC} $1"; }

KEEP_TOKENS=false
KEEP_USERS=false
for arg in "$@"; do
  case "$arg" in
    --keep-tokens) KEEP_TOKENS=true ;;
    --keep-users)  KEEP_USERS=true ;;
  esac
done

echo "════════════════════════════════════════"
echo "  E2E Teardown — 清理测试数据"
echo "════════════════════════════════════════"
echo ""

# ── 1. 清理 Admin Token 缓存 ──
[ "$KEEP_TOKENS" = false ] && rm -f "$SCRIPT_DIR/.admin_token"
pass "Admin Token 缓存已清除"

# ── 2. 清理 MySQL 测试数据 ──
if [ "$KEEP_USERS" = false ]; then
  TEST_USER_IDS=$($MYSQL_CMD -N -e "SELECT GROUP_CONCAT(id) FROM user WHERE username LIKE '${TEST_USER_PREFIX}%'" 2>/dev/null || echo "")

  if [ -n "$TEST_USER_IDS" ] && [ "$TEST_USER_IDS" != "NULL" ]; then
    $MYSQL_CMD -e "
      DELETE FROM review WHERE reviewer_id IN ($TEST_USER_IDS) OR target_user_id IN ($TEST_USER_IDS);
      DELETE FROM task_image WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN ($TEST_USER_IDS));
      DELETE FROM task_order WHERE task_id IN (SELECT id FROM task WHERE publisher_id IN ($TEST_USER_IDS)) OR runner_id IN ($TEST_USER_IDS);
      DELETE FROM transaction_record WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM credit_log WHERE runner_id IN ($TEST_USER_IDS);
      DELETE FROM notification WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM payment_idempotent WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 DAY);
      DELETE FROM task WHERE publisher_id IN ($TEST_USER_IDS);
      DELETE FROM runner_profile WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM user_address WHERE user_id IN ($TEST_USER_IDS);
      DELETE FROM user WHERE username LIKE '${TEST_USER_PREFIX}%';
    " 2>/dev/null
    pass "MySQL 测试用户及相关数据已删除"
  else
    pass "MySQL 无测试用户需清理"
  fi
fi

# ── 3. 清理 Redis 测试 key ──
echo ">>> 清理 Redis 测试 key..."
for pattern in \
  "user:code:*" \
  "user:sms:rate:*" \
  "user:login:rate:*" \
  "user:login:fail:*" \
  "user:refresh:rate:*" \
  "user:reset:pwd:fail:*" \
  "user:reset:paypwd:fail:*" \
  "user:refresh:token:*" \
  "user:upload:daily:*" \
  "admin:login:rate:*" \
  "admin:refresh:rate:*" \
  "admin:login:fail:*"; do
  $REDIS_CMD KEYS "$pattern" 2>/dev/null | while read -r key; do
    [ -n "$key" ] && $REDIS_CMD DEL "$key" >/dev/null 2>&1
  done
done
pass "Redis 速率限制/验证码/失败计数 key 已清理"

# ── 4. 显示剩余测试相关数据 ──
echo ""
echo ">>> 残留检查:"
REMAINING_USERS=$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM user WHERE username LIKE '${TEST_USER_PREFIX}%' OR phone LIKE '${TEST_PHONE_PREFIX}%'" 2>/dev/null || echo "?")
REMAINING_TASKS=$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM task WHERE publisher_id IN (SELECT id FROM user WHERE username LIKE '${TEST_USER_PREFIX}%')" 2>/dev/null || echo "?")
REMAINING_REDIS=$($REDIS_CMD KEYS "*e2e*" 2>/dev/null | wc -l)
echo "  残留测试用户: $REMAINING_USERS"
echo "  残留测试任务: $REMAINING_TASKS"
echo "  残留 Redis key: $REMAINING_REDIS"

echo ""
echo "════════════════════════════════════════"
echo -e "  ${GREEN}Teardown 完成${NC}"
echo "════════════════════════════════════════"
