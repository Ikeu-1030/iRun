#!/usr/bin/env python
"""E2E Full Business Flow — Phase 2-5 with assertions."""
import json, subprocess, sys, time, os

API = "http://localhost:8080/api"
TMP = "e2e/.tmp"
os.makedirs(TMP, exist_ok=True)

PASS = 0
FAIL = 0

def green(s): print(f"\033[0;32m{s}\033[0m")
def red(s):   print(f"\033[0;31m{s}\033[0m")
def cyan(s):  print(f"\033[0;36m>>> {s}\033[0m")

def check(desc, expected, actual):
    global PASS, FAIL
    if str(expected) == str(actual):
        green(f"  [PASS] {desc}")
        PASS += 1
    else:
        red(f"  [FAIL] {desc} (expected={expected} actual={actual})")
        FAIL += 1

def redis(*args):
    return subprocess.run(["redis-cli", "-n", "1"] + list(args), capture_output=True, text=True).stdout.strip()

def api_get(path, auth_header=""):
    cmd = ["curl", "-s", f"{API}{path}"]
    if auth_header:
        cmd += ["-H", auth_header]
    cmd += ["-o", f"{TMP}/resp.json"]
    subprocess.run(cmd)
    with open(f"{TMP}/resp.json", encoding="utf-8") as f:
        return json.load(f)

def api_post(path, body_dict, auth_header=""):
    body_file = f"{TMP}/body.json"
    with open(body_file, "w", encoding="utf-8") as f:
        json.dump(body_dict, f, ensure_ascii=False)
    cmd = ["curl", "-s", "-X", "POST", f"{API}{path}",
           "-H", "Content-Type: application/json; charset=utf-8"]
    if auth_header:
        cmd += ["-H", auth_header]
    cmd += ["--data-binary", f"@{body_file}", "-o", f"{TMP}/resp.json"]
    subprocess.run(cmd)
    with open(f"{TMP}/resp.json", encoding="utf-8") as f:
        return json.load(f)

def api_put(path, body_dict, auth_header=""):
    body_file = f"{TMP}/body.json"
    with open(body_file, "w", encoding="utf-8") as f:
        json.dump(body_dict, f, ensure_ascii=False)
    cmd = ["curl", "-s", "-X", "PUT", f"{API}{path}",
           "-H", "Content-Type: application/json; charset=utf-8"]
    if auth_header:
        cmd += ["-H", auth_header]
    cmd += ["--data-binary", f"@{body_file}", "-o", f"{TMP}/resp.json"]
    subprocess.run(cmd)
    with open(f"{TMP}/resp.json", encoding="utf-8") as f:
        return json.load(f)

# ── Bootstrap ──
cyan("Bootstrap: Login")

# Admin
r = api_post("/admin/login", {"username":"admin","password":"admin"})
check("Admin login", 1, r["code"])
ADMIN_TOKEN = r["data"]["token"]
ADMIN_AUTH = f"token: {ADMIN_TOKEN}"

# User A (existing: ID=1, certified runner, has payPwd)
redis("SET", "user:code:login:18848298061", "888888", "EX", "300")
r = api_post("/user/login", {"loginType":2,"phone":"18848298061","code":"888888"})
check("User A login", 1, r["code"])
TOKEN_A = r["data"]["token"]
USER_ID_A = r["data"]["userId"]
AUTH_A = f"authentication: {TOKEN_A}"
green(f"  User A: id={USER_ID_A}")

# Register User B — use timestamp-based unique phone
import time
PHONE_B = f"139{int(time.time()) % 100000000:08d}"
redis("SET", f"user:code:register:{PHONE_B}", "888888", "EX", "300")
import random, string
UNIQUE_ID = ''.join(random.choices(string.ascii_lowercase, k=6))
r = api_post("/user/register", {"phone":PHONE_B,"code":"888888","username":f"e2e{UNIQUE_ID}","password":"Test123456","sex":"M"})
check("Register User B", 1, r["code"])
if r["code"] != 1:
    red(f"  Register failed: {r.get('msg','?')}")
    sys.exit(1)
