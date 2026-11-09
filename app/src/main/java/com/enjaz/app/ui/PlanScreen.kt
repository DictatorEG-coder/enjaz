package com.enjaz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

@Composable
fun PlanScreen(repo: DataRepository, settings: AppSettings, persona: Persona) {
    var error by remember { mutableStateOf<String?>(null) }
    val plan = Planner.build(repo.tasks, repo.subjects, settings.capacityMin, repo.doneTodayMinutes())
    val names = repo.subjects.associate { it.id to it.name }
    val today = DateUtil.today()
    val noFit = remember(plan.unscheduled.size) { if (plan.unscheduled.isNotEmpty()) persona.planNoFit() else "" }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        ScreenTitle(EIcons.Plan, "الخطة اليومية")
        Spacer(Modifier.height(8.dp))
        Surface(color = Surface1, shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الوقت المتاح يوميًا", color = TextSoft, fontSize = 13.sp)
                    Text("${settings.capacityMin} دقيقة", color = Gold, fontSize = 18.sp)
                }
                OutlinedButton(onClick = { settings.updateCapacity(settings.capacityMin - 30) }) { Text("−30", color = Gold) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { settings.updateCapacity(settings.capacityMin + 30) }) { Text("+30", color = Gold) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        if (repo.pendingCount() == 0) {
            Text("لا مهام معلقة. لا خطة لما لا يوجد.", color = TextSoft)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(plan.days, key = { it.date }) { day ->
                    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            val label = when (day.date) {
                                today -> "اليوم"
                                DateUtil.plus(today, 1) -> "غدًا"
                                else -> day.date
                            }
                            Text("$label · ${day.used}/${day.capacity} دقيقة", color = Gold, fontSize = 16.sp)
                            if (day.items.isEmpty()) {
                                Text("لا شيء مجدول لهذا اليوم.", color = TextSoft.copy(alpha = 0.7f), fontSize = 14.sp)
                            }
                            day.items.forEach { item ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                        Text(item.task.title, color = TextSoft, fontSize = 16.sp)
                                        Text(
                                            "${names[item.task.subjectId] ?: "—"} · ${item.minutes} دقيقة" +
                                                if (item.partial) " (جزء)" else "",
                                            color = TextSoft.copy(alpha = 0.7f), fontSize = 13.sp
                                        )
                                    }
                                    TextButton(onClick = {
                                        error = repo.deferTask(item.task.id, DateUtil.plus(day.date, 1))
                                    }) { Text("أجّل", color = Gold) }
                                }
                            }
                        }
                    }
                }
                if (plan.unscheduled.isNotEmpty()) {
                    item {
                        Surface(color = Crimson.copy(alpha = 0.45f), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("خارج الوقت المتاح", color = Gold, fontSize = 16.sp)
                                Text(noFit, color = TextSoft, fontSize = 14.sp, lineHeight = 22.sp)
                                plan.unscheduled.forEach { u ->
                                    Text("• ${u.task.title} — ${names[u.task.subjectId] ?: "—"} — متبقٍ ${u.remaining} دقيقة",
                                        color = TextSoft, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
