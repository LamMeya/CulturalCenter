import 'package:flutter/material.dart';
import '../utils/constants.dart';

class AppErrorWidget extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;

  const AppErrorWidget({
    super.key,
    required this.message,
    this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 60, horizontal: 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.wifi_off,
            size: 48,
            color: AppColors.muted.withOpacity(0.4),
          ),
          const SizedBox(height: 16),
          Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: AppColors.muted),
          ),
          if (onRetry != null) ...[
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: onRetry,
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.primary,
                side: BorderSide(color: AppColors.primary),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text('重试'),
            ),
          ],
        ],
      ),
    );
  }
}
