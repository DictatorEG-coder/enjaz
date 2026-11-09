package com.enjaz.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.notify.Notifier
import com.enjaz.app.notify.ReminderScheduler
import com.enjaz.app.theme.*

private fun openAppNotificationSettings(ctx: Context): Boolean = try {
    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i); true
} catch (e: Exception) { false }

private fun hh(h: Int) = String.format(java.util.Locale.US, "%02d:00", h)

@Composable
fun NotificationSettingsScreen(settings: AppSettings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var sysOk by remember { mutableStateOf(Notifier.canNotify(ctx)) }

    fun enable() {
        settings.updateNotifEnabled(true)
        ReminderScheduler.apply(ctx, true)
        Notifier.ensureChannel(ctx)
        msg = "فُعّلت الإشعارات التلقائية. يفحص إنجاز سلوكك مرة كل ساعة تقريبًا."
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        sysOk = Notifier.canNotify(ctx)
        if (granted && sysOk) enable()
        else msg = "رُفض إذن الإشعارات، فلن يستطيع إنجاز تنبيهك. يمكنك منحه من إعدادات النظام بزر «فتح إعدادات الإشعارات»."
    }

    Column(
        Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenTitle(EIcons.Bell, "الإشعارات التلقائية")
        Spacer(Modifier.height(4.dp))
        Text("إشعارات شخصية يولّدها التطبيق من مهامك وسلوكك فقط. لا خادم ولا رسائل من أي جهة.",
            color = TextSoft.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp)
        msg?.let { Text(it, color = Gold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))

        Block {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تفعيل الإشعارات التلقائية", color = TextSoft, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.notifEnabled,
                    onCheckedChange = { on ->
                        if (!on) {
                            settings.updateNotifEnabled(false)
                            ReminderScheduler.apply(ctx, false)
                            msg = "أُوقفت الإشعارات."
                        } else if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(ctx)) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else if (!Notifier.canNotify(ctx)) {
                            msg = "الإشعارات مغلقة لهذا التطبيق من إعدادات النظام. افتحها ثم عُد."
                        } else enable()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (sysOk) "إذن النظام: ممنوح" else "إذن النظام: غير ممنوح",
                    color = if (sysOk) Gold else Danger, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { sysOk = Notifier.canNotify(ctx) }) { Text("تحديث", color = Gold) }
            }
            if (!sysOk) {
                OutlinedButton(onClick = { if (!openAppNotificationSettings(ctx)) msg = "تعذّر فتح إعدادات النظام." },
                    modifier = Modifier.fillMaxWidth()) { Text("فتح إعدادات الإشعارات", color = Gold) }
            }
            OutlinedButton(
                onClick = {
                    sysOk = Notifier.canNotify(ctx)
                    val ok = Notifier.send(ctx, 99, "إنجاز · اختبار", "هذا إشعار تجريبي. إن رأيته فالإشعارات تعمل على جهازك.")
                    msg = if (ok) "أُرسل الإشعار التجريبي." else "تعذّر الإرسال. امنح الإذن أولًا."
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { Text("إرسال إشعار تجريبي", color = Gold) }
        }
        Spacer(Modifier.height(10.dp))

        Block {
            Text("أنواع الإشعارات", color = Gold, fontSize = 16.sp)
            TypeRow("مساءلة عند طول الغياب عن الإنجاز", EIcons.Time, "account", settings)
            TypeRow("تذكير بالمهام المتأخرة", EIcons.Overdue, "overdue", settings)
            TypeRow("تنبيه الإفراط في التطبيقات المراقَبة", EIcons.Warning, "distract", settings)
            TypeRow("اقتراب موعد مهمة أو امتحان", EIcons.Plan, "deadline", settings)
            TypeRow("مراجعة نهاية اليوم", EIcons.Done, "evening", settings)
            if (settings.nDistract && !settings.monitoringEnabled) {
                Text("تنبيه الاستخدام لا يعمل قبل تفعيل مراقبة التطبيقات ومنح إذنها.",
                    color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(10.dp))

        Block {
            Text("الأوقات", color = Gold, fontSize = 16.sp)
            Stepper("بداية الإشعارات", hh(settings.startHour),
                { settings.setWindow(settings.startHour - 1, settings.endHour) },
                { settings.setWindow(settings.startHour + 1, settings.endHour) })
            Stepper("نهاية الإشعارات", hh(settings.endHour),
                { settings.setWindow(settings.startHour, settings.endHour - 1) },
                { settings.setWindow(settings.startHour, settings.endHour + 1) })
            Stepper("مراجعة نهاية اليوم", hh(settings.eveningHour),
                { settings.updateEveningHour(settings.eveningHour - 1) },
                { settings.updateEveningHour(settings.eveningHour + 1) })
            Stepper("حد التنبيه للتطبيقات", "${settings.distractThreshold} دقيقة",
                { settings.updateDistractThreshold(settings.distractThreshold - 15) },
                { settings.updateDistractThreshold(settings.distractThreshold + 15) })
            Text("لا تصل أي إشعارات خارج النافذة الزمنية، ولا يزيد المجموع عن 5 يوميًا، ولا يتكرر النوع نفسه في اليوم.",
                color = TextSoft.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("ملاحظة: يؤجل النظام العمل الدوري أحيانًا لتوفير البطارية، فقد يتأخر الإشعار قليلًا عن وقته. استثنِ إنجاز من توفير الطاقة لدقة أعلى.",
            color = TextSoft.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold)) { Text("رجوع") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TypeRow(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, type: String, settings: AppSettings
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextSoft, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = settings.isTypeOn(type), onCheckedChange = { settings.setNotifType(type, it) },
            colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink)
        )
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSoft, fontSize = 14.sp)
            Text(value, color = Gold, fontSize = 16.sp)
        }
        OutlinedButton(onClick = onMinus) { Text("−", color = Gold) }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onPlus) { Text("+", color = Gold) }
    }
}

@Composable
private fun Block(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) { content() }
    }
}
