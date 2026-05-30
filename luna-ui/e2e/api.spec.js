import { test, expect } from '@playwright/test'

const TARGET_CLASS = 'fun.efto.luna.demo.service.UserService'
const TARGET_METHOD = 'createUser'
const TARGET_DESC = '(Ljava/lang/String;I)Lfun/efto/luna/demo/model/User;'

test.describe('Luna - 类浏览与反编译', () => {

  test('GET /api/classes 返回包含 demo 类的类列表', async ({ request }) => {
    const response = await request.get('/api/classes')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data).toBeDefined()
    const appClasses = json.data['sun.misc.Launcher$AppClassLoader']
    expect(appClasses).toBeDefined()
    const found = appClasses.some(c => c.className === TARGET_CLASS)
    expect(found).toBeTruthy()
  })

  test('GET /api/decompile 反编译 UserService', async ({ request }) => {
    const response = await request.get(`/api/decompile?class=${TARGET_CLASS}`)
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.decompiled).toBeDefined()
    expect(json.data.decompiled).toContain('package fun.efto.luna.demo.service')
    expect(json.data.decompiled).toContain('public class UserService')
    expect(json.data.decompiled).toContain('createUser')
  })

  test('GET /api/analysis 分析 UserService 类结构', async ({ request }) => {
    const response = await request.get(`/api/analysis?class=${TARGET_CLASS}`)
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.className).toBe(TARGET_CLASS)
    expect(json.data.methods).toBeDefined()
    const createUser = json.data.methods.find(m => m.name === 'createUser')
    expect(createUser).toBeDefined()
    expect(createUser.descriptor).toBe('(Ljava/lang/String;I)Lfun/efto/luna/demo/model/User;')
  })

  test('GET /api/line-numbers 获取 UserService 行号表', async ({ request }) => {
    const response = await request.get(`/api/line-numbers?class=${TARGET_CLASS}`)
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    const createUserKey = Object.keys(json.data).find(k => k.includes('createUser'))
    expect(createUserKey).toBeDefined()
    const lines = json.data[createUserKey]
    expect(Array.isArray(lines)).toBeTruthy()
    expect(lines.length).toBeGreaterThan(0)
  })

  test('GET /api/local-variables 获取局部变量表', async ({ request }) => {
    const response = await request.get(`/api/local-variables?class=${TARGET_CLASS}&method=createUser&line=20`)
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.variables).toBeDefined()
    expect(json.data.variables.length).toBeGreaterThan(0)
    const thisVar = json.data.variables.find(v => v.name === 'this')
    expect(thisVar).toBeDefined()
    expect(thisVar.slot).toBe(0)
  })

  test('GET /api/decompile 缺少 class 参数返回错误', async ({ request }) => {
    const response = await request.get('/api/decompile')
    expect(response.status()).toBeGreaterThanOrEqual(400)
  })
})

test.describe('Luna - 方法级注入', () => {

  test('POST /api/injections METHOD_ENTER log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'log:Method entered: $1, age: $2',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.injectionPointId).toBeDefined()

    const listResp = await request.get(`/api/injections/list?class=${TARGET_CLASS}`)
    const listJson = await listResp.json()
    const found = listJson.data.injections.some(i => i.id === json.data.injectionPointId)
    expect(found).toBeTruthy()

    const removeResp = await request.delete(`/api/injections/${json.data.injectionPointId}`)
    expect(removeResp.ok()).toBeTruthy()
  })

  test('POST /api/injections METHOD_EXIT log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_EXIT',
        codeType: 'EXPRESSION',
        code: 'log:Method exited',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('POST /api/injections METHOD_AROUND log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_AROUND',
        codeType: 'EXPRESSION',
        code: 'log:Around: $1',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('POST /api/injections METHOD_ENTER snapshot 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'snapshot:true',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })
})

test.describe('Luna - 行号级注入', () => {

  test('POST /api/injections LINE_BEFORE log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_BEFORE',
        codeType: 'EXPRESSION',
        code: 'log:Before line 20',
        desc: TARGET_DESC,
        lineNumber: 20
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.injectionPointId).toBeDefined()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('POST /api/injections LINE_AFTER log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_AFTER',
        codeType: 'EXPRESSION',
        code: 'log:After line 20',
        desc: TARGET_DESC,
        lineNumber: 20
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('POST /api/injections LINE_BEFORE snapshot 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_BEFORE',
        codeType: 'EXPRESSION',
        code: 'snapshot:true',
        desc: TARGET_DESC,
        lineNumber: 21
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('POST /api/injections LINE_BEFORE 条件注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_BEFORE',
        codeType: 'EXPRESSION',
        code: '${param[2] >= 18}::log:Adult user: $1',
        desc: TARGET_DESC,
        lineNumber: 20
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()

    await request.delete(`/api/injections/${json.data.injectionPointId}`)
  })

  test('行号注入缺少 method 参数返回错误', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: '',
        injectionType: 'LINE_BEFORE',
        codeType: 'EXPRESSION',
        code: 'log:test',
        desc: TARGET_DESC,
        lineNumber: 20
      }
    })
    expect(response.ok()).toBeFalsy()
  })
})

