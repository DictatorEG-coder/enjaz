package com.enjaz.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

@Composable
fun QuickItemsScreen(repo: QuickItemsRepository) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val pending = repo.items.filter { !it.done }.sortedByDescending { it.createdAt }
    val completed = repo.items.filter { it.done }.sortedByDescending { it.createdAt }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        ScreenTitle(EIcons.Tasks, "قائمة الإنجاز")
        Text("أي شيء تريد فعله لاحقًا… سجّله هنا قبل أن يطير من دماغك. فعّل تذكيرات القائمة من الإعدادات لتصلك التنبيهات.",
            color = TextSoft.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("معلّق: ${repo.pendingCount()}")
            StatPill("منجز: ${repo.doneCount()}")
        }
        message?.let { Text(it, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ما زال عليك", color = Gold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { showDone = !showDone }) {
                Text(if (showDone) "إخفاء المنجز" else "عرض المنجز (${completed.size})", color = TextSoft)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (pending.isEmpty()) item {
                Surface(color = Surface1, shape = RoundedCornerShape(12.dp)) {
                    Text("القائمة فاضية. يا إما أنت منجز، يا إما لسه ما افتكرتش حاجة.",
                        color = TextSoft, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
            items(pending, key = { it.id }) { item ->
                QuickItemCard(item, { checked ->
                    message = if (repo.setDone(item.id, checked)) "اتسجل الإنجاز. أهو الكلام." else "تعذّر حفظ التغيير."
                }, {
                    message = if (repo.delete(item.id)) "اتحذف العنصر." else "تعذّر الحذف."
                }, {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                        .onFailure { message = "تعذّر فتح الرابط." }
                })
            }
            if (showDone && completed.isNotEmpty()) {
                item { Text("أرشيف الإنجازات", color = Gold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(completed, key = { "done_${it.id}" }) { item ->
                    QuickItemCard(item, { checked ->
                        message = if (repo.setDone(item.id, checked)) "أُعيد العنصر إلى القائمة." else "تعذّر حفظ التغيير."
                    }, {
                        message = if (repo.delete(item.id)) "اتحذف العنصر." else "تعذّر الحذف."
                    }, {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                            .onFailure { message = "تعذّر فتح الرابط." }
                    })
                }
            }
        }
        Button(onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
            modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(EIcons.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("أضف شيئًا لا تريد نسيانه", fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showAdd) AddQuickItemDialog(onDismiss = { showAdd = false }) { title, url, details, kind, hours ->
        val error = repo.add(title, url, details, kind, hours)
        if (error == null) { message = "اتسجلت. المرة دي هنفتكرها إحنا الاتنين."; showAdd = false }
        error
    }
}

@Composable
private fun StatPill(value: String) {
    Surface(color = Surface1, shape = RoundedCornerShape(18.dp)) {
        Text(value, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
private fun QuickItemCard(
    item: QuickItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onOpen: () -> Unit
) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.done, onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Ink, uncheckedColor = Gold))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = if (item.done) TextSoft.copy(alpha = 0.5f) else TextSoft,
                    fontSize = 16.sp, textDecoration = if (item.done) TextDecoration.LineThrough else null)
                Text("${item.kind.label} · تذكير كل ${item.reminderHours} ساعة", color = Gold, fontSize = 12.sp)
                if (item.details.isNotBlank()) Text(item.details, color = TextSoft.copy(alpha = 0.7f),
                    fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
                if (item.url.isNotBlank()) TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
                    Icon(EIcons.Plan, null, tint = Gold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("فتح الرابط المحفوظ", color = Gold, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDelete) { Icon(EIcons.Delete, "حذف", tint = TextSoft) }
        }
    }
}

@Composable
private fun AddQuickItemDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, QuickItemKind, String) -> String?
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("24") }
    var kind by remember { mutableStateOf(QuickItemKind.OTHER) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Surface1,
        title = { Text("إضافة إلى قائمة الإنجاز", color = Gold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عايز تعمل إيه؟") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("الرابط (اختياري)") }, singleLine = true)
                OutlinedTextField(details, { details = it }, label = { Text("ملاحظاتك (اختياري)") }, minLines = 2)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("النوع: ${kind.label}", color = Gold)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        QuickItemKind.values().forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { kind = option; expanded = false })
                        }
                    }
                }
                OutlinedTextField(hours, { hours = it.filter(Char::isDigit) },
                    label = { Text("ذكّرني كل كام ساعة؟ (1 إلى 168)") }, singleLine = true)
                Text("سيستمر التذكير ما دام العنصر غير مكتمل، ويمكنك إنهاؤه بعلامة الصح.",
                    color = TextSoft.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 16.sp)
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSubmit(title, url, details, kind, hours) }) { Text("حفظ", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSoft) } }
    )
}
