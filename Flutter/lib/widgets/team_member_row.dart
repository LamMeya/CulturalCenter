import 'package:flutter/material.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class TeamMemberRow extends StatelessWidget {
  final TeamMember member;

  const TeamMemberRow({super.key, required this.member});

  Color get _roleColor {
    switch (member.role) {
      case TeamRole.leader:
        return AppColors.accent;
      case TeamRole.viceLeader:
        return AppColors.primary;
      case TeamRole.member:
        return AppColors.muted;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Row(
        children: [
          CircleAvatar(
            radius: 20,
            backgroundColor: AppColors.primary.withOpacity(0.15),
            child: Text(
              member.nickname.isNotEmpty ? member.nickname[0] : '?',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: AppColors.primary,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  member.nickname,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppColors.text,
                  ),
                ),
                Text(
                  member.role.displayName,
                  style: TextStyle(fontSize: 12, color: _roleColor),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
