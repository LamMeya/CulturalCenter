import 'package:flutter/material.dart';
import '../utils/constants.dart';

class QuickActionRow extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String title;
  final String? subtitle;
  final VoidCallback? onTap;

  const QuickActionRow({
    super.key,
    required this.icon,
    required this.iconColor,
    required this.title,
    this.subtitle,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppColors.radius),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                color: iconColor.withOpacity(0.12),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon, size: 16, color: iconColor),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(fontSize: 15, color: AppColors.text),
                  ),
                  if (subtitle != null)
                    Text(
                      subtitle!,
                      style: TextStyle(fontSize: 12, color: AppColors.muted),
                    ),
                ],
              ),
            ),
            Icon(
              Icons.chevron_right,
              size: 18,
              color: AppColors.border,
            ),
          ],
        ),
      ),
    );
  }
}
