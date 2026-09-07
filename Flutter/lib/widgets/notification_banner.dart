import 'package:flutter/material.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class NotificationBanner extends StatelessWidget {
  final AppNotification notification;

  const NotificationBanner({super.key, required this.notification});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
      decoration: BoxDecoration(
        color: AppColors.accent.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 6,
            height: 6,
            margin: const EdgeInsets.only(top: 6),
            decoration: BoxDecoration(
              color: AppColors.accent,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              notification.content ?? '',
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 13, color: AppColors.text),
            ),
          ),
        ],
      ),
    );
  }
}
