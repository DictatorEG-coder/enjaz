package com.enjaz.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.R
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

fun openUrl(ctx: Context, url: String): Boolean = try {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: Exception) { false }

@Composable
fun SettingsScreen(
    settings: AppSettings, usage: UsageHelper,
    onFavorites: () -> Unit, onMonitor: () -> Unit, onNotifs: () -> Unit
) {
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
        ScreenTitle(EIcons.Settings, "الإعدادات")
        msg?.let { Text(it, color = Gold, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))

        // عن التطبيق
        Panel(null, "") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.enjaz_logo), "شعار إنجاز",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("إنجاز | ENJAZ", color = Gold, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("الإصدار 0.4.0", color = TextSoft.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // الروابط
        Panel({ TelegramIcon(Modifier.size(20.dp)) }, "تابعنا") {
            Button(
                onClick = { if (!openUrl(ctx, settings.telegramUrl)) msg = "تعذّر فتح الرابط." },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                TelegramIcon(Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("تابعنا على Telegram", fontSize = 17.sp)
            }
            Text("تابع قناتنا لاكتشاف تطبيقاتنا الجديدة ومشاريعنا القادمة", color = TextSoft,
                fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Spacer(Modifier.height(8.dp))
            LinkRow("قناة Telegram", CHANNEL_URL) { if (!openUrl(ctx, CHANNEL_URL)) msg = "تعذّر فتح الرابط." }
            LinkRow("تطبيق تركيز على Telegram", FOCUS_POST_URL) { if (!openUrl(ctx, FOCUS_POST_URL)) msg = "تعذّر فتح الرابط." }
            OutlinedTextField(tgInput, { tgInput = it }, singleLine = true,
                label = { Text("تخصيص رابط الزر (اتركه فارغًا للافتراضي)") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { msg = settings.updateTelegram(tgInput) ?: "حُفظ إعداد القناة."; tgInput = settings.telegramUrl }) {
                Text("حفظ", color = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))

        Panel({ Icon(EIcons.Quote, null, tint = Gold, modifier = Modifier.size(20.dp)) }, "نبرة إنجاز") {
            Row {
                Mode.values().forEach { m ->
                    FilterChip(selected = settings.mode == m, onClick = { settings.updateMode(m) },
                        label = { Text(m.label) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        NavRow({ Icon(EIcons.Bell, null, tint = Gold) }, "الإشعارات التلقائية",
            if (settings.notifEnabled) "مفعّلة" else "متوقفة", onNotifs)
        Spacer(Modifier.height(8.dp))
        NavRow({ PhoneWarnIcon() }, "مراقبة التطبيقات",
            if (settings.monitoringEnabled) "مفعّلة" else "متوقفة", onMonitor)
        Spacer(Modifier.height(8.dp))
        NavRow({ Icon(EIcons.Quote, null, tint = Gold) }, "الحكم المحفوظة", "", onFavorites)
        Spacer(Modifier.height(10.dp))

        Panel({ Icon(EIcons.Focus, null, tint = Gold, modifier = Modifier.size(20.dp)) }, "ربط تركيز") {
            Text("يقارن إنجاز وقت استخدامك لتطبيق تركيز بباقي التطبيقات في التقارير. يعتمد على إذن الاستخدام الذي تمنحه أنت.",
                color = TextSoft, fontSize = 14.sp, lineHeight = 22.sp)
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
                    color = if (perm) Gold else Danger, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { perm = usage.hasPermission() }) { Text("تحديث الحالة", color = Gold) }
            }
            OutlinedTextField(pkgInput, { pkgInput = it }, singleLine = true,
                label = { Text("اسم حزمة تركيز (اختياري)") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { msg = settings.updateFocusPackage(pkgInput) ?: "حُفظت الحزمة." }) {
                Text("حفظ الحزمة", color = Gold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (askPerm) AlertDialog(
        onDismissRequest = { askPerm = false },
        containerColor = Surface1,
        title = { Text("إذن الوصول إلى بيانات الاستخدام", color = Gold) },
        text = {
            Text("سيفتح إنجاز شاشة إعدادات النظام لتمنح الإذن بنفسك. ثم عُد واضغط «تحديث الحالة» وفعّل الربط.",
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
private fun LinkRow(title: String, url: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelegramIcon(Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextSoft, fontSize = 15.sp)
            Text(url, color = Gold.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun NavRow(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp), modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextSoft, fontSize = 16.sp)
                if (subtitle.isNotEmpty()) Text(subtitle, color = Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Panel(icon: (@Composable () -> Unit)?, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            if (icon != null) { SectionHeader(icon, title); Spacer(Modifier.height(6.dp)) }
            content()
        }
    }
}