TOKEN_B = r["data"]["token"]
USER_ID_B = r["data"]["userId"]
AUTH_B = f"authentication: {TOKEN_B}"
green(f"  User B: id={USER_ID_B}")

# ── Phase 2: User B setup ──
cyan("Phase 2: User B — Certify + Runner")

# Add address
r = api_post("/user/address/save", {"contactName":"TestB","contactPhone":PHONE_B,"sex":"M","detail":"TestAddr","isDefault":1}, AUTH_B)
check("Add address", 1, r["code"])

# Get address ID for User A
r = api_get("/user/address/list", AUTH_A)
raw = r.get("data", [])
if isinstance(raw, dict):
    records = raw.get("records", [])
else:
    records = raw
ADDR_A_ID = records[0]["id"] if isinstance(records, list) and records else 1
green(f"  Addr A ID={ADDR_A_ID}")

# Certify + approve
r = api_post("/user/certify", {"realName":"TestB","studentId":"20240003","certImageUrl":"https://example.com/cert.jpg"}, AUTH_B)
if r["code"] != 1:
    red(f"  Certify error: {r.get('msg','?')}")
check("Submit certify", 1, r["code"])
r = api_put(f"/admin/users/{USER_ID_B}/certify?isCertify=2&remark=E2E_OK", {}, ADMIN_AUTH)
if r["code"] != 1:
    red(f"  Approve error: {r.get('msg','?')}")
check("Admin approve certify", 1, r["code"])

# ── Phase 3: Runner ──
cyan("Phase 3: Runner")

# Recharge (needs pay password — workaround: use admin API to set payPwdStatus directly, or login as User B and use change if they had one set)
# BUG: PUT /user/pay-password routes to wrong handler. Workaround: skip payPwd for now, test wallet via User A
# For order flow, User B just needs to be a certified runner who can accept tasks
r = api_post("/runner/apply", {}, AUTH_B)
check("Apply runner", 1, r["code"])

# Get runner profile ID
r = api_get("/admin/runners?page=1&size=20", ADMIN_AUTH)
for rec in r["data"]["records"]:
    if rec.get("userId") == USER_ID_B:
        RUNNER_B_ID = rec.get("profileId") or rec.get("id")
        break
green(f"  Runner B ID={RUNNER_B_ID}")
r = api_put(f"/admin/runners/{RUNNER_B_ID}/review?verifyStatus=2", {}, ADMIN_AUTH)
check("Admin approve runner", 1, r["code"])

r = api_post("/runner/online", {}, AUTH_B)
check("Go online", 1, r["code"])

# ── Phase 4: Publish task (User A) ──
cyan("Phase 4: Publish Task")

import json as j
task_specs = j.dumps({"包裹列表":[{"规格":"小件","数量":1}]}, ensure_ascii=False)
r = api_post("/task/publish", {
    "type":"daiqukuaidi", "subType":"小件快递",
    "publicDesc":"E2E全链路测试-代取快递", "privateNote":"取件码:TEST001",
    "taskSpecs": task_specs,
    "tip":3, "deliveryFee":5, "productCost":0, "payPassword":"123456",
    "pickupAddress":"菜鸟驿站(成都校区)", "pickupCode":"TEST001",
    "deliveryAddressId": ADDR_A_ID, "expireMinutes":60,
    "contactName":"cyf", "contactPhone":"18848298061"
}, AUTH_A)
check("Publish task", 1, r["code"])
TASK_NO = r.get("data", {}).get("taskNo", "?")
green(f"  TaskNo: {TASK_NO}")
# Look up taskId from task hall for order flow
r = api_get("/task/mine?page=1&size=3", AUTH_A)
records = r.get("data", {}).get("records", r.get("data", []))
if isinstance(records, dict):
    records = records.get("records", records if isinstance(records, list) else [records])
