package com.enjaz.app.data

enum class Period(val label: String, val days: Int) {
    DAY("يومي", 1), WEEK("أسبوعي", 7), MONTH("شهري", 30)
}

data class ReportData(
    val completed: Int,
    val overdueNow: Int,
    val ratePct: Int?,
    val bySubject: List<Pair<String, Int>>,
    val buckets: List<Pair<String, Int>>,
    val absentDays: Int?,
    val tiktokMin: Int?,
    val focusMin: Int?,
    val results: List<ExamResult>
)

object Reports {
    fun build(
        period: Period, repo: DataRepository, openDates: Set<String>,
        usage: UsageHelper, focusPackage: String
    ): ReportData {
        val today = DateUtil.today()
        val start = DateUtil.plus(today, -(period.days - 1))
        val startMs = DateUtil.dayStart(start)
        val now = System.currentTimeMillis()

        val doneIn = repo.tasks.filter { it.done && (it.doneAt ?: 0L) in startMs..now }
        val completed = doneIn.size
        val pendingDue = repo.tasks.count { !it.done && (it.dueDate == null || it.dueDate <= today) }
        val denom = completed + pendingDue
        val rate = if (denom > 0) completed * 100 / denom else null

        val names = repo.subjects.associate { it.id to it.name }
        val bySubject = doneIn.groupBy { it.subjectId }
            .map { (id, list) -> (names[id] ?: "—") to list.sumOf { it.durationMin } }
            .sortedByDescending { it.second }.take(5)

        fun countBetween(fromMs: Long, toMs: Long) =
            repo.tasks.count { it.done && (it.doneAt ?: 0L) in fromMs until toMs }

        val buckets: List<Pair<String, Int>> = if (period == Period.MONTH) {
            (3 downTo 0).map { w ->
                val s = DateUtil.plus(today, -(7 * w + 6))
                val e = DateUtil.plus(today, -(7 * w))
                "أ" + (4 - w) to countBetween(DateUtil.dayStart(s), DateUtil.dayStart(DateUtil.plus(e, 1)))
            }
        } else {
            (6 downTo 0).map { d ->
                val date = DateUtil.plus(today, -d)
                date.substring(5) to countBetween(DateUtil.dayStart(date), DateUtil.dayStart(DateUtil.plus(date, 1)))
            }
        }

        val absent: Int? = if (period == Period.DAY) null else {
            val first = openDates.minOrNull()
            if (first == null) 0
            else (0 until period.days).map { DateUtil.plus(start, it) }
                .count { it >= first && it !in openDates }
        }

        val tiktok = usage.minutes(TIKTOK_PACKAGES, startMs, now)
        val focus = if (focusPackage.isBlank()) null else usage.minutes(setOf(focusPackage), startMs, now)
        val results = repo.results.filter { it.date >= start && it.date <= today }
            .sortedByDescending { it.date }

        return ReportData(completed, repo.overdueCount(), rate, bySubject, buckets, absent, tiktok, focus, results)
    }
}
