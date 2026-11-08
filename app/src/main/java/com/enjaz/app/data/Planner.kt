package com.enjaz.app.data

data class PlanItem(val task: Task, val minutes: Int, val partial: Boolean)
data class DayPlan(val date: String, val items: List<PlanItem>, val used: Int, val capacity: Int)
data class Unscheduled(val task: Task, val remaining: Int)
data class PlanResult(val days: List<DayPlan>, val unscheduled: List<Unscheduled>)

/**
 * يوزّع المهام المعلقة على الأيام: الأقرب موعدًا أولًا (موعد المهمة أو امتحان مادتها)، ثم الأعلى أولوية.
 * لا يتجاوز السعة اليومية أبدًا؛ ما لا يتسع قبل موعده يُعرض منفصلًا بدل جدول مستحيل.
 */
object Planner {
    const val HORIZON = 30

    fun build(tasks: List<Task>, subjects: List<Subject>, capacity: Int, doneTodayMin: Int): PlanResult {
        val today = DateUtil.today()
        val dates = (0 until HORIZON).map { DateUtil.plus(today, it) }
        val initial = dates.indices.map { if (it == 0) maxOf(0, capacity - doneTodayMin) else capacity }
        val caps = initial.toMutableList()
        val items = dates.map { mutableListOf<PlanItem>() }
        val exams = subjects.associate { it.id to it.examDate }

        fun deadline(t: Task): String? {
            val d = listOfNotNull(t.dueDate, exams[t.subjectId]).minOrNull() ?: return null
            return if (d < today) today else d
        }

        val pending = tasks.filter { !it.done }.sortedWith(
            compareBy<Task>({ deadline(it) ?: "9999-99-99" }, { it.priority.ordinal }, { it.createdAt })
        )
        val unscheduled = mutableListOf<Unscheduled>()
        for (t in pending) {
            var rem = t.durationMin
            val dl = deadline(t)
            val defer = t.deferUntil
            for (i in dates.indices) {
                val d = dates[i]
                if (dl != null && d > dl) break
                if (defer != null && d < defer) continue
                if (caps[i] <= 0) continue
                val take = minOf(rem, caps[i])
                caps[i] -= take
                rem -= take
                items[i].add(PlanItem(t, take, take < t.durationMin))
                if (rem == 0) break
            }
            if (rem > 0) unscheduled.add(Unscheduled(t, rem))
        }
        val days = dates.indices
            .map { DayPlan(dates[it], items[it], initial[it] - caps[it], capacity) }
            .filter { it.items.isNotEmpty() || it.date == today }
        return PlanResult(days, unscheduled)
    }
}
