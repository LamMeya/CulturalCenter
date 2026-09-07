import 'dart:math' show pi, sin;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../utils/constants.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SingleTickerProviderStateMixin {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  late AnimationController _waveController;

  @override
  void initState() {
    super.initState();
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 3),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  bool get _canLogin {
    return _usernameController.text.trim().isNotEmpty &&
        _passwordController.text.isNotEmpty;
  }

  Future<void> _login() async {
    if (!_canLogin) return;
    FocusScope.of(context).unfocus();

    final provider = context.read<AuthProvider>();
    await provider.login(
      _usernameController.text.trim(),
      _passwordController.text,
    );

    if (provider.errorMessage != null && mounted) {
      _showError(provider.errorMessage!);
      provider.clearError();
    }
  }

  void _showError(String message) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('登录失败'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<AuthProvider>().isLoading;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SingleChildScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        child: Column(
          children: [
            _buildHeader(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
              child: Column(
                children: [
                  _inputField(
                    icon: Icons.person,
                    hint: '请输入账号',
                    controller: _usernameController,
                    onChanged: (_) => setState(() {}),
                  ),
                  const SizedBox(height: 16),
                  _inputField(
                    icon: Icons.lock,
                    hint: '请输入密码',
                    controller: _passwordController,
                    obscureText: true,
                    onChanged: (_) => setState(() {}),
                    onSubmitted: (_) => _login(),
                  ),
                  const SizedBox(height: 24),
                  _loginButton(isLoading),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const RegisterScreen(),
                      ),
                    ),
                    child: Text(
                      '没有账号？立即注册',
                      style: TextStyle(color: AppColors.primary),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '登录即表示同意《用户协议》和《隐私政策》',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return AnimatedBuilder(
      animation: _waveController,
      builder: (context, child) {
        return Container(
          height: 260,
          width: double.infinity,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [AppColors.primary, AppColors.primaryDark],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: const BorderRadius.vertical(
              bottom: Radius.circular(32),
            ),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              Positioned(
                bottom: 0,
                left: 0,
                right: 0,
                child: CustomPaint(
                  size: const Size(double.infinity, 50),
                  painter: _WavePainter(_waveController.value),
                ),
              ),
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircleAvatar(
                    radius: 40,
                    backgroundColor: Colors.white.withOpacity(0.2),
                    child: const Icon(
                      Icons.account_balance,
                      size: 40,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    '珠海文化中心',
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '场地预约平台',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.white.withOpacity(0.9),
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _inputField({
    required IconData icon,
    required String hint,
    required TextEditingController controller,
    bool obscureText = false,
    ValueChanged<String>? onChanged,
    ValueChanged<String>? onSubmitted,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppColors.radius),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: TextField(
        controller: controller,
        obscureText: obscureText,
        onChanged: onChanged,
        onSubmitted: onSubmitted,
        decoration: InputDecoration(
          prefixIcon: Icon(icon, size: 20, color: AppColors.muted),
          hintText: hint,
          hintStyle: TextStyle(color: AppColors.muted),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 16),
        ),
      ),
    );
  }

  Widget _loginButton(bool isLoading) {
    final enabled = _canLogin && !isLoading;

    return GestureDetector(
      onTap: enabled ? _login : null,
      child: Container(
        width: double.infinity,
        height: 52,
        decoration: BoxDecoration(
          gradient: enabled
              ? LinearGradient(
                  colors: [AppColors.primary, AppColors.primaryDark],
                )
              : LinearGradient(
                  colors: [
                    AppColors.muted.withOpacity(0.4),
                    AppColors.muted.withOpacity(0.3),
                  ],
                ),
          borderRadius: BorderRadius.circular(26),
          boxShadow: enabled
              ? [
                  BoxShadow(
                    color: AppColors.primary.withOpacity(0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 4),
                  ),
                ]
              : null,
        ),
        alignment: Alignment.center,
        child: isLoading
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  color: Colors.white,
                  strokeWidth: 2.5,
                ),
              )
            : const Text(
                '登录',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }
}

class _WavePainter extends CustomPainter {
  final double progress;

  _WavePainter(this.progress);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.background
      ..style = PaintingStyle.fill;

    final path = Path();
    path.moveTo(0, size.height * 0.6);

    for (double x = 0; x <= size.width; x++) {
          final relativeX = x / size.width;
          final sine =
              sin(relativeX * pi * 2 + progress * pi * 2);
          final y = size.height * 0.5 + sine * 10;
          path.lineTo(x, y);
        }

    path.lineTo(size.width, size.height);
    path.lineTo(0, size.height);
    path.close();

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _WavePainter oldDelegate) => true;
}
