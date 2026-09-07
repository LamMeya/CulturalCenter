import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class AppConstants {
  static const String appName = '珠海艺术中心';
  // Android 模拟器用 10.0.2.2 指向宿主机；iOS 模拟器/真机用 localhost 或 Mac IP
  static final String baseUrl = kIsWeb
      ? 'http://localhost:8000/api'
      : (Platform.isAndroid
          ? 'http://10.0.2.2:8000/api'
          : 'http://localhost:8000/api');
  static const String tokenKey = 'auth_token';
  static const String userKey = 'current_user';
}

class AppColors {
  static Color fromHex(String hex) {
    final buffer = StringBuffer();
    if (hex.length == 7 || hex.length == 9) buffer.write('ff');
    buffer.write(hex.replaceFirst('#', ''));
    return Color(int.parse(buffer.toString(), radix: 16));
  }

  static Color get primary => fromHex('#3d8e7a');
  static Color get primaryDark => fromHex('#2e6e5e');
  static Color get background => fromHex('#faf8f6');
  static Color get card => fromHex('#ffffff');
  static Color get border => fromHex('#ebe3da');
  static Color get text => fromHex('#211c18');
  static Color get muted => fromHex('#8c7b6a');
  static Color get accent => fromHex('#e8a840');

  static const double radius = 12.0;
}
