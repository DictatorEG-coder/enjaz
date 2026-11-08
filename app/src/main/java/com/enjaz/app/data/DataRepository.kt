package com.enjaz.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class ResultOutcome(val error: String?, val result: ExamResult?)

/** مصدر الحقيقة الوحيد للمواد والمهام والنتائج والملاحظات. كل تعديل يتحقق من المدخلات ويُرجع خطأ أو null. */
class DataRepository(context: Context) {
    private val store = JsonStore(File(context.filesDir, "enjaz_data.json"))
    private var data by mutableStateOf(store.load())

    val subjects: List<Subject> get() = data.subjects
    val tasks: List<Task> get() = data.tasks
    val results: List<ExamResult> get() = data.results
    val notes: List<Note> get() = data.notes

    private fun newId(): Long =
        (subjects.map { it.id } + tasks.map { it.id } + results.map { it.id } + notes.map { it.id })
            .maxOrNull()?.plus(1) ?: 1L

    private fun commit(d: AppData): String? =
        if (store.save(d)) { data = d; null } else "تعذّر حفظ البيانات. لم يُنفَّذ التغيير."

    // ---- المواد ----
    fun addSubject(nameRaw: String, examRaw: String): String? {
        val name = nameRaw.trim()
        val exam = examRaw.trim()
        if (name.isEmpty()) return "اسم المادة مطلوب."
        if (name.length > 60) return "اسم المادة طويل جدًا."
        if (subjects.any { it.name == name }) return "هذه المادة موجودة مسبقًا."
        if (exam.isNotEmpty()) {
            if (!DateUtil.isValid(exam)) return "تاريخ الامتحان غير صالح. الصيغة: 2026-12-31"
            if (exam < DateUtil.today()) return "تاريخ الامتحان مضى."
        }
        return commit(data.copy(subjects = subjects + Subject(newId(), name, exam.ifEmpty { null })))
    }

    fun deleteSubject(id: Long): String? =
        commit(data.copy(subjects = subjects.filter { it.id != id }, tasks = tasks.filter { it.subjectId != id }))

    // ---- المهام ----
    fun addTask(
        subjectId: Long?, type: TaskType, titleRaw: String,
        durationRaw: String, dueRaw: String, priority: Priority
    ): String? {
        val title = titleRaw.trim()
        val due = dueRaw.trim()
        if (subjectId == null || subjects.none { it.id == subjectId }) return "اختر المادة."
        if (title.isEmpty()) return "عنوان المهمة مطلوب."
        if (title.length > 100) return "العنوان طويل جدًا."
        val duration = durationRaw.trim().toIntOrNull() ?: return "المدة يجب أن تكون رقمًا بالدقائق."
        if (duration !in 1..600) return "المدة يجب أن تكون بين 1 و600 دقيقة."
        if (due.isNotEmpty() && !DateUtil.isValid(due)) return "الموعد غير صالح. الصيغة: 2026-12-31"
        val task = Task(newId(), subjectId, type, title, duration, due.ifEmpty { null },
            priority, false, null, System.currentTimeMillis())
        return commit(data.copy(tasks = tasks + task))
    }

    fun setDone(id: Long, done: Boolean): String? = commit(data.copy(tasks = tasks.map {
        if (it.id == id) it.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null) else it
    }))

    fun deferTask(id: Long, until: String?): String? = commit(data.copy(tasks = tasks.map {
        if (it.id == id) it.copy(deferUntil = until) else it
    }))

    fun deleteTask(id: Long): String? = commit(data.copy(tasks = tasks.filter { it.id != id }))

    // ---- النتائج ----
    fun addResult(
        examRaw: String, subjectRaw: String, scoreRaw: String, maxRaw: String,
        targetRaw: String, dateRaw: String, effortAll: Boolean
    ): ResultOutcome {
        fun fail(m: String) = ResultOutcome(m, null)
        val exam = examRaw.trim(); val subject = subjectRaw.trim()
        if (exam.isEmpty()) return fail("اسم الامتحان مطلوب.")
        if (exam.length > 60) return fail("اسم الامتحان طويل جدًا.")
        if (subject.isEmpty()) return fail("اسم المادة مطلوب.")
        val score = scoreRaw.trim().replace(',', '.').toDoubleOrNull() ?: return fail("الدرجة يجب أن تكون رقمًا.")
        val max = maxRaw.trim().replace(',', '.').toDoubleOrNull() ?: return fail("الدرجة النهائية يجب أن تكون رقمًا.")
        if (max <= 0.0) return fail("الدرجة النهائية يجب أن تكون أكبر من صفر.")
        if (score < 0.0 || score > max) return fail("الدرجة يجب أن تكون بين 0 و${max.toInt()}.")
        val targetText = targetRaw.trim().replace(',', '.')
        val target = if (targetText.isEmpty()) null
        else targetText.toDoubleOrNull() ?: return fail("الدرجة المطلوبة يجب أن تكون رقمًا.")
        if (target != null && (target < 0.0 || target > max)) return fail("الدرجة المطلوبة خارج النطاق.")
        val date = dateRaw.trim().ifEmpty { DateUtil.today() }
        if (!DateUtil.isValid(date)) return fail("تاريخ النتيجة غير صالح.")
        val r = ExamResult(newId(), exam, subject, score, max, target, date, effortAll)
        val err = commit(data.copy(results = results + r))
        return if (err == null) ResultOutcome(null, r) else fail(err)
    }

    fun deleteResult(id: Long): String? = commit(data.copy(results = results.filter { it.id != id }))

    fun previousFor(r: ExamResult): ExamResult? = results
        .filter {
            it.id != r.id && it.subjectName.equals(r.subjectName, ignoreCase = true) &&
                (it.date < r.date || (it.date == r.date && it.id < r.id))
        }
        .maxWithOrNull(compareBy<ExamResult>({ it.date }, { it.id }))

    // ---- الأهداف والدروس ----
    fun addNote(type: NoteType, textRaw: String): String? {
        val text = textRaw.trim()
        if (text.isEmpty()) return "النص مطلوب."
        if (text.length > 300) return "النص طويل جدًا (الحد 300 حرف)."
        return commit(data.copy(notes = notes + Note(newId(), type, text, System.currentTimeMillis())))
    }

    fun toggleAchieved(id: Long): String? = commit(data.copy(notes = notes.map {
        if (it.id == id) it.copy(achieved = !it.achieved) else it
    }))

    fun deleteNote(id: Long): String? = commit(data.copy(notes = notes.filter { it.id != id }))

    // ---- إحصاءات ----
    fun pendingCount() = tasks.count { !it.done }
    fun overdueCount(): Int {
        val today = DateUtil.today()
        return tasks.count { !it.done && it.dueDate != null && it.dueDate < today }
    }
    fun doneTodayCount(): Int {
        val start = DateUtil.startOfToday()
        return tasks.count { it.done && (it.doneAt ?: 0L) >= start }
    }
    fun doneTodayMinutes(): Int {
        val start = DateUtil.startOfToday()
        return tasks.filter { it.done && (it.doneAt ?: 0L) >= start }.sumOf { it.durationMin }
    }
}
