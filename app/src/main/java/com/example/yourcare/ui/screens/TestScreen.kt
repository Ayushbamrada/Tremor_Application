package com.example.yourcare.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yourcare.ui.theme.GradientButton
import com.example.yourcare.ui.theme.RippleTeal
import com.example.yourcare.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun TestScreen(
    viewModel: TestViewModel = viewModel(),
    onTestComplete: (Float, Float, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val data by viewModel.currentData.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    // History for the line graph
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(data.magnitude) {
        if (isTesting) {
            history.add(data.magnitude)
            if (history.size > 150) history.removeAt(0)
        }
    }

    // Timer Logic
    var timeLeft by remember { mutableIntStateOf(10) }
    LaunchedEffect(isTesting) {
        if (isTesting) {
            history.clear()
            timeLeft = 10
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            val metrics = viewModel.stopTest()
            onTestComplete(metrics.averageRms, metrics.peakAmplitude, metrics.minAmplitude, metrics.frequency)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isTesting) "Measuring Tremor..." else "Press Start to Begin",
            style = MaterialTheme.typography.headlineSmall,
            color = RippleTeal,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 1. LIVE GRAPH ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFF5F9FA), RoundedCornerShape(12.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val width = size.width
                val height = size.height
                val maxExpectedVal = 6f

                drawLine(Color.LightGray, Offset(0f, height/2), Offset(width, height/2))

                if (history.isNotEmpty()) {
                    val path = Path()
                    val stepX = width / 150f

                    history.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - ((value / maxExpectedVal) * height).coerceAtMost(height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = RippleTeal,
                        style = Stroke(width = 4f)
                    )
                }
            }
            Text("Live Signal", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 2. VIBRATION CIRCLE (CONSTRAINED) ---
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Static Boundary Ring
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(color = Color.LightGray.copy(alpha=0.5f), style = Stroke(width = 4f))
                drawLine(Color.LightGray, Offset(center.x - 20, center.y), Offset(center.x + 20, center.y))
                drawLine(Color.LightGray, Offset(center.x, center.y - 20), Offset(center.x, center.y + 20))
            }

            // Calculation for Clamping
            val maxRadius = 80f

            // Raw offsets based on sensor
            val rawX = if(isTesting) data.x * 30f else 0f
            val rawY = if(isTesting) data.y * 30f else 0f

            // Calculate distance from center
            val distance = sqrt(rawX * rawX + rawY * rawY)

            // Logic: Is it hitting the wall? (FIXED SYNTAX HERE)
            val hittingWall = distance > maxRadius

            // Clamp the values to stay inside the circle
            val scale = if (hittingWall) maxRadius / distance else 1f
            val clampedX = rawX * scale
            val clampedY = rawY * scale

            // Visual Feedback: Turn RED if hitting the wall
            val ballColor = if (hittingWall) Color.Red else RippleTeal

            Box(
                modifier = Modifier
                    .offset(x = clampedX.dp, y = clampedY.dp)
                    .size(40.dp)
                    .background(ballColor, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Text(
            text = "Hold steady in the center",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isTesting) {
            Text(
                text = "$timeLeft",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = RippleTeal
            )
        } else {
            GradientButton(
                text = "START TEST",
                onClick = { viewModel.startTest(context) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}