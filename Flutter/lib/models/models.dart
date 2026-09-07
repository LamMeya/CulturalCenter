import 'dart:convert';

enum SlotPeriod {
  morning,
  afternoon;

  String get value {
    switch (this) {
      case SlotPeriod.morning:
        return 'morning';
      case SlotPeriod.afternoon:
        return 'afternoon';
    }
  }

  String get displayName {
    switch (this) {
      case SlotPeriod.morning:
        return '上午';
      case SlotPeriod.afternoon:
        return '下午';
    }
  }

  static SlotPeriod fromString(String value) {
    return SlotPeriod.values.firstWhere(
      (e) => e.value == value,
      orElse: () => SlotPeriod.morning,
    );
  }
}

enum BookingStatus {
  pending,
  won,
  lost,
  cancelled;

  String get value {
    switch (this) {
      case BookingStatus.pending:
        return 'pending';
      case BookingStatus.won:
        return 'won';
      case BookingStatus.lost:
        return 'lost';
      case BookingStatus.cancelled:
        return 'cancelled';
    }
  }

  String get displayName {
    switch (this) {
      case BookingStatus.pending:
        return '待抽签';
      case BookingStatus.won:
        return '已中签';
      case BookingStatus.lost:
        return '未中签';
      case BookingStatus.cancelled:
        return '已取消';
    }
  }

  String get colorHex {
    switch (this) {
      case BookingStatus.pending:
        return '#e8a840';
      case BookingStatus.won:
        return '#3d8e7a';
      case BookingStatus.lost:
      case BookingStatus.cancelled:
        return '#8c7b6a';
    }
  }

  static BookingStatus fromString(String value) {
    return BookingStatus.values.firstWhere(
      (e) => e.value == value,
      orElse: () => BookingStatus.pending,
    );
  }
}

enum TeamRole {
  leader,
  viceLeader,
  member;

  String get value {
    switch (this) {
      case TeamRole.leader:
        return 'leader';
      case TeamRole.viceLeader:
        return 'vice_leader';
      case TeamRole.member:
        return 'member';
    }
  }

  String get displayName {
    switch (this) {
      case TeamRole.leader:
        return '团长';
      case TeamRole.viceLeader:
        return '副团长';
      case TeamRole.member:
        return '团员';
    }
  }

  static TeamRole fromString(String value) {
    return TeamRole.values.firstWhere(
      (e) => e.value == value,
      orElse: () => TeamRole.member,
    );
  }
}

class User {
  final int id;
  String nickname;
  String? phone;
  String? avatarUrl;
  int? teamId;
  String? teamName;
  String? teamRole;

  User({
    required this.id,
    required this.nickname,
    this.phone,
    this.avatarUrl,
    this.teamId,
    this.teamName,
    this.teamRole,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      nickname: json['nickname'] as String? ?? '',
      phone: json['phone'] as String?,
      avatarUrl: json['avatar_url'] as String?,
      teamId: json['team_id'] as int?,
      teamName: json['team_name'] as String?,
      teamRole: json['team_role'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'nickname': nickname,
      'phone': phone,
      'avatar_url': avatarUrl,
      'team_id': teamId,
      'team_name': teamName,
      'team_role': teamRole,
    };
  }

  User copyWith({
    int? id,
    String? nickname,
    String? phone,
    String? avatarUrl,
    int? teamId,
    String? teamName,
    String? teamRole,
  }) {
    return User(
      id: id ?? this.id,
      nickname: nickname ?? this.nickname,
      phone: phone ?? this.phone,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      teamId: teamId ?? this.teamId,
      teamName: teamName ?? this.teamName,
      teamRole: teamRole ?? this.teamRole,
    );
  }
}

class Venue {
  final int id;
  String name;
  String? imageUrl;
  String description;
  String capacity;
  String area;
  List<String> facilities;
  String address;
  bool isOpen;

  Venue({
    required this.id,
    required this.name,
    this.imageUrl,
    this.description = '',
    this.capacity = '',
    this.area = '',
    this.facilities = const [],
    this.address = '',
    this.isOpen = false,
  });

