import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../utils/constants.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _nicknameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();

  @override
  void dispose() {
    _usernameController.dispose();
    _nicknameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  bool get _canRegister {
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    final confirm = _confirmPasswordController.text;
    return username.isNotEmpty &&
        password.isNotEmpty &&
        password == confirm &&
        password.length >= 4;
  }

  Future<void> _register() async {
    if (!_canRegister) return;
    FocusScope.of(context).unfocus();

    final provider = context.read<AuthProvider>();
    await provider.register(
      username: _usernameController.text.trim(),
      password: _passwordController.text,
      nickname: _nicknameController.text.trim(),
      phone: _phoneController.text.trim(),
    );

    if (provider.isLoggedIn && mounted) {
      Navigator.pop(context);
    } else if (provider.errorMessage != null && mounted) {
      _showError(provider.errorMessage!);
      provider.clearError();
    }
  }

  void _showError(String message) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('注册失败'),
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
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        iconTheme: IconThemeData(color: AppColors.text),
        title: Text('注册账号', style: TextStyle(color: AppColors.text)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 20),
        child: Column(
          children: [
            _inputField(
              icon: Icons.person,
              hint: '账号（用于登录）',
              controller: _usernameController,
            ),
            const SizedBox(height: 16),
            _inputField(
              icon: Icons.face,
              hint: '昵称',
              controller: _nicknameController,
            ),
            const SizedBox(height: 16),
            _inputField(
              icon: Icons.phone,
              hint: '手机号（选填）',
              controller: _phoneController,
              keyboardType: TextInputType.phone,
            ),
            const SizedBox(height: 16),
            _inputField(
              icon: Icons.lock,
              hint: '密码',
              controller: _passwordController,
              obscureText: true,
            ),
            const SizedBox(height: 16),
            _inputField(
              icon: Icons.lock_outline,
              hint: '确认密码',
              controller: _confirmPasswordController,
              obscureText: true,
            ),
            const SizedBox(height: 24),
            _registerButton(isLoading),
          ],
        ),
      ),
    );
  }

  Widget _inputField({
    required IconData icon,
    required String hint,
    required TextEditingController controller,
    bool obscureText = false,
    TextInputType keyboardType = TextInputType.text,
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
        keyboardType: keyboardType,
        onChanged: (_) => setState(() {}),
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

  Widget _registerButton(bool isLoading) {
    final enabled = _canRegister && !isLoading;

    return GestureDetector(
      onTap: enabled ? _register : null,
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
                '注册',
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
