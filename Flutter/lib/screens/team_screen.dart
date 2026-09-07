import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/models.dart';
import '../providers/auth_provider.dart';
import '../providers/team_provider.dart';
import '../utils/constants.dart';
import '../widgets/empty_widget.dart';
import '../widgets/loading_widget.dart';
import '../widgets/team_member_row.dart';

class TeamScreen extends StatefulWidget {
  const TeamScreen({super.key});

  @override
  State<TeamScreen> createState() => _TeamScreenState();
}

class _TeamScreenState extends State<TeamScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadData());
  }

  void _loadData() {
    final user = context.read<AuthProvider>().user;
    context.read<TeamProvider>().loadTeamData(user?.teamId);
  }

  @override
  Widget build(BuildContext context) {
    final teamProvider = context.watch<TeamProvider>();
    final user = context.watch<AuthProvider>().user;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('团队', style: TextStyle(color: AppColors.text)),
        iconTheme: IconThemeData(color: AppColors.text),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        backgroundColor: AppColors.card,
        onRefresh: () async {
          await context.read<TeamProvider>().loadTeamData(user?.teamId);
        },
        child: _buildBody(teamProvider, user?.id, user?.teamId),
      ),
    );
  }

  Widget _buildBody(TeamProvider provider, int? userId, int? userTeamId) {
    if (provider.isLoading && provider.team == null) {
      return const Center(child: LoadingWidget(message: '加载中...'));
    }

    if (provider.team != null) {
      return _TeamDetailView(
        team: provider.team!,
        isMember: userTeamId == provider.team!.id,
        onLeave: () => _leaveTeam(provider),
      );
    }

    return _NoTeamView(
      onCreate: () => _showCreateSheet(provider),
      onJoin: () => _showJoinSheet(provider),
    );
  }

  Future<void> _leaveTeam(TeamProvider provider) async {
    final teamId = provider.team?.id;
    if (teamId == null) return;

    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('退出团队'),
        content: const Text('确定要退出当前团队吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('退出', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      await provider.leaveTeam(teamId: teamId);
      if (mounted) await context.read<AuthProvider>().refreshUser();
    }
  }

  void _showCreateSheet(TeamProvider provider) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _CreateTeamSheet(
        onCreate: (name, intro) async {
          await provider.createTeam(
            name: name,
            intro: intro,
          );
          if (mounted) {
            Navigator.pop(context);
            await context.read<AuthProvider>().refreshUser();
          }
        },
      ),
    );
  }

  void _showJoinSheet(TeamProvider provider) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _JoinTeamSheet(
        teams: provider.teams,
        onJoin: (teamId) async {
          await provider.joinTeam(teamId: teamId);
          if (mounted) {
            Navigator.pop(context);
            await context.read<AuthProvider>().refreshUser();
          }
        },
      ),
    );
  }
}

class _NoTeamView extends StatelessWidget {
  final VoidCallback onCreate;
  final VoidCallback onJoin;

