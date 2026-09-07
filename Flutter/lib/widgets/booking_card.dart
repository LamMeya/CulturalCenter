import 'package:flutter/material.dart';
import '../models/models.dart';
import '../utils/constants.dart';

class BookingCard extends StatelessWidget {
  final Booking booking;
  final String bookerName;
  final bool canCancel;
  final VoidCallback? onCancel;

  const BookingCard({
    super.key,
    required this.booking,
    required this.bookerName,
    this.canCancel = false,
    this.onCancel,
  });

  Color get _statusColor => AppColors.fromHex(booking.status.colorHex);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppColors.radius),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        booking.venueName,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: AppColors.text,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        booking.teamName,
                        style: TextStyle(fontSize: 13, color: AppColors.muted),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: _statusColor.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    booking.status.displayName,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _statusColor,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Divider(color: AppColors.border, height: 1, indent: 14, endIndent: 14),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              children: [
                _infoRow('预约日期', booking.timeSlot?.date ?? '--'),
                const SizedBox(height: 8),
                _infoRow(
                  '预约时间',
                  '${_formatTime(booking.timeSlot?.start)} - ${_formatTime(booking.timeSlot?.end)}',
                ),
                const SizedBox(height: 8),
                _infoRow('预约人', bookerName),
              ],
            ),
          ),
          if (canCancel) ...[
            Divider(color: AppColors.border, height: 1, indent: 14, endIndent: 14),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  OutlinedButton(
                    onPressed: onCancel,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red,
                      side: BorderSide(color: Colors.red.withOpacity(0.3)),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                    ),
                    child: const Text('取消预约'),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Row(
      children: [
        SizedBox(
          width: 56,
          child: Text(
            label,
            style: TextStyle(fontSize: 13, color: AppColors.muted),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            value,
            style: TextStyle(fontSize: 13, color: AppColors.text),
          ),
        ),
      ],
    );
  }

  String _formatTime(String? time) {
    if (time == null || time.isEmpty) return '--';
    return time.length > 5 ? time.substring(0, 5) : time;
  }
}
