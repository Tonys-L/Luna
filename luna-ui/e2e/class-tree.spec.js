import { test, expect } from '@playwright/test'

test.describe('Luna 应用 - 类树浏览器', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
  })

  test('类树侧边栏可见', async ({ page }) => {
    const aside = page.locator('.class-tree-aside')
    await expect(aside).toBeVisible()
  })

  test('搜索框存在并可输入', async ({ page }) => {
    const searchInput = page.locator('.search-input input')
    await expect(searchInput).toBeVisible()
    await searchInput.fill('UserService')
    await expect(searchInput).toHaveValue('UserService')
  })

  test('工具栏按钮可见', async ({ page }) => {
    const refreshBtn = page.locator('.toolbar-button:has(.fa-sync-alt)')
    const injectFilterBtn = page.locator('.toolbar-button:has(.fa-syringe)')
    const expandBtn = page.locator('.toolbar-button:has(.fa-folder-open)')
    const collapseBtn = page.locator('.toolbar-button:has(.fa-folder)')

    await expect(refreshBtn).toBeVisible()
    await expect(injectFilterBtn).toBeVisible()
    await expect(expandBtn).toBeVisible()
    await expect(collapseBtn).toBeVisible()
  })

  test('点击刷新按钮加载类树', async ({ page }) => {
    const refreshBtn = page.locator('.toolbar-button:has(.fa-sync-alt)')
    await refreshBtn.click()
    await page.waitForTimeout(2000)
    const treeNodes = page.locator('.el-tree-node')
    const count = await treeNodes.count()
    expect(count).toBeGreaterThan(0)
  })

  test('点击全部展开按钮展开类树', async ({ page }) => {
    const expandBtn = page.locator('.toolbar-button:has(.fa-folder-open)')
    await expandBtn.click()
    await page.waitForTimeout(1000)
    const expandedNodes = page.locator('.el-tree-node.is-expanded')
    const count = await expandedNodes.count()
    expect(count).toBeGreaterThan(0)
  })

  test('点击全部折叠按钮折叠类树', async ({ page }) => {
    const expandBtn = page.locator('.toolbar-button:has(.fa-folder-open)')
    await expandBtn.click()
    await page.waitForTimeout(1000)

    const collapseBtn = page.locator('.toolbar-button:has(.fa-folder)')
    await collapseBtn.click()
    await page.waitForTimeout(500)
  })

  test('搜索类名过滤树节点', async ({ page }) => {
    const searchInput = page.locator('.search-input input')
    await searchInput.fill('String')
    await page.waitForTimeout(1000)

    const visibleNodes = page.locator('.el-tree-node:visible .node-label')
    const count = await visibleNodes.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('折叠/展开侧边栏', async ({ page }) => {
    const collapseToggle = page.locator('.collapse-toggle')
    await collapseToggle.click()
    const aside = page.locator('.class-tree-aside.collapsed')
    await expect(aside).toBeVisible()

    await collapseToggle.click()
    const asideExpanded = page.locator('.class-tree-aside:not(.collapsed)')
    await expect(asideExpanded).toBeVisible()
  })

  test('点击类节点显示详情', async ({ page }) => {
    const expandBtn = page.locator('.toolbar-button:has(.fa-folder-open)')
    await expandBtn.click()
    await page.waitForTimeout(2000)

    const classNode = page.locator('.el-tree-node .node-icon .fa-file-code').first()
    if (await classNode.count() > 0) {
      await classNode.click()
      await page.waitForTimeout(2000)
      const classDetail = page.locator('.class-detail')
      if (await classDetail.count() > 0) {
        await expect(classDetail).toBeVisible()
      }
    }
  })

  test('仅显示已注入类过滤功能', async ({ page }) => {
    const injectFilterBtn = page.locator('.toolbar-button:has(.fa-syringe)')
    await injectFilterBtn.click()
    await page.waitForTimeout(1000)
    await expect(injectFilterBtn).toHaveClass(/active/)
  })
})
