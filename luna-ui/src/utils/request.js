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
    
    if (!response.ok) {
      let errorMsg = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        if (errorData.error) {
          errorMsg = errorData.error;
        } else if (errorData.message) {
          errorMsg = errorData.message;
        }
      } catch (e) {
        try {
          const errorText = await response.text();
          if (errorText) errorMsg = errorText;
        } catch (e2) {
          // ignore
        }
      }
      const error = new Error(errorMsg);
      error.status = response.status;
      throw error;
    }
    
    // 尝试解析JSON
    try {
      const data = await response.json();
      return unwrapApiResult(data);
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

function unwrapApiResult(data) {
  if (data && typeof data === 'object' && 'success' in data && 'data' in data) {
    if (!data.success) {
      const error = new Error(data.error || '请求失败');
      error.status = data.status || 400;
      throw error;
    }
    return data.data;
  }
  return data;
}