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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*
import java.util.Locale

@Composable
fun ExperienceScreen(repo: DataRepository, persona: Persona) {
    var feedback by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    val results = repo.results.sortedByDescending { it.date }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        ScreenTitle(EIcons.Experience, "سجل التجارب")
        Spacer(Modifier.height(8.dp))
        feedback?.let {
            Surface(color = Crimson.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp)) {
                Text(it, color = TextSoft, fontSize = 16.sp, lineHeight = 27.sp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row {
            Button(
                onClick = { showResult = true },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
                modifier = Modifier.weight(1f)
            ) { Text("إضافة نتيجة") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { showNote = true }, modifier = Modifier.weight(1f)) {
                Text("هدف أو درس", color = Gold)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty() && repo.notes.isEmpty()) {
            Text("لا تجارب محفوظة بعد.", color = TextSoft)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(results, key = { "r" + it.id }) { r ->
                val prev = repo.previousFor(r)
                Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(EIcons.Experience, null, tint = Gold, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${r.subjectName} · ${r.examName}", color = TextSoft, fontSize = 16.sp)
                            Text(
                                String.format(Locale.US, "%.1f / %.0f  (%.0f%%)", r.score, r.max, r.pct) +
                                    (r.target?.let { String.format(Locale.US, " · الهدف %.1f", it) } ?: ""),
                                color = Gold, fontSize = 14.sp
                            )
                            val trend = prev?.let {
                                val d = r.pct - it.pct
                                when {
                                    Math.abs(d) < 0.05 -> "ثابت عن السابق"
                                    d > 0 -> String.format(Locale.US, "تحسّن %.1f نقطة", d)
                                    else -> String.format(Locale.US, "تراجع %.1f نقطة", -d)
                                }
                            }
                            Text(r.date + (trend?.let { " · $it" } ?: ""), color = TextSoft.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        DeleteButton({ error = repo.deleteResult(r.id) })
                    }
                }
            }
            items(repo.notes.sortedByDescending { it.createdAt }, key = { "n" + it.id }) { n ->
                Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (n.type == NoteType.GOAL) {
                            Checkbox(
                                checked = n.achieved, onCheckedChange = { error = repo.toggleAchieved(n.id) },
                                colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Ink, uncheckedColor = Gold)
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = if (n.type == NoteType.GOAL) 0.dp else 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (n.achieved) {
                                    Icon(EIcons.Achievement, null, tint = Gold, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(n.type.label, color = Gold, fontSize = 12.sp)
                            }
                            Text(n.text, color = TextSoft, fontSize = 16.sp, lineHeight = 24.sp,
                                textDecoration = if (n.achieved) TextDecoration.LineThrough else null)
                        }
                        DeleteButton({ error = repo.deleteNote(n.id) })
                    }
                }
            }
        }
    }

    if (showResult) AddResultDialog(
        subjects = repo.subjects,
        onDismiss = { showResult = false },
        onSubmit = { exam, subject, score, max, target, date, effort ->
            val out = repo.addResult(exam, subject, score, max, target, date, effort)
            if (out.error == null && out.result != null) {
                feedback = persona.resultFeedback(out.result, repo.previousFor(out.result))
            }
            out.error
        }
    )
    if (showNote) AddNoteDialog(
        onDismiss = { showNote = false },
        onSubmit = { type, text -> repo.addNote(type, text) }
    )
}
