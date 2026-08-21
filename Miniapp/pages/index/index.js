// pages/index/index.js — 首页
const app = getApp();

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
  fetchVenues() {
    const apiBase = app.globalData.apiBase;
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${apiBase}/venues`,
        method: 'GET',
        success: (res) => {
          if (res.statusCode === 200 && res.data) {
            const venues = Array.isArray(res.data) ? res.data : (res.data.venues || res.data.data || []);
            this.setData({
              venues,
              loading: false,
              errorMsg: ''
            });
            resolve(venues);
          } else {
            this.setData({ loading: false, errorMsg: '获取场馆列表失败' });
            reject(res);
          }
        },
        fail: (err) => {
          console.error('获取场馆列表失败:', err);
          this.setData({ loading: false, errorMsg: '网络异常，请下拉刷新重试' });
          reject(err);
        }
      });
    });
  },

  // ========== 获取活跃通知 ==========
  fetchNotifications() {
    const apiBase = app.globalData.apiBase;
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${apiBase}/notifications/active`,
        method: 'GET',
        success: (res) => {
          if (res.statusCode === 200 && res.data) {
            const notifications = Array.isArray(res.data) ? res.data : (res.data.notifications || res.data.data || []);
            this.setData({ notifications });
            if (notifications.length > 1) {
              this.startNotificationScroll();
            }
            resolve(notifications);
          } else {
            resolve([]);
          }
        },
        fail: (err) => {
          console.error('获取通知失败:', err);
          resolve([]);
        }
      });
    });
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
    if (notification.link_url) {
      // 可跳转外部链接或内部页面
      wx.showToast({ title: notification.title || '通知详情', icon: 'none' });
    }
  }
});