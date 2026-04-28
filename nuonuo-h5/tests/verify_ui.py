import json
import os
import sys
import traceback
from playwright.sync_api import Route, sync_playwright

BASE_URL = os.environ.get("H5_BASE_URL", "http://127.0.0.1:5173")
SHOT_DIR = os.environ.get(
    "H5_SHOT_DIR",
    r"C:\Users\33046\.gemini\antigravity\brain\63b48180-430c-4be9-84d8-4a079fa9c961\scratch\screenshots",
)
os.makedirs(SHOT_DIR, exist_ok=True)


def install_api_routes(page) -> None:
    memories = [
        {
            "id": "mem_101",
            "category": "SKIN_TYPE",
            "profileKey": "skin_base",
            "profileValue": "干性敏感肌，两颊角质层偏薄",
            "confidence": 0.95,
            "updatedAt": "03-20",
        },
        {
            "id": "mem_102",
            "category": "ALLERGY",
            "profileKey": "alcohol",
            "profileValue": "高浓度酒精接触性泛红",
            "confidence": 1.0,
            "updatedAt": "03-15",
        },
        {
            "id": "mem_103",
            "category": "PREFERENCE",
            "profileKey": "texture_dislike",
            "profileValue": "排斥厚重封闭油霜，偏爱清爽水感乳液",
            "confidence": 0.9,
            "updatedAt": "03-22",
        },
    ]

    history = [
        {
            "id": "msg_welcome_assistant",
            "role": "assistant",
            "content": "为你准备了今日护肤便签",
            "routineCard": {
                "title": "晨晚护肤便签",
                "badge": "精简有效",
                "amSteps": [
                    {"order": 1, "action": "温和洁面", "caution": ""},
                    {"order": 2, "action": "保湿水乳", "caution": ""},
                    {"order": 3, "action": "温和防晒", "caution": ""},
                ],
                "pmSteps": [
                    {"order": 1, "action": "温和洁面", "caution": ""},
                    {"order": 2, "action": "修护精华", "caution": ""},
                    {"order": 3, "action": "保湿面霜", "caution": ""},
                ],
            },
            "createdAt": 1700000000000,
        }
    ]

    def handle_route(route: Route) -> None:
        req = route.request
        url = req.url
        method = req.method

        if "/api/v1/chat/history" in url and method == "GET":
            route.fulfill(status=200, content_type="application/json", body=json.dumps(history))
            return

        if "/api/v1/memories" in url:
            if method == "GET":
                route.fulfill(status=200, content_type="application/json", body=json.dumps(memories))
                return
            if method == "PATCH":
                mem_id = url.rstrip("/").split("/")[-1]
                payload = req.post_data_json or {}
                for item in memories:
                    if item["id"] == mem_id:
                        item["profileValue"] = payload.get("profileValue", item["profileValue"])
                        item["updatedAt"] = "刚刚"
                        route.fulfill(status=200, content_type="application/json", body=json.dumps(item))
                        return
                route.fulfill(status=404, content_type="application/json", body='{"message":"not found"}')
                return
            if method == "DELETE":
                tail = url.rstrip("/").split("/")[-1]
                if tail != "memories":
                    memories[:] = [m for m in memories if m["id"] != tail]
                else:
                    memories.clear()
                route.fulfill(status=204, body="")
                return

        if "/api/v1/chat/stream" in url and method == "POST":
            warning_payload = {
                "level": "STRICT_FORBIDDEN",
                "title": "成分配伍禁忌预警 (P0 级阻断)",
                "ingredients": ["水杨酸 / 强酸 (HIGH_ACID)", "视黄醇 A 醇 (RETINOIDS)"],
                "mechanism": "强酸在低 pH 下加速角质剥脱，视黄醇同时加速表皮更新，两者叠加会显著放大刺痛、脱屑与接触性泛红风险。",
                "advice": "建议采用“早 C 晚 A”或隔晚单用交替策略，并配合 B5 舒缓修护。",
            }
            sse_body = "".join(
                [
                    'event: emotion\ndata: {"emotion":"ALERT"}\n\n',
                    'event: delta\ndata: {"content":"宝子先停一下手，这两个成分不能同一时间叠涂哦。"}\n\n',
                    f"event: warning_card\ndata: {json.dumps(warning_payload, ensure_ascii=False)}\n\n",
                    "event: done\ndata: {}\n\n",
                ]
            )
            route.fulfill(status=200, content_type="text/event-stream; charset=utf-8", body=sse_body)
            return

        route.continue_()

    page.route("**/api/v1/**", handle_route)


