package com.enjaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.enjaz.app.data.*
import com.enjaz.app.notify.ReminderScheduler
import com.enjaz.app.theme.*
import com.enjaz.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext
        val wisdomRepo = WisdomRepository(app)
        val data = DataRepository(app)
        val settings = AppSettings(app)
        val persona = Persona(app, settings)
        val usage = UsageHelper(app)
        val tracker = OpenTracker(app)

        val current = wisdomRepo.nextWisdom()
        val missedDays = tracker.recordLaunch()
        val absenceMsg = persona.absence(missedDays)
        val firstDate = tracker.firstDate()

        // يعيد جدولة الفحص الدوري بعد التحديث أو إعادة تشغيل الجهاز (KEEP لا يكرر الجدولة)
        ReminderScheduler.apply(app, settings.notifEnabled)

        val tabs: List<Pair<String, ImageVector>> = listOf(
            "المهام" to EIcons.Tasks,
            "الخطة" to EIcons.Plan,
            "التقارير" to EIcons.Reports,
            "التجارب" to EIcons.Experience,
            "الإعدادات" to EIcons.Settings
        )

        setContent {
            EnjazTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var screen by remember { mutableStateOf("wisdom") }
                    var tab by remember { mutableStateOf(0) }
                    var favs by remember { mutableStateOf(wisdomRepo.favoriteIds()) }
                    var welcome by remember { mutableStateOf(absenceMsg) }

                    when (screen) {
                        "wisdom" -> WisdomScreen(
                            wisdom = current,
                            isFavorite = current.id in favs,
                            onToggleFavorite = { favs = wisdomRepo.toggleFavorite(current.id) },
                            onEnter = { screen = "main" }
                        )
                        "main" -> Scaffold(
                            containerColor = Ink,
                            bottomBar = {
                                NavigationBar(containerColor = Surface1) {
                                    tabs.forEachIndexed { i, (label, icon) ->
                                        val selected = tab == i
                                        val tint by animateColorAsState(if (selected) Gold else TextSoft, label = "tint")
                                        val scale by animateFloatAsState(if (selected) 1.18f else 1f, label = "scale")
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { tab = i },
                                            icon = { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.scale(scale)) },
                                            label = { Text(label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedTextColor = Gold, unselectedTextColor = TextSoft,
                                                indicatorColor = Crimson
                                            )
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            Box(Modifier.padding(padding)) {
                                when (tab) {
                                    0 -> TasksScreen(data, persona, settings, usage, firstDate, welcome) { welcome = null }
                                    1 -> PlanScreen(data, settings, persona)
                                    2 -> ReportsScreen(data, persona, settings, usage, tracker)
                                    3 -> ExperienceScreen(data, persona)
                                    else -> SettingsScreen(
                                        settings, usage,
                                        onFavorites = { screen = "favorites" },
                                        onMonitor = { screen = "monitor" },
                                        onNotifs = { screen = "notifs" }
                                    )
                                }
                            }
                        }
                        "monitor" -> {
                            BackHandler { screen = "main" }
                            MonitoringScreen(settings, usage) { screen = "main" }
                        }
                        "notifs" -> {
                            BackHandler { screen = "main" }
                            NotificationSettingsScreen(settings) { screen = "main" }
                        }
                        else -> {
                            BackHandler { screen = "main" }
                            FavoritesScreen(
                                favorites = favs.sorted().mapNotNull { WisdomBank.byId(it) },
                                onRemove = { favs = wisdomRepo.toggleFavorite(it) },
                                onBack = { screen = "main" }
                            )
                        }
                    }
                }
            }
        }
    }
}
