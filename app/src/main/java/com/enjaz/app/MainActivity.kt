package com.enjaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.enjaz.app.data.*
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
        val tabs = listOf("المهام", "الخطة", "التقارير", "التجارب", "الإعدادات")

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
                                    tabs.forEachIndexed { i, label ->
                                        NavigationBarItem(
                                            selected = tab == i,
                                            onClick = { tab = i },
                                            icon = {
                                                Box(Modifier.size(6.dp).background(if (tab == i) Gold else TextSoft, CircleShape))
                                            },
                                            label = { Text(label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedTextColor = Gold, unselectedTextColor = TextSoft,
                                                selectedIconColor = Gold, unselectedIconColor = TextSoft,
                                                indicatorColor = Crimson
                                            )
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            Box(Modifier.padding(padding)) {
                                when (tab) {
                                    0 -> TasksScreen(data, persona, settings, usage, welcome) { welcome = null }
                                    1 -> PlanScreen(data, settings, persona)
                                    2 -> ReportsScreen(data, persona, settings, usage, tracker)
                                    3 -> ExperienceScreen(data, persona)
                                    else -> SettingsScreen(settings, usage) { screen = "favorites" }
                                }
                            }
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
