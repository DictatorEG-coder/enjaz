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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*

@Composable
fun MonitoringScreen(settings: AppSettings, usage: UsageHelper, onBack: () -> Unit) {
    var perm by remember { mutableStateOf(usage.hasPermission()) }
    var askPerm by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val apps = remember { usage.listLaunchableApps() }
    val minutes = remember(perm) {
        usage.minutesByPackage(apps.map { it.pkg }.toSet(), DateUtil.startOfToday(), System.currentTimeMillis())
    }
    // المراقَبة أولًا ثم الباقي
    val sorted = remember { apps.sortedWith(compareBy<AppEntry>({ !settings.isMonitored(it.pkg) }, { it.label.lowercase() })) }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhoneWarnIcon(size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Text("مراقبة التطبيقات", color = Gold, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "لا يراقب إنجاز شيئًا دون موافقتك. يقرأ فقط وقت الاستخدام من النظام، ولا يمنع ولا يعطّل أي تطبيق. " +
                "اختر ما تريد مراقبته، وما تتركه «مسموحًا» لا يُحتسب.",
            color = TextSoft.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp
        )
        msg?.let { Text(it, color = Gold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تفعيل المراقبة", color = TextSoft, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.monitoringEnabled,
                        onCheckedChange = { on ->
                            if (!on) settings.updateMonitoring(false)
                            else if (usage.hasPermission()) { perm = true; settings.updateMonitoring(true) }
                            else askPerm = true
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (perm) "إذن الاستخدام: ممنوح" else "إذن الاستخدام: غير ممنوح",
                        color = if (perm) Gold else Danger, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        perm = usage.hasPermission()
                        if (perm && settings.monitoringEnabled.not()) msg = "الإذن ممنوح. فعّل المراقبة الآن."
                    }) { Text("تحديث الحالة", color = Gold) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("التطبيقات (${sorted.size})", color = Gold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sorted, key = { it.pkg }) { app ->
                val on = settings.isMonitored(app.pkg)
                Surface(color = Surface1, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = TextSoft, fontSize = 15.sp)
                            val m = minutes?.get(app.pkg)
                            Text(
                                (if (on) "مراقَب" else "مسموح") + (if (m != null) " · اليوم: $m دقيقة" else ""),
                                color = if (on) Gold else TextSoft.copy(alpha = 0.6f), fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = on, onCheckedChange = { settings.updateMonitored(app.pkg, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold)) { Text("رجوع") }
        Spacer(Modifier.height(12.dp))
    }

    if (askPerm) AlertDialog(
        onDismissRequest = { askPerm = false },
        containerColor = Surface1,
        title = { Text("إذن الوصول إلى بيانات الاستخدام", color = Gold) },
        text = {
            Text("سيفتح إنجاز شاشة إعدادات النظام لتمنح الإذن بنفسك. ثم عُد واضغط «تحديث الحالة» وفعّل المراقبة.",
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
