package com.enjaz.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.R
import com.enjaz.app.theme.Danger
import com.enjaz.app.theme.Gold

/** مجموعة أيقونات موحّدة (Material Outlined). الذهبي للعادي والأحمر للتنبيه والتأخير. */
object EIcons {
    val Tasks = Icons.Outlined.Checklist
    val Plan = Icons.Outlined.CalendarMonth
    val Reports = Icons.Outlined.ShowChart
    val Experience = Icons.Outlined.EmojiEvents
    val Settings = Icons.Outlined.Settings
    val Time = Icons.Outlined.Timer
    val Focus = Icons.Outlined.TrackChanges
    val Achievement = Icons.Outlined.WorkspacePremium
    val Priority = Icons.Outlined.Flag
    val Overdue = Icons.Outlined.AccessAlarm
    val Delete = Icons.Outlined.Delete
    val Add = Icons.Outlined.Add
    val Stats = Icons.Outlined.BarChart
    val Phone = Icons.Outlined.PhoneAndroid
    val Warning = Icons.Outlined.Warning
    val Bell = Icons.Outlined.Notifications
    val Streak = Icons.Outlined.LocalFireDepartment
    val Done = Icons.Outlined.CheckCircle
    val Quote = Icons.Outlined.FormatQuote
}

@Composable
fun TelegramIcon(modifier: Modifier = Modifier, tint: Color = Gold) {
    Icon(painterResource(R.drawable.ic_telegram), contentDescription = "Telegram", tint = tint, modifier = modifier)
}

/** هاتف مع علامة صغيرة في الركن (مؤقت أو تحذير). */
@Composable
fun PhoneBadgeIcon(badge: ImageVector, size: Dp = 24.dp, tint: Color = Gold, badgeTint: Color = tint) {
    Box(Modifier.size(size)) {
        Icon(EIcons.Phone, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize())
        Icon(badge, contentDescription = null, tint = badgeTint,
            modifier = Modifier.size(size * 0.55f).align(Alignment.BottomEnd))
    }
}

@Composable
fun PhoneTimerIcon(size: Dp = 24.dp, tint: Color = Gold) = PhoneBadgeIcon(EIcons.Time, size, tint, tint)

@Composable
fun PhoneWarnIcon(size: Dp = 24.dp, tint: Color = Gold) = PhoneBadgeIcon(EIcons.Warning, size, tint, Danger)

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(EIcons.Delete, contentDescription = "حذف", tint = Gold)
    }
}

@Composable
fun ScreenTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Gold, fontSize = 26.sp, fontFamily = FontFamily.Serif)
    }
}

@Composable
fun SectionHeader(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(text, color = Gold, fontSize = 16.sp)
    }
}
