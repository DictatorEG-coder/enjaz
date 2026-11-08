package com.enjaz.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

val TIKTOK_PACKAGES = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

/** قراءة وقت استخدام التطبيقات عبر واجهة النظام الرسمية، ولا تعمل إلا بإذن المستخدم. */
class UsageHelper(private val context: Context) {

    @Suppress("DEPRECATION")
    fun hasPermission(): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    fun openPermissionScreen(): Boolean = try {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) { false }

    /** دقائق الاستخدام الفعلية للحزم في المدى، أو null إن لم يُمنح الإذن. */
    fun minutes(packages: Set<String>, startMs: Long, endMs: Long): Int? {
        if (packages.isEmpty() || !hasPermission()) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val map = usm.queryAndAggregateUsageStats(startMs, endMs)
            var total = 0L
            packages.forEach { total += map[it]?.totalTimeInForeground ?: 0L }
            (total / 60000L).toInt()
        } catch (e: Exception) { null }
    }
}
