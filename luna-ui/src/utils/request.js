/**
 * HTTP 请求封装模块
 */

/**
 * 统一请求方法
 * @param {string} url 请求地址（完整URL）
 * @param {Object} options 请求选项
 * @returns {Promise} 请求Promise
 */
async function request(url, options = {}) {
  // 默认选项
  const defaultOptions = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // 合并选项
  const config = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);
    
    // 检查响应状态
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    // 尝试解析JSON
    try {
      const data = await response.json();
      return data;
    } catch (jsonError) {
      // 如果不是JSON响应，返回文本
      const text = await response.text();
      return text;
    }
  } catch (error) {
    console.error('Request failed:', error);
    throw error;
  }
}

/**
 * GET 请求
 * @param {string} url 请求地址（完整URL）
 * @param {Object} params 查询参数
 * @returns {Promise} 请求Promise
 */
export function get(url, params = {}) {
  // 构造查询参数
  const queryString = new URLSearchParams(params).toString();
  const separator = url.includes('?') ? '&' : '?';
  const fullUrl = queryString ? `${url}${separator}${queryString}` : url;
  
  return request(fullUrl, {
    method: 'GET',
  });
}

/**
 * POST 请求
 * @param {string} url 请求地址（完整URL）
 * @param {Object} data 请求数据
 * @returns {Promise} 请求Promise
 */
export function post(url, data = {}) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * PUT 请求
 * @param {string} url 请求地址（完整URL）
 * @param {Object} data 请求数据
 * @returns {Promise} 请求Promise
 */
export function put(url, data = {}) {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

/**
 * DELETE 请求
 * @param {string} url 请求地址（完整URL）
 * @returns {Promise} 请求Promise
 */
export function del(url) {
  return request(url, {
    method: 'DELETE',
  });
}

// 导出基础请求方法
export { request };