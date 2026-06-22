#!/bin/bash
# E2E Full Business Flow — Phase 2-5
# Usage: bash e2e/full-flow.sh
set -uo pipefail

API="http://localhost:8080/api"
REDIS_CMD="redis-cli -n 1"
TMPDIR="e2e/.tmp"
mkdir -p "$TMPDIR"
PASS=0; FAIL=0

green() { echo -e "\033[0;32m$1\033[0m"; }
red()   { echo -e "\033[0;31m$1\033[0m"; }
cyan()  { echo -e "\033[0;36m>>> $1\033[0m"; }
check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    green "  ✅ $desc"; PASS=$((PASS + 1))
  else
    red "  ❌ $desc (expected=$expected actual=$actual)"; FAIL=$((FAIL + 1))
  fi
}

# Helper: write JSON to file then POST (avoids Git Bash encoding issues)
post_file() {
  local path="$1" auth="$2" json_file="$3" out_file="$4"
  curl -s -X POST "$API$path" -H "Content-Type: application/json; charset=utf-8" -H "$auth" \
    --data-binary "@$TMPDIR/$json_file" -o "$TMPDIR/$out_file"
}
put_file() {
  local path="$1" auth="$2" json_file="$3" out_file="$4"
  curl -s -X PUT "$API$path" -H "Content-Type: application/json; charset=utf-8" -H "$auth" \
    --data-binary "@$TMPDIR/$json_file" -o "$TMPDIR/$out_file"
}
get_api() {
  curl -s "$API$1" -H "$2" -o "$TMPDIR/$3"
}
json() {
  python -c "import json; d=json.load(open('$TMPDIR/$1', encoding='utf-8')); print($2)"
}

echo "══════════════════════════════════════════════"
echo "  E2E Full Business Flow"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "══════════════════════════════════════════════"

# ═════════════════════════════════════════
# BOOTSTRAP: Create 2 users + setup
# ═════════════════════════════════════════
cyan "Bootstrap: Login admin + Setup test users"

# Admin login
post_file "/admin/login" "" "admin_login.json" "admin_login.json" 2>/dev/null || true
python -c "import json; json.dump({'username':'admin','password':'admin'}, open('$TMPDIR/admin_login.json','w',encoding='utf-8'))"
post_file "/admin/login" "" "admin_login.json" "admin_login.json"
ADMIN_TOKEN=$(json admin_login.json "d['data']['token']")
ADMIN_AUTH="token: $ADMIN_TOKEN"
green "Admin logged in"

# Use existing user A (ID=1) — already certified runner
PHONE_A="18848298061"
$REDIS_CMD SET "user:code:login:$PHONE_A" "888888" EX 300 >/dev/null 2>&1
python -c "import json; json.dump({'loginType':2,'phone':'$PHONE_A','code':'888888'}, open('$TMPDIR/login_a.json','w',encoding='utf-8'))"
post_file "/user/login" "" "login_a.json" "login_a.json"
TOKEN_A=$(json login_a.json "d['data']['token']")
USER_ID_A=$(json login_a.json "d['data']['userId']")
AUTH_A="authentication: $TOKEN_A"
green "User A (publisher) logged in — ID=$USER_ID_A"

# Register user B (new runner)
PHONE_B="13900001111"
$REDIS_CMD SET "user:code:register:$PHONE_B" "888888" EX 300 >/dev/null 2>&1
python -c "import json; json.dump({'phone':'$PHONE_B','code':'888888','username':'e2erunner01','password':'Test123456','sex':'F'}, open('$TMPDIR/reg_b.json','w',encoding='utf-8'))"
post_file "/user/register" "" "reg_b.json" "reg_b.json"
check "Register User B" "1" "$(json reg_b.json "d['code']")"
TOKEN_B=$(json reg_b.json "d['data']['token']")
USER_ID_B=$(json reg_b.json "d['data']['userId']")
AUTH_B="authentication: $TOKEN_B"
green "User B (runner) registered — ID=$USER_ID_B"

# ═════════════════════════════════════════
# PHASE 2: User B — Pay password + Profile + Address + Certify
# ═════════════════════════════════════════
echo ""
cyan "Phase 2: User B setup — PayPwd + Address + Certify"

python -c "import json; json.dump({'payPassword':'123456'}, open('$TMPDIR/setpw.json','w',encoding='utf-8'))"
put_file "/user/pay-password" "$AUTH_B" "setpw.json" "setpw.json"
check "Set pay password" "1" "$(json setpw.json "d['code']")"

python -c "import json; json.dump({'nickname':'E2E跑腿员','campus':'成都校区','sex':'F'}, open('$TMPDIR/prof.json','w',encoding='utf-8'))"
put_file "/user/profile" "$AUTH_B" "prof.json" "prof.json"
check "Update profile" "1" "$(json prof.json "d['code']")"

