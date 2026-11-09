package com.enjaz.app.data

/** لقطة من سلوك المستخدم الفعلي. كل رسائل الشخصية والإشعارات تُبنى عليها. */
data class Behavior(
    val total: Int,
    val pending: Int,
    val overdue: Int,
    val doneToday: Int,
    val zeroDays: Int,
    val streak: Int,
    val monitoredMin: Int?,
    val topApp: String?
)

object BehaviorCalc {
    fun snapshot(repo: DataRepository, usage: UsageHelper, settings: AppSettings, firstDate: String?): Behavior {
        val today = DateUtil.today()
        val doneDates = repo.tasks.filter { it.done }
            .mapNotNull { t -> t.doneAt?.let { DateUtil.dateOf(it) } }.toSet()

        var streak = 0
        var d = if (today in doneDates) today else DateUtil.plus(today, -1)
        while (d in doneDates && streak < 3650) { streak++; d = DateUtil.plus(d, -1) }

        var zero = 0
        d = DateUtil.plus(today, -1)
        while (firstDate != null && zero < 30 && d >= firstDate && d !in doneDates) {
            zero++; d = DateUtil.plus(d, -1)
        }

        var minutes: Int? = null
        var top: String? = null
        if (settings.monitoringEnabled) {
            val map = usage.minutesByPackage(
                settings.effectiveMonitored(), DateUtil.startOfToday(), System.currentTimeMillis()
            )
            if (map != null) {
                minutes = map.values.sum()
                val byLabel = map.entries.groupBy { usage.labelOf(it.key) }.mapValues { e -> e.value.sumOf { it.value } }
                top = byLabel.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
            }
        }
        return Behavior(
            repo.tasks.size, repo.pendingCount(), repo.overdueCount(), repo.doneTodayCount(),
            zero, streak, minutes, top
        )
    }
}
