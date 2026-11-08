package com.enjaz.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

@Composable
fun TasksScreen(
    repo: DataRepository, persona: Persona, settings: AppSettings, usage: UsageHelper,
    welcomeBack: String?, onDismissWelcome: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var banner by remember { mutableStateOf<String?>(null) }
    var showAddSubject by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    // رسالة TikTok تظهر فقط عند وجود بيانات فعلية وبإذن المستخدم وربط تركيز
    val tiktokMsg = remember {
        if (settings.focusLinked) {
            val m = usage.minutes(TIKTOK_PACKAGES, DateUtil.startOfToday(), System.currentTimeMillis())
            if (m != null && m >= 45) persona.tiktok(m, true) else null
        } else null
    }

    val status = banner ?: persona.status(
        repo.tasks.size, repo.pendingCount(), repo.overdueCount(), repo.doneTodayCount()
    )
    val today = DateUtil.today()
    val subjectNames = repo.subjects.associate { it.id to it.name }
    val sortedTasks = repo.tasks.sortedWith(
        compareBy<Task>({ it.done }, { it.priority.ordinal }, { it.dueDate ?: "9999-99-99" })
    )

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("إنجاز", color = Gold, fontSize = 30.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        if (welcomeBack != null) {
            Surface(color = Surface1, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Gold)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(welcomeBack, color = TextSoft, fontSize = 16.sp, lineHeight = 26.sp)
                    TextButton(onClick = onDismissWelcome) { Text("فهمت", color = Gold) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (tiktokMsg != null) {
            Surface(color = Surface1, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Crimson)) {
                Text(tiktokMsg, color = TextSoft, fontSize = 15.sp, lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth().padding(14.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = Crimson.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp)) {
            Text(status, color = TextSoft, fontSize = 16.sp, lineHeight = 26.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("المهام") },
                modifier = Modifier.padding(end = 8.dp))
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("المواد") })
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                if (sortedTasks.isEmpty()) {
                    Text("لا مهام بعد.", color = TextSoft)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sortedTasks, key = { it.id }) { t ->
                            val overdue = !t.done && t.dueDate != null && t.dueDate < today
                            TaskCard(
                                task = t, subjectName = subjectNames[t.subjectId] ?: "—",
                                overdue = overdue, today = today,
                                onToggle = { checked ->
                                    val err = repo.setDone(t.id, checked)
                                    banner = err ?: if (checked) persona.afterCompletion(
                                        repo.pendingCount(), repo.doneTodayCount()
                                    ) else null
                                },
                                onDelete = { banner = repo.deleteTask(t.id) }
                            )
                        }
                    }
                }
            } else {
                if (repo.subjects.isEmpty()) {
                    Text("لا مواد بعد.", color = TextSoft)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(repo.subjects, key = { it.id }) { s ->
                            SubjectCard(
                                s, repo.tasks.count { it.subjectId == s.id },
                                repo.tasks.count { it.subjectId == s.id && it.done },
                                onDelete = { subjectToDelete = s }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (tab == 1) showAddSubject = true
                else if (repo.subjects.isEmpty()) banner = "لا مهمة بلا مادة. أضف مادة أولًا."
                else showAddTask = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (tab == 0) "إضافة مهمة" else "إضافة مادة", fontSize = 17.sp) }
        Spacer(Modifier.height(12.dp))
    }

    if (showAddSubject) AddSubjectDialog(
        onDismiss = { showAddSubject = false },
        onSubmit = { n, e -> repo.addSubject(n, e) }
    )
    if (showAddTask) AddTaskDialog(
        subjects = repo.subjects,
        onDismiss = { showAddTask = false },
        onSubmit = { sid, type, title, dur, due, pr -> repo.addTask(sid, type, title, dur, due, pr) }
    )
    subjectToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            containerColor = Surface1,
            title = { Text("حذف المادة", color = Gold) },
            text = { Text("ستُحذف المادة «${s.name}» وكل مهامها. لا رجعة في ذلك.", color = TextSoft) },
            confirmButton = {
                TextButton(onClick = { banner = repo.deleteSubject(s.id); subjectToDelete = null }) {
                    Text("احذف", color = Gold)
                }
            },
            dismissButton = { TextButton(onClick = { subjectToDelete = null }) { Text("إلغاء", color = TextSoft) } }
        )
    }
}

@Composable
private fun TaskCard(
    task: Task, subjectName: String, overdue: Boolean, today: String,
    onToggle: (Boolean) -> Unit, onDelete: () -> Unit
) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.done, onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Ink, uncheckedColor = Gold)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    task.title, fontSize = 17.sp,
                    color = if (task.done) TextSoft.copy(alpha = 0.5f) else TextSoft,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null
                )
                Text("${task.type.label} · $subjectName · ${task.durationMin} دقيقة", color = Gold, fontSize = 13.sp)
                val meta = buildList {
                    task.dueDate?.let { add("الموعد: $it") }
                    add("الأولوية: ${task.priority.label}")
                    task.deferUntil?.let { if (it > today && !task.done) add("مؤجلة إلى $it") }
                    if (overdue) add("متأخرة")
                }.joinToString(" · ")
                Text(meta, fontSize = 13.sp,
                    color = if (overdue) Color(0xFFE57373) else TextSoft.copy(alpha = 0.7f))
            }
            TextButton(onClick = onDelete) { Text("حذف", color = Gold) }
        }
    }
}

@Composable
private fun SubjectCard(s: Subject, total: Int, done: Int, onDelete: () -> Unit) {
    val examText = s.examDate?.let {
        val d = DateUtil.daysUntil(it)
        when {
            d < 0 -> "الامتحان مضى · $it"
            d == 0 -> "الامتحان اليوم"
            else -> "الامتحان بعد $d يومًا · $it"
        }
    } ?: "لا تاريخ امتحان"
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, color = TextSoft, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                Text(examText, color = Gold, fontSize = 13.sp)
                Text("المهام: $total · المنجز: $done", color = TextSoft.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            TextButton(onClick = onDelete) { Text("حذف", color = Gold) }
        }
    }
}
