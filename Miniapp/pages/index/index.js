// pages/index/index.js — 首页
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
    // 通知
    notifications: [],
    notificationIndex: 0,

    // 场馆列表
    venues: [],
    loading: true,
    refreshing: false,

    // 错误提示
    errorMsg: ''
  },

  onLoad() {
    this.fetchVenues();
    this.fetchNotifications();
  },

  onShow() {
    // 每次显示页面时刷新数据
    if (!this.data.loading) {
      this.fetchVenues();
      this.fetchNotifications();
    }
  },

  onPullDownRefresh() {
    this.setData({ refreshing: true });
    Promise.all([this.fetchVenues(), this.fetchNotifications()]).then(() => {
      this.setData({ refreshing: false });
      wx.stopPullDownRefresh();
    }).catch(() => {
      this.setData({ refreshing: false });
      wx.stopPullDownRefresh();
    });
  },

  // ========== 获取场馆列表 ==========
  // api.get 返回 data（已经解包成 venues 数组）
  async fetchVenues() {
    try {
      const venues = await api.get('/venues') || [];
      this.setData({
        venues: Array.isArray(venues) ? venues : [],
        loading: false,
        errorMsg: ''
      });
      return venues;
    } catch (err) {
      console.error('获取场馆列表失败:', err);
      this.setData({ loading: false, errorMsg: '获取场馆列表失败' });
      throw err;
    }
  },

  // ========== 获取活跃通知 ==========
  async fetchNotifications() {
    try {
      const res = await api.get('/notifications/published');
      // 取数组；虽然 publish 路径在有些版本会返回 data=[{通知}]，但 utils/api 会帮我们解包 data
      const notifications = Array.isArray(res) ? res : (res && res.data) || [];
      this.setData({ notifications });
      if (notifications.length > 1) {
        this.startNotificationScroll();
      }
      return notifications;
    } catch (err) {
      console.error('获取通知失败:', err);
      return [];
    }
  },

  // ========== 通知横幅滚动 ==========
  startNotificationScroll() {
    if (this._notificationTimer) {
      clearInterval(this._notificationTimer);
    }
    this._notificationTimer = setInterval(() => {
      const { notifications, notificationIndex } = this.data;
      if (notifications.length <= 1) return;
      this.setData({
        notificationIndex: (notificationIndex + 1) % notifications.length
      });
    }, 3000);
  },

  stopNotificationScroll() {
    if (this._notificationTimer) {
      clearInterval(this._notificationTimer);
      this._notificationTimer = null;
    }
  },

  onUnload() {
    this.stopNotificationScroll();
  },

  onHide() {
    this.stopNotificationScroll();
  },

  // ========== 跳转场馆详情 ==========
  onVenueTap(e) {
    const venueId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/venue-detail/venue-detail?venue_id=${venueId}`
    });
  },

  // ========== 跳转通知详情 ==========
  onNotificationTap(e) {
    const notification = e.currentTarget.dataset.notification;
    if (notification && notification.link_url) {
      wx.showToast({ title: notification.title || '通知详情', icon: 'none' });
    }
  }
});