test.describe('Luna - 注入生命周期', () => {

  test('注入 -> 查询列表 -> 移除 -> 查询列表', async ({ request }) => {
    const injectResp = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'log:Lifecycle test',
        desc: TARGET_DESC
      }
    })
    expect(injectResp.ok()).toBeTruthy()
    const injectJson = await injectResp.json()
    const injectionId = injectJson.data.injectionPointId
    expect(injectionId).toBeDefined()

    const listResp1 = await request.get(`/api/injections/list?class=${TARGET_CLASS}`)
    const listJson1 = await listResp1.json()
    expect(listJson1.data.injections.some(i => i.id === injectionId)).toBeTruthy()

    const removeResp = await request.delete(`/api/injections/${injectionId}`)
    expect(removeResp.ok()).toBeTruthy()
    const removeJson = await removeResp.json()
    expect(removeJson.data.success).toBeTruthy()
    expect(removeJson.data.removedId).toBe(injectionId)

    const listResp2 = await request.get(`/api/injections/list?class=${TARGET_CLASS}`)
    const listJson2 = await listResp2.json()
    expect(listJson2.data.injections.some(i => i.id === injectionId)).toBeFalsy()
  })

  test('移除不存在的注入点返回错误', async ({ request }) => {
    const response = await request.delete('/api/injections/non-existent-id-12345')
    expect(response.ok()).toBeFalsy()
  })

  test('dry-run 预览注入不实际执行', async ({ request }) => {
    const response = await request.post('/api/injections/dry-run', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'log:Dry run test',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.bytecodeSize).toBeDefined()
    expect(json.data.originalSize).toBeDefined()
    expect(json.data.bytecodeSize).toBeGreaterThan(json.data.originalSize)
  })
})

test.describe('Luna - 规则 CRUD', () => {

  let ruleId

  test.afterEach(async ({ request }) => {
    if (ruleId) {
      await request.delete(`/api/rules/${ruleId}`)
      ruleId = null
    }
  })

  test('POST /api/rules 创建规则', async ({ request }) => {
    const response = await request.post('/api/rules', {
      data: {
        targetClass: TARGET_CLASS,
        targetMethod: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        expression: '',
        logContent: 'E2E test rule',
        enabled: true,
        codeType: 'EXPRESSION'
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.id).toBeDefined()
    ruleId = json.data.id
  })

  test('GET /api/rules 获取规则列表', async ({ request }) => {
    const createResp = await request.post('/api/rules', {
      data: {
        targetClass: TARGET_CLASS,
        targetMethod: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        expression: '',
        logContent: 'List test rule',
        enabled: true,
        codeType: 'EXPRESSION'
      }
    })
    const createJson = await createResp.json()
    ruleId = createJson.data.id

    const listResp = await request.get('/api/rules')
    expect(listResp.ok()).toBeTruthy()
    const listJson = await listResp.json()
    expect(listJson.success).toBeTruthy()
    expect(Array.isArray(listJson.data)).toBeTruthy()
    expect(listJson.data.length).toBeGreaterThan(0)
  })

  test('DELETE /api/rules/{id} 删除规则', async ({ request }) => {
    const createResp = await request.post('/api/rules', {
      data: {
        targetClass: TARGET_CLASS,
        targetMethod: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        expression: '',
        logContent: 'Delete test rule',
        enabled: true,
        codeType: 'EXPRESSION'
      }
    })
    const createJson = await createResp.json()
    const id = createJson.data.id

    const deleteResp = await request.delete(`/api/rules/${id}`)
    expect(deleteResp.ok()).toBeTruthy()

    const getResp = await request.get(`/api/rules/${id}`)
    expect(getResp.ok()).toBeFalsy()
  })

  test('GET /api/rules/{id} 不存在返回 404', async ({ request }) => {
    const response = await request.get('/api/rules/999999')
    expect(response.status()).toBe(404)
  })
})

test.describe('Luna - 系统状态与指标', () => {

  test('GET /api/status 返回运行状态', async ({ request }) => {
    const response = await request.get('/api/status')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.status).toBe('running')
    expect(json.data.version).toBeDefined()
  })

  test('GET /api/capabilities 返回核心能力清单', async ({ request }) => {
    const response = await request.get('/api/capabilities')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.coreCapabilities).toBeDefined()
    expect(json.data.coreCapabilities.length).toBeGreaterThanOrEqual(6)
    expect(json.data.plugins).toBeDefined()
    expect(json.data.plugins.length).toBeGreaterThanOrEqual(4)
  })

  test('GET /api/metrics/jvm 返回 JVM 指标含内存信息', async ({ request }) => {
    const response = await request.get('/api/metrics/jvm')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.memory).toBeDefined()
    expect(json.data.memory.heapUsed).toBeGreaterThan(0)
    expect(json.data.memory.heapMax).toBeGreaterThan(0)
    expect(json.data.classes).toBeDefined()
    expect(json.data.classes.loadedCount).toBeGreaterThan(0)
    expect(json.data.threads).toBeDefined()
  })

  test('GET /api/metrics/threads 返回线程信息', async ({ request }) => {
    const response = await request.get('/api/metrics/threads')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
  })

  test('GET /api/test/health 健康检查', async ({ request }) => {
    const response = await request.get('/api/test/health')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.status).toBeDefined()
    expect(json.data.timestamp).toBeDefined()
  })
})

