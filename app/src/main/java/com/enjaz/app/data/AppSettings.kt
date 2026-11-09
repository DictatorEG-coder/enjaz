package com.enjaz.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val CHANNEL_URL = "https://t.me/Dictator_channel2"
const val FOCUS_POST_URL = "https://t.me/Dictator_channel2/13"

class AppSettings(context: Context) {
    private val p = context.getSharedPreferences("enjaz_settings", Context.MODE_PRIVATE)

    private fun strSet(key: String): Set<String> = (p.getStringSet(key, emptySet()) ?: emptySet()).toSet()

    var mode by mutableStateOf(
        runCatching { Mode.valueOf(p.getString("mode", "MIXED") ?: "MIXED") }.getOrDefault(Mode.MIXED)
    ); private set
    var capacityMin by mutableStateOf(p.getInt("capacity", 180)); private set
    var telegramUrl by mutableStateOf((p.getString("telegram", "") ?: "").ifBlank { CHANNEL_URL }); private set
    var focusLinked by mutableStateOf(p.getBoolean("focus_linked", false)); private set
    var focusPackage by mutableStateOf(p.getString("focus_pkg", "") ?: ""); private set

    // مراقبة التطبيقات (تُفعَّل بموافقة المستخدم فقط)
    var monitoringEnabled by mutableStateOf(p.getBoolean("monitoring", p.getBoolean("focus_linked", false))); private set
    var monitored by mutableStateOf(strSet("mon_set")); private set
    var excluded by mutableStateOf(strSet("exc_set")); private set

    // الإشعارات التلقائية
    var notifEnabled by mutableStateOf(p.getBoolean("n_enabled", false)); private set
    var nAccount by mutableStateOf(p.getBoolean("n_account", true)); private set
    var nOverdue by mutableStateOf(p.getBoolean("n_overdue", true)); private set
    var nDistract by mutableStateOf(p.getBoolean("n_distract", true)); private set
    var nDeadline by mutableStateOf(p.getBoolean("n_deadline", true)); private set
    var nEvening by mutableStateOf(p.getBoolean("n_evening", true)); private set
    var startHour by mutableStateOf(p.getInt("n_start", 9)); private set
    var endHour by mutableStateOf(p.getInt("n_end", 23)); private set
    var eveningHour by mutableStateOf(p.getInt("n_evening_hour", 21)); private set
    var distractThreshold by mutableStateOf(p.getInt("n_thresh", 60)); private set

    fun updateMode(m: Mode) { mode = m; p.edit().putString("mode", m.name).apply() }

    fun updateCapacity(min: Int) {
        val v = min.coerceIn(30, 720)
        capacityMin = v; p.edit().putInt("capacity", v).apply()
    }

    fun updateFocusLinked(on: Boolean) { focusLinked = on; p.edit().putBoolean("focus_linked", on).apply() }

    fun updateFocusPackage(raw: String): String? {
        val v = raw.trim()
        if (v.isNotEmpty() && !Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(v))
            return "اسم الحزمة غير صالح. مثال: com.example.focus"
        focusPackage = v; p.edit().putString("focus_pkg", v).apply()
        return null
    }

    /** يقبل @name أو t.me/name أو https://t.me/name. الفراغ يعيد القناة الافتراضية. */
    fun updateTelegram(raw: String): String? {
        var v = raw.trim()
        if (v.isEmpty()) { telegramUrl = CHANNEL_URL; p.edit().putString("telegram", "").apply(); return null }
        if (v.startsWith("@")) v = "https://t.me/" + v.removePrefix("@")
        else if (v.startsWith("t.me/")) v = "https://$v"
        else if (v.startsWith("http://t.me/")) v = v.replace("http://", "https://")
        if (!Regex("https://t\\.me/[A-Za-z0-9_+/]{3,}").matches(v))
            return "رابط غير صالح. أدخل @اسم_القناة أو https://t.me/اسم_القناة"
        telegramUrl = v; p.edit().putString("telegram", v).apply()
        return null
    }

    // ---- المراقبة ----
    fun updateMonitoring(on: Boolean) { monitoringEnabled = on; p.edit().putBoolean("monitoring", on).apply() }

    fun effectiveMonitored(): Set<String> = (PRESET_APPS.keys + monitored) - excluded

    fun isMonitored(pkg: String): Boolean = pkg in effectiveMonitored()

    fun updateMonitored(pkg: String, on: Boolean) {
        val mon = monitored.toMutableSet()
        val exc = excluded.toMutableSet()
        if (on) { mon.add(pkg); exc.remove(pkg) } else { mon.remove(pkg); exc.add(pkg) }
        monitored = mon; excluded = exc
        p.edit().putStringSet("mon_set", mon).putStringSet("exc_set", exc).apply()
    }

    // ---- الإشعارات ----
    fun updateNotifEnabled(on: Boolean) { notifEnabled = on; p.edit().putBoolean("n_enabled", on).apply() }

    fun setNotifType(type: String, on: Boolean) {
        when (type) {
            "account" -> { nAccount = on; p.edit().putBoolean("n_account", on).apply() }
            "overdue" -> { nOverdue = on; p.edit().putBoolean("n_overdue", on).apply() }
            "distract" -> { nDistract = on; p.edit().putBoolean("n_distract", on).apply() }
            "deadline" -> { nDeadline = on; p.edit().putBoolean("n_deadline", on).apply() }
            "evening" -> { nEvening = on; p.edit().putBoolean("n_evening", on).apply() }
        }
    }

    fun isTypeOn(type: String): Boolean = when (type) {
        "account" -> nAccount
        "overdue" -> nOverdue
        "distract" -> nDistract
        "deadline" -> nDeadline
        "evening" -> nEvening
        else -> false
    }

    fun setWindow(start: Int, end: Int) {
        val s = start.coerceIn(0, 22)
        val e = end.coerceIn(s + 1, 24)
        startHour = s; endHour = e
        val ev = eveningHour.coerceIn(s, e - 1)
        eveningHour = ev
        p.edit().putInt("n_start", s).putInt("n_end", e).putInt("n_evening_hour", ev).apply()
    }

    fun updateEveningHour(h: Int) {
        val v = h.coerceIn(startHour, endHour - 1)
        eveningHour = v; p.edit().putInt("n_evening_hour", v).apply()
    }

    fun updateDistractThreshold(min: Int) {
        val v = min.coerceIn(15, 480)
        distractThreshold = v; p.edit().putInt("n_thresh", v).apply()
    }
}

/** يسجل أيام فتح التطبيق ويحسب الأيام الفائتة منذ آخر فتح. */
class OpenTracker(context: Context) {
    private val p = context.getSharedPreferences("enjaz_meta", Context.MODE_PRIVATE)

    fun openDates(): Set<String> =
        (p.getString("dates", "") ?: "").split(",").filter { it.isNotBlank() }.toSet()

    fun firstDate(): String? = openDates().minOrNull()

    /** عدد الأيام الكاملة التي فاتت دون فتح (0 إن فتح أمس أو اليوم أو أول مرة). */
    fun recordLaunch(): Int {
        val today = DateUtil.today()
        val last = p.getString("last", "") ?: ""
        val missed = if (last.isEmpty()) 0 else maxOf(0, DateUtil.daysBetween(last, today) - 1)
        val dates = (openDates() + today).sorted().takeLast(400)
        p.edit().putString("last", today).putString("dates", dates.joinToString(",")).apply()
        return missed
    }
}
