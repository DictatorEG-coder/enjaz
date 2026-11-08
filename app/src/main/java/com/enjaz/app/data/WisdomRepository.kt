package com.enjaz.app.data

import android.content.Context

class WisdomRepository(context: Context) {
    private val prefs = context.getSharedPreferences("enjaz_wisdom", Context.MODE_PRIVATE)

    /** دورة عشوائية لا تكرر حكمة قبل استنفاد القائمة، ولا تبدأ الدورة الجديدة بآخر حكمة معروضة. */
    fun nextWisdom(): Wisdom {
        val total = WisdomBank.all.size
        var queue = prefs.getString("queue", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() }
            .filter { it in 0 until total }.toMutableList()
        val last = prefs.getInt("last", -1)
        if (queue.isEmpty()) {
            queue = (0 until total).shuffled().toMutableList()
            if (total > 1 && queue.first() == last) {
                val first = queue.removeAt(0); queue.add(first)
            }
        }
        val id = queue.removeAt(0)
        prefs.edit().putString("queue", queue.joinToString(",")).putInt("last", id).apply()
        return WisdomBank.byId(id) ?: WisdomBank.all.first()
    }

    fun favoriteIds(): Set<Int> =
        prefs.getString("favs", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet()

    fun toggleFavorite(id: Int): Set<Int> {
        val set = favoriteIds().toMutableSet()
        if (!set.add(id)) set.remove(id)
        prefs.edit().putString("favs", set.joinToString(",")).apply()
        return set
    }
}
