import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  SharedPreferences? _prefs;

  Future<SharedPreferences> get _preferences async {
    _prefs ??= await SharedPreferences.getInstance();
    return _prefs!;
  }

  Future<void> setToken(String token) async {
    final prefs = await _preferences;
    await prefs.setString(AppConstants.tokenKey, token);
  }

  Future<String?> getToken() async {
    final prefs = await _preferences;
    return prefs.getString(AppConstants.tokenKey);
  }

  Future<void> clearToken() async {
    final prefs = await _preferences;
    await prefs.remove(AppConstants.tokenKey);
  }

  Future<void> setUser(User user) async {
    final prefs = await _preferences;
    await prefs.setString(AppConstants.userKey, jsonEncode(user.toJson()));
  }

  Future<User?> getUser() async {
    final prefs = await _preferences;
    final raw = prefs.getString(AppConstants.userKey);
    if (raw == null || raw.isEmpty) return null;
    try {
      return User.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }

  Future<void> clearUser() async {
    final prefs = await _preferences;
    await prefs.remove(AppConstants.userKey);
  }

  Future<void> clearAll() async {
    await Future.wait([clearToken(), clearUser()]);
  }
}
