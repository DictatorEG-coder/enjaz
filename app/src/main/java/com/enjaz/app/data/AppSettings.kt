package com.enjaz.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppSettings(context: Context) {
    private val p = context.getSharedPreferences("enjaz_settings", Context.MODE_PRIVATE)

    var mode by mutableStateOf(
        runCatching { Mode.valueOf(p.getString("mode", "MIXED") ?: "MIXED") }.getOrDefault(Mode.MIXED)
    ); private set
    var capacityMin by mutableStateOf(p.getInt("capacity", 180)); private set
    var telegramUrl by mutableStateOf(p.getString("telegram", "") ?: ""); private set
    var focusLinked by mutableStateOf(p.getBoolean("focus_linked", false)); private set
    var focusPackage by mutableStateOf(p.getString("focus_pkg", "") ?: ""); private set

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

    /** يقبل @name أو t.me/name أو https://t.me/name، ويحفظ الصيغة الكاملة. الفراغ يمسح الإعداد. */
    fun updateTelegram(raw: String): String? {
        var v = raw.trim()
        if (v.isEmpty()) { telegramUrl = ""; p.edit().putString("telegram", "").apply(); return null }
        if (v.startsWith("@")) v = "https://t.me/" + v.removePrefix("@")
        else if (v.startsWith("t.me/")) v = "https://$v"
        else if (v.startsWith("http://t.me/")) v = v.replace("http://", "https://")
        if (!Regex("https://t\\.me/[A-Za-z0-9_+/]{3,}").matches(v))
            return "رابط غير صالح. أدخل @اسم_القناة أو https://t.me/اسم_القناة"
        telegramUrl = v; p.edit().putString("telegram", v).apply()
        return null
    }
}

/** يسجل أيام فتح التطبيق ويحسب الأيام الفائتة منذ آخر فتح. */
class OpenTracker(context: Context) {
    private val p = context.getSharedPreferences("enjaz_meta", Context.MODE_PRIVATE)

    fun openDates(): Set<String> =
        (p.getString("dates", "") ?: "").split(",").filter { it.isNotBlank() }.toSet()

    /** يُرجع عدد الأيام الكاملة التي فاتت دون فتح (0 إن فتح أمس أو اليوم أو أول مرة). */
    fun recordLaunch(): Int {
        val today = DateUtil.today()
        val last = p.getString("last", "") ?: ""
        val missed = if (last.isEmpty()) 0 else maxOf(0, DateUtil.daysBetween(last, today) - 1)
        val dates = (openDates() + today).sorted().takeLast(400)
        p.edit().putString("last", today).putString("dates", dates.joinToString(",")).apply()
        return missed
    }
}
