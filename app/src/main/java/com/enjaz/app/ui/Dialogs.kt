package com.enjaz.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enjaz.app.data.Priority
import com.enjaz.app.data.Subject
import com.enjaz.app.data.TaskType
import com.enjaz.app.theme.*

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> String?) {
    var name by remember { mutableStateOf("") }
    var exam by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("مادة جديدة", color = Gold) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المادة") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    exam, { exam = it }, singleLine = true,
                    label = { Text("تاريخ الامتحان (اختياري) 2026-12-31") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onSubmit(name, exam)
                if (e == null) onDismiss() else error = e
            }) { Text("إضافة", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSoft) } }
    )
}

@Composable
fun AddTaskDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSubmit: (Long?, TaskType, String, String, String, Priority) -> String?
) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var type by remember { mutableStateOf(TaskType.LECTURE) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var due by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("مهمة جديدة", color = Gold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("المادة", color = TextSoft)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    subjects.forEach { s ->
                        FilterChip(
                            selected = subjectId == s.id, onClick = { subjectId = s.id },
                            label = { Text(s.name) }, modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Row {
                    TaskType.values().forEach { t ->
                        FilterChip(
                            selected = type == t, onClick = { type = t },
                            label = { Text(t.label) }, modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                OutlinedTextField(title, { title = it }, label = { Text("العنوان") }, singleLine = true)
                OutlinedTextField(
                    duration, { duration = it }, label = { Text("المدة بالدقائق") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    due, { due = it }, label = { Text("الموعد (اختياري) 2026-12-31") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                Text("الأولوية", color = TextSoft, modifier = Modifier.padding(top = 8.dp))
                Row {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p, onClick = { priority = p },
                            label = { Text(p.label) }, modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onSubmit(subjectId, type, title, duration, due, priority)
                if (e == null) onDismiss() else error = e
            }) { Text("إضافة", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSoft) } }
    )
}
