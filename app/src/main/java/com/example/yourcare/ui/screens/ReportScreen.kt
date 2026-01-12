package com.example.yourcare.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yourcare.ui.theme.GradientButton
import com.example.yourcare.ui.theme.RippleTeal
import com.example.yourcare.utils.PdfHelper

@Composable
fun ReportScreen(
    avg: Float,
    max: Float,
    min: Float,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    // Trigger animation on load
    LaunchedEffect(Unit) { visible = true }

    val status = when {
        avg < 0.2 -> "Normal / Steady"
        avg < 1.5 -> "Mild Tremor"
        else -> "Significant Tremor"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Analysis Result",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RippleTeal
        )

        Spacer(modifier = Modifier.height(30.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    ResultRow("Average (RMS)", "%.3f rad/s".format(avg))
                    Divider(Modifier.padding(vertical = 12.dp))
                    ResultRow("Peak Intensity", "%.3f rad/s".format(max))
                    Divider(Modifier.padding(vertical = 12.dp))
                    ResultRow("Minimum", "%.3f rad/s".format(min))

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Assessment:", color = Color.Gray)
                    Text(
                        text = status,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if(avg < 1.5) RippleTeal else Color.Red
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // PDF Share Button
        Button(
            onClick = { PdfHelper.generateAndSharePdf(context, avg, max, min, status) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Report (PDF)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        GradientButton(
            text = "Back to Home",
            onClick = onHome
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RippleTeal)
    }
}