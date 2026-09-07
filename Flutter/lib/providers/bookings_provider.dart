import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';
import '../utils/date_utils.dart';

class BookingsProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<Booking> _bookings = [];
  bool _isLoading = false;
  String? _errorMessage;

  List<Booking> get bookings => _bookings;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> loadBookings(int userId) async {
    _setLoading(true);
    _errorMessage = null;

    try {
      final Map<String, String> queryParams = {};

      try {
        final teams = await _apiService.fetchTeams(userId: userId);
        if (teams.isNotEmpty) {
          queryParams['team_id'] = teams.first.id.toString();
        } else {
          queryParams['user_id'] = userId.toString();
        }
      } catch (_) {
        queryParams['user_id'] = userId.toString();
      }

      _bookings = await _apiService.fetchUserBookings(
        userId,
        queryParams: queryParams,
      );
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  Future<void> cancelBooking({required int userId, required Booking booking}) async {
    _errorMessage = null;

    try {
      await _apiService.cancelBooking(userId: userId, bookingId: booking.id);
      await loadBookings(userId);
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  bool canCancel(Booking booking) {
    final statusOk =
        booking.status == BookingStatus.pending || booking.status == BookingStatus.won;
    final date = booking.timeSlot?.date;
    final isFuture = date != null && AppDateUtils.isFutureOrToday(date);
    return statusOk && isFuture;
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  /// 退出登录时清空预约缓存，防止残留上一个用户的预约记录
  void clear() {
    _bookings = [];
    _errorMessage = null;
    notifyListeners();
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
