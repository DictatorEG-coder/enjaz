package com.enjaz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.Wisdom
import com.enjaz.app.theme.*

@Composable
fun FavoritesScreen(
    favorites: List<Wisdom>,
    onRemove: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink).padding(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("الحكم المحفوظة", color = Gold, fontSize = 26.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f)) {
            if (favorites.isEmpty()) {
                Text("لا توجد حكم محفوظة بعد.", color = TextSoft, fontSize = 16.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites, key = { it.id }) { w ->
                        Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(w.text, color = TextSoft, fontSize = 18.sp, lineHeight = 30.sp, fontFamily = FontFamily.Serif)
                                w.source?.let { Text("[$it]", color = Gold, fontSize = 14.sp) }
                                TextButton(onClick = { onRemove(w.id) }) { Text("إزالة", color = Gold) }
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("رجوع") }
    }
}
