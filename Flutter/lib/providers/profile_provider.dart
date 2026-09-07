import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class ProfileProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  Team? _team;
  bool _isLoading = false;
  String? _errorMessage;

  Team? get team => _team;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> loadTeamInfo(int? teamId) async {
    if (teamId == null) {
      _team = null;
      notifyListeners();
      return;
    }

    _setLoading(true);
    _errorMessage = null;

    try {
      _team = await _apiService.fetchTeamDetail(teamId);
    } catch (e) {
      _errorMessage = e.toString();
      debugPrint('[ProfileProvider] loadTeamInfo failed: $e');
    } finally {
      _setLoading(false);
    }
  }

  /// 退出登录时清空团队缓存，防止残留上一个用户的团队信息
  void clear() {
    _team = null;
    _errorMessage = null;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
