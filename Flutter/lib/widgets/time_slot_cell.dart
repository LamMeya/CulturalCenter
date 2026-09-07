import 'package:flutter/material.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class TimeSlotCell extends StatelessWidget {
  final TimeSlot slot;
  final bool isSelected;
  final VoidCallback? onTap;

  const TimeSlotCell({
    super.key,
    required this.slot,
    this.isSelected = false,
    this.onTap,
  });

  bool get _enabled => slot.isOpen && !slot.isBooked;

  Color get _textColor {
    if (isSelected) return Colors.white;
    if (!_enabled) return AppColors.muted;
    return AppColors.text;
  }

  Color get _backgroundColor {
    if (isSelected) return AppColors.primary;
    if (!_enabled) return AppColors.border.withOpacity(0.3);
    return AppColors.card;
  }

  Color get _borderColor {
    if (isSelected) return AppColors.primary;
    return AppColors.border;
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _enabled ? onTap : null,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: _backgroundColor,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: _borderColor, width: isSelected ? 2 : 1),
        ),
        child: Text(
          '${slot.startTime} - ${slot.endTime}',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: _textColor,
          ),
        ),
      ),
    );
  }
}