  factory Venue.fromJson(Map<String, dynamic> json) {
    List<String> parseFacilities(dynamic value) {
      if (value == null) return [];
      if (value is List) {
        return value.map((e) => e.toString()).toList();
      }
      if (value is String) {
        try {
          final decoded = jsonDecode(value);
          if (decoded is List) {
            return decoded.map((e) => e.toString()).toList();
          }
        } catch (_) {}
      }
      return [];
    }

    return Venue(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      imageUrl: json['image_url'] as String?,
      description: json['description'] as String? ?? '',
      capacity: json['capacity'] as String? ?? '',
      area: json['area'] as String? ?? '',
      facilities: parseFacilities(json['facilities']),
      address: json['address'] as String? ?? '',
      isOpen: json['is_active'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'image_url': imageUrl,
      'description': description,
      'capacity': capacity,
      'area': area,
      'facilities': facilities,
      'address': address,
      'is_active': isOpen,
    };
  }
}

class TimeSlot {
  final int id;
  int venueId;
  String availableDate;
  String startTime;
  String endTime;
  SlotPeriod period;
  bool isOpen;
  bool isBooked;

  TimeSlot({
    required this.id,
    required this.venueId,
    required this.availableDate,
    required this.startTime,
    required this.endTime,
    required this.period,
    this.isOpen = false,
    this.isBooked = false,
  });

  factory TimeSlot.fromJson(Map<String, dynamic> json) {
    return TimeSlot(
      id: json['id'] as int,
      venueId: json['venue_id'] as int? ?? 0,
      availableDate: json['available_date'] as String? ?? '',
      startTime: json['start_time'] as String? ?? '',
      endTime: json['end_time'] as String? ?? '',
      period: SlotPeriod.fromString(json['period'] as String? ?? 'morning'),
      isOpen: json['is_open'] as bool? ?? false,
      isBooked: json['is_booked'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'venue_id': venueId,
      'available_date': availableDate,
      'start_time': startTime,
      'end_time': endTime,
      'period': period.value,
      'is_open': isOpen,
      'is_booked': isBooked,
    };
  }
}

class BookingTimeSlot {
  String? date;
  String? start;
  String? end;

  BookingTimeSlot({this.date, this.start, this.end});

