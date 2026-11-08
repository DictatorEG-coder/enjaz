package com.enjaz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.*
import com.enjaz.app.theme.*
import java.util.Locale

@Composable
fun ReportsScreen(
    repo: DataRepository, persona: Persona, settings: AppSettings,
    usage: UsageHelper, tracker: OpenTracker
) {
    var period by remember { mutableStateOf(Period.DAY) }
    val report = Reports.build(period, repo, tracker.openDates(), usage, settings.focusPackage)
    val verdict = remember(period, report.ratePct, report.absentDays) { persona.verdict(report.ratePct, report.absentDays) }

    Column(
        Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("التقارير", color = Gold, fontSize = 26.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        Row {
            Period.values().forEach { p ->
                FilterChip(selected = period == p, onClick = { period = p },
                    label = { Text(p.label) }, modifier = Modifier.padding(end = 8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = Crimson.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp)) {
            Text(verdict, color = TextSoft, fontSize = 16.sp, lineHeight = 26.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
        Spacer(Modifier.height(10.dp))
        Section("الحصيلة") {
            Stat("المهام المكتملة", "${report.completed}")
            Stat("المتأخرة حاليًا", "${report.overdueNow}")
            Stat("نسبة الإنجاز", report.ratePct?.let { "$it%" } ?: "—")
            report.absentDays?.let { Stat("أيام الغياب", "$it") }
        }
        Spacer(Modifier.height(10.dp))
        Section("مقارنة الأداء (مهام مكتملة)") {
            val mx = maxOf(1, report.buckets.maxOf { it.second })
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom) {
                report.buckets.forEach { (label, v) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", color = TextSoft, fontSize = 11.sp)
                        Box(Modifier.width(22.dp).height((6 + 64 * v / mx).dp).background(Gold, RoundedCornerShape(4.dp)))
                        Text(label, color = TextSoft.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Section("أكثر المواد استهلاكًا للوقت") {
            if (report.bySubject.isEmpty()) Text("لا إنجاز مسجل في هذه الفترة.", color = TextSoft, fontSize = 14.sp)
            report.bySubject.forEach { (n, m) -> Stat(n, "$m دقيقة") }
        }
        Spacer(Modifier.height(10.dp))
        Section("التشتت") {
            if (report.tiktokMin == null) {
                Text("غير مفعّل. فعّل الإذن من الإعدادات لقراءة وقت الاستخدام الفعلي.", color = TextSoft, fontSize = 14.sp)
            } else {
                Stat("TikTok", "${report.tiktokMin} دقيقة")
                report.focusMin?.let { Stat("تطبيق تركيز", "$it دقيقة") }
                Stat("المنجز في الفترة", "${report.completed} مهمة")
            }
        }
        Spacer(Modifier.height(10.dp))
        Section("نتائج الامتحانات في الفترة") {
            if (report.results.isEmpty()) Text("لا نتائج مسجلة.", color = TextSoft, fontSize = 14.sp)
            report.results.forEach {
                Stat("${it.subjectName} · ${it.examName}", String.format(Locale.US, "%.0f%%", it.pct))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Gold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TextSoft, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = Gold, fontSize = 15.sp)
    }
}
