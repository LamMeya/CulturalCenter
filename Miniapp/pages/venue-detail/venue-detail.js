// pages/venue-detail/venue-detail.js — 场馆详情与预约
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
    venueId: '',
    venue: null,
    loading: true,

    // 日期选择
    dateList: [],
    selectedDate: '',
    dateLoading: false,

    // 时间段
    timeSlots: [],
    morningSlots: [],
    afternoonSlots: [],
    selectedSlots: [],      // 存储选中的 time_slot 对象
    selectedSlotIds: [],    // 存储选中的 slot id
    maxSlots: 4,

    // 导航栏高度
    navBarHeight: 0,

    // 确认弹窗
    showConfirmModal: false,
    bookingResult: null,
    showSuccessModal: false,
    submitting: false,

    // 团队检查
    userTeam: null,
    teamChecked: false
  },

  onLoad(options) {
    const venueId = options.venue_id;
    if (!venueId) {
      wx.showToast({ title: '缺少场馆参数', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const sysInfo = wx.getSystemInfoSync();
    const rpxRatio = 750 / sysInfo.windowWidth;
    const navBarHeight = Math.round((sysInfo.statusBarHeight + 44) * rpxRatio);
    this.setData({ venueId, navBarHeight });
    this.fetchVenueDetail();
    this.generateDateList();
    this.checkUserTeam();
  },

  // ========== 获取场馆详情：api.get 已解包返回 venue ==========
  async fetchVenueDetail() {
    const { venueId } = this.data;
    this.setData({ loading: true });
    try {
      const venue = await api.get(`/venues/${venueId}`);
      this.setData({ venue, loading: false });
      this.autoSelectDate();
    } catch (err) {
      console.error('获取场馆详情失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '获取场馆信息失败', icon: 'none' });
    }
  },

  // ========== 生成日期列表（未来14天，排除周一） ==========
  generateDateList() {
    const dateList = [];
    const today = new Date();
    const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    for (let i = 0; i < 14; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      const dayOfWeek = date.getDay();
      if (dayOfWeek === 1) continue; // 周一闭馆
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      const dateStr = `${y}-${m}-${d}`;
      dateList.push({
        date: dateStr,
        dayName: dayNames[dayOfWeek],
        day: String(date.getDate()),
        monthDay: `${m}/${d}`,
        hasSlots: null,
        available: true
      });
    }
    this.setData({ dateList });
  },

  autoSelectDate() {
    const { dateList } = this.data;
    if (dateList.length > 0) {
      this.selectDate({ currentTarget: { dataset: { date: dateList[0].date } } });
    }
  },

  selectDate(e) {
    const date = e.currentTarget.dataset.date;
    this.setData({
      selectedDate: date,
      selectedSlots: [],
      selectedSlotIds: [],
      timeSlots: [],
      morningSlots: [],
      afternoonSlots: []
    });
    this.fetchTimeSlots(date);
  },

  // ========== 获取时间段：api.get 解包返回 slots 数组 ==========
  async fetchTimeSlots(date) {
    const { venueId } = this.data;
    this.setData({ dateLoading: true });
    try {
      const slotsRaw = await api.get(`/venues/${venueId}/time-slots`, { date }) || [];
      const slots = slotsRaw.map(s => ({
        ...s,
        start_time: this.formatTime(s.start_time),
        end_time: this.formatTime(s.end_time),
        selected: false
      }));
      const morningSlots = slots.filter(s => (parseInt(s.start_time || '00')) < 12);
      const afternoonSlots = slots.filter(s => (parseInt(s.start_time || '00')) >= 12);
      this.setData({
        timeSlots: slots,
        morningSlots,
        afternoonSlots,
        dateLoading: false
      });
      this.updateDateAvailability(date, slots.length > 0);
    } catch (err) {
      console.error('获取时间段失败:', err);
      this.setData({ dateLoading: false });
      this.updateDateAvailability(date, false);
    }
  },

  updateDateAvailability(date, hasSlots) {
    const dateList = this.data.dateList.map(item => {
      if (item.date === date) {
        return { ...item, hasSlots, available: hasSlots };
      }
      return item;
    });
    this.setData({ dateList });
  },

  // ========== 选择/取消时间段（多选，最多4个） ==========
  onSlotTap(e) {
    const slot = e.currentTarget.dataset.slot;
    const { selectedSlots, selectedSlotIds, maxSlots, morningSlots, afternoonSlots, timeSlots } = this.data;
    const slotId = slot.id;
    const idx = selectedSlotIds.indexOf(slotId);
    if (idx >= 0) {
      selectedSlots.splice(idx, 1);
      selectedSlotIds.splice(idx, 1);
      const upd = (arr) => { for (let i = 0; i < arr.length; i++) if (arr[i].id === slotId) arr[i].selected = false; };
      upd(morningSlots); upd(afternoonSlots); upd(timeSlots);
    } else {
      if (selectedSlotIds.length >= maxSlots) {
        wx.showToast({ title: `最多选择 ${maxSlots} 个时段`, icon: 'none' });
        return;
      }
      selectedSlots.push(slot);
      selectedSlotIds.push(slotId);
      const upd = (arr) => { for (let i = 0; i < arr.length; i++) if (arr[i].id === slotId) arr[i].selected = true; };
      upd(morningSlots); upd(afternoonSlots); upd(timeSlots);
    }
    this.setData({ selectedSlots, selectedSlotIds, morningSlots, afternoonSlots, timeSlots });
  },

  // ========== 检查用户是否有团队：api.get 解包 teams 数组 ==========
  async checkUserTeam() {
    const userId = getAppInstance().globalData.userId;
    if (!userId) {
      this.setData({ teamChecked: true });
      return;
    }
    try {
      const teams = await api.get('/teams', userId ? { user_id: userId } : {}) || [];
      const arr = Array.isArray(teams) ? teams : [];
      this.setData({ userTeam: arr[0] || null, teamChecked: true });
    } catch (err) {
      console.warn('获取团队失败:', err);
      this.setData({ teamChecked: true });
    }
  },

  // ========== 点击确认预约 ==========
  onConfirmBooking() {
    const { selectedSlots, userTeam, teamChecked } = this.data;
    if (selectedSlots.length === 0) {
      wx.showToast({ title: '请先选择预约时段', icon: 'none' });
      return;
    }
    getAppInstance().checkLogin().then(() => {
      if (!teamChecked) { wx.showToast({ title: '正在检查团队信息...', icon: 'none' }); return; }
      if (!userTeam) {
        wx.showModal({
          title: '提示',
          content: '您还没有加入团队，需要先创建或加入一个团队才能预约场馆。',
          confirmText: '去创建团队',
          cancelText: '稍后',
          success: (res) => { if (res.confirm) wx.switchTab({ url: '/pages/team/team' }); }
        });
        return;
      }
      this.setData({ showConfirmModal: true });
    }).catch(() => {});
  },

  onCloseConfirmModal() {
    this.setData({ showConfirmModal: false });
  },

  // ========== 提交预约：api.post 解包返回 booking ==========
  async onSubmitBooking() {
    const { venueId, selectedSlotIds, userTeam } = this.data;
    const userId = getAppInstance().globalData.userId;
    if (!userId) { wx.showToast({ title: '请先登录', icon: 'none' }); return; }
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    try {
      const booking = await api.post('/bookings', {
        user_id: userId,
        team_id: userTeam.id,
        venue_id: parseInt(venueId),
        time_slot_ids: selectedSlotIds
      });
      this.setData({
        submitting: false,
        bookingResult: booking,
        showConfirmModal: false,
        showSuccessModal: true,
        selectedSlots: [],
        selectedSlotIds: []
      });
    } catch (err) {
      console.error('预约失败:', err);
      this.setData({ submitting: false });
      wx.showToast({ title: (err && (err.message || err.detail)) || '预约失败，请重试', icon: 'none' });
    }
  },

  onCloseSuccessModal() {
    this.setData({ showSuccessModal: false });
    wx.navigateBack();
  },

  onViewBookings() {
    this.setData({ showSuccessModal: false });
    wx.switchTab({ url: '/pages/bookings/bookings' });
  },

  formatTime(timeStr) {
    if (!timeStr) return '';
    const match = String(timeStr).match(/^(\d{2}:\d{2})/);
    return match ? match[1] : timeStr;
  },

  formatTimeRange(s, e) {
    return `${this.formatTime(s)} - ${this.formatTime(e)}`;
  },

  isSlotSelected(slotId) {
    return this.data.selectedSlotIds.indexOf(slotId) >= 0;
  },

  onShareAppMessage() {
    const { venue } = this.data;
    return {
      title: venue ? `${venue.name} - 文化中心` : '文化中心场馆预约',
      path: `/pages/venue-detail/venue-detail?venue_id=${this.data.venueId}`
    };
  }
});
