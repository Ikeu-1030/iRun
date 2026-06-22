#!/bin/bash
# E2E 全链路测试入口
# 用法:
#   bash e2e/run.sh              # 完整流程: setup → bootstrap → 执行 → teardown
#   bash e2e/run.sh --phase 1    # 仅执行 Phase 1
#   bash e2e/run.sh --quick      # 快速核心路径 (Phase 1-2-5)
#   bash e2e/run.sh --keep       # 保留测试数据不清理
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/.env.e2e"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

pass()  { echo -e "${GREEN}✓${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; }
warn()  { echo -e "${YELLOW}⚠${NC} $1"; }
info()  { echo -e "${CYAN}→${NC} $1"; }
phase_header() { echo ""; echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; echo -e "  ${CYAN}Phase $1${NC}"; echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

# 解析参数
MODE="full"
KEEP_DATA=false
PHASE_NUM=""

for arg in "$@"; do
  case "$arg" in
    --quick)        MODE="quick" ;;
    --phase)        MODE="phase"; PHASE_NUM="${2:-}"; shift ;;
    --keep)         KEEP_DATA=true ;;
    --skip-build)   SETUP_ARGS="${SETUP_ARGS:-} --skip-build" ;;
    --skip-docker)  SETUP_ARGS="${SETUP_ARGS:-} --skip-docker" ;;
  esac
done

# ── 辅助函数 ──
# 加载 admin token
if [ -f "$SCRIPT_DIR/.admin_token" ]; then
  source "$SCRIPT_DIR/.admin_token"
fi

# 轻量级 API 调用 + 断言
api() {
  # api <METHOD> <PATH> <EXPECTED_CODE> [BODY] [AUTH_HEADER]
  local method="$1" path="$2" expected="$3" body="${4:-}" auth="${5:-}"
  local url="$API$path"

  if [ -n "$auth" ]; then
    if [ "$auth" = "user" ]; then
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "authentication: ${TOKEN_CURRENT:-}" \
        -d "$body" 2>/dev/null
    elif [ "$auth" = "admin" ]; then
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "token: ${ADMIN_TOKEN:-}" \
        -d "$body" 2>/dev/null
    elif [ "$auth" = "refresh" ]; then
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "X-Refresh-Token: ${REFRESH_CURRENT:-}" \
        -d "$body" 2>/dev/null
    else
      curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -d "$body" 2>/dev/null
    fi
  else
    curl -s -w "\n%{http_code}" -X "$method" "$url" \
      -H "Content-Type: application/json" \
      -d "$body" 2>/dev/null
  fi
}

check() {
  # check <description> <http_code> <expected_http> <business_code> <expected_biz>
  local desc="$1" http="$2" exp_http="$3" biz="$4" exp_biz="$5"
  if [ "$http" = "$exp_http" ] && [ "$biz" = "$exp_biz" ]; then
    pass "$desc"
    return 0
  else
    fail "$desc (HTTP $http≠$exp_http | code $biz≠$exp_biz)"
    return 1
  fi
}

# ── 入口 ──
echo "══════════════════════════════════════════════"
echo "  E2E 全链路业务流程验证"
echo "  模式: $MODE | 保留数据: $KEEP_DATA"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "══════════════════════════════════════════════"

# Phase 0: Setup
info "执行 setup..."
bash "$SCRIPT_DIR/setup.sh" ${SETUP_ARGS:-}
info "执行 bootstrap..."
bash "$SCRIPT_DIR/bootstrap.sh" ${KEEP_DATA:+--keep-data}
# 重新加载 admin token
[ -f "$SCRIPT_DIR/.admin_token" ] && source "$SCRIPT_DIR/.admin_token"

PASS_COUNT=0
FAIL_COUNT=0

# ═══════════════════════════════════════════
# Phase 1: 认证与注册
# ═══════════════════════════════════════════
if [ "$MODE" = "full" ] || [ "$MODE" = "quick" ] || [ "$PHASE_NUM" = "1" ]; then
phase_header "1/7 — 认证与注册"

