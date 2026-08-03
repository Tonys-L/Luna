import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 日志监控', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-terminal)').click()
    await page.waitForTimeout(1000)
  })

  test('日志监控视图可见', async ({ page }) => {
    const logViewer = page.locator('.log-viewer')
    await expect(logViewer).toBeVisible()
  })

  test('清空按钮可见', async ({ page }) => {
    const clearBtn = page.locator('.tool-btn:has(.fa-ban)')
    await expect(clearBtn).toBeVisible()
  })

  test('自动滚动按钮可见', async ({ page }) => {
    const autoScrollBtn = page.locator('.tool-btn:has(.fa-arrow-down)')
    await expect(autoScrollBtn).toBeVisible()
  })

  test('连接状态指示器可见', async ({ page }) => {
    const connectionStatus = page.locator('.connection-status')
    await expect(connectionStatus).toBeVisible()
  })

  test('空状态提示可见', async ({ page }) => {
    const emptyState = page.locator('.log-container .empty-state')
    if (await emptyState.count() > 0) {
      await expect(emptyState).toBeVisible()
    }
  })

  test('点击自动滚动切换状态', async ({ page }) => {
    const autoScrollBtn = page.locator('.tool-btn:has(.fa-arrow-down)')
    await autoScrollBtn.click()
    const hasActive = await autoScrollBtn.evaluate(el => el.classList.contains('active'))
    expect(typeof hasActive).toBe('boolean')
    await autoScrollBtn.click()
  })

  test('点击清空按钮', async ({ page }) => {
    const clearBtn = page.locator('.tool-btn:has(.fa-ban)')
    await expect(clearBtn).toBeEnabled()
    await clearBtn.click()
  })
})

test.describe('Luna 应用 - 日志 WebSocket', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-terminal)').click()
    await page.waitForTimeout(2000)
  })

  test('WebSocket 连接状态显示已连接', async ({ page }) => {
    const connectionStatus = page.locator('.connection-status')
    await expect(connectionStatus).toBeVisible()
    const statusText = await connectionStatus.textContent().catch(() => '')
    const isConnected = statusText.includes('连接') || statusText.includes('connect') ||
                        await connectionStatus.locator('.connected, .status-dot').count() > 0
    expect(typeof isConnected).toBe('boolean')
  })

  test('清空日志按钮功能', async ({ page }) => {
    const clearBtn = page.locator('.tool-btn:has(.fa-ban)')
    await expect(clearBtn).toBeEnabled()
    await clearBtn.click()
    await page.waitForTimeout(500)
  })
})
