import {get} from './request';

/**
 * 获取类列表
 * @returns {Promise<Object>} 类列表数据
 */
export async function getClassList() {
  try {
    const data = await get('/api/classes');
    return data;
  } catch (error) {
    console.error('获取类列表失败:', error);
    throw error;
  }
}

/**
 * 获取类详细信息
 * @param {string} className 类名
 * @returns {Promise<Object>} 类详细信息
 */
export async function getClassInfo(className) {
  try {
    const classInfo = await get('/api/analysis', { class: className });
    return classInfo;
  } catch (error) {
    console.error('获取类详细信息失败:', error);
    throw error;
  }
}

/**
 * 获取反编译代码
 * @param {string} className 类名
 * @returns {Promise<string>} 反编译代码
 */
export async function getDecompiledCode(className) {
  try {
    const result = await get(`/api/decompile`, { class: className });
    
    if (result.error) {
      return `// 反编译失败: ${result.error}\n// 类名: ${className}`;
    }
    
    return result.decompiled || "// 未获取到反编译代码";
  } catch (error) {
    console.error('获取反编译代码失败:', error);
    return `// 获取反编译代码失败: ${error.message}\n// 类名: ${className}`;
  }
}

/**
 * 获取类树数据
 * @returns {Promise<Object>} 类树数据
 */
export async function getClassTree() {
  try {
    return data;
  } catch (error) {
    console.error('获取类树数据失败:', error);
    throw error;
  }
}

/**
 * 获取类分析信息
 * @param {string} className 类名
 * @returns {Promise<Object>} 类分析信息
 */
export async function getClassAnalysis(className) {
  try {
    const classInfo = await get('/api/analysis', { class: className });
    return classInfo;
  } catch (error) {
    console.error('获取类分析信息失败:', error);
    throw error;
  }
}