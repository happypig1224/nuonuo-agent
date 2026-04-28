import os
import sys
import time
import urllib.request
import urllib.error
import json
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("H5_BASE_URL", "http://127.0.0.1:5174")
API_BASE = os.environ.get("API_BASE_URL", "http://127.0.0.1:8080")


def api_request(method: str, path: str, body: dict | None = None, token: str | None = None) -> tuple[int, dict]:
    url = f"{API_BASE}{path}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def main() -> int:
    test_account = f"e2e_{int(time.time())}@nuonuo.ai"
    test_password = "Test123456"
    updated_nickname = "小糯E2E测试"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 390, "height": 844})
        page = context.new_page()

        print(f"[E2E] Opening {BASE_URL}")
        page.goto(BASE_URL, wait_until="networkidle")

        # 1. 切换到“注册账号”并注册（真实调用 POST /api/v1/user/register）
        page.get_by_role("button", name="注册账号").click()
        page.get_by_placeholder("请输入手机号或邮箱").fill(test_account)
        page.get_by_placeholder("请设置密码（不少于 6 位）").fill(test_password)
        page.get_by_placeholder("请再次输入密码确认").fill(test_password)
        page.locator('input[type="checkbox"]').check()
        page.get_by_role("button", name="立即注册").click()
        print(f"[E2E] Submitted registration for {test_account}")

        # 2. 进入主聊天页，打开“小糯记住了我”抽屉验证登录态
        page.locator('button[aria-label="小糯记住了我"]').wait_for(state="visible", timeout=10000)
        page.locator('button[aria-label="小糯记住了我"]').first.click()
        page.get_by_text(f"账号: {test_account}").wait_for(state="visible", timeout=8000)
        print(f"[E2E] Registered & logged in as {test_account}")

        # 3. 修改昵称（真实调用 PATCH /api/v1/user/profile）
        page.get_by_role("button", name="改昵称").click()
        page.get_by_placeholder("输入新昵称").fill(updated_nickname)
        page.get_by_role("button", name="保存").click()
        page.get_by_text(updated_nickname).wait_for(state="visible", timeout=8000)
        print(f"[E2E] Nickname updated to {updated_nickname}")

        # 4. 刷新页面，验证数据库持久化渲染
        page.reload(wait_until="domcontentloaded")
        page.wait_for_timeout(800)
        page.locator('button[aria-label="小糯记住了我"]').first.click()
        page.get_by_text(updated_nickname).wait_for(state="visible", timeout=8000)
        print("[E2E] Persisted nickname verified after page reload")

        # 5. 退出登录（真实调用 POST /api/v1/user/logout）
        page.get_by_role("button", name="退出").click()
        page.get_by_role("button", name="密码登录").wait_for(state="visible", timeout=8000)
        print("[E2E] Logged out successfully")

        # 6. 异常场景：密码错误登录失败
        page.get_by_placeholder("请输入手机号或邮箱").fill(test_account)
        page.get_by_placeholder("请输入密码").fill("WrongPass999")
        page.locator('input[type="checkbox"]').check()
        page.get_by_role("button", name="登录", exact=True).click()
        page.locator("text=密码错误").or_(page.locator("text=账号或密码错误")).first.wait_for(state="visible", timeout=8000)
        print("[E2E] Wrong password login rejected as expected")

        # 7. 异常场景：重复注册同一账号被拦截
        page.get_by_role("button", name="注册账号").click()
        page.get_by_placeholder("请输入手机号或邮箱").fill(test_account)
        page.get_by_placeholder("请设置密码（不少于 6 位）").fill(test_password)
        page.get_by_placeholder("请再次输入密码确认").fill(test_password)
        page.get_by_role("button", name="立即注册").click()
        page.locator("text=已注册").first.wait_for(state="visible", timeout=8000)
        print("[E2E] Duplicate registration rejected as expected")

        # 8. 异常场景：未勾选协议拦截
        page.locator('input[type="checkbox"]').uncheck()
        page.get_by_role("button", name="立即注册").click()
        page.get_by_text("请先阅读并同意《用户协议》与《隐私政策》").wait_for(state="visible", timeout=5000)
        print("[E2E] Unchecked agreement blocked as expected")

        # 9. 接口层异常场景：未授权访问 /profile 返回 401，非法 token 返回 401
        status_no_auth, _ = api_request("GET", "/api/v1/user/profile")
        assert status_no_auth == 401, f"Expected 401 without token, got {status_no_auth}"
        status_bad_auth, _ = api_request("GET", "/api/v1/user/profile", token="invalid-token-xyz")
        assert status_bad_auth == 401, f"Expected 401 with invalid token, got {status_bad_auth}"
        print("[E2E] AuthInterceptor 401 checks passed")

        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