if isinstance(records, list) and records:
    TASK_ID = records[0].get("taskId") or records[0].get("id")
else:
    red("  Cannot find published task!")
    sys.exit(1)
green(f"  Task: id={TASK_ID}")

# ── Phase 5: Order Lifecycle ──
cyan("Phase 5: Order Lifecycle")

# 5.2 Accept
r = api_post(f"/order/accept/{TASK_ID}", {}, AUTH_B)
check("Accept order", 1, r["code"])
ORDER_ID = r.get("data", {}).get("orderId")
if not ORDER_ID:
    red("  No orderId in response!")
    sys.exit(1)
green(f"  Order: id={ORDER_ID}")

r = api_get(f"/task/{TASK_ID}", AUTH_A)
check("Task status → ACCEPTED(2)", "2", str(r["data"]["status"]))

# 5.3 Pickup
r = api_post(f"/order/{ORDER_ID}/pickup", {"imageUrls":["https://example.com/pickup.jpg"]}, AUTH_B)
check("Confirm pickup", 1, r["code"])

# 5.4 Deliver
r = api_post(f"/order/{ORDER_ID}/deliver", {"imageUrls":["https://example.com/deliver.jpg"]}, AUTH_B)
check("Confirm deliver", 1, r["code"])

r = api_get(f"/task/{TASK_ID}", AUTH_A)
check("Task status → WAIT_CONFIRM(4)", "4", str(r["data"]["status"]))

# 5.5 Complete
r = api_post(f"/order/{ORDER_ID}/confirm", {}, AUTH_A)
check("Confirm complete", 1, r["code"])

r = api_get(f"/admin/orders/{ORDER_ID}", ADMIN_AUTH)
check("Order status → COMPLETED(4)", "4", str(r["data"]["status"]))

r = api_get(f"/admin/tasks/{TASK_ID}", ADMIN_AUTH)
check("Task status → COMPLETED(5)", "5", str(r["data"]["status"]))

# 5.6 Reviews
r = api_post("/review", {
    "taskId": TASK_ID, "targetUserId": USER_ID_B,
    "rating": 5, "content": "E2E-速度快态度好",
    "tags": j.dumps(["速度快","态度好"])
}, AUTH_A)
check("Publisher review runner", 1, r["code"])

r = api_post("/review", {
    "taskId": TASK_ID, "targetUserId": USER_ID_A,
    "rating": 4, "content": "E2E-任务描述清晰"
}, AUTH_B)
check("Runner review publisher", 1, r["code"])

# ── Phase 7: Security ──
cyan("Phase 7: Security Edge Cases")

# SMS cross-operation
redis("SET", f"user:code:register:{PHONE_B}", "777777", "EX", "300")
r = api_put("/user/password/reset", {"phone":PHONE_B,"code":"777777","newPassword":"Hack123"}, AUTH_B)
check("SMS cross-op rejected", 0, r["code"])

# Wrong pay password (on recharge)
r = api_post("/user/transactions/recharge", {"amount":10,"payPassword":"wrongpw"}, AUTH_A)
check("Wrong payPwd rejected", 0, r["code"])

# Duplicate accept
r = api_post(f"/order/accept/{TASK_ID}", {}, AUTH_B)
check("Duplicate accept rejected", 0, r["code"])

# ── Report ──
print()
print("══════════════════════════════════════════════")
print("  E2E Full Flow — Report")
print("══════════════════════════════════════════════")
green(f"  PASS: {PASS}")
if FAIL > 0:
    red(f"  FAIL: {FAIL}")
print()
print(f"  Order: #{ORDER_ID} (task #{TASK_NO})")
print(f"  Flow:  1(WAITING)→2(ACCEPTED)→3(DELIVERING)→4(WAIT_CONFIRM)→5(COMPLETED)")
print()

sys.exit(1 if FAIL > 0 else 0)
