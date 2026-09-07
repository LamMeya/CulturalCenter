/**
 * doumen-culture-admin — 统一 API Client
 * 约定：所有 /api/* 接口统一返回 { code, message, data }
 *   code === 0 → resolve(data)，拿到真实业务对象/数组
 *   code !== 0 → reject({ code, message })，并自动 toast 错误
 *   401       → 清理本地登录态 + reject
 *
 * 用法：
 *   window.AdminAPI.get('/venues').then(list => {...}).catch(err => {...})
 *   window.AdminAPI.post('/venues', {...}).then(data => {...})
 *   window.AdminAPI.put('/venues/1', {...}).then(data => {...})
 *   window.AdminAPI.del('/venues/1').then(data => {...})
 *
 * 如想自定义错误提示：AdminAPI.get(..., { silent: true })
 */
(function () {
  const DEFAULT_BASE = 'http://meyastudio.cn';
  // 本地调试：window.__ADMIN_API_BASE = 'http://localhost:8000'
  // 跨域：浏览器与页面同域则用 ''，跨域则写完整域名
  const BASE = (typeof window.__ADMIN_API_BASE !== 'undefined' && window.__ADMIN_API_BASE)
    ? window.__ADMIN_API_BASE.replace(/\/$/, '')
    : (location.port === '5173' || location.protocol === 'file:' ? DEFAULT_BASE : '');

  function token() {
    try {
      return localStorage.getItem('admin_token') || '';
    } catch (e) {
      return '';
    }
  }
  function saveToken(t) {
    try { localStorage.setItem('admin_token', t || ''); } catch (e) {}
  }
  function clearToken() {
    try { localStorage.removeItem('admin_token'); } catch (e) {}
  }
  function toast(msg, type) {
    // 简易 toast；如果页面引入了第三方/自定义 toast 可替换
    try {
      const root = document.body || document.documentElement;
      const el = document.createElement('div');
      el.textContent = msg || '';
      const bg = (type === 'success') ? '#3d8e7a'
        : (type === 'error') ? '#d4644a'
        : (type === 'warn') ? '#e8a840' : '#211c18';
      el.style.cssText = [
        'position:fixed', 'z-index:9999', 'left:50%', 'top:40px',
        'transform:translateX(-50%)', 'background:' + bg, 'color:#fff',
        'padding:10px 18px', 'border-radius:8px', 'box-shadow:0 6px 20px rgba(0,0,0,0.15)',
        'font:14px/1.4 -apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif',
        'opacity:0', 'transition:opacity .2s ease', 'max-width:80%'
      ].join(';');
      root.appendChild(el);
      requestAnimationFrame(() => { el.style.opacity = '1'; });
      setTimeout(() => {
        el.style.opacity = '0';
        setTimeout(() => { try { root.removeChild(el); } catch (e) {} }, 250);
      }, 2400);
    } catch (e) {}
  }

  function toQueryString(obj) {
    if (!obj) return '';
    const parts = [];
    Object.keys(obj).forEach(k => {
      const v = obj[k];
      if (v === undefined || v === null || v === '') return;
      parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(String(v)));
    });
    return parts.join('&');
  }

  async function request(method, path, opts) {
    opts = opts || {};
    const silent = !!opts.silent;
    const needsAuth = opts.requiresAuth !== false;
    const query = toQueryString(opts.query);
    const isApiPath = /^\/api\//.test(path) || /^\/api$/.test(path);
    const fullPath = (query ? (path + (path.indexOf('?') >= 0 ? '&' : '?') + query) : path);
    const url = /^https?:\/\//.test(fullPath)
      ? fullPath
      : BASE + fullPath;

    const headers = Object.assign({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }, opts.headers || {});
    if (needsAuth) {
      const t = token();
      if (t) headers['Authorization'] = 'Bearer ' + t;
    }

    const reqOpts = { method: method, headers: headers, credentials: 'include' };
    if (opts.body !== undefined && opts.body !== null) {
      if (typeof opts.body === 'string' || opts.body instanceof FormData || opts.body instanceof URLSearchParams) {
        reqOpts.body = opts.body;
        if (!(opts.body instanceof FormData) && headers['Content-Type']) {
          // 保持 application/json；FormData 让浏览器设置 multipart
        }
        if (opts.body instanceof FormData) delete headers['Content-Type'];
      } else {
        reqOpts.body = JSON.stringify(opts.body);
      }
    }

    let response;
    try {
      response = await fetch(url, reqOpts);
    } catch (err) {
      if (!silent) toast('网络异常，请检查网络或重试', 'error');
      throw { code: -1, message: '网络异常', raw: err };
    }

    const status = response.status;
    let body = null;
    const text = await response.text();
    try { body = text ? JSON.parse(text) : null; } catch (_) { body = text; }

    if (status === 401) {
      clearToken();
      if (!silent) toast('登录已过期，请重新登录', 'error');
      const msg = (body && (body.message || body.detail)) || '登录已过期';
      throw { code: 401, message: msg, raw: body };
    }

    if (status < 200 || status >= 300) {
      const msg = (body && (body.message || body.detail)) || ('请求失败(' + status + ')');
      if (!silent) toast(msg, 'error');
      throw { code: body && body.code ? body.code : status, message: msg, raw: body };
    }

    // 2xx：按统一 { code, message, data } 结构处理
    // 无包装结构（健康检查、HTML）则原样透传
    if (isApiPath && body && typeof body === 'object' &&
        ('code' in body) && ('message' in body) && ('data' in body)) {
      if (body.code === 0) {
        return body.data;
      }
      const msg = body.message || '请求失败';
      if (!silent) toast(msg, 'error');
      throw { code: body.code, message: msg, raw: body };
    }
    return body;
  }

  const Client = {
    BASE_URL: BASE,
    token: token,
    saveToken: saveToken,
    clearToken: clearToken,
    toast: toast,
    request: request,
    get: (path, opts) => request('GET', path, opts),
    post: (path, body, opts) => request('POST', path, Object.assign({}, opts, { body: body })),
    put: (path, body, opts) => request('PUT', path, Object.assign({}, opts, { body: body })),
    patch: (path, body, opts) => request('PATCH', path, Object.assign({}, opts, { body: body })),
    del: (path, opts) => request('DELETE', path, opts),
    upload: async (path, file) => {
      const fd = new FormData();
      fd.append('file', file);
      return request('POST', path, { body: fd, headers: { 'Accept': 'application/json' } });
    }
  };

  window.AdminAPI = Client;
})();
