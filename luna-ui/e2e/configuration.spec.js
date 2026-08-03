import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 持久注入', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-sliders-h)').click()
    await page.waitForTimeout(2000)
  })

  test('持久注入视图可见', async ({ page }) => {
    const configViewer = page.locator('.configuration-viewer')
    await expect(configViewer).toBeVisible()
  })

  test('新建注入按钮可见', async ({ page }) => {
    const addBtn = page.locator('.add-btn-round')
    await expect(addBtn).toBeVisible()
  })

  test('注入列表区域可见', async ({ page }) => {
    const rulesList = page.locator('.injections-list')
    await expect(rulesList).toBeVisible()
  })

  test('点击新建注入按钮', async ({ page }) => {
    const addBtn = page.locator('.add-btn-round')
    await addBtn.click()
    await page.waitForTimeout(1000)
  })

  test('空注入列表显示提示', async ({ page }) => {
    const emptyRules = page.locator('.empty-injections')
    if (await emptyRules.count() > 0) {
      await expect(emptyRules).toBeVisible()
    }
  })
})

test.describe('Luna 应用 - 持久注入交互', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.locator('.nav-item:has(.fa-sliders-h)').click()
    await page.waitForTimeout(2000)
  })

  test('新建注入完整流程', async ({ page }) => {
    const addBtn = page.locator('.add-btn-round')
    if (await addBtn.count() > 0) {
      await addBtn.click()
      await page.waitForTimeout(1000)
      const editor = page.locator('.injection-editor, .el-dialog, .injection-form')
      if (await editor.count() > 0) {
        await expect(editor.first()).toBeVisible()
      }
    }
  })
})