def run() -> None:
    console_errors: list[str] = []
    page_errors: list[str] = []

    with sync_playwright() as p:
        browser = p.chromium.launch()

        # --- 移动端 390x844 完整交互验收 ---
        mobile_ctx = browser.new_context(viewport={"width": 390, "height": 844}, has_touch=True)
        page = mobile_ctx.new_page()
        install_api_routes(page)
        page.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)
        page.on("pageerror", lambda err: page_errors.append(str(err)))

        page.goto(BASE_URL, wait_until="networkidle")
        page.wait_for_timeout(450)

        # 1. 首屏断言：小糯主舞台 + 便签卡片 + 底部三胶囊 Dock + 无横向溢出
        assert page.locator(".avatar-character").is_visible()
        assert page.locator(".routine-card").is_visible()
        assert page.locator(".dock-voice-btn").is_visible()
        overflow_x = page.evaluate("document.documentElement.scrollWidth > window.innerWidth")
        assert not overflow_x, "Horizontal overflow detected"
        page.screenshot(path=os.path.join(SHOT_DIR, "01-initial.png"))

        # 2. 轻戳小糯形象交互
        before_greeting = page.locator(".avatar-speech-text").inner_text()
        page.locator(".avatar-character").click(force=True)
        page.wait_for_timeout(200)
        assert page.locator(".avatar-speech-text").inner_text() != before_greeting

        # 3. 左上角快捷菜单 & 动态背景主题切换 + 退出动画验证
        page.locator(".nav-icon-btn").click()
        page.wait_for_timeout(300)
        assert page.locator("[aria-label='快捷功能菜单']").is_visible()
        page.screenshot(path=os.path.join(SHOT_DIR, "02-menu-sheet.png"))
        page.get_by_role("radio", name="晴空水雾").click()
        assert page.locator(".app-shell[data-theme='sky']").count() == 1
        page.get_by_role("radio", name="静谧深夜").click()
        assert page.locator(".app-shell[data-theme='night']").count() == 1
        page.get_by_role("radio", name="蜜桃暖阳").click()
        assert page.locator(".app-shell[data-theme='peach']").count() == 1
        page.locator(".sheet-backdrop").click(position={"x": 195, "y": 120})
        page.wait_for_timeout(420)
        assert page.locator("[aria-label='快捷功能菜单']").count() == 0

        # 4. 记忆抽屉：纠偏 + 滑动删除 + 退出动画验证
        page.locator(".nav-memory-btn").click()
        page.wait_for_timeout(320)
        assert page.locator(".memory-drawer").is_visible()
        page.screenshot(path=os.path.join(SHOT_DIR, "03-memory-drawer.png"))
        initial_count = page.locator(".memory-card").count()
        page.locator(".memory-card-val").first.click()
        page.locator(".memory-edit-input").fill("干性敏感肌，两颊角质层偏薄（已实测）")
        page.locator(".memory-edit-input").press("Enter")
        page.wait_for_timeout(200)
        assert "已实测" in page.locator(".memory-card-val").first.inner_text()
        last_card = page.locator(".memory-card").last
        box = last_card.bounding_box()
        assert box is not None
        start_x = box["x"] + box["width"] - 24
        y = box["y"] + box["height"] / 2
        page.mouse.move(start_x, y)
        page.mouse.down()
        page.mouse.move(start_x - 65, y, steps=6)
        page.mouse.up()
        page.wait_for_timeout(220)
        page.locator(".memory-delete-action").last.click()
        page.wait_for_timeout(200)
        assert page.locator(".memory-card").count() == initial_count - 1
        page.get_by_role("button", name="收起").click()
        page.wait_for_timeout(420)
        assert page.locator(".memory-drawer").count() == 0

        # 5. 拍照 ActionSheet
        page.locator(".dock-pill-side").click()
        page.wait_for_timeout(280)
        assert page.locator(".action-sheet").is_visible()
        page.screenshot(path=os.path.join(SHOT_DIR, "04-camera-sheet.png"))
        page.get_by_role("button", name="取消").click()
        page.wait_for_timeout(200)

        # 6. 文本输入 + 真实 SSE 接口推流（打字气泡 -> 警告卡）
        page.locator(".dock-pill-mint").click()
        page.locator(".dock-input").fill("水杨酸和视黄醇能一起涂吗？")
        page.get_by_role("button", name="发送").click()
        page.locator(".warning-card").wait_for(state="visible", timeout=8000)
        page.wait_for_timeout(400)
        assert page.locator(".warning-card").count() >= 1
        assert page.locator(".avatar-character.is-alert").count() == 1
        page.screenshot(path=os.path.join(SHOT_DIR, "05-streaming-warning.png"))
        mobile_ctx.close()

        # --- 桌面端 1280x800 居中手机框渲染检查 ---
        desktop_ctx = browser.new_context(viewport={"width": 1280, "height": 800})
        desktop_page = desktop_ctx.new_page()
        install_api_routes(desktop_page)
        desktop_page.goto(BASE_URL, wait_until="networkidle")
        desktop_page.wait_for_timeout(300)
        shell_box = desktop_page.locator(".app-shell").bounding_box()
        assert shell_box is not None and shell_box["width"] <= 432
        desktop_page.screenshot(path=os.path.join(SHOT_DIR, "06-desktop-frame.png"))
        desktop_ctx.close()

        browser.close()

    assert not console_errors, f"Console errors: {console_errors}"
    assert not page_errors, f"Page errors: {page_errors}"
    print("PLAYWRIGHT_FULL_UX_PASS")


if __name__ == "__main__":
    try:
        run()
    except Exception:
        traceback.print_exc()
        sys.exit(1)
