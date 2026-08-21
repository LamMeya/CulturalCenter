App({
  globalData: {
    userInfo: null,
    userId: null,
    openid: null,
    token: null,
    apiBase: 'http://localhost:8000/api'
  },

  onLaunch() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.userId = userInfo.user_id;
    }
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

  // 微信登录
  wxLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            wx.getUserProfile({
              desc: '用于完善用户资料',
              success: (profileRes) => {
                const nickname = profileRes.userInfo.nickName;
                wx.request({
                  url: `${this.globalData.apiBase}/users/login/miniprogram`,
                  method: 'POST',
                  data: { code: res.code, nickname: nickname },
                  success: (apiRes) => {
                    if (apiRes.data.user_id) {
                      this.globalData.userId = apiRes.data.user_id;
                      this.globalData.userInfo = apiRes.data;
                      this.globalData.openid = apiRes.data.openid;
                      wx.setStorageSync('userInfo', apiRes.data);
                      resolve(apiRes.data);
                    } else {
                      reject(apiRes.data);
                    }
                  },
                  fail: reject
                });
              },
              fail: () => {
                wx.request({
                  url: `${this.globalData.apiBase}/users/login/miniprogram`,
                  method: 'POST',
                  data: { code: res.code, nickname: '微信用户' },
                  success: (apiRes) => {
                    this.globalData.userId = apiRes.data.user_id;
                    this.globalData.userInfo = apiRes.data;
                    wx.setStorageSync('userInfo', apiRes.data);
                    resolve(apiRes.data);
                  },
                  fail: reject
                });
              }
            });
          } else {
            reject(res);
          }
        },
        fail: reject
      });
    });
  },

  bindPhoneNumber(encryptedData, iv) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${this.globalData.apiBase}/users/bind-phone`,
        method: 'POST',
        data: { user_id: this.globalData.userId, encrypted_data: encryptedData, iv: iv },
        success: resolve,
        fail: reject
      });
    });
  }
});
