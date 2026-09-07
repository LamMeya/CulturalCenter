import 'package:intl/intl.dart';

class AppDateUtils {
  static final DateFormat _apiFormat = DateFormat('yyyy-MM-dd');
  static final DateFormat _displayFormat = DateFormat('M月d日 EEEE', 'zh_CN');
  static final DateFormat _weekdayFormat = DateFormat('EEE', 'zh_CN');
  static final DateFormat _monthFormat = DateFormat('M月', 'zh_CN');
  static final DateFormat _dayFormat = DateFormat('d');

  static String formatApiDate(DateTime date) => _apiFormat.format(date);

  static String formatDisplayDate(DateTime date) => _displayFormat.format(date);

  static String formatWeekday(DateTime date) => _weekdayFormat.format(date);

  static String formatMonth(DateTime date) => _monthFormat.format(date);

  static String formatDay(DateTime date) => _dayFormat.format(date);

  static String formatTimeRange(String start, String end) => '$start - $end';

  static String todayString() => _apiFormat.format(DateTime.now());

  /// 生成未来 14 个可用日期，排除周一（weekday == DateTime.monday，即 1）
  static List<DateTime> nextAvailableDates({int count = 14}) {
    final List<DateTime> dates = [];
    final now = DateTime.now();
    var date = DateTime(now.year, now.month, now.day);

    while (dates.length < count) {
      if (date.weekday != DateTime.monday) {
        dates.add(date);
      }
      date = date.add(const Duration(days: 1));
    }
    return dates;
  }

  static bool isSameDay(DateTime a, DateTime b) {
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  static bool isFutureOrToday(String dateString) {
    final date = _apiFormat.parse(dateString);
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    return date.isAfter(todayDate.subtract(const Duration(days: 1)));
  }
}
