package com.example.yourcare.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yourcare.ui.theme.GradientButton
import com.example.yourcare.ui.theme.RippleTeal
import com.example.yourcare.ui.viewmodel.TestViewModel
import com.example.yourcare.utils.TremorReportData
import com.example.yourcare.utils.generateTremorPdf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    avg: Float,
    max: Float,
    min: Float,
    freq: Float,
    viewModel: TestViewModel = viewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    // --- Dialog State ---
    var showDialog by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { visible = true }

    val severity = when {
        avg < 0.2 -> "Normal"
        avg < 1.0 -> "Mild"
        avg < 2.0 -> "Moderate"
        else -> "Severe"
    }

    val typeAnalysis = when {
        avg < 0.2 -> "No significant tremor."
        freq in 3.5..6.5 -> "Resting Tremor (Parkinsonian)"
        freq in 6.6..12.0 -> "Essential / Action Tremor"
        else -> "Indeterminate Frequency"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Tremor Analysis",
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
                    ResultRow("Intensity (RMS)", "%.2f rad/s".format(avg))
                    Divider(Modifier.padding(vertical = 12.dp))

                    ResultRow("Frequency", "%.1f Hz".format(freq))
                    Divider(Modifier.padding(vertical = 12.dp))

                    Text("Interpretation:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = typeAnalysis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(typeAnalysis.contains("Parkinson")) Color.Red else RippleTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- PDF BUTTON (Opens Dialog) ---
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Professional Report")
        }

        Spacer(modifier = Modifier.height(16.dp))

        GradientButton(text = "Back to Home", onClick = onHome)

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- Doctor Details Dialog ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter Professional Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Doctor's Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = clinicName,
                        onValueChange = { clinicName = it },
                        label = { Text("Clinic / Hospital Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        generateAndShare(context, viewModel, avg, max, freq, doctorName, clinicName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RippleTeal)
                ) {
                    Text("Generate PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun generateAndShare(
    context: android.content.Context,
    viewModel: TestViewModel,
    avg: Float,
    max: Float,
    freq: Float,
    doctorName: String,
    clinicName: String
) {
    try {
        // Check if we have data
        if (viewModel.rawX.isEmpty()) {
            Toast.makeText(context, "No graph data available", Toast.LENGTH_SHORT).show()
            return
        }

        val reportData = TremorReportData(
            patientName = "Guest Patient",
            patientAge = "N/A",
            patientId = UUID.randomUUID().toString().take(6).uppercase(),
            // Use the inputs from the dialog
            doctorName = doctorName.ifEmpty { "Dr. Ripple AI" },
            clinicName = clinicName.ifEmpty { "Ripple Healthcare Dept." },
            testDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
            rmsScore = avg,
            frequencyPeak = freq,
            maxAmplitude = max,
            rawX = viewModel.rawX,
            rawY = viewModel.rawY,
            rawZ = viewModel.rawZ,
            frequencySpectrum = listOf(0.1f, 0.4f, 0.9f, 0.3f)
        )

        val pdfUri = generateTremorPdf(context, reportData)

        if (pdfUri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Report via"))
        } else {
            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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