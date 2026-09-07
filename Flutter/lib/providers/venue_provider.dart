import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class VenueProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<Venue> _venues = [];
  bool _isLoading = false;
  String? _errorMessage;
  List<AppNotification> _notifications = [];

  List<Venue> get venues => _venues;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  /// 首页使用：取第一个有内容的通知作为活跃横幅
  AppNotification? get activeNotification {
    for (final n in _notifications) {
      if ((n.content?.isNotEmpty ?? false) || (n.title?.isNotEmpty ?? false)) {
        return n;
      }
    }
    return _notifications.isNotEmpty ? _notifications.first : null;
  }

  List<AppNotification> get notifications => _notifications;

  Future<void> loadData() async {
    _setLoading(true);
    _errorMessage = null;

    try {
      final results = await Future.wait([
        _apiService.fetchVenues(),
        _apiService.fetchActiveNotifications(),
      ]);

      _venues = results[0] as List<Venue>;
      _notifications = results[1] as List<AppNotification>;
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

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
