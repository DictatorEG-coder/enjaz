package com.enjaz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enjaz.app.data.Wisdom
import com.enjaz.app.theme.*

@Composable
fun WisdomScreen(
    wisdom: Wisdom,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Ink).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("إنجاز", color = Gold, fontSize = 44.sp, fontFamily = FontFamily.Serif)
        Text("ENJAZ", color = Gold.copy(alpha = 0.6f), fontSize = 13.sp, letterSpacing = 6.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(64.dp).height(2.dp).background(Gold))
        Spacer(Modifier.height(36.dp))
        Surface(color = Surface1, shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    wisdom.text, color = TextSoft, fontSize = 24.sp, lineHeight = 40.sp,
                    fontFamily = FontFamily.Serif, textAlign = TextAlign.Center
                )
                wisdom.source?.let {
                    Spacer(Modifier.height(16.dp))
                    Text("[$it]", color = Gold, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onToggleFavorite) {
            Text(if (isFavorite) "محفوظة في المفضلة" else "احفظ في المفضلة", color = Gold)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEnter,
            colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Gold),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("ادخل", fontSize = 18.sp) }
    }
}
