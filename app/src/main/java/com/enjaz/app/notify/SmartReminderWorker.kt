package com.enjaz.app.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.enjaz.app.data.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val NAME = "enjaz_smart"

    /** عمل دوري كل ساعة (استهلاك بطارية منخفض). يتحقق من السلوك ويرسل إشعارًا واحدًا كحد أقصى في كل مرة. */
    fun apply(ctx: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (enabled) {
            val req = PeriodicWorkRequest.Builder(SmartReminderWorker::class.java, 1, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        } else {
            wm.cancelUniqueWork(NAME)
        }
    }
}

class SmartReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        try {
            run(applicationContext)
        } catch (e: Exception) {
            // لا نُسقط العمل الدوري بسبب خطأ عابر
        }
        return Result.success()
    }

    companion object {
        private const val MAX_PER_DAY = 5

        fun run(ctx: Context) {
            val settings = AppSettings(ctx)
            if (!settings.notifEnabled || !Notifier.canNotify(ctx)) return
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour < settings.startHour || hour >= settings.endHour) return

            val state = NotifState(ctx)
            if (state.countToday() >= MAX_PER_DAY) return

            val repo = DataRepository(ctx)
            val usage = UsageHelper(ctx)
            val persona = Persona(ctx, settings)
            val b = BehaviorCalc.snapshot(repo, usage, settings, OpenTracker(ctx).firstDate())
            val today = DateUtil.today()

            fun fire(type: String, id: Int, zeroEvening: Boolean = false, vararg kv: Pair<String, Any>): Boolean {
                val (title, body) = persona.notification(type, zeroEvening, *kv)
                val ok = Notifier.send(ctx, id, title, body)
                if (ok) state.markSent(type)
                return ok
            }

            // 1) حساب نهاية اليوم
            if (settings.nEvening && !state.sentToday(NType.EVENING) && hour >= settings.eveningHour) {
                if (fire(NType.EVENING, 1, b.doneToday == 0, "d" to b.doneToday, "p" to b.pending)) return
            }

            // 2) موعد قريب (مهمة خلال يوم أو امتحان خلال يومين)
            if (settings.nDeadline && !state.sentToday(NType.DEADLINE)) {
                val pending = repo.tasks.filter { !it.done }
                data class Near(val title: String, val days: Int)
                val near = mutableListOf<Near>()
                pending.forEach { t ->
                    t.dueDate?.let { d ->
                        val n = DateUtil.daysUntil(d)
                        if (n in 0..1) near.add(Near(t.title, n))
                    }
                }
                repo.subjects.forEach { s ->
                    val exam = s.examDate
                    if (exam != null && pending.any { it.subjectId == s.id }) {
                        val n = DateUtil.daysUntil(exam)
                        if (n in 0..2) near.add(Near("امتحان " + s.name, n))
                    }
                }
                val first = near.minByOrNull { it.days }
                if (first != null) {
                    val w = when (first.days) { 0 -> "اليوم"; 1 -> "غدًا"; else -> "بعد يومين" }
                    if (fire(NType.DEADLINE, 2, false, "t" to first.title, "w" to w)) return
                }
            }

            // 3) استخدام مفرط لتطبيقات مراقَبة (يعتمد على بيانات النظام الفعلية فقط)
            val mins = b.monitoredMin
            if (settings.nDistract && settings.monitoringEnabled && mins != null && mins >= settings.distractThreshold) {
                val level = mins / settings.distractThreshold
                if (level > state.distractLevel()) {
                    val sent = fire(NType.DISTRACT, 3, false, "m" to mins, "app" to (b.topApp ?: "الهاتف"))
                    if (sent) { state.setDistractLevel(level); return }
                }
            }

            // 4) متأخرات
            if (settings.nOverdue && !state.sentToday(NType.OVERDUE) && b.overdue > 0) {
                val oldest = repo.tasks.filter { !it.done && it.dueDate != null && it.dueDate < today }
                    .minByOrNull { it.dueDate ?: "9999" }
                if (fire(NType.OVERDUE, 4, false, "n" to b.overdue, "t" to (oldest?.title ?: ""))) return
            }

            // 5) مساءلة عند طول الغياب عن الإنجاز
            if (settings.nAccount && !state.sentToday(NType.ACCOUNT) && b.pending > 0) {
                val startOfWindow = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, settings.startHour); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val startToday = DateUtil.startOfToday()
                val lastDone = repo.tasks.mapNotNull { it.doneAt }.filter { it >= startToday }.maxOrNull()
                val ref = maxOf(lastDone ?: startOfWindow, startOfWindow)
                val hours = ((System.currentTimeMillis() - ref) / 3600000L).toInt()
                if (hours >= 5) fire(NType.ACCOUNT, 5, false, "h" to hours, "p" to b.pending)
            }
        }
    }
}