python -c "import json; json.dump({'contactName':'李四','contactPhone':'$PHONE_B','sex':'F','detail':'成都校区宿舍楼1栋101','isDefault':1}, open('$TMPDIR/addr.json','w',encoding='utf-8'))"
post_file "/user/address/save" "$AUTH_B" "addr.json" "addr.json"
check "Add address" "1" "$(json addr.json "d['code']")"

get_api "/user/address/list" "$AUTH_B" "addrlist.json"
ADDR_B_ID=$(python -c "
import json
d = json.load(open('$TMPDIR/addrlist.json', encoding='utf-8'))
records = d.get('data', {}).get('records', d.get('data', []))
if isinstance(records, list) and records:
    print(records[0]['id'])
else:
    print(0)
")
green "Address ID=$ADDR_B_ID"

python -c "import json; json.dump({'realName':'李四','studentId':'20240002','certifyImg':'https://example.com/cert_b.jpg'}, open('$TMPDIR/cert.json','w',encoding='utf-8'))"
post_file "/user/certify" "$AUTH_B" "cert.json" "cert.json"
check "Submit certify" "1" "$(json cert.json "d['code']")"

# Admin approve certification
python -c "import json; json.dump({'isCertify':2,'remark':'E2E OK'}, open('$TMPDIR/certok.json','w',encoding='utf-8'))"
put_file "/admin/users/$USER_ID_B/certify" "$ADMIN_AUTH" "certok.json" "certok.json"
check "Admin approve certify" "1" "$(json certok.json "d['code']")"

# ═════════════════════════════════════════
# PHASE 3: Runner application + approve + online
# ═════════════════════════════════════════
echo ""
cyan "Phase 3: Runner certification"

post_file "/runner/apply" "$AUTH_B" "addr.json" "apply.json"  # any valid JSON
check "Apply runner" "1" "$(json apply.json "d['code'])"

# Get runner profile ID via admin API
get_api "/admin/runners?page=1&size=10" "$ADMIN_AUTH" "rlist.json"
RUNNER_B_ID=$(python -c "
import json
records = json.load(open('$TMPDIR/rlist.json', encoding='utf-8'))['data']['records']
for r in records:
    if r.get('userId') == $USER_ID_B:
        print(r['id'])
        break
")

put_file "/admin/runners/$RUNNER_B_ID/review?verifyStatus=2" "$ADMIN_AUTH" "certok.json" "certok.json"
check "Admin approve runner" "1" "$(json certok.json "d['code'])"

# Recharge
python -c "import json; json.dump({'amount':100.00,'payPassword':'123456'}, open('$TMPDIR/charge.json','w',encoding='utf-8'))"
post_file "/user/transactions/recharge" "$AUTH_B" "charge.json" "charge.json"
check "Recharge ¥100" "1" "$(json charge.json "d['code'])"

# Online
post_file "/runner/online" "$AUTH_B" "addr.json" "online.json"
check "Go online" "1" "$(json online.json "d['code'])"

green "Runner B ready — certified, online, balance=100"

# ═════════════════════════════════════════
# PHASE 4: Publish tasks (User A)
# ═════════════════════════════════════════
echo ""
cyan "Phase 4: Publish tasks"

# Also ensure User A has an address and pay password (already set from earlier)
get_api "/user/address/list" "$AUTH_A" "alist.json"
ADDR_A_ID=$(python -c "import json; d=json.load(open('$TMPDIR/alist.json',encoding='utf-8')); records=d.get('data',{}).get('records',d.get('data',[])); print(records[0]['id'] if isinstance(records,list) and records else 1)")

# 4.1 Express task (type=daiqukuaidi)
python -c "
import json
json.dump({
    'type':'daiqukuaidi','subType':'小件快递',
    'publicDesc':'E2E-代取快递测试','privateNote':'取件码:SF12345678',
    'taskSpecs':json.dumps({'包裹列表':[{'规格':'小件','数量':1}]}),
    'tip':3,'deliveryFee':5,'productCost':0,'payPassword':'123456',
    'pickupAddress':'菜鸟驿站(成都校区)','pickupCode':'SF12345678',
    'deliveryAddressId':$ADDR_A_ID,'expireMinutes':60,
    'contactName':'cyf','contactPhone':'$PHONE_A'
}, open('$TMPDIR/task1.json','w',encoding='utf-8'), ensure_ascii=False)
"
post_file "/task/publish" "$AUTH_A" "task1.json" "task1.json"
check "Publish express task" "1" "$(json task1.json "d['code'])"
TASK_ID_1=$(json task1.json "d['data']['taskId']")
TASK_NO_1=$(json task1.json "d['data']['taskNo']")
green "  Task #1: id=$TASK_ID_1 no=$TASK_NO_1"

# ═════════════════════════════════════════
# PHASE 5: Order lifecycle
# ═════════════════════════════════════════
echo ""
cyan "Phase 5: Order Lifecycle"

# 5.1 Task hall
get_api "/task/list?page=1&size=5" "$AUTH_B" "hall.json"
check "Task hall OK" "1" "$(json hall.json "d['code'])"
HALL_TOTAL=$(json hall.json "d['data']['total']")
green "  Task hall: $HALL_TOTAL tasks"