  const _NoTeamView({required this.onCreate, required this.onJoin});

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        const SizedBox(height: 60),
        EmptyWidget(
          icon: Icons.people,
          title: '你还没有加入团队',
          subtitle: '加入或创建一个团队，即可预约场地',
        ),
        const SizedBox(height: 32),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 40),
          child: Column(
            children: [
              _actionButton(
                label: '创建团队',
                icon: Icons.add_circle,
                filled: true,
                onTap: onCreate,
              ),
              const SizedBox(height: 12),
              _actionButton(
                label: '加入团队',
                icon: Icons.person_add,
                filled: false,
                onTap: onJoin,
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _actionButton({
    required String label,
    required IconData icon,
    required bool filled,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: filled ? AppColors.primary : null,
          border: filled
              ? null
              : Border.all(color: AppColors.primary, width: 1.5),
          borderRadius: BorderRadius.circular(AppColors.radius),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: filled ? Colors.white : AppColors.primary),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: filled ? Colors.white : AppColors.primary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TeamDetailView extends StatelessWidget {
  final Team team;
  final bool isMember;
  final VoidCallback onLeave;

  const _TeamDetailView({
    required this.team,
    required this.isMember,
    required this.onLeave,
  });

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _teamHeader(),
        const SizedBox(height: 16),
        _membersSection(),
        if (isMember) ...[
          const SizedBox(height: 16),
          _leaveButton(),
        ],
      ],
    );
  }

  Widget _teamHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppColors.radius),
      ),
      child: Column(
        children: [
          CircleAvatar(
            radius: 36,
            backgroundColor: AppColors.primary,
            child: Text(
              team.name.toString().isNotEmpty
                  ? team.name.toString()[0]
                  : '?',
              style: const TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            team.name,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.text,
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: Text(
              team.intro,
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: AppColors.muted),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _statItem('${team.memberCount}', '成员'),
              _statItem('${team.maxMembers}', '上限'),
              _statItem(team.leaderName, '团长'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statItem(String value, String label) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(fontSize: 12, color: AppColors.muted),
        ),
      ],
    );
  }

  Widget _membersSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.only(left: 4, bottom: 8),
          child: Text(
            '团队成员',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        Container(
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: BorderRadius.circular(AppColors.radius),
          ),
          child: Column(
            children: team.members.asMap().entries.map((entry) {
              final member = entry.value;
              final isLast = entry.key == team.members.length - 1;
              return Column(
                children: [
                  TeamMemberRow(member: member),
                  if (!isLast)
                    Divider(
                      color: AppColors.border,
                      height: 1,
                      indent: 56,
                    ),
                ],
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _leaveButton() {
    return OutlinedButton(
      onPressed: onLeave,
      style: OutlinedButton.styleFrom(
        foregroundColor: Colors.red,
        side: BorderSide(color: Colors.red.withOpacity(0.4)),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppColors.radius),
        ),
        padding: const EdgeInsets.symmetric(vertical: 14),
      ),
      child: const Text('退出团队'),
    );
  }
}

class _CreateTeamSheet extends StatefulWidget {
  final Function(String name, String intro) onCreate;

  const _CreateTeamSheet({required this.onCreate});

  @override
  State<_CreateTeamSheet> createState() => _CreateTeamSheetState();
}

class _CreateTeamSheetState extends State<_CreateTeamSheet> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _introController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _introController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).viewInsets.bottom;

    return Padding(
      padding: EdgeInsets.only(bottom: bottom),
      child: Container(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '创建团队',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: AppColors.text,
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _nameController,
              decoration: InputDecoration(
                labelText: '团队名称',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppColors.radius),
                ),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _introController,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: '团队简介',
                alignLabelWithHint: true,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppColors.radius),
                ),
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                if (_nameController.text.trim().isNotEmpty) {
                  widget.onCreate(
                    _nameController.text.trim(),
                    _introController.text.trim(),
                  );
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppColors.radius),
                ),
              ),
              child: const Text('创建'),
            ),
          ],
        ),
      ),
    );
  }
}

class _JoinTeamSheet extends StatelessWidget {
  final List<dynamic> teams;
  final ValueChanged<int> onJoin;

  const _JoinTeamSheet({required this.teams, required this.onJoin});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.6,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '加入团队',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: AppColors.text,
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close),
                ),
              ],
            ),
          ),
          if (teams.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Text(
                '暂无可加入的团队',
                style: TextStyle(color: AppColors.muted),
              ),
            )
          else
            Expanded(
              child: ListView.builder(
                itemCount: teams.length,
                itemBuilder: (context, index) {
                  final team = teams[index];
                  return ListTile(
                    title: Text(
                      team.name,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        color: AppColors.text,
                      ),
                    ),
                    subtitle: Text(
                      '${team.memberCount}/${team.maxMembers}人 | ${team.leaderName}',
                      style: TextStyle(fontSize: 13, color: AppColors.muted),
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => onJoin(team.id as int),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}
