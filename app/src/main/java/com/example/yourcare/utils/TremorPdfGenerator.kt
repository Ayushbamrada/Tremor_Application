package com.example.yourcare.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.yourcare.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

// --- 1. Data Models ---

data class TremorReportData(
    val patientName: String,
    val patientAge: String,
    val patientId: String,
    val doctorName: String? = null, // Optional Doctor Name
    val clinicName: String? = null, // Optional Clinic Name
    val testDate: String,
    // Metrics
    val rmsScore: Float, // rad/s
    val frequencyPeak: Float, // Hz
    val maxAmplitude: Float,
    // Raw Data for Graphs (History)
    val rawX: List<Float>,
    val rawY: List<Float>,
    val rawZ: List<Float>,
    val frequencySpectrum: List<Float> // Frequencies for FFT Graph
)

// --- 2. The Renderer Class ---

private const val PAGE_WIDTH = 595 // A4 Width
private const val PAGE_HEIGHT = 842 // A4 Height
private const val MARGIN = 40f
private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

class TremorReportRenderer(
    private val context: Context,
    private val doc: PdfDocument,
    private val data: TremorReportData
) {
    private var pageNumber = 0
    private lateinit var currentPage: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var yPos = MARGIN

    // --- Colors & Gradients ---
    private val rippleTeal = Color.rgb(29, 143, 155) // #1D8F9B
    private val rippleDark = Color.rgb(16, 95, 104)
    private val accentOrange = Color.rgb(255, 127, 80)

    // Gradient Paint for Header
    private val headerGradient = LinearGradient(
        0f, 0f, PAGE_WIDTH.toFloat(), 0f,
        intArrayOf(rippleTeal, rippleDark),
        null, Shader.TileMode.CLAMP
    )

    fun generate() {
        startNewPage()

        // 1. Doctor / Clinic Letterhead
        drawHeader()

        // 2. Patient Demographics
        drawPatientInfo()

        // 3. Clinical Summary (RMS & Freq)
        drawClinicalSummary()

        // 4. Time Domain Graph (Smooth Waves)
        checkSpace(200f)
        drawTimeDomainGraph()

        // 5. Frequency Graph (FFT)
        checkSpace(200f)
        drawFrequencyGraph()

        // 6. Circular Deviation Plot (Scatter)
        checkSpace(250f)
        drawCircularDeviation()

        // 7. Footer
        drawFooter()

        doc.finishPage(currentPage)
    }

    private fun startNewPage() {
        if (pageNumber > 0) doc.finishPage(currentPage)
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = doc.startPage(pageInfo)
        canvas = currentPage.canvas
        yPos = MARGIN
    }

    private fun checkSpace(height: Float) {
        if (yPos + height > PAGE_HEIGHT - 100) { // 100 padding for footer
            drawFooter() // Draw footer on old page
            startNewPage()
            drawHeader(isContinuation = true) // Simple header for next page
        }
    }

    // --- Drawing Sections ---

    private fun drawHeader(isContinuation: Boolean = false) {
        if (isContinuation) {
            val paint = Paint().apply { textSize = 12f; color = Color.GRAY }
            canvas.drawText("Tremor Analysis Report (Cont.)", MARGIN, yPos + 10, paint)
            yPos += 40f
            return
        }

        // 1. Doctor/Clinic Name (Letterpad Style)
        if (!data.doctorName.isNullOrEmpty() || !data.clinicName.isNullOrEmpty()) {
            val docPaint = Paint().apply {
                color = rippleDark
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val clinicPaint = Paint().apply {
                color = Color.GRAY
                textSize = 12f
                textAlign = Paint.Align.RIGHT
            }

            val headerX = PAGE_WIDTH - MARGIN
            if (data.doctorName != null) {
                canvas.drawText("Dr. ${data.doctorName}", headerX, yPos + 10, docPaint)
            }
            if (data.clinicName != null) {
                canvas.drawText(data.clinicName, headerX, yPos + 30, clinicPaint)
            }

            // Draw a separator line
            val linePaint = Paint().apply { color = rippleTeal; strokeWidth = 2f }
            canvas.drawLine(MARGIN, yPos + 45, PAGE_WIDTH - MARGIN, yPos + 45, linePaint)
            yPos += 60f
        }

        // 2. Report Title with Gradient Background
        val titleRect = RectF(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 40f)
        val bgPaint = Paint().apply { shader = headerGradient }

        // Rounded Rect for Title
        canvas.drawRoundRect(titleRect, 10f, 10f, bgPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PARKINSON'S TREMOR ANALYSIS", MARGIN + 20f, yPos + 26f, textPaint)

        // Logo (Assuming resource exists, else skip)
        val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ripple_logo)
        if (logoBmp != null) {
            val scaled = Bitmap.createScaledBitmap(logoBmp, 100, 40, true)
            canvas.drawBitmap(scaled, PAGE_WIDTH - MARGIN - 110, yPos, null)
        }

        yPos += 60f
    }

    private fun drawPatientInfo() {
        val labelPaint = Paint().apply { textSize = 10f; color = Color.GRAY; isAntiAlias = true }
        val valuePaint = Paint().apply { textSize = 12f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

        val col1 = MARGIN
        val col2 = MARGIN + 200f
        val col3 = MARGIN + 350f

        canvas.drawText("PATIENT NAME", col1, yPos, labelPaint)
        canvas.drawText(data.patientName, col1, yPos + 15, valuePaint)

        canvas.drawText("AGE / ID", col2, yPos, labelPaint)
        canvas.drawText("${data.patientAge} / ${data.patientId}", col2, yPos + 15, valuePaint)

        canvas.drawText("DATE", col3, yPos, labelPaint)
        canvas.drawText(data.testDate, col3, yPos + 15, valuePaint)

        yPos += 40f
        // Divider
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, Paint().apply { color = Color.LTGRAY })
        yPos += 20f
    }

    private fun drawClinicalSummary() {
        drawSectionTitle("Clinical Metrics")

        // Draw 3 Gradient Cards
        val cardWidth = CONTENT_WIDTH / 3 - 10
        val cardHeight = 60f

        val rmsStatus = if(data.rmsScore < 0.2) "Normal" else if(data.rmsScore < 1.0) "Mild" else "Severe"
        val freqStatus = if(data.frequencyPeak in 4.0..6.5) "Parkinsonian" else if(data.frequencyPeak > 6.5) "Essential" else "Indeterminate"

        drawMetricCard(MARGIN, yPos, cardWidth, "Tremor Intensity (RMS)", "%.2f rad/s".format(data.rmsScore), rmsStatus)
        drawMetricCard(MARGIN + cardWidth + 15, yPos, cardWidth, "Dominant Frequency", "%.1f Hz".format(data.frequencyPeak), freqStatus)
        drawMetricCard(MARGIN + (cardWidth + 15)*2, yPos, cardWidth, "Max Amplitude", "%.2f rad/s".format(data.maxAmplitude), "")

        yPos += 90f
    }

    private fun drawTimeDomainGraph() {
        drawSectionTitle("Motion Analysis (Time Domain)")

        val graphHeight = 120f
        val bottom = yPos + graphHeight

        // Background Box
        val bgPaint = Paint().apply { color = Color.argb(10, 29, 143, 155); style = Paint.Style.FILL }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, bottom, bgPaint)

        // Axes
        val axisPaint = Paint().apply { color = Color.GRAY; strokeWidth = 1f }
        canvas.drawLine(MARGIN, yPos, MARGIN, bottom, axisPaint) // Y
        canvas.drawLine(MARGIN, bottom/2 + yPos/2, PAGE_WIDTH-MARGIN, bottom/2 + yPos/2, axisPaint) // X (Center)

        if (data.rawX.isNotEmpty()) {
            val stepX = CONTENT_WIDTH / data.rawX.size

            // Draw smooth curves
            drawSmoothPath(data.rawX, stepX, yPos, graphHeight, Color.RED)
            drawSmoothPath(data.rawY, stepX, yPos, graphHeight, Color.parseColor("#1D8F9B")) // Teal
            drawSmoothPath(data.rawZ, stepX, yPos, graphHeight, Color.BLUE)
        }

        // Legend
        val legendY = bottom + 15f
        val textP = Paint().apply { textSize = 8f }
        canvas.drawText("X-Axis (Red)", MARGIN, legendY, textP)
        canvas.drawText("Y-Axis (Teal)", MARGIN + 60, legendY, textP)
        canvas.drawText("Z-Axis (Blue)", MARGIN + 120, legendY, textP)

        yPos = legendY + 30f
    }

    private fun drawSmoothPath(points: List<Float>, stepX: Float, topY: Float, height: Float, colorInt: Int) {
        val path = Path()
        val paint = Paint().apply {
            color = colorInt
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val midY = topY + height / 2
        // Scale factor: assuming max value roughly 5.0 rad/s
        val scaleY = (height / 2) / 5.0f

        points.forEachIndexed { i, value ->
            val x = MARGIN + (i * stepX)
            val y = midY - (value * scaleY)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawFrequencyGraph() {
        drawSectionTitle("Frequency Spectrum Analysis")

        val graphHeight = 100f
        val bottom = yPos + graphHeight

        // Draw Frequency Bars
        val barWidth = CONTENT_WIDTH / 12f // 0 to 12 Hz
        val maxFreqMag = data.frequencySpectrum.maxOrNull() ?: 1f

        for (i in 0 until 12) {
            val mag = if (i < data.frequencySpectrum.size) data.frequencySpectrum[i] else 0f
            val barHeight = (mag / maxFreqMag) * (graphHeight - 20)

            val left = MARGIN + (i * barWidth) + 5
            val right = left + barWidth - 5
            val top = bottom - barHeight

            // Color Logic: 4-6Hz is Parkinson's Range (Red), others Teal
            val barColor = if (i in 4..6) accentOrange else rippleTeal
            val barPaint = Paint().apply { color = barColor }

            canvas.drawRect(left, top, right, bottom, barPaint)

            // Label
            val textP = Paint().apply { textSize = 8f; textAlign = Paint.Align.CENTER }
            canvas.drawText("${i}Hz", left + (barWidth/2) - 5, bottom + 10, textP)
        }

        yPos = bottom + 30f
    }

    private fun drawCircularDeviation() {
        drawSectionTitle("Tremor Path (Scatter Plot)")

        val size = 150f
        val cx = PAGE_WIDTH / 2f
        val cy = yPos + size / 2 + 10

        // Draw Targets
        val targetPaint = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY }
        canvas.drawCircle(cx, cy, size/2, targetPaint) // Outer
        canvas.drawCircle(cx, cy, size/4, targetPaint) // Inner

        // Crosshair
        canvas.drawLine(cx - size/2, cy, cx + size/2, cy, targetPaint)
        canvas.drawLine(cx, cy - size/2, cx, cy + size/2, targetPaint)

        // Plot Points (X vs Y deviation)
        val pointPaint = Paint().apply { color = Color.argb(100, 29, 143, 155); strokeWidth = 4f }
        val scale = size / 10f // Scale logic

        // Limit points to keep PDF light
        val limit = 200
        val skip = max(1, data.rawX.size / limit)

        for (i in data.rawX.indices step skip) {
            val x = cx + (data.rawX[i] * scale)
            val y = cy + (data.rawY[i] * scale)
            // Clamp
            if (x > cx - size && x < cx + size && y > cy - size && y < cy + size) {
                canvas.drawPoint(x, y, pointPaint)
            }
        }

        canvas.drawText("Center Stability Visualization", cx, cy + size/2 + 15, Paint().apply { textAlign = Paint.Align.CENTER; textSize=10f })

        yPos = cy + size/2 + 40f
    }

    private fun drawFooter() {
        val footerY = PAGE_HEIGHT - 60f

        // Footer Line
        val linePaint = Paint().apply { color = rippleTeal; strokeWidth = 3f }
        canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, linePaint)

        // Powered By
        val textPaint = Paint().apply { textSize = 9f; color = Color.GRAY }
        canvas.drawText("Generated by TremorScan Pro", MARGIN, footerY + 15, textPaint)

        val rightPaint = Paint().apply { textSize = 9f; color = rippleDark; textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText("Powered by Ripple Healthcare", PAGE_WIDTH - MARGIN, footerY + 15, rightPaint)

        val contactPaint = Paint().apply { textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.RIGHT }
        canvas.drawText("www.ripplehealthcare.in | info@ripplehealthcare.in", PAGE_WIDTH - MARGIN, footerY + 28, contactPaint)
    }

    // Helpers
    private fun drawSectionTitle(title: String) {
        val paint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = rippleDark
        }
        canvas.drawText(title, MARGIN, yPos, paint)
        yPos += 20f
    }

    private fun drawMetricCard(x: Float, y: Float, width: Float, title: String, value: String, subtitle: String) {
        val rect = RectF(x, y, x + width, y + 60f)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.LTGRAY
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        val titleP = Paint().apply { textSize = 9f; color = Color.GRAY }
        canvas.drawText(title, x + 10, y + 20, titleP)

        val valP = Paint().apply { textSize = 16f; color = rippleTeal; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(value, x + 10, y + 40, valP)

        if(subtitle.isNotEmpty()) {
            val subP = Paint().apply { textSize = 10f; color = if(subtitle.contains("Parkin") || subtitle == "Severe") Color.RED else Color.rgb(0, 150, 0); textAlign = Paint.Align.RIGHT }
            canvas.drawText(subtitle, x + width - 10, y + 40, subP)
        }
    }
}

// --- 3. Public Entry Point ---

fun generateTremorPdf(context: Context, data: TremorReportData): Uri? {
    val pdfDocument = PdfDocument()
    val renderer = TremorReportRenderer(context, pdfDocument, data)

    try {
        renderer.generate()

        // Save Logic
        val fileName = "TremorReport_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { pdfDocument.writeTo(it) }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        pdfDocument.close()
    }
}