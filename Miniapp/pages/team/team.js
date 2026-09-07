// pages/team/team.js — 团队管理
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
    hasTeam: false,
    team: null,
    loading: true,
    isLeader: false,
    isViceLeader: false,

    showCreateModal: false,
    teamName: '',
    teamIntro: '',
    creating: false,

    showJoinModal: false,
    searchKeyword: '',
    searchResults: [],
    searching: false,
    searched: false,
    joiningTeamId: null,
    allTeams: [],
    allTeamsLoading: false,

    showMemberList: false,
    members: [],

    showViceLeaderModal: false,
    potentialViceLeaders: [],
    selectedViceLeaderIds: [],
    currentViceLeaderIds: [],

    bookingStats: null,
    showStatsDetail: false,

    showDissolveModal: false,

    showTransferModal: false,
    transferCandidates: [],

    showLeaveModal: false,

    userId: null
  },

  onLoad() {
    const userId = getAppInstance().globalData.userId;
    const token = getAppInstance().globalData.token;
    this.setData({ userId, isLoggedIn: !!(token && userId) });
  },

  onShow() {
    const token = getAppInstance().globalData.token;
    const userId = getAppInstance().globalData.userId;
    const isLoggedIn = !!(token && userId);
    this.setData({ isLoggedIn, userId: userId || null });
    if (isLoggedIn) {
      this.fetchTeamInfo();
    } else {
      this.setData({ loading: false });
    }
  },

  onGoToLogin() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  // ========== 获取团队信息：已解包，teams 是数组 ==========
  async fetchTeamInfo() {
    const userId = getAppInstance().globalData.userId;
    this.setData({ loading: true });
    try {
      const teamsRaw = await api.get('/teams', userId ? { user_id: userId } : {}) || [];
      const teams = Array.isArray(teamsRaw) ? teamsRaw : [];
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
        this.setData({ hasTeam: false, team: null, loading: false });
      }
    } catch (err) {
      console.error('获取团队信息失败:', err);
      this.setData({ loading: false });
    }
  },

  // ========== 获取团队详情：已解包为 team ==========
  async fetchTeamDetail(teamId) {
    try {
      const t = await api.get(`/teams/${teamId}`);
      const team = t || {};
      const members = team.members || [];
      this.setData({
        team,
        members,
        isLeader: team.leader_user_id === this.data.userId,
        isViceLeader: (team.vice_leader_ids || []).indexOf(this.data.userId) >= 0,
        currentViceLeaderIds: team.vice_leader_ids || []
      });
    } catch (err) {
      console.error('获取团队详情失败:', err);
    }
  },

  // ========== 获取预约统计：已解包为 stats ==========
  async fetchBookingStats(teamId) {
    try {
      const stats = await api.get(`/teams/${teamId}/booking-stats`);
      this.setData({ bookingStats: stats });
    } catch (err) {
      console.error('获取预约统计失败:', err);
    }
  },

  onOpenCreateModal() {
    this.setData({ showCreateModal: true, teamName: '', teamIntro: '' });
  },
  onCloseCreateModal() { this.setData({ showCreateModal: false }); },
  onTeamNameInput(e) { this.setData({ teamName: e.detail.value }); },
  onTeamIntroInput(e) { this.setData({ teamIntro: e.detail.value }); },

  async onCreateTeam() {
    const { teamName, teamIntro } = this.data;
    const userId = getAppInstance().globalData.userId;
    if (!teamName.trim()) { wx.showToast({ title: '请输入团队名称', icon: 'none' }); return; }
    if (this.data.creating) return;
    this.setData({ creating: true });
    try {
      await api.post('/teams', {
        name: teamName.trim(),
        intro: teamIntro.trim(),
        leader_user_id: userId
      });
      this.setData({ creating: false, showCreateModal: false });
      wx.showToast({ title: '团队创建成功', icon: 'success' });
      this.fetchTeamInfo();
    } catch (err) {
      this.setData({ creating: false });
      wx.showToast({ title: (err && err.message) || '创建失败，请重试', icon: 'none' });
    }
  },

  onOpenJoinModal() {
    this.setData({
      showJoinModal: true,
      searchKeyword: '',
      searchResults: [],
      searched: false,
      allTeams: [],
      allTeamsLoading: true
    });
    this.fetchAllTeams();
  },

  async fetchAllTeams() {
    try {
      const teamsRaw = await api.get('/teams') || [];
      const teams = Array.isArray(teamsRaw) ? teamsRaw : [];
      const myTeamIds = this.data.team ? [this.data.team.id] : [];
      const filtered = teams.filter(t => myTeamIds.indexOf(t.id) === -1);
      this.setData({ allTeams: filtered, allTeamsLoading: false });
    } catch (err) {
      console.error('获取全部团队失败:', err);
      this.setData({ allTeams: [], allTeamsLoading: false });
    }
  },

  onCloseJoinModal() { this.setData({ showJoinModal: false }); },
  onSearchInput(e) { this.setData({ searchKeyword: e.detail.value }); },

  async onSearchTeam() {
    const { searchKeyword } = this.data;
    if (!searchKeyword.trim()) { wx.showToast({ title: '请输入搜索关键词', icon: 'none' }); return; }
    this.setData({ searching: true, searched: false });
    try {
      const resRaw = await api.get('/teams', { keyword: searchKeyword.trim() }) || [];
      const results = Array.isArray(resRaw) ? resRaw : [];
      this.setData({ searching: false, searched: true, searchResults: results });
    } catch (err) {
      console.error('搜索团队失败:', err);
      this.setData({ searching: false, searched: true, searchResults: [] });
    }
  },

  async onJoinTeam(e) {
    const teamId = e.currentTarget.dataset.id;
    const userId = getAppInstance().globalData.userId;
    if (this.data.joiningTeamId === teamId) return;
    this.setData({ joiningTeamId: teamId });
    try {
      await api.post(`/teams/${teamId}/join`, { user_id: userId });
      this.setData({ joiningTeamId: null, showJoinModal: false, searchResults: [], searched: false, allTeams: [] });
      wx.showToast({ title: '加入成功', icon: 'success' });
      this.fetchTeamInfo();
    } catch (err) {
      this.setData({ joiningTeamId: null });
      wx.showToast({ title: (err && err.message) || '加入失败，请重试', icon: 'none' });
    }
  },

  onToggleMemberList() { this.setData({ showMemberList: !this.data.showMemberList }); },

  onOpenViceLeaderModal() {
    const { members, currentViceLeaderIds, userId } = this.data;
    const candidates = members.filter(m => m.user_id !== userId);
    this.setData({
      showViceLeaderModal: true,
      potentialViceLeaders: candidates,
      selectedViceLeaderIds: [...currentViceLeaderIds]
    });
  },
  onCloseViceLeaderModal() { this.setData({ showViceLeaderModal: false }); },

  onToggleViceLeader(e) {
    const userId = e.currentTarget.dataset.userId;
    let { selectedViceLeaderIds } = this.data;
    const idx = selectedViceLeaderIds.indexOf(userId);
    if (idx >= 0) {
      selectedViceLeaderIds.splice(idx, 1);
    } else {
      if (selectedViceLeaderIds.length >= 4) { wx.showToast({ title: '最多设置 4 名副队长', icon: 'none' }); return; }
      selectedViceLeaderIds.push(userId);
    }
    this.setData({ selectedViceLeaderIds: [...selectedViceLeaderIds] });
  },

  async onConfirmViceLeaders() {
    const { team, selectedViceLeaderIds } = this.data;
    try {
      await api.put(`/teams/${team.id}`, { vice_leader_ids: selectedViceLeaderIds });
      this.setData({ showViceLeaderModal: false });
      wx.showToast({ title: '副队长已更新', icon: 'success' });
      this.fetchTeamDetail(team.id);
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '更新失败', icon: 'none' });
    }
  },

  onOpenTransferModal() {
    const { members, userId } = this.data;
    const candidates = members.filter(m => m.user_id !== userId);
    if (candidates.length === 0) { wx.showToast({ title: '没有可转让的成员', icon: 'none' }); return; }
    this.setData({ showTransferModal: true, transferCandidates: candidates });
  },
  onCloseTransferModal() { this.setData({ showTransferModal: false }); },

  onTransferLeader(e) {
    const newLeaderId = e.currentTarget.dataset.userId;
    const { team } = this.data;
    wx.showModal({
      title: '确认转让',
      content: '转让队长后您将失去队长权限，确认继续？',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.put(`/teams/${team.id}`, { leader_user_id: newLeaderId });
          this.setData({ showTransferModal: false });
          wx.showToast({ title: '队长已转让', icon: 'success' });
          this.fetchTeamInfo();
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '转让失败', icon: 'none' });
        }
      }
    });
  },

  onOpenDissolveModal() { this.setData({ showDissolveModal: true }); },
  onCloseDissolveModal() { this.setData({ showDissolveModal: false }); },

  onDissolveTeam() {
    const { team } = this.data;
    wx.showModal({
      title: '解散团队',
      content: '此操作不可撤销，确认解散团队？',
      confirmText: '确认解散',
      confirmColor: '#d4644a',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.del(`/teams/${team.id}`);
          wx.showToast({ title: '团队已解散', icon: 'success' });
          this.setData({ showDissolveModal: false, hasTeam: false, team: null });
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '操作失败', icon: 'none' });
        }
      }
    });
  },

  onOpenLeaveModal() { this.setData({ showLeaveModal: true }); },
  onCloseLeaveModal() { this.setData({ showLeaveModal: false }); },

  onLeaveTeam() {
    const { team } = this.data;
    const userId = getAppInstance().globalData.userId;
    wx.showModal({
      title: '退出团队',
      content: '退出后您将无法代表团队预约，确认退出？',
      confirmText: '确认退出',
      confirmColor: '#d4644a',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.post(`/teams/${team.id}/leave`, { user_id: userId });
          wx.showToast({ title: '已退出团队', icon: 'success' });
          this.setData({ showLeaveModal: false, hasTeam: false, team: null });
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '操作失败', icon: 'none' });
        }
      }
    });
  },

  onToggleStatsDetail() {
    this.setData({ showStatsDetail: !this.data.showStatsDetail });
  }
});
