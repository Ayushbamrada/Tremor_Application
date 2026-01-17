package com.example.yourcare.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.yourcare.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

// --- Data Model ---
data class TremorReportData(
    val patientName: String,
    val patientAge: String,
    val patientId: String,
    val doctorName: String? = null,
    val clinicName: String? = null,
    val testDate: String,
    val rmsScore: Float,
    val frequencyPeak: Float,
    val maxAmplitude: Float,
    val rawX: List<Float>,
    val rawY: List<Float>,
    val rawZ: List<Float>,
    val frequencySpectrum: List<Float>
)

// --- Renderer ---
class TremorReportRenderer(
    private val context: Context,
    private val doc: PdfDocument,
    private val data: TremorReportData
) {
    private var pageNumber = 0
    private lateinit var currentPage: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var yPos = 40f // Track vertical position

    // Constants
    private val PAGE_WIDTH = 595
    private val PAGE_HEIGHT = 842
    private val MARGIN = 40f
    private val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    // Brand Colors
    private val rippleTeal = Color.rgb(29, 143, 155)
    private val rippleDark = Color.rgb(16, 95, 104)
    private val accentOrange = Color.rgb(255, 127, 80)

    fun generate() {
        startNewPage()

        drawHeader()
        drawPatientInfo()
        drawClinicalSummary()

        checkSpace(180f)
        drawTimeDomainGraph()

        checkSpace(180f)
        drawFrequencyGraph()

        checkSpace(250f)
        drawCircularDeviation()

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
        if (yPos + height > PAGE_HEIGHT - 80) {
            drawFooter()
            startNewPage()
            drawHeader(isContinuation = true)
        }
    }

    // --- SECTIONS ---

    private fun drawHeader(isContinuation: Boolean = false) {
        if (isContinuation) {
            canvas.drawText("Tremor Analysis Report (Cont.)", MARGIN, yPos + 15, Paint().apply { textSize = 12f; color = Color.GRAY })
            yPos += 40f
            return
        }

        // 1. Logo (Top Right)
        val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ripple_logo)
        if (logoBmp != null) {
            val scaled = Bitmap.createScaledBitmap(logoBmp, 120, 50, true)
            canvas.drawBitmap(scaled, PAGE_WIDTH - MARGIN - 120, MARGIN, null)
        }

        // 2. Doctor Info (Top Left)
        val docPaint = Paint().apply { color = rippleDark; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        var headerH = 0f
        if (!data.doctorName.isNullOrEmpty()) {
            canvas.drawText("Dr. ${data.doctorName}", MARGIN, yPos + 15, docPaint)
            canvas.drawText(data.clinicName ?: "", MARGIN, yPos + 30, Paint().apply { textSize = 10f; color = Color.GRAY })
            headerH = 40f
        } else {
            // Default header if no doctor
            canvas.drawText("TremorScan Analysis", MARGIN, yPos + 15, docPaint)
            headerH = 30f
        }

        yPos += headerH + 20f

        // 3. Title Bar
        val titleRect = RectF(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 30f)
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, PAGE_WIDTH.toFloat(), 0f, intArrayOf(rippleTeal, rippleDark), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(titleRect, 5f, 5f, bgPaint)
        canvas.drawText("REPORT SUMMARY", MARGIN + 10, yPos + 20, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.DEFAULT_BOLD })

        yPos += 50f
    }

    private fun drawPatientInfo() {
        val labelP = Paint().apply { textSize = 9f; color = Color.GRAY }
        val valP = Paint().apply { textSize = 11f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD }

        val rowY = yPos
        // Name
        canvas.drawText("PATIENT NAME", MARGIN, rowY, labelP)
        canvas.drawText(data.patientName, MARGIN, rowY + 15, valP)

        // Age/ID
        canvas.drawText("AGE / ID", MARGIN + 200, rowY, labelP)
        canvas.drawText("${data.patientAge} / ${data.patientId}", MARGIN + 200, rowY + 15, valP)

        // Date
        canvas.drawText("DATE", MARGIN + 350, rowY, labelP)
        canvas.drawText(data.testDate, MARGIN + 350, rowY + 15, valP)

        yPos += 30f
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, Paint().apply { color = Color.LTGRAY })
        yPos += 20f
    }

    private fun drawClinicalSummary() {
        drawSectionTitle("Clinical Metrics")

        val width = (CONTENT_WIDTH / 3) - 10
        val height = 50f

        drawCard(MARGIN, yPos, width, height, "Intensity (RMS)", "%.2f rad/s".format(data.rmsScore))
        drawCard(MARGIN + width + 10, yPos, width, height, "Frequency", "%.1f Hz".format(data.frequencyPeak))
        drawCard(MARGIN + (width + 10)*2, yPos, width, height, "Max Peak", "%.2f rad/s".format(data.maxAmplitude))

        yPos += height + 30f
    }

    private fun drawTimeDomainGraph() {
        drawSectionTitle("Motion History (Time Domain)")
        val height = 120f
        val bottom = yPos + height

        // --- CLIP RECT (Prevents drawing outside) ---
        canvas.save()
        val rect = RectF(MARGIN, yPos, PAGE_WIDTH - MARGIN, bottom)
        canvas.clipRect(rect)

        // Background
        canvas.drawRect(rect, Paint().apply { color = Color.argb(10, 29, 143, 155) })

        // Draw Lines
        if (data.rawX.isNotEmpty()) {
            val stepX = CONTENT_WIDTH / data.rawX.size
            drawSmoothPath(data.rawX, stepX, yPos, height, Color.RED)
            drawSmoothPath(data.rawY, stepX, yPos, height, rippleTeal)
            drawSmoothPath(data.rawZ, stepX, yPos, height, Color.BLUE)
        }
        canvas.restore() // End Clipping

        // Border
        canvas.drawRect(rect, Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY })

        yPos = bottom + 25f
    }

    private fun drawSmoothPath(points: List<Float>, stepX: Float, topY: Float, height: Float, colorInt: Int) {
        val path = Path()
        val paint = Paint().apply { color = colorInt; style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }

        val midY = topY + height / 2
        val scaleY = (height / 2) / 6.0f // Scale factor

        points.forEachIndexed { i, value ->
            val x = MARGIN + (i * stepX)
            val y = (midY - (value * scaleY))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawFrequencyGraph() {
        drawSectionTitle("Frequency Spectrum")
        val height = 80f
        val bottom = yPos + height

        // Axis
        canvas.drawLine(MARGIN, bottom, PAGE_WIDTH - MARGIN, bottom, Paint().apply { color = Color.GRAY })

        val barW = CONTENT_WIDTH / 12f
        val maxVal = data.frequencySpectrum.maxOrNull() ?: 1f

        for (i in 0 until 12) {
            val mag = if(i < data.frequencySpectrum.size) data.frequencySpectrum[i] else 0f
            val barH = (mag / maxVal) * (height - 10)
            val left = MARGIN + (i * barW) + 5
            val right = left + barW - 5
            val top = bottom - barH

            val p = Paint().apply { color = if(i in 4..6) accentOrange else rippleTeal }
            canvas.drawRect(left, top, right, bottom, p)
            canvas.drawText("${i}Hz", left, bottom + 12, Paint().apply { textSize = 8f })
        }
        yPos = bottom + 30f
    }

    private fun drawCircularDeviation() {
        drawSectionTitle("Tremor Scatter Plot")
        val size = 160f
        val cx = PAGE_WIDTH / 2f
        val cy = yPos + size / 2

        // Background Targets
        val paint = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY }
        canvas.drawCircle(cx, cy, size/2, paint)
        canvas.drawCircle(cx, cy, size/4, paint)
        canvas.drawLine(cx - size/2, cy, cx + size/2, cy, paint)
        canvas.drawLine(cx, cy - size/2, cx, cy + size/2, paint)

        // --- CLIP CIRCLE ---
        canvas.save()
        val path = Path().apply { addCircle(cx, cy, size/2, Path.Direction.CW) }
        canvas.clipPath(path)

        // Points
        val dotP = Paint().apply { color = rippleTeal; alpha = 150; style = Paint.Style.FILL }
        val scale = size / 10f
        val step = max(1, data.rawX.size / 200)

        for (i in data.rawX.indices step step) {
            val x = cx + (data.rawX[i] * scale)
            val y = cy + (data.rawY[i] * scale)
            canvas.drawCircle(x, y, 2f, dotP)
        }
        canvas.restore() // End Clipping

        yPos = cy + size/2 + 40f
    }

    private fun drawFooter() {
        val y = PAGE_HEIGHT - 40f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = rippleTeal; strokeWidth = 2f })
        canvas.drawText("Powered by Ripple Healthcare", PAGE_WIDTH - MARGIN, y + 15, Paint().apply { textAlign = Paint.Align.RIGHT; textSize = 9f; color = Color.GRAY })
    }

    private fun drawSectionTitle(title: String) {
        canvas.drawText(title, MARGIN, yPos, Paint().apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; color = rippleDark })
        yPos += 20f
    }

    private fun drawCard(x: Float, y: Float, w: Float, h: Float, label: String, value: String) {
        val r = RectF(x, y, x+w, y+h)
        val p = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY }
        canvas.drawRoundRect(r, 5f, 5f, p)
        canvas.drawText(label, x+5, y+15, Paint().apply { textSize=9f; color=Color.GRAY })
        canvas.drawText(value, x+5, y+35, Paint().apply { textSize=14f; color=rippleTeal; typeface=Typeface.DEFAULT_BOLD })
    }
}

fun generateTremorPdf(context: Context, data: TremorReportData): Uri? {
    val pdfDocument = PdfDocument()
    val renderer = TremorReportRenderer(context, pdfDocument, data)
    try {
        renderer.generate()
        val file = File(context.cacheDir, "TremorReport_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        pdfDocument.close()
    }
}