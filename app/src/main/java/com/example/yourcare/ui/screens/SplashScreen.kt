package com.example.yourcare.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yourcare.R
import com.example.yourcare.ui.theme.RippleTeal
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500)
        onNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // --- MIDDLE: "Tremor Line" Animation ---
        // This simulates a live tremor signal passing through the screen
        val infiniteTransition = rememberInfiniteTransition(label = "line")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2000f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "phase"
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TremorScan Pro",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = RippleTeal,
                    fontSize = 32.sp
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // The Tremor Graph Animation
            Canvas(modifier = Modifier.width(300.dp).height(100.dp)) {
                val path = Path()
                val width = size.width
                val height = size.height
                val midY = height / 2

                path.moveTo(0f, midY)

                // Draw a sine wave with "noise" to simulate tremor
                for (x in 0..width.toInt() step 5) {
                    val normalizedX = x / width
                    // High frequency jitter
                    val jitter = if (normalizedX > 0.2 && normalizedX < 0.8) {
                        (sin((x + phase) * 0.1f) * 40f) + (sin((x - phase) * 0.3f) * 20f)
                    } else {
                        0f // Flat line at edges
                    }
                    path.lineTo(x.toFloat(), midY + jitter)
                }

                drawPath(
                    path = path,
                    color = RippleTeal,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
        }

        // --- BOTTOM: Powered By Logo (Your Custom Code) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered by",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ripple Healthcare Logo
            // Make sure 'ripple_logo' exists in res/drawable
            Image(
                painter = painterResource(id = R.drawable.ripple_logo),
                contentDescription = "Ripple Healthcare",
                modifier = Modifier
                    .width(140.dp)
                    .height(60.dp)
            )
        }
    }
}