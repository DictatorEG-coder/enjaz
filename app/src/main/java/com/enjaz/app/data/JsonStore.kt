package com.enjaz.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** تخزين دائم محلي بصيغة JSON مع كتابة آمنة (ملف مؤقت ثم استبدال). متوافق مع ملفات المرحلة 2. */
class JsonStore(private val file: File) {

    fun load(): AppData {
        val empty = AppData(emptyList(), emptyList(), emptyList(), emptyList())
        if (!file.exists()) return empty
        return try {
            val root = JSONObject(file.readText())
            val sa = root.optJSONArray("subjects") ?: JSONArray()
            val ta = root.optJSONArray("tasks") ?: JSONArray()
            val ra = root.optJSONArray("results") ?: JSONArray()
            val na = root.optJSONArray("notes") ?: JSONArray()
            val subjects = (0 until sa.length()).map {
                val o = sa.getJSONObject(it)
                Subject(o.getLong("id"), o.getString("name"), o.optString("exam", "").ifBlank { null })
            }
            val tasks = (0 until ta.length()).map {
                val o = ta.getJSONObject(it)
                Task(
                    id = o.getLong("id"),
                    subjectId = o.getLong("subjectId"),
                    type = runCatching { TaskType.valueOf(o.getString("type")) }.getOrDefault(TaskType.LECTURE),
                    title = o.getString("title"),
                    durationMin = o.getInt("duration"),
                    dueDate = o.optString("due", "").ifBlank { null },
                    priority = runCatching { Priority.valueOf(o.getString("priority")) }.getOrDefault(Priority.MEDIUM),
                    done = o.optBoolean("done", false),
                    doneAt = if (o.has("doneAt")) o.getLong("doneAt") else null,
                    createdAt = o.optLong("createdAt", 0L),
                    deferUntil = o.optString("defer", "").ifBlank { null }
                )
            }
            val results = (0 until ra.length()).map {
                val o = ra.getJSONObject(it)
                ExamResult(
                    id = o.getLong("id"),
                    examName = o.getString("exam"),
                    subjectName = o.getString("subject"),
                    score = o.getDouble("score"),
                    max = o.getDouble("max"),
                    target = if (o.has("target")) o.getDouble("target") else null,
                    date = o.getString("date"),
                    effortAll = o.optBoolean("effort", false)
                )
            }
            val notes = (0 until na.length()).map {
                val o = na.getJSONObject(it)
                Note(
                    id = o.getLong("id"),
                    type = runCatching { NoteType.valueOf(o.getString("type")) }.getOrDefault(NoteType.LESSON),
                    text = o.getString("text"),
                    createdAt = o.optLong("createdAt", 0L),
                    achieved = o.optBoolean("achieved", false)
                )
            }
            AppData(subjects, tasks, results, notes)
        } catch (e: Exception) {
            runCatching { file.copyTo(File(file.parentFile, file.name + ".corrupt"), overwrite = true) }
            empty
        }
    }

    fun save(d: AppData): Boolean {
        return try {
            val root = JSONObject()
            val sa = JSONArray()
            d.subjects.forEach {
                val o = JSONObject().put("id", it.id).put("name", it.name)
                if (it.examDate != null) o.put("exam", it.examDate)
                sa.put(o)
            }
            val ta = JSONArray()
            d.tasks.forEach {
                val o = JSONObject()
                    .put("id", it.id).put("subjectId", it.subjectId)
                    .put("type", it.type.name).put("title", it.title)
                    .put("duration", it.durationMin).put("priority", it.priority.name)
                    .put("done", it.done).put("createdAt", it.createdAt)
                if (it.dueDate != null) o.put("due", it.dueDate)
                if (it.doneAt != null) o.put("doneAt", it.doneAt)
                if (it.deferUntil != null) o.put("defer", it.deferUntil)
                ta.put(o)
            }
            val ra = JSONArray()
            d.results.forEach {
                val o = JSONObject()
                    .put("id", it.id).put("exam", it.examName).put("subject", it.subjectName)
                    .put("score", it.score).put("max", it.max).put("date", it.date)
                    .put("effort", it.effortAll)
                if (it.target != null) o.put("target", it.target)
                ra.put(o)
            }
            val na = JSONArray()
            d.notes.forEach {
                na.put(
                    JSONObject().put("id", it.id).put("type", it.type.name).put("text", it.text)
                        .put("createdAt", it.createdAt).put("achieved", it.achieved)
                )
            }
            root.put("subjects", sa).put("tasks", ta).put("results", ra).put("notes", na)
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            true
        } catch (e: Exception) { false }
    }
}
