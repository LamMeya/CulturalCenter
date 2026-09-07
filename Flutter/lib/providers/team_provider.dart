import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class TeamProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  Team? _team;
  List<Team> _teams = [];
  bool _isLoading = false;
  String? _errorMessage;

  Team? get team => _team;
  List<Team> get teams => _teams;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> loadTeamData(int? currentUserTeamId) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      _teams = await _apiService.fetchTeams();

      if (currentUserTeamId != null) {
        try {
          _team = await _apiService.fetchTeamDetail(currentUserTeamId);
        } catch (e) {
          _team = null;
        }
      }
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> createTeam({
    required String name,
    required String intro,
  }) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      _team = await _apiService.createTeam(
        name: name.trim(),
        intro: intro.trim(),
      );
      await loadTeamData(_team?.id);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> joinTeam({required int teamId}) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      _team = await _apiService.joinTeam(teamId: teamId);
      await loadTeamData(_team?.id);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> leaveTeam({required int teamId}) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      await _apiService.leaveTeam(teamId: teamId);
      _team = null;
      await loadTeamData(null);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  /// 退出登录时清空团队缓存，防止残留上一个用户的团队信息
  void clear() {
    _team = null;
    _teams = [];
    _errorMessage = null;
    notifyListeners();
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
