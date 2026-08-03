import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 导航与布局', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('应用标题显示 LUNA', async ({ page }) => {
    const brandName = page.locator('.brand-name')
    await expect(brandName).toHaveText('LUNA')
  })

  test('默认激活类树浏览器标签页', async ({ page }) => {
    const activeTab = page.locator('.nav-item.active')
    await expect(activeTab).toHaveCount(1)
    await expect(activeTab.locator('.nav-label')).toHaveText(/类树|Class/)
  })

  test('点击日志监控标签页切换视图', async ({ page }) => {
    await page.locator('.nav-item:has(.fa-terminal)').click()
    const logViewer = page.locator('.log-viewer')
    await expect(logViewer).toBeVisible()
  })

  test('点击监控大盘标签页切换视图', async ({ page }) => {
    await page.locator('.nav-item:has(.fa-tachometer-alt)').click()
    const dashboard = page.locator('.dashboard')
    await expect(dashboard).toBeVisible()
  })

  test('点击线程分析标签页切换视图', async ({ page }) => {
    await page.locator('.nav-item:has(.fa-microchip)').click()
    const threadAnalyzer = page.locator('.thread-analyzer')
    await expect(threadAnalyzer).toBeVisible()
  })

  test('点击策略中心标签页切换视图', async ({ page }) => {
    await page.locator('.nav-item:has(.fa-sliders-h)').click()
    const configViewer = page.locator('.configuration-viewer')
    await expect(configViewer).toBeVisible()
  })

  test('语言切换按钮切换中英文', async ({ page }) => {
    const langBtn = page.locator('.lang-switcher')
    const initialText = await langBtn.textContent()
    await langBtn.click()
    const afterClickText = await langBtn.textContent()
    expect(initialText).not.toBe(afterClickText)
  })

  test('底部状态栏显示类数量', async ({ page }) => {
    const footerInfo = page.locator('.footer-info .info-item').first()
    await expect(footerInfo).toContainText('CLASSES:')
  })

  test('刷新按钮可点击', async ({ page }) => {
    const refreshBtn = page.locator('.footer-button')
    await expect(refreshBtn).toBeEnabled()
  })
})
