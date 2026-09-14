package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Fast, thread-safe, zero-allocation date and time formatting utilities.
 * Avoids instantiating heavy SimpleDateFormat objects on the main UI thread during list scroll.
 */
object DateTimeUtils {

    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    }

    private val monthDayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM d", Locale.getDefault())
    }

    // Cache formatted timestamp strings by timestamp (rounded to minute) to avoid repeated formatting during scroll
    private val timestampCache = ConcurrentHashMap<Long, String>(128)
    private val callTimestampCache = ConcurrentHashMap<Long, String>(128)
    private val lastSeenCache = ConcurrentHashMap<Long, String>(128)
    private val storyTimestampCache = ConcurrentHashMap<Long, String>(128)

    fun formatMessageTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        // Round to nearest minute for cache lookup
        val minuteKey = timestamp / 60_000L
        return timestampCache.computeIfAbsent(minuteKey) {
            timeFormat.get()?.format(Date(timestamp)) ?: ""
        }
    }

    fun formatCallTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Recent"
        val minuteKey = timestamp / 60_000L
        return callTimestampCache.computeIfAbsent(minuteKey) {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val oneDay = 24 * 60 * 60 * 1000L

            when {
                diff < 60 * 1000L -> "Just now"
                diff < oneDay -> "Today, ${timeFormat.get()?.format(Date(timestamp))}"
                diff < 2 * oneDay -> "Yesterday, ${timeFormat.get()?.format(Date(timestamp))}"
                else -> dateFormat.get()?.format(Date(timestamp)) ?: "Recent"
            }
        }
    }

    fun formatDateShort(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return monthDayFormat.get()?.format(Date(timestamp)) ?: ""
    }

    fun formatLastSeen(timestamp: Long): String {
        if (timestamp <= 0L) return "Offline"
        val minuteKey = timestamp / 60_000L
        return lastSeenCache.computeIfAbsent(minuteKey) {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            when {
                diff < 60_000L -> "Just now"
                diff < 3600_000L -> "${diff / 60_000L}m ago"
                diff < 86400_000L -> "${diff / 3600_000L}h ago"
                else -> monthDayFormat.get()?.format(Date(timestamp)) ?: "Offline"
            }
        }
    }

    fun formatStoryTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Just now"
        val minuteKey = timestamp / 60_000L
        return storyTimestampCache.computeIfAbsent(minuteKey) {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            when {
                diff < 60_000L -> "Just now"
                diff < 3600_000L -> "${diff / 60_000L}m ago"
                diff < 86400_000L -> "${diff / 3600_000L}h ago"
                else -> timeFormat.get()?.format(Date(timestamp)) ?: "Recently"
            }
        }
    }

    fun formatActiveUsageTime(timeMs: Long): String {
        if (timeMs <= 0L) return "0m"
        val totalSecs = timeMs / 1000L
        if (totalSecs < 60L) return "${totalSecs}s"

        val totalMins = totalSecs / 60L
        val hours = totalMins / 60L
        val mins = totalMins % 60L
        val days = hours / 24L
        val remHours = hours % 24L

        return when {
            days > 0L -> if (remHours > 0L) "${days}d ${remHours}h" else "${days}d"
            hours > 0L -> if (mins > 0L) "${hours}h ${mins}m" else "${hours}h"
            else -> "${mins}m"
        }
    }
}
