package com.enjaz.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings

/** قراءة وقت استخدام التطبيقات عبر واجهة النظام الرسمية، ولا تعمل إلا بإذن المستخدم. لا يُعطَّل أي تطبيق. */
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

    /** دقائق الاستخدام لكل حزمة، أو null إن لم يُمنح الإذن. */
    fun minutesByPackage(packages: Set<String>, startMs: Long, endMs: Long): Map<String, Int>? {
        if (packages.isEmpty() || !hasPermission()) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val all = usm.queryAndAggregateUsageStats(startMs, endMs)
            packages.associateWith { ((all[it]?.totalTimeInForeground ?: 0L) / 60000L).toInt() }
        } catch (e: Exception) { null }
    }

    fun minutes(packages: Set<String>, startMs: Long, endMs: Long): Int? =
        minutesByPackage(packages, startMs, endMs)?.values?.sum()

    fun labelOf(pkg: String): String = PRESET_APPS[pkg] ?: try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    /** التطبيقات القابلة للتشغيل التي يستطيع المستخدم اختيارها. تطبيقات النظام الأصلية (الاتصال، الطوارئ...) مستبعدة. */
    fun listLaunchableApps(): List<AppEntry> = try {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val ai = ri.activityInfo.applicationInfo
            val pkg = ai.packageName
            val pureSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (pkg == context.packageName || (pureSystem && pkg !in PRESET_APPS)) null
            else AppEntry(pkg, ri.loadLabel(pm).toString())
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    } catch (e: Exception) { emptyList() }
}
