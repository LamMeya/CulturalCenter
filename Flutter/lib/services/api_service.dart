import 'dart:convert';
import 'package:dio/dio.dart';
import '../models/models.dart';
import '../utils/constants.dart';
import 'auth_service.dart';

class ApiException implements Exception {
  final String message;
  final int? code;

  ApiException(this.message, {this.code});

  @override
  String toString() => message;
}

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal() {
    _dio = Dio(
      BaseOptions(
        baseUrl: AppConstants.baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 60),
        headers: {'Accept': 'application/json'},
        contentType: 'application/json',
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          if (options.extra['requiresAuth'] != false) {
            final token = await _authService.getToken();
            if (token != null && token.isNotEmpty) {
              options.headers['Authorization'] = 'Bearer $token';
            }
          }
          handler.next(options);
        },
      ),
    );
  }

  late final Dio _dio;
  final AuthService _authService = AuthService();

  static String _parseError(DioException err) {
    final response = err.response;
    if (response == null) {
      return err.message ?? '网络请求失败';
    }
    final data = response.data;
    if (data is Map<String, dynamic>) {
      // FastAPI 4xx/5xx 格式 { detail: ... } 或统一包装 { message: ..., code: ... }
      if (data.containsKey('message')) {
        return data['message'] as String? ?? '请求失败';
      }
      if (data.containsKey('detail')) {
        return data['detail'] as String? ?? '请求失败';
      }
    }
    return '请求失败（${response.statusCode}）';
  }

  /// 统一解析 2xx 返回体：
  /// 后端 200/201 统一格式为 { code: 0, message: "ok", data: T }
  /// code != 0 视为业务错误
  /// T 为列表/对象/EmptyResponse；data 为 null 时列表类型返回空数组
  static bool _isListResponseType<T>() {
    final tStr = T.toString();
    return tStr.startsWith('List<');
  }

  T _unwrapResponseBody<T>(Object? body, T Function(dynamic) fromJsonT) {
    if (body is Map<String, dynamic> &&
        body.containsKey('code') &&
        body.containsKey('message') &&
        body.containsKey('data')) {
      final int code = body['code'] as int? ?? -1;
      final String message = body['message'] as String? ?? '';
      if (code != 0) {
        throw ApiException(message, code: code);
      }
      final rawData = body['data'];
      if (rawData != null) {
        // 防御：后端本应返回列表却返回空对象 {} 时，列表类型直接返回空数组，
        // 避免把 Map 强转成 List 抛 type cast 错误。
        // 用 const <Never>[]：List<Never> 是所有 List<T> 的子类型，as T 安全通过。
        if (rawData is Map && _isListResponseType<T>()) {
          return const <Never>[] as T;
        }
        return fromJsonT(rawData);
      }
      // data == null：按常见 T 类型兜底
      if (T == EmptyResponse) {
        return EmptyResponse() as T;
      }
      if (T == List<Venue>) {
        return <Venue>[] as T;
      }
      if (T == List<TimeSlot>) {
        return <TimeSlot>[] as T;
      }
      if (T == List<Team>) {
        return <Team>[] as T;
      }
      if (T == List<Booking>) {
        return <Booking>[] as T;
      }
      if (T == List<AppNotification>) {
        return <AppNotification>[] as T;
      }
      throw ApiException('后端返回 data 为空', code: code);
    }

    // 非标准包装（健康检查、非 JSON 响应、下载接口等）：直接把 body 交给 fromJsonT
    // 但为了避免把 { code,message,data } 意外交给 fromJsonT，上面已经排除该分支
    // 因此这里仅用于真正无包装的情况
    return fromJsonT(body);
  }

  Future<T> _request<T>({
    required String method,
    required String path,
    Map<String, dynamic>? queryParameters,
    Object? data,
    bool requiresAuth = true,
    required T Function(dynamic) fromJsonT,
  }) async {
    try {
      final response = await _dio.request(
        path,
        queryParameters: queryParameters,
        data: data,
        options: Options(method: method, extra: {'requiresAuth': requiresAuth}),
      );

      final statusCode = response.statusCode;
      if (statusCode == 200 || statusCode == 201) {
        return _unwrapResponseBody<T>(response.data, fromJsonT);
      }
      throw ApiException('请求失败（$statusCode）', code: statusCode);
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) {
        throw ApiException('登录已过期，请重新登录', code: 401);
      }
      throw ApiException(_parseError(e), code: e.response?.statusCode);
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException(e.toString());
    }
  }

  // —— Auth ——

  Future<LoginResponse> login({
    required String username,
    required String password,
  }) async {
    return _request(
      method: 'POST',
      path: '/users/login/password',
      data: PasswordLoginRequest(username: username, password: password).toJson(),
      requiresAuth: false,
      fromJsonT: (json) => LoginResponse.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<LoginResponse> register({
    required String username,
    required String password,
    required String nickname,
    required String phone,
  }) async {
    return _request(
      method: 'POST',
      path: '/users/register',
      data: RegisterRequest(
        username: username,
        password: password,
        nickname: nickname,
        phone: phone,
      ).toJson(),
      requiresAuth: false,
      fromJsonT: (json) => LoginResponse.fromJson(json as Map<String, dynamic>),
    );
  }

  // —— Venues ——

  Future<List<Venue>> fetchVenues() async {
    return _request(
      method: 'GET',
      path: '/venues',
      fromJsonT: (json) => (json as List<dynamic>)
          .map((e) => Venue.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<Venue> fetchVenueDetail(int id) async {
    return _request(
      method: 'GET',
      path: '/venues/$id',
      fromJsonT: (json) => Venue.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<List<TimeSlot>> fetchTimeSlots(int venueId, String date) async {
    return _request(
      method: 'GET',
      path: '/venues/$venueId/time-slots',
      queryParameters: {'date': date},
      fromJsonT: (json) => (json as List<dynamic>)
          .map((e) => TimeSlot.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  // —— Teams ——

  Future<List<Team>> fetchTeams({int? userId}) async {
    return _request(
      method: 'GET',
      path: '/teams',
      queryParameters: userId != null ? {'user_id': userId.toString()} : null,
      fromJsonT: (json) => (json as List<dynamic>)
          .map((e) => Team.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<Team> createTeam({
    required String name,
    required String intro,
  }) async {
    return _request(
      method: 'POST',
      path: '/teams',
      data: CreateTeamRequest(name: name, intro: intro).toJson(),
      fromJsonT: (json) => Team.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<Team> fetchTeamDetail(int id) async {
    return _request(
      method: 'GET',
      path: '/teams/$id',
      fromJsonT: (json) => Team.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<Team> joinTeam({required int teamId}) async {
    return _request(
      method: 'POST',
      path: '/teams/$teamId/join',
      data: JoinTeamRequest(teamId: teamId).toJson(),
      fromJsonT: (json) => Team.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<EmptyResponse> leaveTeam({required int teamId}) async {
    return _request(
      method: 'POST',
      path: '/teams/$teamId/leave',
      data: {},
      fromJsonT: (_) => EmptyResponse(),
    );
  }

  // —— Bookings ——

  Future<EmptyResponse> createBooking({
    required int venueId,
    required int teamId,
    required List<int> timeSlotIds,
  }) async {
    return _request(
      method: 'POST',
      path: '/bookings',
      data: CreateBookingRequest(
        venueId: venueId,
        teamId: teamId,
        timeSlotIds: timeSlotIds,
      ).toJson(),
      // 后端返回 { message, booking_ids }，不是 Booking 对象
      fromJsonT: (_) => EmptyResponse(),
    );
  }

  Future<List<Booking>> fetchUserBookings(
    int userId, {
    Map<String, String>? queryParams,
  }) async {
    return _request(
      method: 'GET',
      path: '/users/$userId/bookings',
      queryParameters: queryParams,
      fromJsonT: (json) => (json as List<dynamic>)
          .map((e) => Booking.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<EmptyResponse> cancelBooking({required int userId, required int bookingId}) async {
    return _request(
      method: 'POST',
      path: '/users/$userId/bookings/$bookingId/cancel',
      // 后端返回 { message, redraw_booking_id }，不是 Booking 对象
      fromJsonT: (_) => EmptyResponse(),
    );
  }

  // —— User ——

  Future<User> fetchUserProfile(int userId) async {
    return _request(
      method: 'GET',
      path: '/users/$userId/profile',
      fromJsonT: (json) => User.fromJson(json as Map<String, dynamic>),
    );
  }

  // —— Notifications ——
  // 后端 /notifications/published 返回单个通知对象（无通知时为
  // {"content": null, "notif_type": null}），这里适配两种形态：
  //  - List：兼容后端未来改为数组
  //  - Map：单条通知，有意义则包成 [notification]，空占位则返回 []
  Future<List<AppNotification>> fetchActiveNotifications() async {
    try {
      return _request(
        method: 'GET',
        path: '/notifications/published',
        fromJsonT: (json) {
          if (json is List) {
            return json
                .map((e) => AppNotification.fromJson(e as Map<String, dynamic>))
                .toList();
          }
          if (json is Map<String, dynamic>) {
            final n = AppNotification.fromJson(json);
            final hasContent =
                (n.content?.isNotEmpty ?? false) ||
                (n.title?.isNotEmpty ?? false) ||
                n.id != null;
            return hasContent ? [n] : <AppNotification>[];
          }
          return <AppNotification>[];
        },
      );
    } catch (_) {
      return <AppNotification>[];
    }
  }
}
