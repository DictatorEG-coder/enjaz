package com.enjaz.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enjaz.app.data.DateUtil
import com.enjaz.app.data.NoteType
import com.enjaz.app.data.Subject
import com.enjaz.app.theme.*

@Composable
fun AddResultDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, Boolean) -> String?
) {
    var exam by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("100") }
    var target by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtil.today()) }
    var effort by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("نتيجة جديدة", color = Gold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(exam, { exam = it }, label = { Text("اسم الامتحان (مثال: الثانوية العامة)") }, singleLine = true)
                if (subjects.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                        subjects.forEach { s ->
                            FilterChip(selected = subject == s.name, onClick = { subject = s.name },
                                label = { Text(s.name) }, modifier = Modifier.padding(end = 6.dp))
                        }
                    }
                }
                OutlinedTextField(subject, { subject = it }, label = { Text("المادة") }, singleLine = true)
                OutlinedTextField(score, { score = it }, label = { Text("درجتك") }, singleLine = true, keyboardOptions = decimal)
                OutlinedTextField(max, { max = it }, label = { Text("الدرجة النهائية") }, singleLine = true, keyboardOptions = decimal)
                OutlinedTextField(target, { target = it }, label = { Text("الدرجة التي طمحت إليها (اختياري)") }, singleLine = true, keyboardOptions = decimal)
                OutlinedTextField(date, { date = it }, label = { Text("التاريخ 2026-12-31") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = effort, onCheckedChange = { effort = it },
                        colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Ink, uncheckedColor = Gold))
                    Text("بذلت كل ما أستطيع", color = TextSoft)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onSubmit(exam, subject, score, max, target, date, effort)
                if (e == null) onDismiss() else error = e
            }) { Text("حفظ", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSoft) } }
    )
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onSubmit: (NoteType, String) -> String?) {
    var type by remember { mutableStateOf(NoteType.GOAL) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("هدف أو درس", color = Gold) },
        text = {
            Column {
                Row {
                    NoteType.values().forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.label) }, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                OutlinedTextField(text, { text = it }, label = { Text("النص") }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onSubmit(type, text)
                if (e == null) onDismiss() else error = e
            }) { Text("حفظ", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSoft) } }
    )
}