# 5.2 Accept
post_file "/order/accept/$TASK_ID_1" "$AUTH_B" "addr.json" "accept.json"
check "Accept order" "1" "$(json accept.json "d['code'])"
ORDER_ID=$(json accept.json "d['data']['orderId']")
green "  Order: id=$ORDER_ID"

# Verify: task status → ACCEPTED(2)
get_api "/task/$TASK_ID_1" "$AUTH_A" "task_chk.json"
check "Task status → ACCEPTED" "2" "$(python -c "import json; print(json.load(open('$TMPDIR/task_chk.json',encoding='utf-8'))['data']['status'])")"

# 5.3 Pickup
python -c "import json; json.dump({'imageUrls':['https://example.com/pickup.jpg']}, open('$TMPDIR/proof.json','w',encoding='utf-8'))"
post_file "/order/$ORDER_ID/pickup" "$AUTH_B" "proof.json" "pickup.json"
check "Confirm pickup" "1" "$(json pickup.json "d['code'])"

# 5.4 Deliver
python -c "import json; json.dump({'imageUrls':['https://example.com/deliver.jpg']}, open('$TMPDIR/proof2.json','w',encoding='utf-8'))"
post_file "/order/$ORDER_ID/deliver" "$AUTH_B" "proof2.json" "deliver.json"
check "Confirm deliver" "1" "$(json deliver.json "d['code'])"

# 5.5 Complete (publisher confirms)
post_file "/order/$ORDER_ID/confirm" "$AUTH_A" "addr.json" "complete.json"
check "Confirm complete" "1" "$(json complete.json "d['code'])"

# Verify: order → COMPLETED(4), task → COMPLETED(5)
get_api "/admin/orders/$ORDER_ID" "$ADMIN_AUTH" "order_final.json"
check "Order status → COMPLETED" "4" "$(json order_final.json "d['data']['status'])"
get_api "/admin/tasks/$TASK_ID_1" "$ADMIN_AUTH" "task_final.json"
check "Task status → COMPLETED" "5" "$(json task_final.json "d['data']['status'])"

# 5.6 Reviews
python -c "import json; json.dump({'taskId':$TASK_ID_1,'targetUserId':$USER_ID_B,'rating':5,'content':'E2E-速度快态度好','tags':json.dumps(['速度快','态度好'])}, open('$TMPDIR/review1.json','w',encoding='utf-8'))"
post_file "/review" "$AUTH_A" "review1.json" "review1.json"
check "Publisher review runner" "1" "$(json review1.json "d['code'])"

python -c "import json; json.dump({'taskId':$TASK_ID_1,'targetUserId':$USER_ID_A,'rating':4,'content':'E2E-任务描述清晰'}, open('$TMPDIR/review2.json','w',encoding='utf-8'))"
post_file "/review" "$AUTH_B" "review2.json" "review2.json"
check "Runner review publisher" "1" "$(json review2.json "d['code'])"

# ═════════════════════════════════════════
# PHASE 7 (partial): Security edge cases
# ═════════════════════════════════════════
echo ""
cyan "Phase 7: Security edge cases"

# SMS cross-operation isolation
$REDIS_CMD SET "user:code:register:$PHONE_B" "777777" EX 300 >/dev/null 2>&1
python -c "import json; json.dump({'phone':'$PHONE_B','code':'777777','newPassword':'Hacked123'}, open('$TMPDIR/sms_cross.json','w',encoding='utf-8'))"
put_file "/user/password/reset" "$AUTH_B" "sms_cross.json" "sms_cross.json"
check "SMS cross-op rejected" "0" "$(json sms_cross.json "d['code'])"

# Wrong pay password
python -c "import json; json.dump({'amount':10.00,'payPassword':'wrongpw'}, open('$TMPDIR/badpw.json','w',encoding='utf-8'))"
post_file "/user/transactions/recharge" "$AUTH_B" "badpw.json" "badpw.json"
check "Wrong pay password rejected" "0" "$(json badpw.json "d['code'])"

# Duplicate accept rejected
post_file "/order/accept/$TASK_ID_1" "$AUTH_B" "addr.json" "dup_accept.json"
check "Duplicate accept rejected" "0" "$(json dup_accept.json "d['code'])"

# ═════════════════════════════════════════
# REPORT
# ═════════════════════════════════════════
echo ""
echo "══════════════════════════════════════════════"
echo "  E2E Full Flow — Report"
echo "══════════════════════════════════════════════"
green "  PASS: $PASS"
[ "$FAIL" -gt 0 ] && red "  FAIL: $FAIL" || echo ""
echo ""
echo "  Order: #$ORDER_ID (task #$TASK_NO_1)"
echo "  Flow:  1(WAITING) → 2(ACCEPTED) → 3(DELIVERING) → 4(WAIT_CONFIRM) → 5(COMPLETED)"
echo ""

[ "$FAIL" -gt 0 ] && exit 1 || exit 0