test.describe('Luna - 模板管理', () => {
  test('GET /api/templates 返回模板列表', async ({ request }) => {
    const response = await request.get('/api/templates')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(Array.isArray(json.data)).toBeTruthy()
  })

  test('GET /api/templates/categories 返回按分类分组', async ({ request }) => {
    const response = await request.get('/api/templates/categories')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(typeof json.data).toBe('object')
  })

  test('GET /api/templates/{name} 不存在返回 404', async ({ request }) => {
    const response = await request.get('/api/templates/nonexistent-template-xyz')
    expect(response.status()).toBe(404)
  })

  test('POST /api/templates/apply 应用模板', async ({ request }) => {
    const listResp = await request.get('/api/templates')
    const listJson = await listResp.json()
    if (listJson.data && listJson.data.length > 0) {
      const template = listJson.data[0]
      const response = await request.post('/api/templates/apply', {
        data: {
          templateName: template.name,
          targetClass: TARGET_CLASS,
          targetMethod: TARGET_METHOD,
          parameters: {}
        }
      })
      expect(response.ok()).toBeTruthy()
      const json = await response.json()
      expect(json.success).toBeTruthy()
    }
  })
})

test.describe.skip('Luna - 插件管理', () => {
  test('GET /api/plugins 返回插件列表', async ({ request }) => {
    const response = await request.get('/api/plugins')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(Array.isArray(json.data)).toBeTruthy()
  })

  test('GET /api/plugins/ui-manifest 返回注入类型清单', async ({ request }) => {
    const response = await request.get('/api/plugins/ui-manifest')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.injectionTypes).toBeDefined()
    expect(json.data.expressionProtocols).toBeDefined()
    expect(json.data.templates).toBeDefined()
  })

  test('GET /api/plugins/{pluginId} 不存在返回 404', async ({ request }) => {
    const response = await request.get('/api/plugins/nonexistent-plugin-xyz')
    expect(response.status()).toBe(404)
  })

  test('GET /api/plugins/{pluginId}/config 获取配置', async ({ request }) => {
    const listResp = await request.get('/api/plugins')
    const listJson = await listResp.json()
    if (listJson.data && listJson.data.length > 0) {
      const pluginId = listJson.data[0].id
      const response = await request.get(`/api/plugins/${pluginId}/config`)
      expect(response.ok()).toBeTruthy()
      const json = await response.json()
      expect(json.success).toBeTruthy()
    }
  })
})

test.describe.skip('Luna - 插件市场', () => {
  test('GET /api/plugins/market/search 搜索插件', async ({ request }) => {
    const response = await request.get('/api/plugins/market/search?keyword=test')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
  })

  test('GET /api/plugins/market/check-updates 检查更新', async ({ request }) => {
    const response = await request.get('/api/plugins/market/check-updates')
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
  })
})

test.describe('Luna - 注入测试与验证', () => {
  test.skip('POST /api/injections/test 完整注入测试', async ({ request }) => {
    const response = await request.post('/api/injections/test', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'snapshot:true',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data.steps).toBeDefined()
    expect(json.data.steps.dryRun).toBeDefined()
    expect(json.data.steps.inject).toBeDefined()
    expect(json.data.steps.verify).toBeDefined()
  })

  test('POST /api/injections/verify 验证注入', async ({ request }) => {
    const response = await request.post('/api/injections/verify', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'log:verify test',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
  })

  test('DELETE /api/injections/{id} 删除注入点', async ({ request }) => {
    const injectResp = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'METHOD_ENTER',
        codeType: 'EXPRESSION',
        code: 'log:delete test',
        desc: TARGET_DESC
      }
    })
    const injectJson = await injectResp.json()
    if (injectJson.success && injectJson.data.injectionPointId) {
      const id = injectJson.data.injectionPointId
      const deleteResp = await request.delete(`/api/injections/${id}`)
      expect(deleteResp.ok()).toBeTruthy()
      const deleteJson = await deleteResp.json()
      expect(deleteJson.success).toBeTruthy()
    }
  })
})