  factory BookingTimeSlot.fromJson(Map<String, dynamic>? json) {
    if (json == null) return BookingTimeSlot();
    return BookingTimeSlot(
      date: json['date'] as String?,
      start: json['start'] as String?,
      end: json['end'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {'date': date, 'start': start, 'end': end};
  }
}

class Booking {
  final int id;
  String venueName;
  String teamName;
  BookingTimeSlot? timeSlot;
  BookingStatus status;
  String? cancelReason;
  String createdAt;

  Booking({
    required this.id,
    required this.venueName,
    required this.teamName,
    this.timeSlot,
    required this.status,
    this.cancelReason,
    required this.createdAt,
  });

  factory Booking.fromJson(Map<String, dynamic> json) {
    return Booking(
      id: json['id'] as int,
      venueName: json['venue_name'] as String? ?? '',
      teamName: json['team_name'] as String? ?? '',
      timeSlot: BookingTimeSlot.fromJson(json['time_slot'] as Map<String, dynamic>?),
      status: BookingStatus.fromString(json['status'] as String? ?? 'pending'),
      cancelReason: json['cancel_reason'] as String?,
      createdAt: json['created_at'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'venue_name': venueName,
      'team_name': teamName,
      'time_slot': timeSlot?.toJson(),
      'status': status.value,
      'cancel_reason': cancelReason,
      'created_at': createdAt,
    };
  }
}

class TeamMember {
  final int id;
  int userId;
  String nickname;
  String? avatarUrl;
  TeamRole role;

  TeamMember({
    required this.id,
    required this.userId,
    required this.nickname,
    this.avatarUrl,
    required this.role,
  });

  factory TeamMember.fromJson(Map<String, dynamic> json) {
    return TeamMember(
      id: json['id'] as int,
      userId: json['user_id'] as int? ?? 0,
      nickname: json['nickname'] as String? ?? '',
      avatarUrl: json['avatar_url'] as String?,
      role: TeamRole.fromString(json['role'] as String? ?? 'member'),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'user_id': userId,
      'nickname': nickname,
      'avatar_url': avatarUrl,
      'role': role.value,
    };
  }
}

class Team {
  final int id;
  String name;
  String intro;
  int memberCount;
  int maxMembers;
  int? leaderId;
  String leaderName;
  List<TeamMember> members;
  String createdAt;

  Team({
    required this.id,
    required this.name,
    required this.intro,
    required this.memberCount,
    required this.maxMembers,
    this.leaderId,
    required this.leaderName,
    required this.members,
    required this.createdAt,
  });

  factory Team.fromJson(Map<String, dynamic> json) {
    return Team(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      intro: json['intro'] as String? ?? '',
      memberCount: json['member_count'] as int? ?? 0,
      maxMembers: json['max_members'] as int? ?? 0,
      leaderId: json['leader_id'] as int?,
      leaderName: json['leader_name'] as String? ?? '',
      members: (json['members'] as List<dynamic>?)
              ?.map((e) => TeamMember.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      createdAt: json['created_at'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'intro': intro,
      'member_count': memberCount,
      'max_members': maxMembers,
      'leader_id': leaderId,
      'leader_name': leaderName,
      'members': members.map((e) => e.toJson()).toList(),
      'created_at': createdAt,
    };
  }
}

class AppNotification {
  final int? id;
  String? title;
  String? content;
  String? notifType;

  AppNotification({this.id, this.title, this.content, this.notifType});

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: json['id'] as int?,
      title: json['title'] as String?,
      content: json['content'] as String?,
      notifType: json['notif_type'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'content': content,
      'notif_type': notifType,
    };
  }
}

class LoginResponse {
  final String token;
  final User user;

  LoginResponse({required this.token, required this.user});

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      token: json['token'] as String? ?? '',
      user: User.fromJson(json['user'] as Map<String, dynamic>),
    );
  }

  Map<String, dynamic> toJson() {
    return {'token': token, 'user': user.toJson()};
  }
}

class PasswordLoginRequest {
  final String username;
  final String password;

  PasswordLoginRequest({required this.username, required this.password});

  Map<String, dynamic> toJson() {
    return {'username': username, 'password': password};
  }
}

class RegisterRequest {
  final String username;
  final String password;
  final String nickname;
  final String phone;

  RegisterRequest({
    required this.username,
    required this.password,
    required this.nickname,
    required this.phone,
  });

  Map<String, dynamic> toJson() {
    return {
      'username': username,
      'password': password,
      'nickname': nickname,
      'phone': phone,
    };
  }
}

class CreateBookingRequest {
  final int venueId;
  final int teamId;
  final List<int> timeSlotIds;

  CreateBookingRequest({
    required this.venueId,
    required this.teamId,
    required this.timeSlotIds,
  });

  Map<String, dynamic> toJson() {
    return {
      'venue_id': venueId,
      'team_id': teamId,
      'time_slot_ids': timeSlotIds,
    };
  }
}

class CreateTeamRequest {
  final String name;
  final String intro;

  CreateTeamRequest({required this.name, required this.intro});

  Map<String, dynamic> toJson() {
    return {'name': name, 'intro': intro};
  }
}

class JoinTeamRequest {
  final int teamId;

  JoinTeamRequest({required this.teamId});

  Map<String, dynamic> toJson() {
    return {'team_id': teamId};
  }
}

class LeaveTeamRequest {
  final int userId;

  LeaveTeamRequest({required this.userId});

  Map<String, dynamic> toJson() {
    return {'user_id': userId};
  }
}

class ApiResponse<T> {
  final int code;
  final String message;
  final T? data;

  ApiResponse({required this.code, required this.message, this.data});

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic) fromJsonT,
  ) {
    return ApiResponse(
      code: json['code'] as int? ?? -1,
      message: json['message'] as String? ?? '',
      data: json['data'] != null ? fromJsonT(json['data']) : null,
    );
  }
}

class EmptyResponse {
  EmptyResponse();
  factory EmptyResponse.fromJson(Map<String, dynamic> _) => EmptyResponse();
  Map<String, dynamic> toJson() => {};
}
