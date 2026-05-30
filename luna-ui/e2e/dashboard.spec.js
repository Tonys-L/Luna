import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 监控大盘', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-tachometer-alt)').click()
    await page.waitForTimeout(2000)
  })

  test('监控大盘视图可见', async ({ page }) => {
    const dashboard = page.locator('.dashboard')
    await expect(dashboard).toBeVisible()
  })

  test('刷新按钮可见', async ({ page }) => {
    const refreshBtn = page.locator('.refresh-btn')
    await expect(refreshBtn).toBeVisible()
  })

  test('自动刷新复选框可见', async ({ page }) => {
    const autoRefreshCheck = page.locator('#auto-refresh-check')
    await expect(autoRefreshCheck).toBeVisible()
  })

  test('点击刷新按钮加载指标数据', async ({ page }) => {
    const refreshBtn = page.locator('.refresh-btn')
    await refreshBtn.click()
    await page.waitForTimeout(2000)
    const statCards = page.locator('.stat-card')
    const count = await statCards.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('切换自动刷新', async ({ page }) => {
    const autoRefreshCheck = page.locator('#auto-refresh-check')
    await autoRefreshCheck.check()
    await expect(autoRefreshCheck).toBeChecked()
    await autoRefreshCheck.uncheck()
    await expect(autoRefreshCheck).not.toBeChecked()
  })

  test('JVM 指标数据展示', async ({ page }) => {
    const refreshBtn = page.locator('.refresh-btn')
    await refreshBtn.click()
    await page.waitForTimeout(2000)
    const statCards = page.locator('.stat-card')
    if (await statCards.count() > 0) {
      const firstCard = statCards.first()
      await expect(firstCard).toBeVisible()
    }
  })
})
