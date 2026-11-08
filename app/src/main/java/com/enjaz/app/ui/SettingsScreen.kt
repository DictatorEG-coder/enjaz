package com.enjaz.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

private fun openUrl(ctx: Context, url: String): Boolean = try {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: Exception) { false }

@Composable
fun SettingsScreen(settings: AppSettings, usage: UsageHelper, onFavorites: () -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var tgInput by remember { mutableStateOf(settings.telegramUrl) }
    var pkgInput by remember { mutableStateOf(settings.focusPackage) }
    var perm by remember { mutableStateOf(usage.hasPermission()) }
    var askPerm by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("الإعدادات", color = Gold, fontSize = 26.sp, fontFamily = FontFamily.Serif)
        msg?.let { Text(it, color = Gold, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))

        Section2("تابعنا") {
            Button(
                onClick = {
                    if (settings.telegramUrl.isBlank()) msg = "قناة المطور لم تُحدد بعد. أدخل رابطها في الحقل أدناه."
                    else if (!openUrl(ctx, settings.telegramUrl)) msg = "تعذّر فتح الرابط."
                },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("تابعنا على Telegram", fontSize = 17.sp) }
            Text("تابع قناتنا لاكتشاف تطبيقاتنا الجديدة ومشاريعنا القادمة", color = TextSoft,
                fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            OutlinedTextField(tgInput, { tgInput = it }, singleLine = true,
                label = { Text("رابط القناة (@name أو t.me/name)") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { msg = settings.updateTelegram(tgInput) ?: "حُفظ إعداد القناة."; tgInput = settings.telegramUrl }) {
                Text("حفظ القناة", color = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))

        Section2("نبرة إنجاز") {
            Row {
                Mode.values().forEach { m ->
                    FilterChip(selected = settings.mode == m, onClick = { settings.updateMode(m) },
                        label = { Text(m.label) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Section2("ربط تركيز وتقارير الاستخدام") {
            Text(
                "يقرأ إنجاز وقت استخدام التطبيقات من النظام بإذنك فقط، ولا يعرض أي ملاحظة عن TikTok دون بيانات فعلية.",
                color = TextSoft, fontSize = 14.sp, lineHeight = 22.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تفعيل الربط", color = TextSoft, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.focusLinked,
                    onCheckedChange = { on ->
                        if (!on) settings.updateFocusLinked(false)
                        else if (usage.hasPermission()) { perm = true; settings.updateFocusLinked(true) }
                        else askPerm = true
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (perm) "إذن الاستخدام: ممنوح" else "إذن الاستخدام: غير ممنوح",
                    color = Gold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { perm = usage.hasPermission() }) { Text("تحديث الحالة", color = Gold) }
            }
            OutlinedTextField(pkgInput, { pkgInput = it }, singleLine = true,
                label = { Text("اسم حزمة تركيز (اختياري)") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { msg = settings.updateFocusPackage(pkgInput) ?: "حُفظت الحزمة." }) {
                Text("حفظ الحزمة", color = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedButton(onClick = onFavorites, modifier = Modifier.fillMaxWidth()) {
            Text("الحكم المحفوظة", color = Gold)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (askPerm) AlertDialog(
        onDismissRequest = { askPerm = false },
        containerColor = Surface1,
        title = { Text("إذن الوصول إلى بيانات الاستخدام", color = Gold) },
        text = {
            Text("سيفتح إنجاز شاشة إعدادات النظام لتمنح الإذن بنفسك. ثم عُد إلى التطبيق واضغط «تحديث الحالة» وفعّل الربط.",
                color = TextSoft)
        },
        confirmButton = {
            TextButton(onClick = {
                askPerm = false
                if (!usage.openPermissionScreen()) msg = "تعذّر فتح إعدادات النظام."
            }) { Text("فتح الإعدادات", color = Gold) }
        },
        dismissButton = { TextButton(onClick = { askPerm = false }) { Text("إلغاء", color = TextSoft) } }
    )
}

@Composable
private fun Section2(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Gold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
