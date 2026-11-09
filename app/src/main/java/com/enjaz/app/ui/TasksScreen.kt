package com.enjaz.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.R
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

@Composable
fun TasksScreen(
    repo: DataRepository, persona: Persona, settings: AppSettings, usage: UsageHelper,
    firstDate: String?, welcomeBack: String?, onDismissWelcome: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var banner by remember { mutableStateOf<String?>(null) }
    var showAddSubject by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    // لقطة السلوك الفعلي (المهام + استخدام التطبيقات المراقَبة إن وُجد إذن وموافقة)
    val b = remember(repo.tasks, settings.monitoringEnabled, settings.monitored, settings.excluded) {
        BehaviorCalc.snapshot(repo, usage, settings, firstDate)
    }
    val status = banner ?: remember(b, settings.mode) { persona.status(b) }
    val today = DateUtil.today()
    val subjectNames = repo.subjects.associate { it.id to it.name }
    val sortedTasks = repo.tasks.sortedWith(
        compareBy<Task>({ it.done }, { it.priority.ordinal }, { it.dueDate ?: "9999-99-99" })
    )

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.enjaz_logo), contentDescription = "شعار إنجاز",
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text("إنجاز", color = Gold, fontSize = 28.sp, fontFamily = FontFamily.Serif)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("سلسلة ${b.streak}", Gold) { Icon(EIcons.Streak, null, tint = Gold, modifier = Modifier.size(18.dp)) }
            b.monitoredMin?.let { m ->
                StatChip("$m د", Gold) { PhoneTimerIcon(size = 18.dp) }
            }
            if (b.overdue > 0) {
                StatChip("${b.overdue} متأخرة", Danger) { Icon(EIcons.Overdue, null, tint = Danger, modifier = Modifier.size(18.dp)) }
            }
        }
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
        Surface(color = Crimson.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp), modifier = Modifier.animateContentSize()) {
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
        ) {
            Icon(EIcons.Add, contentDescription = null, tint = Gold)
            Spacer(Modifier.width(8.dp))
            Text(if (tab == 0) "إضافة مهمة" else "إضافة مادة", fontSize = 17.sp)
        }
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
private fun StatChip(text: String, tint: Color, icon: @Composable () -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(text, color = tint, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MetaItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = tint, fontSize = 13.sp)
    }
}

@Composable
private fun TaskCard(
    task: Task, subjectName: String, overdue: Boolean, today: String,
    onToggle: (Boolean) -> Unit, onDelete: () -> Unit
) {
    val flagAlpha = when (task.priority) { Priority.HIGH -> 1f; Priority.MEDIUM -> 0.65f; Priority.LOW -> 0.35f }
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp), modifier = Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Row(Modifier.padding(top = 2.dp)) {
                    MetaItem(EIcons.Priority, task.priority.label, Gold.copy(alpha = flagAlpha))
                    task.dueDate?.let { MetaItem(EIcons.Plan, it, if (overdue) Danger else TextSoft.copy(alpha = 0.7f)) }
                    if (overdue) MetaItem(EIcons.Overdue, "متأخرة", Danger)
                    if (task.done) MetaItem(EIcons.Done, "مكتملة", Gold)
                }
                task.deferUntil?.let {
                    if (it > today && !task.done)
                        Text("مؤجلة إلى $it", color = TextSoft.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            DeleteButton(onDelete)
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
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, color = TextSoft, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                Text(examText, color = Gold, fontSize = 13.sp)
                Row(Modifier.padding(top = 2.dp)) {
                    MetaItem(EIcons.Tasks, "$total مهمة", TextSoft.copy(alpha = 0.7f))
                    MetaItem(EIcons.Done, "$done منجزة", Gold)
                }
            }
            DeleteButton(onDelete)
        }
    }
}
