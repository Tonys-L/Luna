import { get, post, put, del } from './request'

const API_BASE = 'http://localhost:8421/api'

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
    if (data && data.decompiled) {
      return data.decompiled.replace(/\\n/g, '\n').replace(/\\"/g, '"')
    }
    return data || ''
  } catch (error) {
    console.error('获取反编译代码失败:', error)
    return `// 获取反编译代码失败: ${error.message}\n// 类名: ${className}`
  }
}

export async function injectMethodLog(injectionData) {
  try {
    const data = await post(apiUrl('/inject'), injectionData)
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
