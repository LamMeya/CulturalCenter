// pages/venue-detail/venue-detail.js — 场馆详情与预约
const app = getApp();

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
    this.setData({ venueId });
    this.fetchVenueDetail();
    this.generateDateList();
    this.checkUserTeam();
  },

  // ========== 获取场馆详情 ==========
  fetchVenueDetail() {
    const { venueId } = this.data;
    const apiBase = app.globalData.apiBase;
    this.setData({ loading: true });

    wx.request({
      url: `${apiBase}/venues/${venueId}`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const venue = res.data.venue || res.data;
          this.setData({
            venue,
            loading: false
          });
          // 获取场馆后，默认选中第一个可用日期
          this.autoSelectDate();
        } else {
          this.setData({ loading: false });
          wx.showToast({ title: '获取场馆信息失败', icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('获取场馆详情失败:', err);
        this.setData({ loading: false });
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
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

      // 排除周一 (dayOfWeek === 1)
      if (dayOfWeek === 1) continue;

      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const dateStr = `${year}-${month}-${day}`;

      dateList.push({
        date: dateStr,
        dayName: dayNames[dayOfWeek],
        day: String(date.getDate()),
        monthDay: `${month}/${day}`,
        hasSlots: null,       // null = 未检查, true/false 检查后
        available: true       // 默认可用
      });
    }

    this.setData({ dateList });
  },

  // ========== 自动选中第一个可用日期 ==========
  autoSelectDate() {
    const { dateList } = this.data;
    if (dateList.length > 0) {
      this.selectDate({ currentTarget: { dataset: { date: dateList[0].date } } });
    }
  },

  // ========== 选择日期 ==========
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

  // ========== 获取时间段 ==========
  fetchTimeSlots(date) {
    const { venueId } = this.data;
    const apiBase = app.globalData.apiBase;
    this.setData({ dateLoading: true });

    wx.request({
      url: `${apiBase}/venues/${venueId}/time-slots`,
      method: 'GET',
      data: { date },
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const slots = Array.isArray(res.data) ? res.data : (res.data.slots || res.data.time_slots || []);

          // 按上下午分组
          const morningSlots = slots.filter(s => {
            const hour = parseInt(s.start_time || '00');
            return hour < 12;
          });
          const afternoonSlots = slots.filter(s => {
            const hour = parseInt(s.start_time || '00');
            return hour >= 12;
          });

          this.setData({
            timeSlots: slots,
            morningSlots,
            afternoonSlots,
            dateLoading: false
          });

          // 更新日期列表中的可用状态
          this.updateDateAvailability(date, slots.length > 0);
        } else {
          this.setData({ dateLoading: false });
          this.updateDateAvailability(date, false);
        }
      },
      fail: (err) => {
        console.error('获取时间段失败:', err);
        this.setData({ dateLoading: false });
        this.updateDateAvailability(date, false);
      }
    });
  },

  // ========== 更新日期可用性 ==========
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
    const { selectedSlots, selectedSlotIds, maxSlots } = this.data;

    const slotId = slot.id;
    const index = selectedSlotIds.indexOf(slotId);

    if (index >= 0) {
      // 取消选择
      selectedSlots.splice(index, 1);
      selectedSlotIds.splice(index, 1);
      this.setData({ selectedSlots, selectedSlotIds });
    } else {
      // 选择
      if (selectedSlotIds.length >= maxSlots) {
        wx.showToast({ title: `最多选择 ${maxSlots} 个时段`, icon: 'none' });
        return;
      }
      selectedSlots.push(slot);
      selectedSlotIds.push(slotId);
      this.setData({ selectedSlots, selectedSlotIds });
    }
  },

  // ========== 检查用户是否有团队 ==========
  checkUserTeam() {
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    if (!userId) {
      this.setData({ teamChecked: true });
      return;
    }

    wx.request({
      url: `${apiBase}/teams`,
      method: 'GET',
      data: { user_id: userId },
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const teams = Array.isArray(res.data) ? res.data : (res.data.teams || []);
          const userTeam = teams.length > 0 ? teams[0] : null;
          this.setData({ userTeam, teamChecked: true });
        } else {
          this.setData({ teamChecked: true });
        }
      },
      fail: () => {
        this.setData({ teamChecked: true });
      }
    });
  },

  // ========== 点击确认预约（先检查登录，再检查团队） ==========
  onConfirmBooking() {
    const { selectedSlots, userTeam, teamChecked } = this.data;

    // 检查是否已选时段
    if (selectedSlots.length === 0) {
      wx.showToast({ title: '请先选择预约时段', icon: 'none' });
      return;
    }

    // 第一步：检查登录状态
    app.checkLogin().then(() => {
      // 已登录，继续检查团队状态
      if (!teamChecked) {
        wx.showToast({ title: '正在检查团队信息...', icon: 'none' });
        return;
      }

      if (!userTeam) {
        wx.showModal({
          title: '提示',
          content: '您还没有加入团队，需要先创建或加入一个团队才能预约场馆。',
          confirmText: '去创建团队',
          cancelText: '稍后',
          success: (res) => {
            if (res.confirm) {
              wx.switchTab({
                url: '/pages/team/team'
              });
            }
          }
        });
        return;
      }

      // 显示确认弹窗
      this.setData({ showConfirmModal: true });
    }).catch(() => {
      // 用户取消登录，不做任何操作
    });
  },

  // ========== 关闭确认弹窗 ==========
  onCloseConfirmModal() {
    this.setData({ showConfirmModal: false });
  },

  // ========== 提交预约 ==========
  onSubmitBooking() {
    const { venueId, selectedSlots, selectedSlotIds, userTeam } = this.data;
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    if (this.data.submitting) return;
    this.setData({ submitting: true });

    wx.request({
      url: `${apiBase}/bookings`,
      method: 'POST',
      data: {
        user_id: userId,
        team_id: userTeam.id,
        venue_id: parseInt(venueId),
        time_slot_ids: selectedSlotIds
      },
      success: (res) => {
        this.setData({ submitting: false });
        if (res.statusCode === 200 || res.statusCode === 201) {
          const booking = res.data.booking || res.data;
          this.setData({
            bookingResult: booking,
            showConfirmModal: false,
            showSuccessModal: true,
            selectedSlots: [],
            selectedSlotIds: []
          });
        } else {
          const msg = (res.data && res.data.message) || '预约失败，请重试';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('预约失败:', err);
        this.setData({ submitting: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  // ========== 关闭成功弹窗 ==========
  onCloseSuccessModal() {
    this.setData({ showSuccessModal: false });
    wx.navigateBack();
  },

  // ========== 查看我的预约 ==========
  onViewBookings() {
    this.setData({ showSuccessModal: false });
    wx.switchTab({
      url: '/pages/bookings/bookings'
    });
  },

  // ========== 格式化时间显示 ==========
  formatTimeRange(startTime, endTime) {
    return `${startTime} - ${endTime}`;
  },

  // ========== 判断时段是否被选中 ==========
  isSlotSelected(slotId) {
    return this.data.selectedSlotIds.indexOf(slotId) >= 0;
  },

  onShareAppMessage() {
    const { venue } = this.data;
    return {
      title: venue ? `${venue.name} - 斗门文化中心` : '斗门文化中心场馆预约',
      path: `/pages/venue-detail/venue-detail?venue_id=${this.data.venueId}`
    };
  }
});
