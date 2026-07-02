#!/bin/bash
# E2E Quick Verification — local environment
# Usage: bash e2e/quick-verify.sh
set -uo pipefail

API="http://localhost:8080/api"
REDIS_CMD="redis-cli -n 1"
TMPDIR="e2e/.tmp"
mkdir -p "$TMPDIR"
PASS=0
FAIL=0

green() { echo -e "\033[0;32m$1\033[0m"; }
red()   { echo -e "\033[0;31m$1\033[0m"; }

check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    green "  ✅ $desc"; PASS=$((PASS + 1))
  else
    red "  ❌ $desc (expected=$expected actual=$actual)"; FAIL=$((FAIL + 1))
  fi
}

api_get() { curl -s "$API$1" -H "$2" -o "$TMPDIR/$3"; }
api_post() { curl -s -X POST "$API$1" -H "Content-Type: application/json" -H "$2" -d "$3" -o "$TMPDIR/$4"; }
api_put()  { curl -s -X PUT "$API$1" -H "Content-Type: application/json" -H "$2" -d "$3" -o "$TMPDIR/$4"; }
json() {
  python -c "import json; d=json.load(open('$TMPDIR/$1', encoding='utf-8')); print($2)"
}

echo "══════════════════════════════════════════════"
echo "  E2E Quick Verify — $(date '+%Y-%m-%d %H:%M:%S')"
echo "══════════════════════════════════════════════"
echo ""

# ── 0. Pre-flight ──
echo ">>> Phase 0: Pre-flight"
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$API/actuator/health")
check "Backend health" "200" "$HEALTH"
REDIS_PING=$($REDIS_CMD PING)
check "Redis ping" "PONG" "$REDIS_PING"

# ── 1. Auth ──
echo ""
echo ">>> Phase 1: Authentication"

api_post "/admin/login" "" '{"username":"admin","password":"admin"}' "admin_login.json"
check "Admin login" "1" "$(json admin_login.json "d['code']")"
ADMIN_TOKEN=$(json admin_login.json "d['data']['token']")
ADMIN_AUTH="token: $ADMIN_TOKEN"

$REDIS_CMD SET "user:code:login:18848298061" "888888" EX 300 >/dev/null 2>&1
api_post "/user/login" "" '{"loginType":2,"phone":"18848298061","code":"888888"}' "user_login.json"
check "User login" "1" "$(json user_login.json "d['code']")"
USER_TOKEN=$(json user_login.json "d['data']['token']")
USER_AUTH="authentication: $USER_TOKEN"

REFRESH=$(json user_login.json "d['data']['refreshToken']")
api_post "/user/refresh" "X-Refresh-Token: $REFRESH" "" "user_refresh.json"
check "Token refresh" "1" "$(json user_refresh.json "d['code']")"

# Registration test (via UTF-8 file body to avoid Git Bash encoding issue)
echo "--- Registration test ---"
REG_PHONE="13900000099"
$REDIS_CMD SET "user:code:register:$REG_PHONE" "888888" EX 300 >/dev/null 2>&1
python -c "
import json
json.dump({'phone':'$REG_PHONE','code':'888888','username':'e2eregtest','password':'Test123456','sex':'M'},
          open('$TMPDIR/reg_body.json','w',encoding='utf-8'))
"
curl -s -X POST "$API/user/register" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary "@$TMPDIR/reg_body.json" -o "$TMPDIR/reg_resp.json"
check "User registration" "1" "$(json reg_resp.json "d['code']")"
# Clean up: disable test user
REG_USER_ID=$(json reg_resp.json "d['data']['userId']")
curl -s -X PUT "$API/admin/users/$REG_USER_ID/status?enabled=0" -H "$ADMIN_AUTH" >/dev/null 2>&1

# ── 2. User profile ──
echo ""
echo ">>> Phase 2: User Profile"
api_get "/user/info" "$USER_AUTH" "user_info.json"
check "User info" "1" "$(json user_info.json "d['code']")"
check "isCertify=2" "2" "$(json user_info.json "d['data']['isCertify']")"
check "verifyStatus=2" "2" "$(json user_info.json "d['data']['verifyStatus']")"
echo "  Balance: ¥$(json user_info.json "d['data']['balance']")"

api_get "/user/pay-password/status" "$USER_AUTH" "pwpwd.json"
check "Has pay password" "True" "$(json pwpwd.json "d['data']")"

# ── 3. Admin dashboard ──
echo ""
echo ">>> Phase 3: Admin Dashboard"
api_get "/admin/dashboard" "$ADMIN_AUTH" "dashboard.json"
check "Dashboard" "1" "$(json dashboard.json "d['code']")"
echo "  userCount=$(json dashboard.json "d['data']['userCount']")"

