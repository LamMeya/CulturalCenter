// pages/bookings/bookings.js — 我的预约
const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    // 登录状态
    isLoggedIn: false,
    bookings: [],
    loading: false,
    empty: false,
    // 取消弹窗
    showCancelModal: false,
    cancelBookingId: null,
    cancelLoading: false
  },

  onShow() {
    const token = app.globalData.token;
    const userId = app.globalData.userId;
    const isLoggedIn = !!(token && userId);
    this.setData({ isLoggedIn });

    if (isLoggedIn) {
      this.loadBookings();
    } else {
      this.setData({ loading: false });
    }
  },

  onPullDownRefresh() {
    if (!this.data.isLoggedIn) {
      wx.stopPullDownRefresh();
      return;
    }
    this.loadBookings().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 前往登录页
  onGoToLogin() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  /**
   * 加载预约列表
   */
  async loadBookings() {
    const userId = app.globalData.userId;
    if (!userId) {
      this.setData({ bookings: [], empty: true, loading: false });
      return;
    }

    this.setData({ loading: true });

    try {
      const res = await api.get(`/api/users/${userId}/bookings`);
      const bookings = (res.bookings || res.data || res || []).map((item) => ({
        ...item,
        statusText: this.getStatusText(item.status)
      }));
      this.setData({
        bookings,
        empty: bookings.length === 0,
        loading: false
      });
    } catch (err) {
      console.error('加载预约列表失败:', err);
      this.setData({ loading: false, empty: true });
    }
  },

  /**
   * 获取状态文本
   */
  getStatusText(status) {
    const map = {
      pending: '待抽签',
      won: '已中签',
      lost: '未中签',
      cancelled: '已取消'
    };
    return map[status] || status;
  },

  /**
   * 判断是否可取消
   */
  canCancel(status) {
    return status === 'pending' || status === 'won';
  },

  /**
   * 点击取消按钮
   */
  onCancelTap(e) {
    const bookingId = e.currentTarget.dataset.id;
    this.setData({
      showCancelModal: true,
      cancelBookingId: bookingId
    });
  },

  /**
   * 关闭取消弹窗
   */
  onCancelModalClose() {
    this.setData({
      showCancelModal: false,
      cancelBookingId: null
    });
  },

  /**
   * 确认取消
   */
  async onConfirmCancel() {
    const userId = app.globalData.userId;
    const bookingId = this.data.cancelBookingId;
    if (!userId || !bookingId) return;

    this.setData({ cancelLoading: true });

    try {
      await api.post(`/api/users/${userId}/bookings/${bookingId}/cancel`);
      wx.showToast({ title: '取消成功', icon: 'success' });
      this.setData({
        showCancelModal: false,
        cancelBookingId: null,
        cancelLoading: false
      });
      this.loadBookings();
    } catch (err) {
      console.error('取消预约失败:', err);
      this.setData({ cancelLoading: false });
    }
  },

  /**
   * 格式化时间
   */
  formatTime(start, end) {
    return `${start}-${end}`;
  }
});
