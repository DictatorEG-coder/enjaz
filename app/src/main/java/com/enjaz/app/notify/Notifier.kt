package com.enjaz.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.enjaz.app.MainActivity
import com.enjaz.app.R
import com.enjaz.app.data.DateUtil

object Notifier {
    private const val CHANNEL = "enjaz_main"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL, "تنبيهات إنجاز", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "مساءلة وتذكيرات شخصية مبنية على مهامك واستخدام هاتفك"
            nm.createNotificationChannel(ch)
        }
    }

    fun permissionGranted(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** يعمل فقط إن مُنح الإذن ولم يُعطَّل الإشعار من إعدادات النظام. */
    fun canNotify(ctx: Context): Boolean =
        permissionGranted(ctx) && NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun send(ctx: Context, id: Int, title: String, body: String): Boolean {
        if (!canNotify(ctx)) return false
        ensureChannel(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(0xFF650D24.toInt())
            .build()
        return try { NotificationManagerCompat.from(ctx).notify(id, n); true } catch (e: SecurityException) { false }
    }
}

/** يمنع التكرار: كل نوع مرة يوميًا (والاستخدام المفرط عند كل مضاعف جديد)، وبحد أقصى للإشعارات اليومية. */
class NotifState(context: Context) {
    private val p = context.getSharedPreferences("enjaz_notif", Context.MODE_PRIVATE)
    private val today get() = DateUtil.today()

    fun sentToday(type: String) = p.getString("last_$type", "") == today
    fun markSent(type: String) {
        val c = countToday() + 1
        p.edit().putString("last_$type", today).putString("count_date", today).putInt("count", c).apply()
    }
    fun countToday(): Int = if (p.getString("count_date", "") == today) p.getInt("count", 0) else 0
    fun distractLevel(): Int = if (p.getString("dl_date", "") == today) p.getInt("dl", 0) else 0
    fun setDistractLevel(l: Int) { p.edit().putString("dl_date", today).putInt("dl", l).apply() }
}
