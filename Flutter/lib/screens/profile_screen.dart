import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/bookings_provider.dart';
import '../providers/profile_provider.dart';
import '../providers/team_provider.dart';
import '../utils/constants.dart';
import '../widgets/quick_action_row.dart';
import 'bookings_screen.dart';
import 'team_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final teamId = context.read<AuthProvider>().user?.teamId;
      context.read<ProfileProvider>().loadTeamInfo(teamId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;
    final profileProvider = context.watch<ProfileProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('我的', style: TextStyle(color: AppColors.text)),
        iconTheme: IconThemeData(color: AppColors.text),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _UserInfoCard(user: user),
            if (profileProvider.team != null) ...[
              const SizedBox(height: 16),
              _TeamInfoCard(team: profileProvider.team),
            ],
            const SizedBox(height: 16),
            _QuickActionsSection(),
            const SizedBox(height: 16),
            _SettingsSection(),
            const SizedBox(height: 16),
            _LogoutButton(),
          ],
        ),
      ),
    );
  }
}

class _UserInfoCard extends StatelessWidget {
  final dynamic user;

  const _UserInfoCard({this.user});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppColors.radius),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 32,
            backgroundColor: AppColors.primary,
            child: Text(
              (user?.nickname ?? '?').toString().isNotEmpty
                  ? (user?.nickname ?? '?').toString()[0]
                  : '?',
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  user?.nickname ?? '未知用户',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: AppColors.text,
                  ),
                ),
                if (user?.phone != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    user.phone,
                    style: TextStyle(fontSize: 14, color: AppColors.muted),
                  ),
                ],
              ],
            ),
          ),
          Icon(Icons.chevron_right, color: AppColors.muted),
        ],
      ),
    );
  }
}

class _TeamInfoCard extends StatelessWidget {
  final dynamic team;

  const _TeamInfoCard({this.team});

  @override
  Widget build(BuildContext context) {
    final role = context.watch<AuthProvider>().user?.teamRole;

    return InkWell(
      onTap: () => Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => const TeamScreen()),
      ),
      borderRadius: BorderRadius.circular(AppColors.radius),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(AppColors.radius),
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 22,
              backgroundColor: AppColors.accent.withOpacity(0.15),
              child: Icon(Icons.people, color: AppColors.accent),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    team?.name ?? '我的团队',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w500,
                      color: AppColors.text,
                    ),
                  ),
                  if (role != null)
                    Text(
                      _roleDisplayName(role),
                      style: TextStyle(fontSize: 13, color: AppColors.muted),
                    ),
                ],
              ),
            ),
            Icon(Icons.chevron_right, color: AppColors.muted),
          ],
        ),
      ),
    );
  }

  String _roleDisplayName(String role) {
    switch (role) {
      case 'leader':
        return '团长';
      case 'vice_leader':
        return '副团长';
      case 'member':
        return '团员';
      default:
        return role;
    }
  }
}

class _QuickActionsSection extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return _SectionCard(
      title: '快捷操作',
      children: [
        QuickActionRow(
          icon: Icons.calendar_month,
          iconColor: AppColors.primary,
          title: '我的预约',
          subtitle: '查看预约记录',
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const BookingsScreen()),
          ),
        ),
        Divider(color: AppColors.border, height: 1, indent: 52),
        QuickActionRow(
          icon: Icons.people,
          iconColor: AppColors.accent,
          title: '我的团队',
          subtitle: '管理团队成员',
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const TeamScreen()),
          ),
        ),
        Divider(color: AppColors.border, height: 1, indent: 52),
        const QuickActionRow(
          icon: Icons.notifications,
          iconColor: Color(0xFF8c7b6a),
          title: '消息通知',
          subtitle: '查看系统通知',
        ),
      ],
    );
  }
}

class _SettingsSection extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return _SectionCard(
      title: '设置',
      children: [
        const QuickActionRow(
          icon: Icons.settings,
          iconColor: Color(0xFF8c7b6a),
          title: '通用设置',
        ),
        Divider(color: AppColors.border, height: 1, indent: 52),
        const QuickActionRow(
          icon: Icons.description,
          iconColor: Color(0xFF8c7b6a),
          title: '用户协议',
        ),
        Divider(color: AppColors.border, height: 1, indent: 52),
        const QuickActionRow(
          icon: Icons.lock,
          iconColor: Color(0xFF8c7b6a),
          title: '隐私政策',
        ),
        Divider(color: AppColors.border, height: 1, indent: 52),
        const QuickActionRow(
          icon: Icons.info,
          iconColor: Color(0xFF8c7b6a),
          title: '关于',
          subtitle: 'v1.0.0',
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _SectionCard({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(
            title,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
              color: AppColors.muted,
            ),
          ),
        ),
        Container(
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: BorderRadius.circular(AppColors.radius),
          ),
          child: Column(children: children),
        ),
      ],
    );
  }
}

class _LogoutButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: () async {
          final confirm = await showDialog<bool>(
            context: context,
            builder: (_) => AlertDialog(
              title: const Text('退出登录'),
              content: const Text('退出后需要重新登录才能使用。'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: const Text('取消'),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(context, true),
                  child: const Text('确定退出',
                      style: TextStyle(color: Colors.red)),
                ),
              ],
            ),
          );

          if (confirm == true && context.mounted) {
            await context.read<AuthProvider>().logout();
            // 清空其他 Provider 缓存，防止残留上一个用户数据
            if (context.mounted) {
              context.read<ProfileProvider>().clear();
              context.read<TeamProvider>().clear();
              context.read<BookingsProvider>().clear();
            }
          }
        },
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.card,
          foregroundColor: Colors.red,
          elevation: 0,
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppColors.radius),
          ),
        ),
        child: const Text(
          '退出登录',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
        ),
      ),
    );
  }
}
