package best.app.offlinehisab.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import best.app.offlinehisab.data.db.Customer
import best.app.offlinehisab.data.db.TxnType
import best.app.offlinehisab.ui.screens.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

// Simple extension used earlier
fun String.toColorInt(): Int = Color.parseColor(this)

// -----------------------------
// Full PDF generator function
// -----------------------------

@RequiresApi(Build.VERSION_CODES.O)
suspend fun generateCustomerPdfAndSaveModernUpdated(
    context: Context,
    customer: Customer,
    previousBalance: Double? = null,
    recentBalance: Double? = null,
    txns: List<Transaction>,
    filterOptionType: FilterOptionType,
    totals: Pair<Double, Double>, // (totalGot = credit, totalGave = debit)
    filename: String = "Hisab_${customer.name}_${System.currentTimeMillis()}.pdf",
): android.net.Uri? = withContext(Dispatchers.IO) {
    return@withContext try {
        // Page setup (A4-ish)
        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 36f
        val usableWidth = pageWidth - margin * 2

        val pdf = PdfDocument()

        // Paints
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.BLACK
        }
        val headerPaint =
            Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        val normalPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val smallPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY }
        val dividerPaint = Paint().apply { strokeWidth = 0.9f; color = Color.LTGRAY }
        val totalsLabelPaint =
            Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        val totalsValuePaint =
            Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK }

        val noteTextPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = normalPaint.textSize
            color = Color.BLACK
        }

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val downloadedDateStr = dateFormatter.format(Date())

        // Helpers
        fun drawTextWrapped(
            canvas: Canvas,
            text: String,
            x: Float,
            startY: Float,
            paint: TextPaint,
            maxWidth: Float,
            spacingAdd: Float = 0f,
        ): Float {
            if (text.isBlank()) return startY
            val staticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(spacingAdd, 1.0f)
                    .setIncludePad(false)
                    .build()
            canvas.save()
            canvas.translate(x, startY)
            staticLayout.draw(canvas)
            canvas.restore()
            return startY + staticLayout.height
        }

        // Right aligned text in a fixed column; scales down if overflow
        fun drawRightAlignedTextInColumn(
            canvas: Canvas,
            paint: Paint,
            text: String,
            colLeft: Float,
            colWidth: Float,
            baselineY: Float,
            padding: Float = 4f,
        ) {
            val origSize = paint.textSize
            var textWidth = paint.measureText(text)
            val maxAllowed = colWidth - padding * 2f
            /*if (textWidth > maxAllowed && text.isNotEmpty()) {
                val scale = maxAllowed / textWidth
                paint.textSize = origSize * scale
                textWidth = paint.measureText(text)
            }*/
            val x = colLeft + (colWidth - textWidth) - padding
            canvas.drawText(text, x, baselineY, paint)
            paint.textSize = origSize
        }

        // Page header drawing
        fun drawPageHeader(
            canvas: Canvas,
            pageNo: Int,
            leftX: Float,
            rightX: Float,
            usableWidth: Float,
        ) {
            var y = margin
            // Title centered
            val title = customer.name + " Statement"
            val titleX = leftX + (usableWidth - titlePaint.measureText(title)) / 2f
            canvas.drawText(title, titleX, y + titlePaint.textSize, titlePaint)
            y += titlePaint.textSize + 4f

            val generateReportRange = generateReportRangeTitle(filterOptionType)
            val reportRang =
                leftX + (usableWidth - headerPaint.measureText(generateReportRange)) / 2f
            canvas.drawText(generateReportRange, reportRang, y + headerPaint.textSize, headerPaint)

            val phone = "Phone: ${customer.phone ?: "-"}"
            canvas.drawText(
                phone,
                leftX,
                y + headerPaint.textSize + normalPaint.textSize + 4f,
                normalPaint
            )

            val dateText = "Report Generated: $downloadedDateStr"
            val dateW = smallPaint.measureText(dateText)
            canvas.drawText(dateText, rightX - dateW, margin, smallPaint)

            val divY = margin + titlePaint.textSize + 40f
            canvas.drawLine(leftX, divY, rightX, divY, dividerPaint)
        }

        // Column sizes
        val headerBlockHeight = 70f
        val rowMinHeight = 24f
        val noteLineHeight = normalPaint.textSize + 6f

        val colSnoWidth = 36f
        val colDateWidth = 120f
        val colReceivedWidth = 80f
        val colPaidWidth = 80f
        val colBalanceWidth = 100f
        val gap = 8f
        val colNoteWidth =
            usableWidth - (colSnoWidth + colDateWidth + colReceivedWidth + colPaidWidth + colBalanceWidth) - gap * 4

        // Precompute column left positions
        val leftX = margin
        val rightX = pageWidth - margin + 7

        val snoColLeft = leftX
        val dateColLeft = snoColLeft + colSnoWidth + gap
        val noteColLeft = dateColLeft + colDateWidth + gap
        val receivedColLeft = noteColLeft + colNoteWidth + gap
        val paidColLeft = receivedColLeft + colReceivedWidth + gap
        val balanceColLeft = paidColLeft + colPaidWidth + gap
        val tableRightEdge = balanceColLeft + colBalanceWidth

        // Function to draw vertical column lines for the table area (from topY to bottomY)
        fun drawColumnLines(canvas: Canvas, topY: Float, bottomY: Float) {
            val xs = listOf(
                snoColLeft,
                dateColLeft,
                noteColLeft,
                receivedColLeft,
                paidColLeft,
                balanceColLeft,
                tableRightEdge
            )
            xs.forEach { x ->
                if (x == snoColLeft || x == tableRightEdge) {
                    if(previousBalance != null) {
                        canvas.drawLine(x, topY - 82, x, bottomY, dividerPaint)
                    } else {
                        canvas.drawLine(x, topY - 40, x, bottomY, dividerPaint)
                    }
                } else {
                    canvas.drawLine(x, topY - 7, x, bottomY, dividerPaint)
                }
            }
        }

        // Begin first page
        var pageIndex = 1
        var pageInfo =
            PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        drawPageHeader(canvas, pageIndex, leftX, rightX, usableWidth)
        var y = margin + headerBlockHeight

        // Totals block
        val (totalGot, totalGave) = totals
        val remaining = totalGot - totalGave
        val remainingLabel = if (remaining >= 0.0) "You will Pay" else "You will Receive"
        val remainingColorInt =
            if (remaining >= 0.0) "#2E7D32".toColorInt() else "#C62828".toColorInt()

        val totalsStartY = y + 6f
        canvas.drawText(
            "Total Received:",
            leftX,
            totalsStartY + totalsLabelPaint.textSize,
            totalsLabelPaint
        )
        val totalGotStr = "₹${"%.2f".format(totalGot)}"
        drawRightAlignedTextInColumn(
            canvas,
            totalsValuePaint,
            totalGotStr,
            rightX - colBalanceWidth - 4f,
            colBalanceWidth + 4f,
            totalsStartY + totalsValuePaint.textSize
        )

        val totalsY2 = totalsStartY + totalsValuePaint.textSize + 8f
        canvas.drawText(
            "Total Paid:",
            leftX,
            totalsY2 + totalsLabelPaint.textSize,
            totalsLabelPaint
        )
        val totalGaveStr = "₹${"%.2f".format(totalGave)}"
        drawRightAlignedTextInColumn(
            canvas,
            totalsValuePaint,
            totalGaveStr,
            rightX - colBalanceWidth - 4f,
            colBalanceWidth + 4f,
            totalsY2 + totalsValuePaint.textSize
        )

        val totalsY3 = totalsY2 + totalsValuePaint.textSize + 8f
        canvas.drawText("Remaining:", leftX, totalsY3 + totalsLabelPaint.textSize, totalsLabelPaint)
        val remAmount = "₹${"%.2f".format(kotlin.math.abs(remaining))}"
        val remLabel = "  —  $remainingLabel"
        totalsValuePaint.color = remainingColorInt
        drawRightAlignedTextInColumn(
            canvas,
            totalsValuePaint,
            remAmount + remLabel,
            rightX - colBalanceWidth - 4f,
            colBalanceWidth + 4f,
            totalsY3 + totalsValuePaint.textSize
        )
        totalsValuePaint.color = Color.BLACK

        y = totalsY3 + totalsLabelPaint.textSize + totalsValuePaint.textSize + 14f

        // Divider & table header
        canvas.drawLine(leftX, y, rightX, y, dividerPaint)

        y += 10f

        canvas.drawText("S. No", snoColLeft + 4f, y + headerPaint.textSize, headerPaint)
        canvas.drawText("Date", dateColLeft + 4f, y + headerPaint.textSize, headerPaint)
        canvas.drawText("Note", noteColLeft + 4f, y + headerPaint.textSize, headerPaint)
        canvas.drawText(
            "Received",
            receivedColLeft + (colReceivedWidth - headerPaint.measureText("Received")),
            y + headerPaint.textSize,
            headerPaint
        )
        canvas.drawText(
            "Paid",
            paidColLeft + (colPaidWidth - headerPaint.measureText("Paid")),
            y + headerPaint.textSize,
            headerPaint
        )
        canvas.drawText(
            "Balance",
            balanceColLeft + (colBalanceWidth - headerPaint.measureText("Balance")),
            y + headerPaint.textSize,
            headerPaint
        )

        y += headerPaint.textSize + 8f
        canvas.drawLine(leftX, y, rightX, y, dividerPaint)
        y += 8f

        // Record table header end Y for column lines
        var tableHeaderEndY = if (previousBalance != null) y + 44 else y

        // Running balance starts from previousBalance if provided
        var runningBalance = previousBalance ?: 0.0

        // Helper to finish current page: draw column lines for txn area, footer and finish
        fun finishPageAndStartNew() {
            // compute bottom of txn area for this page
            val tableAreaBottomForThisPage = (y - 8f).coerceAtLeast(tableHeaderEndY + 10f)
            drawColumnLines(canvas, tableHeaderEndY, tableAreaBottomForThisPage)

            val footerPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY }
            val pageNoText = "Page $pageIndex"
            val footerX = leftX + (usableWidth - footerPaint.measureText(pageNoText)) / 2f
            canvas.drawText(pageNoText, footerX, pageHeight - margin + 8f, footerPaint)

            pdf.finishPage(page)

            // new page
            pageIndex += 1
            pageInfo =
                PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex)
                    .create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas

            drawPageHeader(canvas, pageIndex, leftX, rightX, usableWidth)
            y = margin + headerBlockHeight

            // draw compact table header on new page
            canvas.drawLine(leftX, y, rightX, y, dividerPaint)
            y += 12f
            canvas.drawText("S. No", snoColLeft + 4f, y + headerPaint.textSize, headerPaint)
            canvas.drawText("Date", dateColLeft + 4f, y + headerPaint.textSize, headerPaint)
            canvas.drawText("Note", noteColLeft + 4f, y + headerPaint.textSize, headerPaint)
            canvas.drawText(
                "Received",
                receivedColLeft + (colReceivedWidth - headerPaint.measureText("Received")),
                y + headerPaint.textSize,
                headerPaint
            )
            canvas.drawText(
                "Paid",
                paidColLeft + (colPaidWidth - headerPaint.measureText("Paid")),
                y + headerPaint.textSize,
                headerPaint
            )
            canvas.drawText(
                "Balance",
                balanceColLeft + (colBalanceWidth - headerPaint.measureText("Balance")),
                y + headerPaint.textSize,
                headerPaint
            )
            y += headerPaint.textSize + 8f
            canvas.drawLine(leftX, y, rightX, y, dividerPaint)
            y += 8f

            // set new table header end
            tableHeaderEndY = y
        }

        // If previousBalance present, draw previous row first (WITHOUT vertical lines covering it)
        previousBalance?.let { prev ->
            val label = "Previous amount"
            // center the label across S.No .. Paid columns (from snoColLeft to paidColLeft + colPaidWidth)
            val leftSpan = snoColLeft
            val rightSpan = paidColLeft + colPaidWidth
            val spanWidth = rightSpan - leftSpan
            val centerX = leftSpan + spanWidth / 2f

            val prevRowHeight = rowMinHeight
            if (y + prevRowHeight > pageHeight - margin - 30f) {
                finishPageAndStartNew()
            }

            // Draw centered label across first columns
            val labelPaint = Paint().apply {
                textSize = headerPaint.textSize
                isFakeBoldText = true
                color = Color.DKGRAY
            }
            val labelW = labelPaint.measureText(label)
            canvas.drawText(label, centerX - labelW / 2f, y + labelPaint.textSize, labelPaint)

            // Draw balance in balance column (right aligned)
            val prevBalanceStr = "₹${"%.2f".format(prev)}"
            totalsValuePaint.color = if(previousBalance < 0) "#C62828".toColorInt() else "#2E7D32".toColorInt()
            drawRightAlignedTextInColumn(
                canvas,
                totalsValuePaint,
                prevBalanceStr,
                balanceColLeft,
                colBalanceWidth,
                y + normalPaint.textSize
            )

            y += rowMinHeight
            canvas.drawLine(leftX, y, rightX, y, dividerPaint)
            y += 8f
        }

        // Draw transactions
        txns.reversed().forEachIndexed { index, t ->
            // compute how many note lines will be needed
            val note = t.note ?: ""
            val noteLines = if (note.isBlank()) 1 else {
                val approx = noteTextPaint.measureText(note)
                ceil(approx / colNoteWidth.toDouble()).toInt().coerceAtLeast(1)
            }
            val requiredHeight = rowMinHeight + (noteLines - 1) * noteLineHeight

            // pagination check
            if (y + requiredHeight > pageHeight - margin - 30f) {
                finishPageAndStartNew()
            }

            // Update running balance based on txn type
            val amount = t.amount
            val isReceived = when (t.type) {
                TxnType.CREDIT -> true
                TxnType.DEBIT -> false
                else -> true
            }
            runningBalance += if (isReceived) amount else -amount

            // Row values
            val snoStr = "${index + 1}"
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(t.date))
            val noteStartY = y
            val noteDrawX = noteColLeft + 4f

            val receivedStr = if (isReceived) "₹${"%.2f".format(amount)}" else ""
            val paidStr = if (!isReceived) "₹${"%.2f".format(amount)}" else ""
            val balanceStr = "₹${"%.2f".format(runningBalance)}"

            // Draw S. No (left)
            canvas.drawText(snoStr, snoColLeft + 4f, y + normalPaint.textSize, normalPaint)

            // Draw Date
            canvas.drawText(dateStr, dateColLeft + 4f, y + smallPaint.textSize, smallPaint)

            // Draw Note (wrapped)
            val wrappedEndY =
                drawTextWrapped(canvas, note, noteDrawX, noteStartY, noteTextPaint, colNoteWidth)

            // Draw Received (right aligned in its column)
            if (receivedStr.isNotEmpty()) {
                drawRightAlignedTextInColumn(
                    canvas,
                    normalPaint,
                    receivedStr,
                    receivedColLeft,
                    colReceivedWidth,
                    y + normalPaint.textSize
                )
            }

            // Draw Paid (right aligned)
            if (paidStr.isNotEmpty()) {
                drawRightAlignedTextInColumn(
                    canvas,
                    normalPaint,
                    paidStr,
                    paidColLeft,
                    colPaidWidth,
                    y + normalPaint.textSize
                )
            }

            // Draw Balance (right aligned, constrained to column)
            val totalsPaintToUse = totalsValuePaint
            if (runningBalance >= 0) totalsPaintToUse.color =
                "#2E7D32".toColorInt() else totalsPaintToUse.color = "#C62828".toColorInt()
            drawRightAlignedTextInColumn(
                canvas,
                totalsPaintToUse,
                balanceStr,
                balanceColLeft,
                colBalanceWidth,
                y + normalPaint.textSize
            )
            totalsPaintToUse.color = Color.BLACK

            // consumed height
            val consumedHeight =
                if (note.isBlank()) rowMinHeight else (wrappedEndY - noteStartY).coerceAtLeast(
                    rowMinHeight
                )
            y += consumedHeight
            canvas.drawLine(leftX, y, rightX, y, dividerPaint)
            y += 8f
        }

        // After txns, draw recent amount row if present (WITHOUT vertical lines covering it)

        // Before finishing last page: draw column lines for the actual txn area and footer
        val tableAreaBottomForLastPage = (y - 8f).coerceAtLeast(tableHeaderEndY + 10f)
        drawColumnLines(canvas, tableHeaderEndY, tableAreaBottomForLastPage)

        val footerPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY }
        val pageNoText = "Page $pageIndex"
        val footerX = leftX + (usableWidth - footerPaint.measureText(pageNoText)) / 2f
        canvas.drawText(pageNoText, footerX, pageHeight - margin + 8f, footerPaint)
        pdf.finishPage(page)

        // Save to MediaStore
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/Hisab"
                )
            }
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        if (uri == null) {
            pdf.close()
            return@withContext null
        }
        resolver.openOutputStream(uri).use { out -> pdf.writeTo(out) }
        pdf.close()
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// -----------------------------
// Small helper required earlier
// -----------------------------
fun generateReportRangeTitle(filterOptionType: FilterOptionType): String {
    return when (filterOptionType) {
        FilterOptionType.Today -> "Today"
        FilterOptionType.ThisWeek -> "This Week"
        FilterOptionType.LastWeek -> "Last Week"
        FilterOptionType.ThisMonth -> "This Month"
        FilterOptionType.LastMonth -> "Last Month"
        FilterOptionType.All -> "All Time"
    }
}
