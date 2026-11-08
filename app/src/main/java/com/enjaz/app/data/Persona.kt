package com.enjaz.app.data

import android.content.Context
import java.util.Locale
import kotlin.random.Random

/** صوت التطبيق: صارم وبارد وواثق، بلا إهانة. النبرة تتبع النمط المختار من الإعدادات. */
class Persona(context: Context, private val settings: AppSettings) {
    private val picker = MessagePicker(context)

    private fun pick(key: String, m: Msg): String =
        picker.pick(key + "_" + settings.mode.name, m.forMode(settings.mode))

    fun status(total: Int, pending: Int, overdue: Int, doneToday: Int): String = when {
        total == 0 -> pick("st_empty", Lib.ST_EMPTY)
        overdue > 0 -> pick("st_overdue", Lib.ST_OVERDUE).fill("n" to overdue)
        pending == 0 -> pick("st_alldone", Lib.ST_ALLDONE)
        doneToday == 0 -> pick("st_idle", Lib.ST_IDLE).fill("p" to pending)
        else -> pick("st_progress", Lib.ST_PROGRESS).fill("d" to doneToday, "p" to pending)
    }

    fun afterCompletion(pending: Int, doneToday: Int): String {
        if (settings.mode != Mode.STRICT && pending > 0 && Random.nextInt(100) < 30)
            return picker.pick("youth", Lib.YOUTH)
        return when {
            pending == 0 -> pick("done_all", Lib.DONE_ALL)
            doneToday == 1 -> pick("done_first", Lib.DONE_FIRST)
            else -> pick("done_more", Lib.DONE_MORE).fill("d" to doneToday, "p" to pending)
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

    fun tiktok(minutes: Int, focusLinked: Boolean): String {
        val base = pick("tiktok", Lib.TIKTOK).fill("m" to minutes)
        return if (focusLinked) base + " " + pick("focus", Lib.FOCUS) else base
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
