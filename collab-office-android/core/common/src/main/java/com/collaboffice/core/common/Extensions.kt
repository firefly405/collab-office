package com.collaboffice.core.common

import java.text.SimpleDateFormat
import java.util.*

fun Long.toDateString(pattern: String = "yyyy-MM-dd HH:mm"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> toDateString("MM-dd HH:mm")
    }
}

fun Long.toFileSizeString(): String = when {
    this < 1024 -> "${this}B"
    this < 1048576 -> "${this / 1024}KB"
    this < 1073741824 -> String.format("%.1fMB", this / 1048576.0)
    else -> String.format("%.2fGB", this / 1073741824.0)
}
