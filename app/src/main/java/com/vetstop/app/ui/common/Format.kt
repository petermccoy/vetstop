package com.vetstop.app.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

/** "Never", "Today", "Yesterday", "12 days ago", ... */
fun formatLastVisit(lastVisitAt: Long?, now: Long = System.currentTimeMillis()): String {
    if (lastVisitAt == null) return "Never"
    val days = TimeUnit.MILLISECONDS.toDays(now - lastVisitAt)
    return when {
        days <= 0 -> "Today"
        days == 1L -> "Yesterday"
        days < 60 -> "$days days ago"
        else -> "${days / 30} months ago"
    }
}
