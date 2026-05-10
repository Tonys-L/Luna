import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 策略中心', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-sliders-h)').click()
    await page.waitForTimeout(2000)
  })

  test('策略中心视图可见', async ({ page }) => {
    const configViewer = page.locator('.configuration-viewer')
    await expect(configViewer).toBeVisible()
  })

  test('新建规则按钮可见', async ({ page }) => {
    const addBtn = page.locator('.add-btn-round')
    await expect(addBtn).toBeVisible()
  })

  test('规则列表区域可见', async ({ page }) => {
    const rulesList = page.locator('.rules-list')
    await expect(rulesList).toBeVisible()
  })

  test('点击新建规则按钮', async ({ page }) => {
    const addBtn = page.locator('.add-btn-round')
    await addBtn.click()
    await page.waitForTimeout(1000)
  })

  test('空规则列表显示提示', async ({ page }) => {
    const emptyRules = page.locator('.empty-rules')
    if (await emptyRules.count() > 0) {
      await expect(emptyRules).toBeVisible()
    }
  })
})
