/**
 * API 请求工具 — 文化中心
 * 封装 wx.request，统一处理请求、token、错误
 * —— 2026.09 后端统一返回 { code, message, data } 包装格式，本工具自动解包 data 返回，
 *    上层调用拿到的就是「真实业务数据」（数组或对象，不再是外层包裹结构）。
 * —— 2026.09 修复：模块加载期不再调用 getApp()（App 实例可能未就绪，返回 undefined 导致
 *    Cannot read properties of undefined (reading 'globalData')），改为运行期按需获取并加
 *    空值保护；apiBase / token 优先从 storage 与默认常量读取。
 */

// 默认 API 基址（兜底用，避免 getApp() 未就绪时取不到）
const DEFAULT_API_BASE = 'http://meyastudio.cn/api';

/**
 * 安全获取 App 实例；App 未就绪时返回 null（不再直接抛错）
 */
function safeGetApp() {
  try {
    if (typeof getApp === 'function') {
      const a = getApp();
      return a || null;
    }
  } catch (e) {
    // 某些加载阶段调用 getApp 会抛错
  }
  return null;
}

/**
 * 获取完整请求 URL
 */
function getBaseUrl() {
  const a = safeGetApp();
  if (a && a.globalData && a.globalData.apiBase) {
    return a.globalData.apiBase;
  }
  // 兜底：从 storage 或常量取
  const stored = wx.getStorageSync('apiBase');
  if (stored) return stored;
  return DEFAULT_API_BASE;
}

/**
 * 获取请求头：token 优先从 storage 读，其次再从 globalData 读
 */
function getHeaders() {
  const headers = {
    'Content-Type': 'application/json'
  };
  let token = wx.getStorageSync('token');
  if (!token) {
    const a = safeGetApp();
    if (a && a.globalData) token = a.globalData.token;
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * 清理登录态（401 时调用）—— 加空值保护，App 未就绪时只清 storage
 */
function clearLoginState() {
  try {
    const a = safeGetApp();
    if (a && a.globalData) {
      a.globalData.token = null;
      a.globalData.userInfo = null;
      a.globalData.userId = null;
    }
  } catch (e) {}
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
}

/**
 * 对后端返回的 { code, message, data } 做统一处理
 *   code === 0  → 返回 data 字段（真实数据）
 *   code !== 0  → 抛错（message 为错误文案）
 *   401         → 清理登录态并抛错
 */
function _unwrapResponse(res, resolve, reject) {
  const body = res.data;
  // 401 处理
  if (res.statusCode === 401) {
    clearLoginState();
    wx.showToast({
      title: '登录已过期，请重新登录',
      icon: 'none',
      duration: 2000
    });
    reject(body || { message: '登录已过期' });
    return;
  }
  if (res.statusCode < 200 || res.statusCode >= 300) {
    // FastAPI 的错误格式 { detail: ... } 或 App 兼容 { message: ... }
    const msg = (body && (body.message || body.detail)) || `请求失败(${res.statusCode})`;
    wx.showToast({ title: msg, icon: 'none', duration: 2000 });
    reject(body || { message: msg, code: res.statusCode });
    return;
  }
  // 2xx 状态：优先按 { code, message, data } 结构处理
  // 如果后端返回的不是标准结构（健康检查、上传文件等特殊接口），直接返回 body 原样
  if (body && typeof body === 'object' && 'code' in body && 'data' in body && 'message' in body) {
    if (body.code === 0) {
      resolve(body.data);
    } else {
      const msg = body.message || '请求失败';
      wx.showToast({ title: msg, icon: 'none', duration: 2000 });
      reject({ code: body.code, message: msg, raw: body });
    }
  } else {
    // 非标准结构（/api/health、HTML、上传接口等），原样透传
    resolve(body);
  }
}

/**
 * 发起请求
 * @param {string} method - HTTP 方法
 * @param {string} url - 请求路径（会自动拼上 baseURL）
 * @param {object} data - 请求参数（GET 时作为 query string，其他作为 body JSON）
 * @returns {Promise<any>} 成功时 resolve 真实 data；失败 reject { code, message, ... }
 */
function request(method, url, data) {
  return new Promise((resolve, reject) => {
    const reqOpts = {
      url: `${getBaseUrl()}${url}`,
      method: method,
      header: getHeaders(),
      success(res) {
        _unwrapResponse(res, resolve, reject);
      },
      fail(err) {
        wx.showToast({
          title: '网络异常，请检查网络连接',
          icon: 'none',
          duration: 2000
        });
        reject(err);
      }
    };
    const isGet = method === 'GET';
    if (isGet) {
      // GET 时 data 自动转 query params（wx.request 原生支持，但这里显式传 data 保证兼容性）
      if (data && typeof data === 'object' && Object.keys(data).length > 0) {
        reqOpts.data = data;
      }
    } else {
      reqOpts.data = data;
    }
    wx.request(reqOpts);
  });
}

/** GET 请求 */
function get(url, params) {
  return request('GET', url, params);
}

/** POST 请求 */
function post(url, data) {
  return request('POST', url, data);
}

/** PUT 请求 */
function put(url, data) {
  return request('PUT', url, data);
}

/** DELETE 请求 */
function del(url, data) {
  return request('DELETE', url, data);
}

module.exports = {
  get,
  post,
  put,
  del,
  // 暴露给 app.js 里 wx.request 直调场景使用的解包函数（统一逻辑）
  unwrap: _unwrapResponse
};
