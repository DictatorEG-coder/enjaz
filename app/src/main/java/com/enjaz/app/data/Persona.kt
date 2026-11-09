package com.enjaz.app.data

import android.content.Context
import java.util.Locale
import kotlin.random.Random

object NType {
    const val ACCOUNT = "account"
    const val OVERDUE = "overdue"
    const val DISTRACT = "distract"
    const val DEADLINE = "deadline"
    const val EVENING = "evening"
}

/** مدرب صارم: حاد وواثق وبلا مجاملة ولا إهانة شخصية. الرسائل تُبنى على سلوك المستخدم الفعلي. */
class Persona(context: Context, private val settings: AppSettings) {
    private val picker = MessagePicker(context)

    private fun pick(key: String, m: Msg): String =
        picker.pick(key + "_" + settings.mode.name, m.forMode(settings.mode))

    fun status(b: Behavior): String = when {
        b.total == 0 -> pick("empty", Lib.EMPTY)
        b.zeroDays >= 3 || b.overdue >= 3 ->
            pick("repeated", Lib.REPEATED).fill("z" to maxOf(b.zeroDays, 1), "n" to b.overdue)
        b.monitoredMin != null && b.monitoredMin >= settings.distractThreshold && b.pending > 0 -> {
            val base = pick("waste", Lib.WASTE).fill("m" to b.monitoredMin, "app" to (b.topApp ?: "الهاتف"))
            if (settings.focusLinked) base + " " + pick("focus", Lib.FOCUS) else base
        }
        b.zeroDays >= 1 && b.doneToday == 0 && b.pending > 0 -> pick("noday", Lib.NO_DAY)
        b.overdue > 0 -> pick("overdue", Lib.OVERDUE).fill("n" to b.overdue)
        b.pending == 0 -> pick("excellent", Lib.EXCELLENT).fill("d" to b.doneToday)
        b.doneToday >= 1 -> pick("progress", Lib.PROGRESS).fill("d" to b.doneToday, "p" to b.pending)
        else -> pick("idle", Lib.IDLE).fill("p" to b.pending)
    }

    fun afterCompletion(pending: Int, doneToday: Int): String {
        if (settings.mode != Mode.STRICT && pending > 0 && Random.nextInt(100) < 20)
            return picker.pick("youth", Lib.YOUTH)
        return when {
            pending == 0 || doneToday >= 3 -> pick("excellent", Lib.EXCELLENT).fill("d" to doneToday)
            doneToday == 1 -> pick("good", Lib.GOOD)
            else -> pick("progress", Lib.PROGRESS).fill("d" to doneToday, "p" to pending)
        }
    }

    /** missed = عدد الأيام الكاملة الفائتة. */
    fun absence(missed: Int): String? = when {
        missed <= 0 -> null
        missed < 3 -> pick("abs1", Lib.ABS1)
        missed < 7 -> pick("abs3", Lib.ABS3)
        missed < 30 -> pick("abs7", Lib.ABS7)
        else -> pick("abs30", Lib.ABS30)
    }

    /** يُرجع (العنوان، النص) للإشعار. */
    fun notification(type: String, zeroEvening: Boolean = false, vararg kv: Pair<String, Any>): Pair<String, String> {
        val (title, pool) = when (type) {
            NType.ACCOUNT -> "إنجاز · مساءلة" to Lib.N_ACCOUNT
            NType.OVERDUE -> "إنجاز · متأخرات" to Lib.N_OVERDUE
            NType.DISTRACT -> "إنجاز · تنبيه استخدام" to Lib.N_DISTRACT
            NType.DEADLINE -> "إنجاز · موعد قريب" to Lib.N_DEADLINE
            else -> "إنجاز · حساب اليوم" to (if (zeroEvening) Lib.N_EVENING_ZERO else Lib.N_EVENING)
        }
        return title to pick("n_" + type + (if (zeroEvening) "0" else ""), pool).fill(*kv)
    }

    fun verdict(ratePct: Int?, absentDays: Int?): String {
        val main = when {
            ratePct == null -> "لا بيانات كافية في هذه الفترة."
            ratePct >= 80 -> pick("v_high", Lib.VERDICT_HIGH)
            ratePct >= 50 -> pick("v_mid", Lib.VERDICT_MID)
            else -> pick("v_low", Lib.VERDICT_LOW)
        }
        return if (absentDays != null && absentDays > 0)
            main + " " + pick("v_abs", Lib.ABSENT_NOTE).fill("n" to absentDays)
        else main
    }

    fun planNoFit(): String = pick("plan_nofit", Lib.PLAN_NO_FIT)

    fun resultFeedback(r: ExamResult, prev: ExamResult?): String {
        val pct = r.pct
        val pctText = String.format(Locale.US, "%.0f", pct)
        val target = r.target
        val belowTarget = target != null && r.score < target
        val parts = mutableListOf<String>()

        if (belowTarget && r.effortAll) {
            parts += picker.pick("console", Lib.CONSOLE)
            parts += Lib.CONSOLE_TAIL
        } else {
            val msg = when {
                pct >= 90.0 && !belowTarget -> pick("res_exc", Lib.RES_EXC)
                target != null && !belowTarget -> pick("res_met", Lib.RES_MET)
                target != null && belowTarget && pct >= 50.0 -> pick("res_close", Lib.RES_CLOSE)
                target == null && pct >= 75.0 -> pick("res_met", Lib.RES_MET)
                target == null && pct >= 50.0 -> pick("res_close", Lib.RES_CLOSE)
                else -> pick("res_weak", Lib.RES_WEAK)
            }
            val gap = r.targetPct?.let { String.format(Locale.US, "%.0f", maxOf(0.0, it - pct)) } ?: "0"
            parts += msg.fill("pct" to pctText, "gap" to gap)
        }

        if (prev != null) {
            val delta = pct - prev.pct
            val d = String.format(Locale.US, "%.1f", Math.abs(delta))
            parts += when {
                Math.abs(delta) < 0.05 -> "مثل نتيجتك السابقة في ${r.subjectName} تمامًا."
                delta > 0 -> "تحسّن بمقدار $d نقطة مئوية عن نتيجتك السابقة في ${r.subjectName}."
                else -> "تراجع بمقدار $d نقطة مئوية عن نتيجتك السابقة في ${r.subjectName}."
            }
        }
        return parts.joinToString("\n")
    }
}
