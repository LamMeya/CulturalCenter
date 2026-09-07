import 'package:flutter/material.dart';
import '../utils/constants.dart';

class LoadingWidget extends StatelessWidget {
  final String? message;
  final double topPadding;

  const LoadingWidget({
    super.key,
    this.message,
    this.topPadding = 100,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(top: topPadding),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircularProgressIndicator(color: AppColors.primary),
          if (message != null) ...[
            const SizedBox(height: 12),
            Text(
              message!,
              style: TextStyle(fontSize: 14, color: AppColors.muted),
            ),
          ],
        ],
      ),
    );
  }
}
