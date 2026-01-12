package com.example.yourcare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yourcare.ui.theme.RippleTeal

@Composable
fun GuideScreen(onStartTest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Instructions",
            style = MaterialTheme.typography.headlineMedium,
            color = RippleTeal
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Video Player Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap to Watch Tutorial", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Instruction Steps
        Column(modifier = Modifier.fillMaxWidth()) {
            StepItem(number = "1", text = "Sit comfortably and hold the phone in your dominant hand.")
            StepItem(number = "2", text = "Extend your arm fully in front of you.")
            StepItem(number = "3", text = "Try to keep your hand steady.")
            StepItem(number = "4", text = "Press Start. The test runs for 10 seconds.")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartTest,
            colors = ButtonDefaults.buttonColors(containerColor = RippleTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Ready to Start")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleMedium,
            color = RippleTeal,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}