# 1.1 SMS 发送
info "1.1 SMS 发送验证码..."
RESP=$(api POST "/user/send" 200 "{\"phone\":\"$PHONE_A\",\"operation\":\"register\"}" "")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "SMS 发送正常" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 1.2 注册
info "1.2 用户注册..."
$REDIS_CMD SET "user:code:register:$PHONE_A" "$TEST_CODE" EX 300 >/dev/null 2>&1
RESP=$(api POST "/user/register" 200 "{\"phone\":\"$PHONE_A\",\"code\":\"$TEST_CODE\",\"username\":\"e2e_user_a\",\"password\":\"$TEST_PASSWORD\",\"sex\":\"男\"}" "")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "用户注册" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
TOKEN_A=$(echo "$BODY" | jq -r '.data.token // empty')
REFRESH_A=$(echo "$BODY" | jq -r '.data.refreshToken // empty')
USER_ID_A=$(echo "$BODY" | jq -r '.data.userId // empty')
TOKEN_CURRENT="$TOKEN_A"; REFRESH_CURRENT="$REFRESH_A"

# 1.3 密码登录
info "1.3 密码登录..."
RESP=$(api POST "/user/login" 200 "{\"loginType\":1,\"username\":\"e2e_user_a\",\"password\":\"$TEST_PASSWORD\"}" "")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "密码登录" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
TOKEN_A=$(echo "$BODY" | jq -r '.data.token // empty')
REFRESH_A=$(echo "$BODY" | jq -r '.data.refreshToken // empty')
TOKEN_CURRENT="$TOKEN_A"; REFRESH_CURRENT="$REFRESH_A"

