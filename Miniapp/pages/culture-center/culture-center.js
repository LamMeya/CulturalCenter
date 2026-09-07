// pages/culture-center/culture-center.js — 文化中心介绍
Page({
  data: {
    // 预约规则列表
    rules: [
      '每团队每2周最多成功预约2次',
      '每次预约最多选择4个时间段',
      '预约后进入抽签，由系统自动分配',
      '抽中后30天内取消不得超过2次',
      '周一为闭馆日，不开放预约'
    ],
    // 联系方式
    contact: {
      address: '珠海市区井岸镇文化路88号',
      phone: '0756-xxxxxxx',
      hours: '周二至周日 9:00 - 18:00（周一闭馆）'
    }
  },

  onLoad() {
    // 页面加载
  },

  /**
   * 拨打电话
   */
  onCallPhone() {
    wx.makePhoneCall({
      phoneNumber: this.data.contact.phone.replace(/-/g, ''),
      fail: () => {
        wx.showToast({ title: '拨号失败', icon: 'none' });
      }
    });
  },

  /**
   * 复制地址
   */
  onCopyAddress() {
    wx.setClipboardData({
      data: this.data.contact.address,
      success: () => {
        wx.showToast({ title: '地址已复制', icon: 'success' });
      }
    });
  }
});