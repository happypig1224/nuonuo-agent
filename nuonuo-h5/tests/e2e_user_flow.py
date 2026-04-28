import os
import sys
import time
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("H5_BASE_URL", "http://127.0.0.1:5173")


def main() -> int:
    test_account = f"e2e_{int(time.time())}@nuonuo.ai"
    updated_nickname = "小糯E2E测试"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 390, "height": 844})
        page = context.new_page()

        print(f"[E2E] Opening {BASE_URL}")
        page.goto(BASE_URL, wait_until="networkidle")

        # 1. 打开“小糯记住了我”抽屉
        page.locator('button[aria-label="小糯记住了我"]').click()
        page.get_by_placeholder("输入手机号或邮箱登录").wait_for(state="visible", timeout=5000)
        print("[E2E] Unauthenticated login form is visible")

        # 2. 输入账号并点击登录（真实调用 POST /api/v1/user/login）
        page.get_by_placeholder("输入手机号或邮箱登录").fill(test_account)
        page.get_by_role("button", name="登录", exact=True).click()
        page.get_by_text(f"账号: {test_account}").wait_for(state="visible", timeout=8000)
        print(f"[E2E] Logged in as {test_account}")

        # 3. 修改昵称（真实调用 PATCH /api/v1/user/profile）
        page.get_by_role("button", name="改昵称").click()
        page.get_by_placeholder("输入新昵称").fill(updated_nickname)
        page.get_by_role("button", name="保存").click()
        page.get_by_text(updated_nickname).wait_for(state="visible", timeout=8000)
        print(f"[E2E] Nickname updated to {updated_nickname}")

        # 4. 刷新页面，验证持久化渲染
        page.reload(wait_until="networkidle")
        page.locator('button[aria-label="小糯记住了我"]').click()
        page.get_by_text(updated_nickname).wait_for(state="visible", timeout=8000)
        print("[E2E] Persisted nickname verified after page reload")

        # 5. 退出登录（真实调用 POST /api/v1/user/logout）
        page.get_by_role("button", name="退出").click()
        page.get_by_placeholder("输入手机号或邮箱登录").wait_for(state="visible", timeout=8000)
        print("[E2E] Logged out successfully")

        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
