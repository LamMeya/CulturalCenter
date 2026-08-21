// pages/profile/profile.js — 我的个人中心
const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    teamInfo: null,
    loading: false
  },

  onShow() {
    this.checkLoginStatus();
    if (app.globalData.userId) {
      this.loadProfile();
    }
  },

  /**
   * 检查登录状态
   */
  checkLoginStatus() {
    const isLoggedIn = !!(app.globalData.token && app.globalData.userId);
    this.setData({
      isLoggedIn,
      userInfo: isLoggedIn ? app.globalData.userInfo : null
    });
  },

  /**
   * 加载用户资料
   */
  async loadProfile() {
    const userId = app.globalData.userId;
    if (!userId) return;

    this.setData({ loading: true });

    try {
      const res = await api.get(`/api/users/${userId}/profile`);
      const profile = res.profile || res.data || res;
      this.setData({
        userInfo: {
          nickname: profile.nickname || '用户',
          phone: profile.phone || '',
          avatar: profile.avatar || ''
        },
        teamInfo: profile.team || null,
        loading: false
      });

      // 更新全局数据
      app.globalData.userInfo = profile;
      wx.setStorageSync('userInfo', profile);
    } catch (err) {
      console.error('加载用户资料失败:', err);
      this.setData({ loading: false });
    }
  },

  /**
   * 微信登录
   */
  async onLogin() {
    wx.showLoading({ title: '登录中...', mask: true });
    try {
      const userInfo = await app.wxLogin();
      this.setData({
        isLoggedIn: true,
        userInfo: userInfo
      });
      wx.hideLoading();
      wx.showToast({ title: '登录成功', icon: 'success' });
      this.loadProfile();
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '登录失败，请重试', icon: 'none' });
      console.error('登录失败:', err);
    }
  },

  /**
   * 绑定手机号
   */
  onGetPhoneNumber(e) {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '取消授权', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '绑定中...', mask: true });
    app.bindPhoneNumber(e.detail.encryptedData, e.detail.iv)
      .then((res) => {
        wx.hideLoading();
        wx.showToast({ title: '手机号绑定成功', icon: 'success' });
        this.loadProfile();
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({ title: '绑定失败，请重试', icon: 'none' });
        console.error('绑定手机号失败:', err);
      });
  },

  /**
   * 退出登录
   */
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '退出后需要重新登录',
      confirmColor: '#d4644a',
      success: (res) => {
        if (res.confirm) {
          app.globalData.token = null;
          app.globalData.userInfo = null;
          app.globalData.userId = null;
          app.globalData.openid = null;
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          this.setData({
            isLoggedIn: false,
            userInfo: null,
            teamInfo: null
          });
          wx.showToast({ title: '已退出登录', icon: 'none' });
        }
      }
    });
  },

  /**
   * 跳转团队页面
   */
  onGoToTeam() {
    wx.switchTab({ url: '/pages/team/team' });
  },

  /**
   * 跳转预约记录
   */
  onGoToBookings() {
    wx.switchTab({ url: '/pages/bookings/bookings' });
  }
});