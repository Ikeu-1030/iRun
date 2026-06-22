"""E2E test for banner carousel feature on admin dashboard."""
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:3002/api'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()

    # --- Step 1: Login ---
    page.goto(f'{BASE}/login')
    page.wait_for_load_state('networkidle')
    page.fill('input[type="text"]', 'admin')
    page.fill('input[type="password"]', 'admin')
    page.click('.login-btn')
    page.wait_for_url('**/dashboard')
    page.wait_for_load_state('networkidle')
    print('[OK] Login successful')

    # --- Step 2: Dashboard - no carousel (no banners) ---
    page.wait_for_timeout(2000)
    carousel = page.locator('.banner-section')
    assert carousel.count() == 0, 'Carousel should not appear without banners'
    print('[OK] Dashboard: no carousel (no banners configured)')

    # --- Step 3: Navigate to Settings via sidebar (client-side) ---
    # Find sidebar menu item for settings and click it
    sidebar = page.locator('.el-menu--vertical, .sidebar-menu, .el-menu')
    menu_items = page.locator('.el-menu-item')
    count = menu_items.count()
    print(f'[INFO] Menu items: {count}')
    for i in range(count):
        text = menu_items.nth(i).inner_text()
        print(f'  [{i}] {repr(text)}')

    # Click the settings/item (typically has text like "系统设置")
    settings_item = page.locator('.el-menu-item').filter(has_text='设置')
    if settings_item.count() == 0:
        # Try finding by index or alternative text
        # Print all sidebar items for debugging
        print('[INFO] Looking for settings menu item...')
        settings_item = page.locator('.el-menu-item').last()  # fallback

    settings_item.click()
    page.wait_for_url('**/settings')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    print('[OK] Navigated to settings')

    # --- Step 4: Check settings page content ---
    cards = page.locator('.color-card')
    print(f'[INFO] Color cards: {cards.count()}')
    for i in range(cards.count()):
        header = cards.nth(i).locator('.card-title-text').inner_text()
        print(f'  Card {i}: {repr(header)}')

    # Check for banner management
    banner_manage = page.locator('.banner-manage')
    print(f'[INFO] .banner-manage count: {banner_manage.count()}')

    upload_btn = page.locator('.banner-upload-area button')
    print(f'[INFO] Upload button count: {upload_btn.count()}')

    interval_row = page.locator('.banner-interval-row')
    print(f'[INFO] Interval row count: {interval_row.count()}')

    page.screenshot(path='F:/ikeu_runningerrands/admin/screenshots/settings_v2.png', full_page=True)

    # --- Step 5: Navigate back to dashboard ---
    page.locator('.el-menu-item').filter(has_text='仪表盘').click()
    page.wait_for_url('**/dashboard')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    # Check localStorage cache
    cache = page.evaluate('() => localStorage.getItem("rr_banner_cache")')
    print(f'[INFO] Banner cache: {cache}')

    browser.close()
    print('\n=== Test complete ===')
