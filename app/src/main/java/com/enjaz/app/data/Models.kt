package com.enjaz.app.data

enum class TaskType(val label: String) { LECTURE("محاضرة"), ASSIGNMENT("واجب") }
enum class Priority(val label: String) { HIGH("عالية"), MEDIUM("متوسطة"), LOW("منخفضة") }
enum class Mode(val label: String) { STRICT("صارم بالفصحى"), EGYPTIAN("مصري ساخر"), MIXED("مختلط") }
enum class NoteType(val label: String) { GOAL("هدف"), LESSON("درس") }

data class Subject(val id: Long, val name: String, val examDate: String?)

data class Task(
    val id: Long,
    val subjectId: Long,
    val type: TaskType,
    val title: String,
    val durationMin: Int,
    val dueDate: String?,
    val priority: Priority,
    val done: Boolean,
    val doneAt: Long?,
    val createdAt: Long,
    val deferUntil: String? = null
)

data class ExamResult(
    val id: Long,
    val examName: String,
    val subjectName: String,
    val score: Double,
    val max: Double,
    val target: Double?,
    val date: String,
    val effortAll: Boolean
) {
    val pct: Double get() = score / max * 100.0
    val targetPct: Double? get() = target?.let { it / max * 100.0 }
}

data class Note(
    val id: Long,
    val type: NoteType,
    val text: String,
    val createdAt: Long,
    val achieved: Boolean = false
)

data class AppData(
    val subjects: List<Subject>,
    val tasks: List<Task>,
    val results: List<ExamResult>,
    val notes: List<Note>
)
