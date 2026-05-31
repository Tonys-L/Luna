import { get, post, put, del } from './request'

const API_BASE = '/api'

function apiUrl(path) {
  return `${API_BASE}${path}`
}

export async function getClassTree() {
  try {
    const data = await get(apiUrl('/classes'))
    return data
  } catch (error) {
    console.error('获取类树数据失败:', error)
    throw error
  }
}

export async function getClassAnalysis(className) {
  try {
    const data = await get(apiUrl('/analysis'), { class: className })
    return data
  } catch (error) {
    console.error('获取类分析信息失败:', error)
    throw error
  }
}

export async function getDecompiledCode(className) {
  try {
    const data = await get(apiUrl('/decompile'), { class: className })
    if (data && data.decompiled !== undefined) {
      return data.decompiled
    }
    return ''
  } catch (error) {
    console.error('获取反编译代码失败:', error)
    return `// Failed to decompile: ${error.message}\n// Class: ${className}`
  }
}

export async function injectMethodLog(injectionData) {
  try {
    const data = await post(apiUrl('/injections'), injectionData)
    return data
  } catch (error) {
    console.error('方法注入失败:', error)
    throw error
  }
}

export async function getRules() {
  try {
    const data = await get(apiUrl('/rules'))
    return data || []
  } catch (error) {
    console.error('获取规则列表失败:', error)
    throw error
  }
}

export async function addRule(rule) {
  try {
    const data = await post(apiUrl('/rules'), rule)
    return data
  } catch (error) {
    console.error('添加规则失败:', error)
    throw error
  }
}

export async function updateRule(id, rule) {
  try {
    const data = await put(apiUrl(`/rules/${id}`), rule)
    return data
  } catch (error) {
    console.error('更新规则失败:', error)
    throw error
  }
}

export async function deleteRule(id) {
  try {
    const data = await del(apiUrl(`/rules/${id}`))
    return data
  } catch (error) {
    console.error('删除规则失败:', error)
    throw error
  }
}

export async function getStatus() {
  try {
    const data = await get(apiUrl('/status'))
    return data
  } catch (error) {
    console.error('获取状态失败:', error)
    return { status: 'offline' }
  }
}

export async function getLineNumbers(className) {
  try {
    const data = await get(apiUrl('/line-numbers'), { class: className })
    return data || {}
  } catch (error) {
    console.error('获取行号表失败:', error)
    return {}
  }
}

export async function getLocalVariables(className, methodName, methodDesc, lineNumber) {
  try {
    const params = { class: className, method: methodName, line: lineNumber }
    if (methodDesc) {
      params.desc = methodDesc
    }
    const data = await get(apiUrl('/local-variables'), params)
    return data || { variables: [] }
  } catch (error) {
    console.error('获取局部变量表失败:', error)
    return { variables: [] }
  }
}

export async function getInjectionList(className) {
  try {
    const data = await get(apiUrl('/injections/list'), { class: className })
    return data || { injections: [] }
  } catch (error) {
    console.error('获取注入点列表失败:', error)
    return { injections: [] }
  }
}

export async function removeInjection(id) {
  try {
    const data = await del(apiUrl(`/injections/${id}`))
    return data
  } catch (error) {
    console.error('删除注入点失败:', error)
    throw error
  }
}

export async function getJvmMetrics() {
  try {
    const data = await get(apiUrl('/metrics/jvm'))
    return data
  } catch (error) {
    console.error('获取 JVM 指标失败:', error)
    throw error
  }
}

export async function getThreadDump() {
  try {
    const data = await get(apiUrl('/metrics/threads'))
    return data
  } catch (error) {
    console.error('获取线程堆栈失败:', error)
    throw error
  }
}

export async function getPlugins() {
  try {
    const data = await get(apiUrl('/plugins'))
    return data || []
  } catch (error) {
    console.error('获取插件列表失败:', error)
    throw error
  }
}

export async function getPluginDetail(pluginId) {
  try {
    const data = await get(apiUrl(`/plugins/${pluginId}`))
    return data
  } catch (error) {
    console.error('获取插件详情失败:', error)
    throw error
  }
}

export async function disablePlugin(pluginId) {
  try {
    const data = await post(apiUrl(`/plugins/${pluginId}/disable`))
    return data
  } catch (error) {
    console.error('禁用插件失败:', error)
    throw error
  }
}

export async function enablePlugin(pluginId) {
  try {
    const data = await post(apiUrl(`/plugins/${pluginId}/enable`))
    return data
  } catch (error) {
    console.error('启用插件失败:', error)
    throw error
  }
}

export async function unloadPlugin(pluginId) {
  try {
    const data = await post(apiUrl(`/plugins/${pluginId}/unload`))
    return data
  } catch (error) {
    console.error('卸载插件失败:', error)
    throw error
  }
}

export async function searchPlugins(keyword) {
  try {
    const data = await get(apiUrl('/plugins/market/search'), { keyword })
    return data || []
  } catch (error) {
    console.error('搜索插件失败:', error)
    throw error
  }
}

export async function installPlugin(pluginId) {
  try {
    const data = await post(apiUrl(`/plugins/market/install/${pluginId}`))
    return data
  } catch (error) {
    console.error('安装插件失败:', error)
    throw error
  }
}

export async function getPluginConfig(pluginId) {
  try {
    const data = await get(apiUrl(`/plugins/${pluginId}/config`))
    return data || {}
  } catch (error) {
    console.error('获取插件配置失败:', error)
    throw error
  }
}

export async function savePluginConfig(pluginId, config) {
  try {
    const data = await put(apiUrl(`/plugins/${pluginId}/config`), config)
    return data
  } catch (error) {
    console.error('保存插件配置失败:', error)
    throw error
  }
}

export async function checkPluginUpdate(pluginId) {
  try {
    const data = await post(apiUrl(`/plugins/market/plugins/${pluginId}/update`))
    return data
  } catch (error) {
    console.error('检查插件更新失败:', error)
    throw error
  }
}

export async function getProbes() {
  try {
    const data = await get(apiUrl('/probes'))
    return data || []
  } catch (error) {
    console.error('获取探针列表失败:', error)
    return []
  }
}

export async function getEngines() {
  try {
    const data = await get(apiUrl('/probes/engines'))
    return data || []
  } catch (error) {
    console.error('获取引擎列表失败:', error)
    return []
  }
}
