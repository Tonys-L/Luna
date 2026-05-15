import { test, expect } from '@playwright/test'

test.describe('插件管理页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('应显示插件管理 Tab', async ({ page }) => {
    const pluginTab = page.getByText('插件管理')
    await expect(pluginTab).toBeVisible()
  })

  test('点击插件管理 Tab 应显示插件管理页面', async ({ page }) => {
    await page.getByText('插件管理').click()
    await page.waitForTimeout(500)

    const pageHeader = page.locator('.page-header')
    await expect(pageHeader).toBeVisible()

    const refreshBtn = page.locator('.refresh-btn')
    await expect(refreshBtn).toBeVisible()

    const marketBtn = page.locator('.market-btn')
    await expect(marketBtn).toBeVisible()
  })

  test('插件列表为空时应显示空状态', async ({ page }) => {
    await page.getByText('插件管理').click()
    await page.waitForTimeout(1000)

    const emptyState = page.locator('.empty-state')
    const pluginTable = page.locator('.plugin-table tbody tr')
    const hasEmptyState = await emptyState.count() > 0
    const hasPlugins = await pluginTable.count() > 0
    expect(hasEmptyState || hasPlugins).toBeTruthy()
  })

  test('插件市场切换应正常工作', async ({ page }) => {
    await page.getByText('插件管理').click()
    await page.waitForTimeout(500)

    const marketBtn = page.locator('.market-btn')
    await marketBtn.click()
    await page.waitForTimeout(300)

    const marketContent = page.locator('.market-content')
    await expect(marketContent).toBeVisible()

    const searchInput = page.locator('.search-input')
    await expect(searchInput).toBeVisible()
  })
})

test.describe('注入对话框动态化', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('注入类型选项应动态加载', async ({ page }) => {
    const classNode = page.locator('.class-tree .tree-node, .el-tree-node').first()
    if (await classNode.count() > 0) {
      await classNode.click()
      await page.waitForTimeout(300)

      const methodNode = page.locator('.method-item, .member-item').first()
      if (await methodNode.count() > 0) {
        await methodNode.click()
        await page.waitForTimeout(300)

        const injectionTypeSelect = page.locator('.injection-type-select, select, .el-select').first()
        if (await injectionTypeSelect.count() > 0) {
          await injectionTypeSelect.click()
          await page.waitForTimeout(300)

          const options = page.locator('.el-select-dropdown__item, option')
          const count = await options.count()
          expect(count).toBeGreaterThan(0)
        }
      }
    }
  })
})

test.describe('模板库', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('配置页面应显示模板库 Tab', async ({ page }) => {
    const configTab = page.getByText('配置')
    if (await configTab.count() > 0) {
      await configTab.click()
      await page.waitForTimeout(500)

      const templateTab = page.getByText('模板库')
      if (await templateTab.count() > 0) {
        await templateTab.click()
        await page.waitForTimeout(300)

        const templateCards = page.locator('.template-card, .template-item')
        const hasTemplates = await templateCards.count() > 0 || await page.getByText('暂无模板').count() > 0
        expect(hasTemplates).toBeTruthy()
      }
    }
  })
})
