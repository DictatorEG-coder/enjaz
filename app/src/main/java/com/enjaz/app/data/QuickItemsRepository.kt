package com.enjaz.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class QuickItemKind(val label: String) {
    COURSE("كورس"), VIDEO("فيديو"), APP("تطبيق"), WORK("عمل"), IDEA("فكرة"), OTHER("شيء آخر")
}

data class QuickItem(
    val id: Long, val title: String, val url: String, val details: String,
    val kind: QuickItemKind, val reminderHours: Int, val createdAt: Long,
    val done: Boolean = false, val lastReminderAt: Long = 0L
)

/** تخزين مستقل يحافظ على ملف الدراسة القديم دون ترحيل أو مخاطرة بفقد البيانات. */
class QuickItemsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("enjaz_quick_items", Context.MODE_PRIVATE)
    private var state by mutableStateOf(loadItems())
    val items: List<QuickItem> get() = state

    private fun loadItems(): List<QuickItem> = try {
        val array = JSONArray(prefs.getString("items", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            QuickItem(
                id = o.optLong("id"), title = o.optString("title"), url = o.optString("url"),
                details = o.optString("details"),
                kind = runCatching { QuickItemKind.valueOf(o.optString("kind")) }.getOrDefault(QuickItemKind.OTHER),
                reminderHours = o.optInt("hours", 24).coerceIn(1, 168),
                createdAt = o.optLong("created", 0L), done = o.optBoolean("done", false),
                lastReminderAt = o.optLong("lastReminder", 0L)
            )
        }
    } catch (_: Exception) { emptyList() }

    private fun persist(next: List<QuickItem>): Boolean = try {
        val array = JSONArray()
        next.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title).put("url", item.url)
                .put("details", item.details).put("kind", item.kind.name).put("hours", item.reminderHours)
                .put("created", item.createdAt).put("done", item.done).put("lastReminder", item.lastReminderAt))
        }
        prefs.edit().putString("items", array.toString()).commit().also { if (it) state = next }
    } catch (_: Exception) { false }

    fun add(titleRaw: String, urlRaw: String, detailsRaw: String, kind: QuickItemKind, hoursRaw: String): String? {
        val title = titleRaw.trim()
        val url = urlRaw.trim()
        val details = detailsRaw.trim()
        if (title.isBlank()) return "اكتب اسم الشيء الذي تريد إنجازه."
        if (title.length > 100) return "العنوان طويل جدًا."
        if (url.isNotBlank() && !url.matches(Regex("https?://\\S+"))) return "الرابط يجب أن يبدأ بـ https:// أو http://"
        if (url.length > 1500 || details.length > 500) return "الرابط أو الملاحظات أطول من المسموح."
        val hours = hoursRaw.trim().toIntOrNull() ?: return "اكتب عدد ساعات صحيحًا للتذكير."
        if (hours !in 1..168) return "الفاصل يجب أن يكون من ساعة إلى أسبوع."
        val now = System.currentTimeMillis()
        val item = QuickItem(
            id = now, title = title, url = url, details = details, kind = kind,
            reminderHours = hours, createdAt = now, lastReminderAt = now
        )
        return if (persist(state + item)) null else "تعذّر حفظ العنصر."
    }

    fun setDone(id: Long, done: Boolean): Boolean =
        persist(state.map { if (it.id == id) it.copy(done = done) else it })
    fun delete(id: Long): Boolean = persist(state.filterNot { it.id == id })
    fun markReminded(id: Long, at: Long): Boolean =
        persist(state.map { if (it.id == id) it.copy(lastReminderAt = at) else it })
    fun pendingCount(): Int = state.count { !it.done }
    fun doneCount(): Int = state.count { it.done }
}
