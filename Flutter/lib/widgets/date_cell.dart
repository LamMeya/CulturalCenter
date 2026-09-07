import 'package:flutter/material.dart';
import '../utils/constants.dart';
import '../utils/date_utils.dart';

class DateCell extends StatelessWidget {
  final DateTime date;
  final bool isSelected;
  final VoidCallback? onTap;

  const DateCell({
    super.key,
    required this.date,
    this.isSelected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        width: 56,
        height: 72,
        decoration: BoxDecoration(
          color: isSelected ? AppColors.primary : AppColors.card,
          borderRadius: BorderRadius.circular(AppColors.radius),
          border: isSelected
              ? null
              : Border.all(color: AppColors.border),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 2,
              offset: const Offset(0, 1),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              AppDateUtils.formatWeekday(date),
              style: TextStyle(
                fontSize: 12,
                color: isSelected ? Colors.white : AppColors.muted,
              ),
            ),
            Text(
              AppDateUtils.formatDay(date),
              style: TextStyle(
                fontSize: 18,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                color: isSelected ? Colors.white : AppColors.text,
              ),
            ),
            Text(
              AppDateUtils.formatMonth(date),
              style: TextStyle(
                fontSize: 11,
                color: isSelected
                    ? Colors.white.withOpacity(0.8)
                    : AppColors.muted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
