// pages/profile/profile.js — 个人中心
const api = require('../../utils/api');
// 安全获取 App 实例，未就绪时返回空壳 globalData，避免 getApp() 返回 undefined 导致崩溃
let _appInstance = null;
function getAppInstance() {
  if (!_appInstance) {
    try { _appInstance = getApp(); } catch (e) { _appInstance = null; }
  }
  return _appInstance || { globalData: {} };
}

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    teamInfo: null,
    loading: false,
    // 登录/注册切换
    authMode: 'login', // 'login' | 'register'
    username: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    phone: '',
    authLoading: false
  },

  onShow() {
    this.checkLoginStatus();
    if (getAppInstance().globalData.userId) {
      this.loadProfile();
    }
  },

  checkLoginStatus() {
    const gd = getAppInstance().globalData || {};
    const isLoggedIn = !!(gd.token && gd.userId);
    this.setData({
      isLoggedIn,
      userInfo: isLoggedIn ? gd.userInfo : null
    });
  },

  onSwitchAuthMode(e) {
    const mode = e.currentTarget.dataset.mode;
    this.setData({
      authMode: mode,
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      phone: ''
    });
  },

  onInputField(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },

  async onPhoneLogin() {
    const { username, password } = this.data;
    if (!username.trim()) { wx.showToast({ title: '请输入账号', icon: 'none' }); return; }
    if (!password || password.length < 4) { wx.showToast({ title: '密码至少4位', icon: 'none' }); return; }
    this.setData({ authLoading: true });
    try {
      const userInfo = await getAppInstance().phoneLogin(username.trim(), password);
      this.setData({ isLoggedIn: true, userInfo, authLoading: false });
      wx.showToast({ title: '登录成功', icon: 'success' });
      this.loadProfile();
    } catch (err) {
      this.setData({ authLoading: false });
      wx.showToast({ title: (err && err.message) || '登录失败', icon: 'none' });
    }
  },

  async onPhoneRegister() {
    const { username, password, confirmPassword, nickname, phone } = this.data;
    if (!username.trim()) { wx.showToast({ title: '请输入账号', icon: 'none' }); return; }
    if (!password || password.length < 4) { wx.showToast({ title: '密码至少4位', icon: 'none' }); return; }
    if (password !== confirmPassword) { wx.showToast({ title: '两次密码不一致', icon: 'none' }); return; }
    this.setData({ authLoading: true });
    try {
      const userInfo = await getAppInstance().phoneRegister(
        username.trim(), password,
        nickname.trim() || username.trim(),
        phone.trim()
      );
      this.setData({ isLoggedIn: true, userInfo, authLoading: false });
      wx.showToast({ title: '注册成功', icon: 'success' });
      this.loadProfile();
    } catch (err) {
      this.setData({ authLoading: false });
      wx.showToast({ title: (err && err.message) || '注册失败', icon: 'none' });
    }
  },

  async onWxLogin() {
    wx.showLoading({ title: '微信登录中...', mask: true });
    try {
      const userInfo = await getAppInstance().wxLogin();
      this.setData({ isLoggedIn: true, userInfo });
      wx.hideLoading();
      wx.showToast({ title: '登录成功', icon: 'success' });
      this.loadProfile();
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '微信登录失败，请重试', icon: 'none' });
      console.error('微信登录失败:', err);
    }
  },

  // ========== 加载用户资料：api.get 已解包返回 profile ==========
  async loadProfile() {
    const userId = getAppInstance().globalData.userId;
    if (!userId) return;
    this.setData({ loading: true });
    try {
      const profile = await api.get(`/users/${userId}/profile`);
      // profile 字段：{ id, nickname, phone, avatar_url, team_id, team_name, team_role }
      const userInfo = {
        nickname: profile.nickname || '用户',
        phone: profile.phone || '',
        avatar: profile.avatar_url || '',
        team_id: profile.team_id || null,
        team_name: profile.team_name || null,
        team_role: profile.team_role || null,
        id: profile.id,
        user_id: profile.id
      };
      this.setData({
        userInfo,
        teamInfo: {
          team_id: profile.team_id,
          team_name: profile.team_name,
          team_role: profile.team_role
        },
        loading: false
      });
      getAppInstance().globalData.userInfo = userInfo;
      wx.setStorageSync('userInfo', userInfo);
    } catch (err) {
      console.error('加载用户资料失败:', err);
      this.setData({ loading: false });
    }
  },

  onGetPhoneNumber(e) {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '取消授权', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '绑定中...', mask: true });
    getAppInstance().bindPhoneNumber(e.detail.encryptedData, e.detail.iv)
      .then(() => {
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

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '退出后需要重新登录',
      confirmColor: '#d4644a',
      success: (res) => {
        if (res.confirm) {
          const gd = getAppInstance().globalData || {};
          gd.token = null;
          gd.userInfo = null;
          gd.userId = null;
          gd.openid = null;
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

  onGoToTeam() {
    wx.switchTab({ url: '/pages/team/team' });
  },

  onGoToBookings() {
    wx.switchTab({ url: '/pages/bookings/bookings' });
  }
});
