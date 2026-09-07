import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();
  final AuthService _authService = AuthService();

  bool _isLoggedIn = false;
  User? _user;
  String? _token;
  bool _isLoading = false;
  String? _errorMessage;

  bool get isLoggedIn => _isLoggedIn;
  User? get user => _user;
  String? get token => _token;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  AuthProvider() {
    _restoreState();
  }

  Future<void> _restoreState() async {
    _token = await _authService.getToken();
    _user = await _authService.getUser();
    _isLoggedIn = _token != null && _token!.isNotEmpty && _user != null;
    notifyListeners();
  }

  Future<void> _saveState() async {
    if (_token != null) await _authService.setToken(_token!);
    if (_user != null) await _authService.setUser(_user!);
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> login(String username, String password) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      final response = await _apiService.login(
        username: username.trim(),
        password: password,
      );
      _token = response.token;
      // 先用登录返回的基础 user，再调用 refreshUser 拉取完整最新资料
      _user = response.user;
      _isLoggedIn = true;
      await _saveState();
      notifyListeners();
      await refreshUser();
    } catch (e) {
      _errorMessage = e.toString();
      // 失败清理（避免残留上一个用户的 token/user）
      _token = null;
      _user = null;
      _isLoggedIn = false;
      await _authService.clearAll();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> register({
    required String username,
    required String password,
    required String nickname,
    required String phone,
  }) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      final response = await _apiService.register(
        username: username.trim(),
        password: password,
        nickname: nickname.trim().isEmpty ? username.trim() : nickname.trim(),
        phone: phone.trim(),
      );
      _token = response.token;
      // 先用注册返回的基础 user，再调用 refreshUser 拉取完整最新资料
      _user = response.user;
      _isLoggedIn = true;
      await _saveState();
      notifyListeners();
      await refreshUser();
    } catch (e) {
      _errorMessage = e.toString();
      // 失败清理（避免残留上一个用户的 token/user）
      _token = null;
      _user = null;
      _isLoggedIn = false;
      await _authService.clearAll();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> logout() async {
    await _authService.clearAll();
    _token = null;
    _user = null;
    _isLoggedIn = false;
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> refreshUser() async {
    final userId = _user?.id;
    if (userId == null) return;

    try {
      final updated = await _apiService.fetchUserProfile(userId);
      _user = updated;
      await _saveState();
      notifyListeners();
    } catch (e) {
      debugPrint('[AuthProvider] refreshUser failed: $e');
    }
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
