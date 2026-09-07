package com.meya.doumenculture.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
    private val dayFormatter = DateTimeFormatter.ofPattern("d")
    private val monthFormatter = DateTimeFormatter.ofPattern("M月")
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.CHINESE)

    fun today(): LocalDate = LocalDate.now()

    fun formatApi(date: LocalDate): String = date.format(apiFormatter)

    fun formatDisplay(date: LocalDate): String = date.format(displayFormatter)

    fun formatDay(date: LocalDate): String = date.format(dayFormatter)

    fun formatMonth(date: LocalDate): String = date.format(monthFormatter)

    fun formatWeekday(date: LocalDate): String = date.format(weekdayFormatter)

    fun parseApi(date: String): LocalDate = LocalDate.parse(date, apiFormatter)

    /**
     * 生成未来可用日期，默认 14 天，排除周一
     */
    fun nextAvailableDates(days: Int = 14): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var date = today()
        while (result.size < days) {
            if (date.dayOfWeek != DayOfWeek.MONDAY) {
                result.add(date)
            }
            date = date.plusDays(1)
        }
        return result
    }

    fun formatTimeRange(start: String?, end: String?): String {
        val s = start?.take(5) ?: "--"
        val e = end?.take(5) ?: "--"
        return "$s - $e"
    }
}

/**
 * 顶层函数包装：Compose 代码可直接 import formatTimeRange
 */
fun formatTimeRange(start: String?, end: String?): String =
    DateUtils.formatTimeRange(start, end)
