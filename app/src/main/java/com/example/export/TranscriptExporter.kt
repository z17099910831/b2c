package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.RecordingEntity
import com.example.data.local.RecordingSerialization
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranscriptExporter(
    private val context: Context
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun buildPlainTextDocument(recording: RecordingEntity): String {
        val sourceLang = SupportedLanguage.fromCode(recording.sourceLanguageCode)
        val targetLang = SupportedLanguage.fromCode(recording.targetLanguageCode)
        val segments = RecordingSerialization.decodeSegments(recording.timestampsJson)
        val summary = RecordingSerialization.decodeSummary(recording.summaryJson)

        return buildString {
            appendLine("LinguaBridge AI — Offline Transcript Export")
            appendLine("==========================================")
            appendLine("Title: ${recording.title}")
            appendLine("Date: ${dateFormatter.format(Date(recording.createdAtMillis))}")
            appendLine("Duration: ${TranscriptSegment.formatMillis(recording.durationMillis)}")
            appendLine("Language Pair: ${sourceLang.displayName} → ${targetLang.displayName}")
            appendLine("Model Version: ${recording.modelVersion}")
            appendLine()

            if (segments.isNotEmpty()) {
                appendLine("--- Timestamped Bilingual Transcript ---")
                for (seg in segments) {
                    appendLine("[${seg.formattedTimestamp}] (${seg.sourceLanguage.shortTag}) ${seg.originalText}")
                    if (seg.translatedText.isNotBlank()) {
                        appendLine("        (${seg.targetLanguage.shortTag}) ${seg.translatedText}")
                    }
                    appendLine()
                }
            } else {
                appendLine("--- Original Transcript (${sourceLang.displayName}) ---")
                appendLine(recording.originalTranscript.ifBlank { "(No original transcript recorded)" })
                appendLine()
                appendLine("--- Translation (${targetLang.displayName}) ---")
                appendLine(recording.chineseTranslation.ifBlank { "(No translation text recorded)" })
                appendLine()
            }

            if (summary != null && !summary.isEmpty) {
                appendLine("--- Offline Extractive Summary ---")
                appendLine(summary.chineseSummary)
                if (summary.keywords.isNotEmpty()) {
                    appendLine("Keywords: ${summary.keywords.joinToString(", ")}")
                }
                if (summary.keyPoints.isNotEmpty()) {
                    appendLine("Key Points:")
                    summary.keyPoints.forEach { appendLine("• $it") }
                }
                if (summary.actionItems.isNotEmpty()) {
                    appendLine("Action Items:")
                    summary.actionItems.forEach { appendLine("• $it") }
                }
                if (summary.bibleReferences.isNotEmpty()) {
                    appendLine("Bible References:")
                    summary.bibleReferences.forEach { appendLine("• $it") }
                }
            }
        }
    }

    fun writeTxtToStream(recording: RecordingEntity, outputStream: OutputStream) {
        val text = buildPlainTextDocument(recording)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(text)
        }
    }

    fun writePdfToStream(recording: RecordingEntity, outputStream: OutputStream) {
        val contentText = buildPlainTextDocument(recording)
        val pdfDocument = PdfDocument()
        try {
            val pageWidth = 595 // A4 points
            val pageHeight = 842
            val margin = 44
            val printableWidth = pageWidth - margin * 2

            val titlePaint = TextPaint().apply {
                color = Color.parseColor("#0A1628")
                textSize = 16f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 11f
                typeface = Typeface.SANS_SERIF
                isAntiAlias = true
            }
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 9f
                isAntiAlias = true
            }

            val lines = contentText.lines()
            var pageNumber = 1
            var currentPage = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas: Canvas = currentPage.canvas
            var yPos = margin.toFloat()

            fun drawFooter(c: Canvas, page: Int) {
                c.drawText(
                    "LinguaBridge AI • Offline Local Export • Page $page",
                    margin.toFloat(),
                    (pageHeight - 22).toFloat(),
                    footerPaint
                )
            }

            for ((index, line) in lines.withIndex()) {
                val paint = if (index == 0) titlePaint else bodyPaint
                val displayLine = if (line.isEmpty()) " " else line
                val layout = StaticLayout.Builder.obtain(
                    displayLine,
                    0,
                    displayLine.length,
                    paint,
                    printableWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.15f)
                    .build()

                if (yPos + layout.height > pageHeight - margin - 24) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(currentPage)
                    pageNumber += 1
                    currentPage = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = currentPage.canvas
                    yPos = margin.toFloat()
                }

                canvas.save()
                canvas.translate(margin.toFloat(), yPos)
                layout.draw(canvas)
                canvas.restore()
                yPos += layout.height + 3f
            }

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            outputStream.use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
    }

    fun exportAndShareTxt(recording: RecordingEntity): Uri {
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = recording.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(32)
        val outFile = File(exportsDir, "${safeName}_${recording.id.take(6)}.txt")
        outFile.outputStream().use { writeTxtToStream(recording, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun exportAndSharePdf(recording: RecordingEntity): Uri {
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = recording.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(32)
        val outFile = File(exportsDir, "${safeName}_${recording.id.take(6)}.pdf")
        outFile.outputStream().use { writePdfToStream(recording, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun createShareIntent(uri: Uri, mimeType: String, chooserTitle: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
