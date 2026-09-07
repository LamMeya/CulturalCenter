// pages/bookings/bookings.js — 我的预约
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
    bookings: [],
    loading: false,
    empty: false,
    currentUserId: null,
    showCancelModal: false,
    cancelBookingId: null,
    cancelLoading: false
  },

  onShow() {
    const token = getAppInstance().globalData.token;
    const userId = getAppInstance().globalData.userId;
    const isLoggedIn = !!(token && userId);
    this.setData({ isLoggedIn, currentUserId: userId });
    if (isLoggedIn) {
      this.loadBookings();
    } else {
      this.setData({ loading: false });
    }
  },

  onPullDownRefresh() {
    if (!this.data.isLoggedIn) { wx.stopPullDownRefresh(); return; }
    this.loadBookings().then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh());
  },

  onGoToLogin() { wx.switchTab({ url: '/pages/profile/profile' }); },

  /**
   * 加载预约列表
   * utils/api 已解包：
   *   GET /teams               → teams 数组
   *   GET /users/{id}/bookings → bookings 数组（或 { bookings: [...] } 做兼容）
   */
  async loadBookings() {
    const userId = getAppInstance().globalData.userId;
    if (!userId) {
      this.setData({ bookings: [], empty: true, loading: false });
      return;
    }
    this.setData({ loading: true, currentUserId: userId });
    try {
      let params = {};
      try {
        const teamsRaw = await api.get('/teams', { user_id: userId });
        const teams = Array.isArray(teamsRaw) ? teamsRaw : [];
        if (teams.length > 0) {
          params.team_id = teams[0].id;
        } else {
          params.user_id = userId;
        }
      } catch (e) {
        console.warn('获取团队信息失败，回退到 user_id:', e);
        params.user_id = userId;
      }

      const res = await api.get(`/users/${userId}/bookings`, params);
      // 优先认为 res 就是 bookings 数组（已解包），如果是对象且有 bookings 字段则取该字段
      const raw = Array.isArray(res) ? res : ((res && res.bookings) || []);
      const userInfo = getAppInstance().globalData.userInfo || {};
      const myNickname = userInfo.nickname || ('用户' + userId);
      const today = new Date();
      const todayStr = today.getFullYear() + '-' +
        String(today.getMonth() + 1).padStart(2, '0') + '-' +
        String(today.getDate()).padStart(2, '0');
      const bookings = raw.map((item) => {
        const ts = item.time_slot || {};
        const bookingDate = ts.date || '';
        const isFuture = bookingDate >= todayStr;
        const canCancel = (item.status === 'pending' || item.status === 'won') && isFuture;
        return {
          ...item,
          bookingDate: bookingDate,
          bookingStart: (ts.start || '').substring(0, 5),
          bookingEnd: (ts.end || '').substring(0, 5),
          statusText: this.getStatusText(item.status),
          bookerName: myNickname,
          isOwn: true,
          canCancel: canCancel
        };
      });
      this.setData({ bookings, empty: bookings.length === 0, loading: false });
    } catch (err) {
      console.error('加载预约列表失败:', err);
      this.setData({ loading: false, empty: true });
    }
  },

  getStatusText(status) {
    const map = {
      pending: '待抽签',
      won: '已中签',
      lost: '未中签',
      cancelled: '已取消'
    };
    return map[status] || status;
  },

  onCancelTap(e) {
    const bookingId = e.currentTarget.dataset.id;
    this.setData({ showCancelModal: true, cancelBookingId: bookingId });
  },

  onCancelModalClose() {
    this.setData({ showCancelModal: false, cancelBookingId: null });
  },

  async onConfirmCancel() {
    const userId = getAppInstance().globalData.userId;
    const bookingId = this.data.cancelBookingId;
    if (!userId || !bookingId) return;
    this.setData({ cancelLoading: true });
    try {
      await api.post(`/users/${userId}/bookings/${bookingId}/cancel`);
      wx.showToast({ title: '取消成功', icon: 'success' });
      this.setData({ showCancelModal: false, cancelBookingId: null, cancelLoading: false });
      this.loadBookings();
    } catch (err) {
      console.error('取消预约失败:', err);
      this.setData({ cancelLoading: false });
      wx.showToast({ title: (err && err.message) || '取消失败', icon: 'none' });
    }
  }
});