# ── 4. Admin user management ──
echo ""
echo ">>> Phase 4: Admin User Management"
api_get "/admin/users?page=1&size=5" "$ADMIN_AUTH" "users.json"
check "User list" "1" "$(json users.json "d['code']")"
echo "  total=$(json users.json "d['data']['total']")"

api_get "/admin/users/1" "$ADMIN_AUTH" "user_detail.json"
check "User detail" "1" "$(json user_detail.json "d['code']")"

# ── 5. Admin task/order ──
echo ""
echo ">>> Phase 5: Admin Task/Order"
api_get "/admin/tasks?page=1&size=2" "$ADMIN_AUTH" "tasks.json"
check "Task list" "1" "$(json tasks.json "d['code']")"
echo "  total=$(json tasks.json "d['data']['total']")"

api_get "/admin/orders?page=1&size=5" "$ADMIN_AUTH" "orders.json"
check "Order list" "1" "$(json orders.json "d['code']")"
echo "  total=$(json orders.json "d['data']['total']")"

# ── 6. Admin runners ──
echo ""
echo ">>> Phase 6: Admin Runners"
api_get "/admin/runners?page=1&size=5" "$ADMIN_AUTH" "runners.json"
check "Runner list" "1" "$(json runners.json "d['code']")"
echo "  total=$(json runners.json "d['data']['total']")"

# ── 7. Admin transactions & settings ──
echo ""
echo ">>> Phase 7: Admin Transactions & Settings"
api_get "/admin/transactions?page=1&size=5" "$ADMIN_AUTH" "txns.json"
check "Transactions" "1" "$(json txns.json "d['code']")"

api_get "/admin/settings" "$ADMIN_AUTH" "settings.json"
check "Settings" "1" "$(json settings.json "d['code']")"
check "Settings count=11" "11" "$(json settings.json "len(d['data'])")"

# ── 8. Security ──
echo ""
echo ">>> Phase 8: Security"
NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$API/user/info")
check "No-auth guard" "401" "$NOAUTH"

CROSS1=$(curl -s -o /dev/null -w "%{http_code}" "$API/admin/dashboard" -H "token: $USER_TOKEN")
check "user→admin rejected" "401" "$CROSS1"

CROSS2=$(curl -s -o /dev/null -w "%{http_code}" "$API/user/info" -H "authentication: $ADMIN_TOKEN")
check "admin→user rejected" "401" "$CROSS2"

# ── 9. Public endpoints ──
echo ""
echo ">>> Phase 9: Public Endpoints"
api_get "/common/announcement" "" "ann.json"
check "Announcement" "1" "$(json ann.json "d['code']")"

api_get "/common/banners" "" "banners.json"
check "Banners" "1" "$(json banners.json "d['code']")"
echo "  interval=$(json banners.json "d['data']['interval']") images=$(json banners.json "len(d['data']['images'])")"

# ── 10. Notification ──
echo ""
echo ">>> Phase 10: Notification"
api_post "/admin/notifications/broadcast" "$ADMIN_AUTH" '{"type":1,"title":"E2E","content":"test"}' "notif.json"
check "Broadcast" "1" "$(json notif.json "d['code']")"

# ── 11. Config update ──
echo ""
echo ">>> Phase 11: Config Update"
api_put "/admin/settings" "$ADMIN_AUTH" '{"items":[{"configKey":"platform.announcement","configValue":"E2E_VERIFIED"}]}' "cfg_upd.json"
check "Config update" "1" "$(json cfg_upd.json "d['code']")"

api_get "/common/announcement" "" "ann2.json"
check "Announcement updated" "E2E_VERIFIED" "$(json ann2.json "d['data']")"

# ── 12. File upload security ──
echo ""
echo ">>> Phase 12: File Upload Auth"
UPLOAD_NOAUTH=$(curl -s -X POST "$API/common/upload" -H "Content-Type: multipart/form-data")
echo "$UPLOAD_NOAUTH" > "$TMPDIR/upload.json"
check "Upload no-auth rejected" "0" "$(json upload.json "d['code']")"

# ── Report ──
echo ""
echo "══════════════════════════════════════════════"
echo "  E2E Quick Verify — Report"
echo "══════════════════════════════════════════════"
green "  PASS: $PASS"
[ "$FAIL" -gt 0 ] && red "  FAIL: $FAIL" || echo ""
echo ""

# Cleanup temp files
rm -rf "$TMPDIR"

if [ "$FAIL" -gt 0 ]; then
  red "⚠️  $FAIL assertion(s) failed"
  exit 1
else
  green "✅ All $PASS assertions passed — 1.0 Ready"
  exit 0
fi
