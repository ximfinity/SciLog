package com.scilog.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDate(ms: Long): String = dateFormat.format(Date(ms))
    fun formatTime(ms: Long): String = timeFormat.format(Date(ms))
    fun formatDateTime(ms: Long): String = dateTimeFormat.format(Date(ms))
    fun toDateKey(ms: Long): String = dateKeyFormat.format(Date(ms))

    fun startOfDayMs(ms: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = ms
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun hoursAgo(ms: Long): Double =
        (System.currentTimeMillis() - ms) / 3_600_000.0

    fun relativeTime(ms: Long): String {
        val diff = System.currentTimeMillis() - ms
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> formatDate(ms)
        }
    }
}
