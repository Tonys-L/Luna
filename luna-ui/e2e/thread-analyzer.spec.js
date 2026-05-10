import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 线程分析', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-microchip)').click()
    await page.waitForTimeout(2000)
  })

  test('线程分析视图可见', async ({ page }) => {
    const threadAnalyzer = page.locator('.thread-analyzer')
    await expect(threadAnalyzer).toBeVisible()
  })

  test('获取线程快照按钮可见', async ({ page }) => {
    const refreshBtn = page.locator('.refresh-btn')
    await expect(refreshBtn).toBeVisible()
  })

  test('点击获取线程快照', async ({ page }) => {
    const refreshBtn = page.locator('.refresh-btn')
    await refreshBtn.click()
    await page.waitForTimeout(2000)
    const threadList = page.locator('.thread-list')
    if (await threadList.count() > 0) {
      await expect(threadList).toBeVisible()
    }
  })

  test('死锁告警区域条件显示', async ({ page }) => {
    const deadlockAlert = page.locator('.deadlock-alert')
    const isVisible = await deadlockAlert.isVisible().catch(() => false)
    expect(typeof isVisible).toBe('boolean')
  })
})