# 1.4 验证码登录
info "1.4 验证码登录..."
$REDIS_CMD SET "user:code:login:$PHONE_A" "$TEST_CODE" EX 300 >/dev/null 2>&1
RESP=$(api POST "/user/login" 200 "{\"loginType\":2,\"phone\":\"$PHONE_A\",\"code\":\"$TEST_CODE\"}" "")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "验证码登录" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 1.5 Token 刷新
info "1.5 Token 刷新..."
RESP=$(api POST "/user/refresh" 200 "" "refresh")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "Token 刷新" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 1.6 Logout
info "1.6 退出登录..."
TOKEN_CURRENT="$TOKEN_A"
RESP=$(api POST "/user/logout" 200 "" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "退出登录" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 重新登录以继续后续 Phase
RESP=$(api POST "/user/login" 200 "{\"loginType\":1,\"username\":\"e2e_user_a\",\"password\":\"$TEST_PASSWORD\"}" "")
TOKEN_A=$(echo "$RESP" | head -n -1 | jq -r '.data.token // empty')
TOKEN_CURRENT="$TOKEN_A"
fi

# ═══════════════════════════════════════════
# Phase 2: 用户资料与认证
# ═══════════════════════════════════════════
if [ "$MODE" = "full" ] || [ "$MODE" = "quick" ] || [ "$PHASE_NUM" = "2" ]; then
phase_header "2/7 — 用户资料与认证"

# 2.1 设置支付密码
info "2.1 设置支付密码..."
TOKEN_CURRENT="$TOKEN_A"
RESP=$(api PUT "/user/pay-password" 200 "{\"payPassword\":\"$TEST_PAY_PASSWORD\"}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "设置支付密码" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 2.2 修改个人资料
info "2.2 修改个人资料..."
RESP=$(api PUT "/user/profile" 200 "{\"nickname\":\"测试用户A\",\"campus\":\"成都校区\",\"sex\":\"男\"}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "修改个人资料" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 2.3 地址簿 CRUD
info "2.3 地址簿..."
RESP=$(api POST "/user/address/save" 200 "{\"contactName\":\"张三\",\"contactPhone\":\"$PHONE_A\",\"sex\":\"男\",\"detail\":\"成都校区宿舍楼3栋501室\",\"isDefault\":1}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "新增地址" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 获取地址 ID
ADDR_RESP=$(api GET "/user/address/list" 200 "" "user")
ADDR_ID=$(echo "$ADDR_RESP" | head -n -1 | jq -r '.data.records[0].id // empty')

# 2.4 实名认证
info "2.4 提交实名认证..."
RESP=$(api POST "/user/certify" 200 "{\"realName\":\"张三\",\"studentId\":\"20240001\",\"certifyImg\":\"https://example.com/cert_sample.jpg\"}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "提交实名认证" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# 管理端审核通过
info "2.4b 管理端审核认证..."
RESP=$(api PUT "/admin/users/$USER_ID_A/certify" 200 "{\"isCertify\":2,\"remark\":\"E2E自动通过\"}" "admin")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "管理端审核认证" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
fi

# ═══════════════════════════════════════════
# Phase 5 (Quick 模式核心路径): 创建跑腿员 + 发布任务 + 订单流转
# ═══════════════════════════════════════════
if [ "$MODE" = "full" ] || [ "$MODE" = "quick" ]; then
phase_header "(Quick) 订单核心路径"

# --- 创建用户B（跑腿员） ---
info "创建跑腿员账号..."
$REDIS_CMD SET "user:code:register:$PHONE_B" "$TEST_CODE" EX 300 >/dev/null 2>&1
RESP=$(api POST "/user/register" 200 "{\"phone\":\"$PHONE_B\",\"code\":\"$TEST_CODE\",\"username\":\"e2e_user_b\",\"password\":\"$TEST_PASSWORD\",\"sex\":\"女\"}" "")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "跑腿员注册" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
TOKEN_B=$(echo "$BODY" | jq -r '.data.token // empty')
USER_ID_B=$(echo "$BODY" | jq -r '.data.userId // empty')

# 设置支付密码
TOKEN_CURRENT="$TOKEN_B"
api PUT "/user/pay-password" 200 "{\"payPassword\":\"$TEST_PAY_PASSWORD\"}" "user" >/dev/null 2>&1

# 实名认证
api POST "/user/certify" 200 "{\"realName\":\"李四\",\"studentId\":\"20240002\",\"certifyImg\":\"https://example.com/cert_b.jpg\"}" "user" >/dev/null 2>&1
api PUT "/admin/users/$USER_ID_B/certify" 200 "{\"isCertify\":2}" "admin" >/dev/null 2>&1

# 申请跑腿员 + 审核 + 上线
api POST "/runner/apply" 200 "" "user" >/dev/null 2>&1
RUNNER_B_ID=$($MYSQL_CMD -N -e "SELECT id FROM runner_profile WHERE user_id=$USER_ID_B" 2>/dev/null)
api PUT "/admin/runners/$RUNNER_B_ID/review?verifyStatus=2" 200 "" "admin" >/dev/null 2>&1
api POST "/user/transactions/recharge" 200 "{\"amount\":50.00,\"payPassword\":\"$TEST_PAY_PASSWORD\"}" "user" >/dev/null 2>&1
api POST "/runner/online" 200 "" "user" >/dev/null 2>&1
pass "跑腿员就绪"

# --- 发布任务 ---
info "发布任务..."
TOKEN_CURRENT="$TOKEN_A"
RESP=$(api POST "/task/publish" 200 "{\"type\":\"daiqukuaidi\",\"subType\":\"小件快递\",\"publicDesc\":\"E2E测试代取快递\",\"taskSpecs\":\"{\\\"包裹列表\\\":[{\\\"规格\\\":\\\"小件\\\",\\\"数量\\\":1}]}\",\"tip\":3,\"deliveryFee\":5,\"productCost\":0,\"payPassword\":\"$TEST_PAY_PASSWORD\",\"pickupAddress\":\"菜鸟驿站(成都校区)\",\"pickupCode\":\"SF12345678\",\"deliveryAddressId\":\"$ADDR_ID\",\"expireMinutes\":60,\"contactName\":\"张三\",\"contactPhone\":\"$PHONE_A\"}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "发布任务" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
TASK_ID=$(echo "$BODY" | jq -r '.data.taskId // empty')

# --- 接单 ---
info "跑腿员接单..."
TOKEN_CURRENT="$TOKEN_B"
RESP=$(api POST "/order/accept/$TASK_ID" 200 "" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "接单" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
ORDER_ID=$(echo "$BODY" | jq -r '.data.orderId // empty')

# --- 取货 ---
info "确认取货..."
RESP=$(api POST "/order/$ORDER_ID/pickup" 200 "{\"imageUrls\":[\"https://example.com/pickup.jpg\"]}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "确认取货" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# --- 送达 ---
info "确认送达..."
RESP=$(api POST "/order/$ORDER_ID/deliver" 200 "{\"imageUrls\":[\"https://example.com/deliver.jpg\"]}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "确认送达" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# --- 确认完成 ---
info "确认完成..."
TOKEN_CURRENT="$TOKEN_A"
RESP=$(api POST "/order/$ORDER_ID/confirm" 200 "" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "确认完成" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# --- 评价 ---
info "互相评价..."
RESP=$(api POST "/review" 200 "{\"taskId\":$TASK_ID,\"targetUserId\":$USER_ID_B,\"rating\":5,\"content\":\"E2E测试评价\"}" "user")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "评价" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

# --- 验证 DB 状态 ---
info "数据库完整性验证..."
ORDER_STATUS=$($MYSQL_CMD -N -e "SELECT status FROM task_order WHERE id=$ORDER_ID" 2>/dev/null)
TASK_STATUS=$($MYSQL_CMD -N -e "SELECT status FROM task WHERE id=$TASK_ID" 2>/dev/null)
BALANCE_B=$($MYSQL_CMD -N -e "SELECT balance FROM user WHERE id=$USER_ID_B" 2>/dev/null)
[ "$ORDER_STATUS" = "4" ] && pass "Order status=4 (COMPLETED)" || fail "Order status=$ORDER_STATUS 期望 4"
[ "$TASK_STATUS" = "5" ] && pass "Task status=5 (COMPLETED)" || fail "Task status=$TASK_STATUS 期望 5"
echo "  跑腿员余额: ¥$BALANCE_B"
((PASS_COUNT+=2))
fi

# ═══════════════════════════════════════════
# Phase 6: 管理端验证
# ═══════════════════════════════════════════
if [ "$MODE" = "full" ] || [ "$PHASE_NUM" = "6" ]; then
phase_header "6/7 — 管理端验证"

info "6.1 仪表盘..."
RESP=$(api GET "/admin/dashboard" 200 "" "admin")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "仪表盘数据" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

info "6.2 用户列表..."
RESP=$(api GET "/admin/users?page=1&size=10" 200 "" "admin")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "用户列表" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))

info "6.4 广播通知..."
RESP=$(api POST "/admin/notifications/broadcast" 200 "{\"type\":1,\"title\":\"E2E测试\",\"content\":\"全链路测试广播\"}" "admin")
HTTP=$(echo "$RESP" | tail -1); BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$BODY" | jq -r '.code')
check "广播通知" "$HTTP" "200" "$CODE" "1" && ((PASS_COUNT++)) || ((FAIL_COUNT++))
fi

# ═══════════════════════════════════════════
# 报告
# ═══════════════════════════════════════════
echo ""
echo "══════════════════════════════════════════════"
echo "  E2E 验证完成"
echo "══════════════════════════════════════════════"
echo -e "  通过: ${GREEN}$PASS_COUNT${NC}"
echo -e "  失败: ${RED}$FAIL_COUNT${NC}"
echo ""

# Teardown
if [ "$KEEP_DATA" = false ]; then
  info "自动清理测试数据..."
  bash "$SCRIPT_DIR/teardown.sh"
else
  info "保留测试数据（--keep 模式）"
fi

if [ "$FAIL_COUNT" -gt 0 ]; then
  echo -e "${RED}存在 $FAIL_COUNT 项失败，请检查日志${NC}"
  exit 1
else
  echo -e "${GREEN}全部通过 ✅${NC}"
  exit 0
fi
