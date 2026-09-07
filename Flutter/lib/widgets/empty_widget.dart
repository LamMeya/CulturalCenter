import 'package:flutter/material.dart';
import '../utils/constants.dart';

class EmptyWidget extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;

  const EmptyWidget({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 60),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            size: 56,
            color: AppColors.muted.withOpacity(0.3),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.text,
            ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 8),
            Text(
              subtitle!,
              style: TextStyle(fontSize: 14, color: AppColors.muted),
            ),
          ],
        ],
      ),
    );
  }
}
