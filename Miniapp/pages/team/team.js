// pages/team/team.js — 团队管理
const app = getApp();

Page({
  data: {
    // 登录状态
    isLoggedIn: false,

    // 团队状态
    hasTeam: false,
    team: null,
    loading: true,
    isLeader: false,
    isViceLeader: false,

    // 创建团队表单
    showCreateForm: false,
    teamName: '',
    teamIntro: '',
    creating: false,

    // 搜索/加入团队
    searchKeyword: '',
    searchResults: [],
    searching: false,
    joiningTeamId: null,

    // 成员管理
    showMemberList: false,
    members: [],

    // 设置副队长
    showViceLeaderModal: false,
    potentialViceLeaders: [],
    selectedViceLeaderIds: [],
    currentViceLeaderIds: [],

    // 预约统计
    bookingStats: null,
    showStatsDetail: false,

    // 解散确认
    showDissolveModal: false,

    // 转让队长
    showTransferModal: false,
    transferCandidates: [],

    // 退出确认
    showLeaveModal: false,

    // 用户信息
    userId: null
  },

  onLoad() {
    const userId = app.globalData.userId;
    const token = app.globalData.token;
    this.setData({
      userId,
      isLoggedIn: !!(token && userId)
    });
  },

  onShow() {
    const token = app.globalData.token;
    const userId = app.globalData.userId;
    const isLoggedIn = !!(token && userId);
    this.setData({ isLoggedIn, userId: userId || null });

    if (isLoggedIn) {
      this.fetchTeamInfo();
    } else {
      this.setData({ loading: false });
    }
  },

  // 前往登录页
  onGoToLogin() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  // ========== 获取团队信息 ==========
  fetchTeamInfo() {
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    this.setData({ loading: true });

    wx.request({
      url: `${apiBase}/teams`,
      method: 'GET',
      data: userId ? { user_id: userId } : {},
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const teams = Array.isArray(res.data) ? res.data : (res.data.teams || []);
          if (teams.length > 0) {
            const team = teams[0];
            this.setData({
              hasTeam: true,
              team,
              loading: false,
              isLeader: team.leader_user_id === userId,
              isViceLeader: (team.vice_leader_ids || []).indexOf(userId) >= 0
            });
            this.fetchTeamDetail(team.id);
            this.fetchBookingStats(team.id);
          } else {
            this.setData({
              hasTeam: false,
              team: null,
              loading: false
            });
          }
        } else {
          this.setData({
            hasTeam: false,
            team: null,
            loading: false
          });
        }
      },
      fail: (err) => {
        console.error('获取团队信息失败:', err);
        this.setData({ loading: false });
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  fetchTeamDetail(teamId) {
    const apiBase = app.globalData.apiBase;

    wx.request({
      url: `${apiBase}/teams/${teamId}`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const team = res.data.team || res.data;
          const members = team.members || [];
          this.setData({
            team,
            members,
            isLeader: team.leader_user_id === this.data.userId,
            isViceLeader: (team.vice_leader_ids || []).indexOf(this.data.userId) >= 0,
            currentViceLeaderIds: team.vice_leader_ids || []
          });
        }
      },
      fail: (err) => {
        console.error('获取团队详情失败:', err);
      }
    });
  },

  fetchBookingStats(teamId) {
    const apiBase = app.globalData.apiBase;

    wx.request({
      url: `${apiBase}/teams/${teamId}/booking-stats`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const stats = res.data.stats || res.data;
          this.setData({ bookingStats: stats });
        }
      },
      fail: (err) => {
        console.error('获取预约统计失败:', err);
      }
    });
  },

  onToggleCreateForm() {
    this.setData({
      showCreateForm: !this.data.showCreateForm,
      teamName: '',
      teamIntro: ''
    });
  },

  onTeamNameInput(e) {
    this.setData({ teamName: e.detail.value });
  },

  onTeamIntroInput(e) {
    this.setData({ teamIntro: e.detail.value });
  },

  onCreateTeam() {
    const { teamName, teamIntro } = this.data;
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    if (!teamName.trim()) {
      wx.showToast({ title: '请输入团队名称', icon: 'none' });
      return;
    }

    if (this.data.creating) return;
    this.setData({ creating: true });

    wx.request({
      url: `${apiBase}/teams`,
      method: 'POST',
      data: {
        name: teamName.trim(),
        intro: teamIntro.trim(),
        leader_user_id: userId
      },
      success: (res) => {
        this.setData({ creating: false });
        if (res.statusCode === 200 || res.statusCode === 201) {
          wx.showToast({ title: '团队创建成功', icon: 'success' });
          this.setData({ showCreateForm: false });
          this.fetchTeamInfo();
        } else {
          const msg = (res.data && res.data.message) || '创建失败，请重试';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('创建团队失败:', err);
        this.setData({ creating: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
  },

  onSearchTeam() {
    const { searchKeyword } = this.data;
    const apiBase = app.globalData.apiBase;

    if (!searchKeyword.trim()) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' });
      return;
    }

    this.setData({ searching: true });

    wx.request({
      url: `${apiBase}/teams`,
      method: 'GET',
      data: { keyword: searchKeyword.trim() },
      success: (res) => {
        this.setData({ searching: false });
        if (res.statusCode === 200 && res.data) {
          const results = Array.isArray(res.data) ? res.data : (res.data.teams || []);
          this.setData({ searchResults: results });
          if (results.length === 0) {
            wx.showToast({ title: '未找到匹配的团队', icon: 'none' });
          }
        } else {
          this.setData({ searchResults: [] });
        }
      },
      fail: (err) => {
        console.error('搜索团队失败:', err);
        this.setData({ searching: false });
        wx.showToast({ title: '搜索失败', icon: 'none' });
      }
    });
  },

  onJoinTeam(e) {
    const teamId = e.currentTarget.dataset.id;
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    if (this.data.joiningTeamId === teamId) return;
    this.setData({ joiningTeamId: teamId });

    wx.request({
      url: `${apiBase}/teams/${teamId}/join`,
      method: 'POST',
      data: { user_id: userId },
      success: (res) => {
        this.setData({ joiningTeamId: null });
        if (res.statusCode === 200 || res.statusCode === 201) {
          wx.showToast({ title: '加入成功', icon: 'success' });
          this.setData({ searchResults: [] });
          this.fetchTeamInfo();
        } else {
          const msg = (res.data && res.data.message) || '加入失败，请重试';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('加入团队失败:', err);
        this.setData({ joiningTeamId: null });
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  onToggleMemberList() {
    this.setData({ showMemberList: !this.data.showMemberList });
  },

  onOpenViceLeaderModal() {
    const { members, currentViceLeaderIds, userId } = this.data;
    const candidates = members.filter(m => m.user_id !== userId);
    this.setData({
      showViceLeaderModal: true,
      potentialViceLeaders: candidates,
      selectedViceLeaderIds: [...currentViceLeaderIds]
    });
  },

  onCloseViceLeaderModal() {
    this.setData({ showViceLeaderModal: false });
  },

  onToggleViceLeader(e) {
    const userId = e.currentTarget.dataset.userId;
    let { selectedViceLeaderIds } = this.data;
    const index = selectedViceLeaderIds.indexOf(userId);

    if (index >= 0) {
      selectedViceLeaderIds.splice(index, 1);
    } else {
      if (selectedViceLeaderIds.length >= 4) {
        wx.showToast({ title: '最多设置 4 名副队长', icon: 'none' });
        return;
      }
      selectedViceLeaderIds.push(userId);
    }
    this.setData({ selectedViceLeaderIds: [...selectedViceLeaderIds] });
  },

  onConfirmViceLeaders() {
    const { team, selectedViceLeaderIds } = this.data;
    const apiBase = app.globalData.apiBase;

    wx.request({
      url: `${apiBase}/teams/${team.id}`,
      method: 'PUT',
      data: { vice_leader_ids: selectedViceLeaderIds },
      success: (res) => {
        if (res.statusCode === 200) {
          wx.showToast({ title: '副队长已更新', icon: 'success' });
          this.setData({ showViceLeaderModal: false });
          this.fetchTeamDetail(team.id);
        } else {
          wx.showToast({ title: '更新失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  onOpenTransferModal() {
    const { members, userId } = this.data;
    const candidates = members.filter(m => m.user_id !== userId);
    if (candidates.length === 0) {
      wx.showToast({ title: '没有可转让的成员', icon: 'none' });
      return;
    }
    this.setData({
      showTransferModal: true,
      transferCandidates: candidates
    });
  },

  onCloseTransferModal() {
    this.setData({ showTransferModal: false });
  },

  onTransferLeader(e) {
    const newLeaderId = e.currentTarget.dataset.userId;
    const { team } = this.data;
    const apiBase = app.globalData.apiBase;

    wx.showModal({
      title: '确认转让',
      content: '转让队长后您将失去队长权限，确认继续？',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: `${apiBase}/teams/${team.id}`,
            method: 'PUT',
            data: { leader_user_id: newLeaderId },
            success: (res) => {
              if (res.statusCode === 200) {
                wx.showToast({ title: '队长已转让', icon: 'success' });
                this.setData({ showTransferModal: false });
                this.fetchTeamInfo();
              } else {
                wx.showToast({ title: '转让失败', icon: 'none' });
              }
            },
            fail: () => {
              wx.showToast({ title: '网络异常', icon: 'none' });
            }
          });
        }
      }
    });
  },

  onOpenDissolveModal() {
    this.setData({ showDissolveModal: true });
  },

  onCloseDissolveModal() {
    this.setData({ showDissolveModal: false });
  },

  onDissolveTeam() {
    const { team } = this.data;
    const apiBase = app.globalData.apiBase;

    wx.showModal({
      title: '解散团队',
      content: '此操作不可撤销，确认解散团队？',
      confirmText: '确认解散',
      confirmColor: '#d4644a',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: `${apiBase}/teams/${team.id}`,
            method: 'DELETE',
            success: () => {
              wx.showToast({ title: '团队已解散', icon: 'success' });
              this.setData({
                showDissolveModal: false,
                hasTeam: false,
                team: null
              });
            },
            fail: () => {
              wx.showToast({ title: '操作失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  onOpenLeaveModal() {
    this.setData({ showLeaveModal: true });
  },

  onCloseLeaveModal() {
    this.setData({ showLeaveModal: false });
  },

  onLeaveTeam() {
    const { team } = this.data;
    const apiBase = app.globalData.apiBase;
    const userId = app.globalData.userId;

    wx.showModal({
      title: '退出团队',
      content: '退出后您将无法代表团队预约，确认退出？',
      confirmText: '确认退出',
      confirmColor: '#d4644a',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: `${apiBase}/teams/${team.id}/leave`,
            method: 'POST',
            data: { user_id: userId },
            success: () => {
              wx.showToast({ title: '已退出团队', icon: 'success' });
              this.setData({
                showLeaveModal: false,
                hasTeam: false,
                team: null
              });
            },
            fail: () => {
              wx.showToast({ title: '操作失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  onToggleStatsDetail() {
    this.setData({ showStatsDetail: !this.data.showStatsDetail });
  }
});
