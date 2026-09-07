/**
 * 小程序入口
 * —— 2026.09 后端统一返回 { code: 0, message: "ok", data } 结构，
 *    phoneLogin / phoneRegister / wxLogin / bindPhoneNumber 都按此结构取 data 后再用。
 */
const apiUtils = require('./utils/api');

App({
  globalData: {
    userInfo: null,
    userId: null,
    openid: null,
    token: null,
    // 本地调试：模拟器访问 Mac 用 http://10.0.2.2:8000/api；真机/小程序要用 HTTPS + 真实域名
    // apiBase: 'http://10.0.2.2:8000/api'
    apiBase: 'http://192.168.0.109:8000/api'
    // apiBase: 'http://meyastudio.cn/api'
  },

  onLaunch() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.userId = userInfo.user_id || userInfo.id;
    }
  },

  /**
   * 根据后端返回的 { code, message, data } 结构 + login/register 的 data 字段存登录态
   * data 结构为 { token: "...", user: { id, nickname, phone, avatar_url, team_id... } }
   */
  _saveLoginState(payload) {
    const user = payload.user || {};
    const userInfo = {
      user_id: user.id || null,
      id: user.id || null,
      nickname: user.nickname || '',
      phone: user.phone || '',
      avatar_url: user.avatar_url || '',
      team_id: user.team_id || null,
      team_name: user.team_name || null,
      team_role: user.team_role || null
    };
    this.globalData.token = payload.token || 'token_placeholder';
    this.globalData.userId = userInfo.user_id;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', this.globalData.token);
    wx.setStorageSync('userInfo', userInfo);
    return userInfo;
  },

  // 检查登录状态：已登录直接 resolve，未登录弹窗引导去登录
  checkLogin() {
    return new Promise((resolve, reject) => {
      if (this.globalData.token && this.globalData.userId) {
        resolve(true);
        return;
      }
      wx.showModal({
        title: '请先登录',
        content: '预约场地和管理团队需要先登录',
        confirmText: '去登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.switchTab({ url: '/pages/profile/profile' });
          }
          reject({ code: 'NOT_LOGIN', message: '未登录' });
        }
      });
    });
  },

  // ——— 手机号注册（直接走 utils/api.request → 已经解包，拿到的就是 data = {token,user}）———
  phoneRegister(username, password, nickname, phone) {
    return apiUtils.post('/users/register', { username, password, nickname, phone }).then(data => {
      return this._saveLoginState(data);
    });
  },

  // ——— 手机号登录（同上，返回 data = {token,user} 后存状态）———
  phoneLogin(username, password) {
    return apiUtils.post('/users/login/password', { username, password }).then(data => {
      return this._saveLoginState(data);
    });
  },

  // ——— 微信登录（两个 success 分支都改为 apiUtils.post，已解包 data）———
  wxLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (!res.code) { reject(res); return; }

          const postMiniLogin = (nickname) => {
            return apiUtils.post('/users/login/miniprogram', {
              code: res.code,
              nickname: nickname || '微信用户'
            });
          };

          // 老版 getUserProfile（基础库 2.10.4+），失败时退化为昵称传空
          wx.getUserProfile({
            desc: '用于完善用户资料',
            success: (profileRes) => {
              postMiniLogin(profileRes.userInfo.nickName).then(data => {
                resolve(this._saveLoginState(data));
              }).catch(err => {
                // 业务错误已经在 unwrap 里 showToast 过，这里再 reject 给页面
                reject(err);
              });
            },
            fail: () => {
              postMiniLogin('微信用户').then(data => {
                resolve(this._saveLoginState(data));
              }).catch(reject);
            }
          });
        },
        fail: reject
      });
    });
  },

  // 绑定手机号
  bindPhoneNumber(encryptedData, iv) {
    return apiUtils.post('/users/bind-phone', {
      user_id: this.globalData.userId,
      encrypted_data: encryptedData,
      iv: iv
    });
  }
});