test.describe.skip('Luna - 前后端 API 一致性', () => {
  test('POST /api/plugins/{id}/disable 端点存在性检查', async ({ request }) => {
    const response = await request.post('/api/plugins/test-plugin/disable')
    expect(response.status()).not.toBe(404)
  })

  test('POST /api/plugins/{id}/enable 端点存在性检查', async ({ request }) => {
    const response = await request.post('/api/plugins/test-plugin/enable')
    expect(response.status()).not.toBe(404)
  })

  test('POST /api/plugins/{id}/unload 路径风格检查', async ({ request }) => {
    const response = await request.post('/api/plugins/test-plugin/unload')
    const pathStyleWorks = response.status() !== 404
    if (!pathStyleWorks) {
      const queryStyleResp = await request.post('/api/plugins/unload?pluginId=test-plugin')
      const queryStyleWorks = queryStyleResp.status() !== 404
      expect(queryStyleWorks).toBeTruthy()
    }
  })
})

test.describe('Luna - 行号注入', () => {

  test('POST /api/injections LINE_BEFORE log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_BEFORE',
        lineNumber: 20,
        lineNumberOffset: 0,
        code: 'log:line before check',
        codeType: 'EXPRESSION',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data).toBeDefined()
    expect(json.data.injectionPointId).toBeDefined()
    if (json.data && json.data.injectionPointId) {
      await request.delete(`/api/injections/${json.data.injectionPointId}`)
    }
  })

  test('POST /api/injections LINE_AFTER log 注入', async ({ request }) => {
    const response = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_AFTER',
        lineNumber: 20,
        lineNumberOffset: 0,
        code: 'log:line after check',
        codeType: 'EXPRESSION',
        desc: TARGET_DESC
      }
    })
    expect(response.ok()).toBeTruthy()
    const json = await response.json()
    expect(json.success).toBeTruthy()
    expect(json.data).toBeDefined()
    expect(json.data.injectionPointId).toBeDefined()
    if (json.data && json.data.injectionPointId) {
      await request.delete(`/api/injections/${json.data.injectionPointId}`)
    }
  })

  test('行号注入完整生命周期', async ({ request }) => {
    const createResp = await request.post('/api/injections', {
      data: {
        clazz: TARGET_CLASS,
        method: TARGET_METHOD,
        injectionType: 'LINE_BEFORE',
        lineNumber: 20,
        lineNumberOffset: 0,
        code: 'log:lifecycle test',
        codeType: 'EXPRESSION',
        desc: TARGET_DESC
      }
    })
    expect(createResp.ok()).toBeTruthy()
    const createJson = await createResp.json()
    expect(createJson.success).toBeTruthy()
    const injectionId = createJson.data.injectionPointId
    expect(injectionId).toBeDefined()

    const listResp = await request.get(`/api/injections/list?class=${TARGET_CLASS}`)
    expect(listResp.ok()).toBeTruthy()
    const listJson = await listResp.json()
    expect(listJson.data.injections).toBeDefined()
    const found = listJson.data.injections.some(i => i.id === injectionId)
    expect(found).toBeTruthy()

    const deleteResp = await request.delete(`/api/injections/${injectionId}`)
    expect(deleteResp.ok()).toBeTruthy()

    const listResp2 = await request.get(`/api/injections/list?class=${TARGET_CLASS}`)
    expect(listResp2.ok()).toBeTruthy()
    const listJson2 = await listResp2.json()
    const stillExists = listJson2.data.injections.some(i => i.id === injectionId)
    expect(stillExists).toBeFalsy()
  })

  test('GET /api/line-numbers + GET /api/local-variables 联合验证', async ({ request }) => {
    const lineResp = await request.get(`/api/line-numbers?class=${TARGET_CLASS}`)
    expect(lineResp.ok()).toBeTruthy()
    const lineJson = await lineResp.json()
    expect(lineJson.success).toBeTruthy()
    const createUserKey = Object.keys(lineJson.data).find(k => k.includes('createUser'))
    expect(createUserKey).toBeDefined()
    const lines = lineJson.data[createUserKey]
    expect(Array.isArray(lines)).toBeTruthy()
    expect(lines.length).toBeGreaterThan(0)

    const localVarResp = await request.get(`/api/local-variables?class=${TARGET_CLASS}&method=createUser&line=${lines[0]}`)
    expect(localVarResp.ok()).toBeTruthy()
    const localVarJson = await localVarResp.json()
    expect(localVarJson.success).toBeTruthy()
    expect(localVarJson.data.variables).toBeDefined()
    expect(localVarJson.data.variables.length).toBeGreaterThan(0)
  })
})
