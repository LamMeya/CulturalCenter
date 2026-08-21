/**
 * API 请求工具 — 斗门文化中心
 * 封装 wx.request，统一处理请求、token、错误
 */

const app = getApp();

/**
 * 获取完整请求 URL
 */
function getBaseUrl() {
  return app.globalData.apiBase;
}

/**
 * 获取请求头
 */
function getHeaders() {
  const headers = {
    'Content-Type': 'application/json'
  };
  const token = app.globalData.token || wx.getStorageSync('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * 发起请求
 * @param {string} method - HTTP 方法
 * @param {string} url - 请求路径
 * @param {object} data - 请求参数（GET 时为 params，其他为 body）
 * @returns {Promise}
 */
function request(method, url, data) {
  return new Promise((resolve, reject) => {
    const isGet = method === 'GET';
    wx.request({
      url: `${getBaseUrl()}${url}`,
      method: method,
      data: data,
      header: getHeaders(),
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          // token 过期，清除登录状态
          app.globalData.token = null;
          app.globalData.userInfo = null;
          app.globalData.userId = null;
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          wx.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none',
            duration: 2000
          });
          reject(res.data);
        } else {
          wx.showToast({
            title: res.data.message || '请求失败',
            icon: 'none',
            duration: 2000
          });
          reject(res.data);
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络异常，请检查网络连接',
          icon: 'none',
          duration: 2000
        });
        reject(err);
      }
    });
  });
}

/**
 * GET 请求
 * @param {string} url - 请求路径
 * @param {object} params - 查询参数
 * @returns {Promise}
 */
function get(url, params) {
  return request('GET', url, params);
}

/**
 * POST 请求
 * @param {string} url - 请求路径
 * @param {object} data - 请求体
 * @returns {Promise}
 */
function post(url, data) {
  return request('POST', url, data);
}

/**
 * PUT 请求
 * @param {string} url - 请求路径
 * @param {object} data - 请求体
 * @returns {Promise}
 */
function put(url, data) {
  return request('PUT', url, data);
}

/**
 * DELETE 请求
 * @param {string} url - 请求路径
 * @param {object} data - 请求体（可选）
 * @returns {Promise}
 */
function del(url, data) {
  return request('DELETE', url, data);
}

module.exports = {
  get,
  post,
  put,
  del
};