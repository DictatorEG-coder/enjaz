package com.enjaz.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    private val pattern = Regex("\\d{4}-\\d{2}-\\d{2}")
    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    fun isValid(s: String): Boolean {
        if (!pattern.matches(s)) return false
        return try { fmt().parse(s) != null } catch (e: Exception) { false }
    }

    fun today(): String = fmt().format(Date())

    fun dateOf(ms: Long): String = fmt().format(Date(ms))

    /** بداية اليوم (منتصف الليل) لتاريخ بصيغة yyyy-MM-dd. */
    fun dayStart(s: String): Long = fmt().parse(s)?.time ?: 0L

    fun startOfToday(): Long = dayStart(today())

    fun plus(s: String, days: Int): String {
        val c = Calendar.getInstance()
        c.timeInMillis = dayStart(s)
        c.add(Calendar.DAY_OF_YEAR, days)
        return fmt().format(c.time)
    }

    fun daysBetween(from: String, to: String): Int =
        Math.round((dayStart(to) - dayStart(from)) / 86400000.0).toInt()

    /** عدد الأيام من اليوم حتى التاريخ (سالب إن مضى). */
    fun daysUntil(s: String): Int = daysBetween(today(), s)
}
