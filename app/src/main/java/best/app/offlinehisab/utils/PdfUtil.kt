package best.app.offlinehisab.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import best.app.offlinehisab.data.db.Customer
import best.app.offlinehisab.data.db.Txn
import best.app.offlinehisab.data.db.TxnType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

suspend fun generateCustomerPdfAndSaveModernUpdated(
    context: Context,
    customer: Customer,
    txns: List<Txn>,
    filterOptionType: FilterOptionType,
    totals: Pair<Double, Double>, // (totalGot = credit, totalGave = debit)
    filename: String = "Hisab_${customer.name}_${System.currentTimeMillis()}.pdf",
): Uri? = withContext(Dispatchers.IO) {
    return@withContext try {
        val pageWidth = 595f      // points (A4-ish)
        val pageHeight = 842f
        val margin = 36f
        val usableWidth = pageWidth - margin * 2
        val usableHeight = pageHeight - margin * 2

        val pdf = PdfDocument()

        // Paints and TextPaint for StaticLayout
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val normalPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val smallPaint = Paint().apply {
            textSize = 9f
            color = android.graphics.Color.DKGRAY
        }
        val dividerPaint = Paint().apply { strokeWidth = 0.9f; color = android.graphics.Color.LTGRAY }
        val totalsLabelPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
        val totalsValuePaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.BLACK }

        val noteTextPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = normalPaint.textSize
            color = android.graphics.Color.BLACK
        }

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val downloadedDateStr = dateFormatter.format(Date())

        // Helpers
        fun drawTextWrapped(
            canvas: android.graphics.Canvas,
            text: String,
            x: Float,
            startY: Float,
            paint: TextPaint,
            maxWidth: Float,
            spacingAdd: Float = 0f,
        ): Float {
            if (text.isBlank()) {
                canvas.drawText("", x, startY, normalPaint)
                return startY
            }
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(spacingAdd, 1.0f)
                .setIncludePad(false)
                .build()
            // StaticLayout draws relative to (x, startY - firstLineAscent). We translate canvas to draw at x,startY
            canvas.save()
            canvas.translate(x, startY)
            staticLayout.draw(canvas)
            canvas.restore()
            return startY + staticLayout.height
        }

        // Page header drawing
        @RequiresApi(Build.VERSION_CODES.O)
        fun drawPageHeader(canvas: android.graphics.Canvas, pageNo: Int) {
            var y = margin
            val leftX = margin
            val rightX = pageWidth - margin

            // Title centered
            val title = customer.name + " Statement"
            val titleX = leftX + (usableWidth - titlePaint.measureText(title)) / 2f
            canvas.drawText(title, titleX, y + titlePaint.textSize, titlePaint)
            y += titlePaint.textSize + 4f

            //report generate range
            val generateReportRange = generateReportRangeTitle(
                filterOptionType = filterOptionType
            )
            val reportRang = leftX + (usableWidth - headerPaint.measureText(generateReportRange)) / 2f
            canvas.drawText(generateReportRange, reportRang, y + headerPaint.textSize, headerPaint)


            // Customer details (left)
            val phone = "Phone: ${customer.phone ?: "-"}"
            canvas.drawText(phone, leftX, y + headerPaint.textSize + normalPaint.textSize + 4f, normalPaint)

            // Downloaded date right
            val dateText = "Report Generated: $downloadedDateStr"
            val dateW = smallPaint.measureText(dateText)
            canvas.drawText(dateText, rightX - dateW, margin, smallPaint)

            // horizontal divider
            val divY = margin + titlePaint.textSize + 40f
            canvas.drawLine(leftX, divY, rightX, divY, dividerPaint)
        }

        // Layout constants and column widths
        val headerBlockHeight = 70f
        val rowMinHeight = 36f
        val noteLineHeight = normalPaint.textSize + 6f

        val colAmountWidth = 100f
        val colDateWidth = 140f
        val colDirWidth = 90f
        val colNoteWidth = usableWidth - (colAmountWidth + colDateWidth + colDirWidth + 20f)

        // Start page
        var pageIndex = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        drawPageHeader(canvas, pageIndex)
        var y = margin + headerBlockHeight

        // Totals block (Remaining shown first then label)
        val (totalGot, totalGave) = totals
        val remaining = totalGot - totalGave
        val remainingLabel = if (remaining >= 0.0) "You will Pay" else "You will Receive"
        val remainingColorInt = if (remaining >= 0.0) "#2E7D32".toColorInt() else "#C62828".toColorInt()

        val leftX = margin
        val rightX = pageWidth - margin

        val totalsStartY = y + 6f
        // Total Received
        canvas.drawText("Total Received:", leftX, totalsStartY + totalsLabelPaint.textSize, totalsLabelPaint)
        val totalGotStr = "₹${"%.2f".format(totalGot)}"
        canvas.drawText(totalGotStr, rightX - totalsValuePaint.measureText(totalGotStr), totalsStartY + totalsValuePaint.textSize, totalsValuePaint)

        // Total Paid
        val totalsY2 = totalsStartY + totalsValuePaint.textSize + 8f
        canvas.drawText("Total Paid:", leftX, totalsY2 + totalsLabelPaint.textSize, totalsLabelPaint)
        val totalGaveStr = "₹${"%.2f".format(totalGave)}"
        canvas.drawText(totalGaveStr, rightX - totalsValuePaint.measureText(totalGaveStr), totalsY2 + totalsValuePaint.textSize, totalsValuePaint)

        // Remaining (amount first then label)
        val totalsY3 = totalsY2 + totalsValuePaint.textSize + 8f
        canvas.drawText("Remaining:", leftX, totalsY3 + totalsLabelPaint.textSize, totalsLabelPaint)
        val remainingStr = "₹${"%.2f".format(kotlin.math.abs(remaining))}  —  $remainingLabel"
        // draw remaining amount colored; split draw: amount colored, label normal
        val remAmount = "₹${"%.2f".format(kotlin.math.abs(remaining))}"
        val remLabel = "  —  $remainingLabel"
        val remAmountW = totalsValuePaint.measureText(remAmount)
        // draw amount
        totalsValuePaint.color = remainingColorInt
        canvas.drawText(remAmount, rightX - totalsValuePaint.measureText(remAmount + remLabel), totalsY3 + totalsValuePaint.textSize, totalsValuePaint)
        // draw label next to it
        totalsLabelPaint.color = android.graphics.Color.DKGRAY
        canvas.drawText(remLabel, rightX - totalsValuePaint.measureText(remLabel), totalsY3 + totalsValuePaint.textSize, totalsLabelPaint)
        // reset paints
        totalsValuePaint.color = android.graphics.Color.BLACK
        totalsLabelPaint.color = android.graphics.Color.BLACK

        y = totalsY3 + totalsLabelPaint.textSize + totalsValuePaint.textSize + 14f

        // Divider & table header
        canvas.drawLine(leftX, y, rightX, y, dividerPaint)
        y += 10f
        // Draw headers: Type, Note, Date, Amount (amount right-aligned)
        val amountColLeft = rightX - colAmountWidth
        val dateColLeft = amountColLeft - colDateWidth - 8f
        val noteColLeft = leftX + colDirWidth + 8f
        val dirColLeft = leftX

        canvas.drawText("Type", dirColLeft + 4f, y + headerPaint.textSize, headerPaint)
        canvas.drawText("Note", noteColLeft + 4f, y + headerPaint.textSize, headerPaint)
        // date right-aligned in date column
        canvas.drawText("Date", dateColLeft + (colDateWidth - headerPaint.measureText("Date")), y + headerPaint.textSize, headerPaint)
        // amount header right-aligned
        canvas.drawText("Amount", amountColLeft + (colAmountWidth - headerPaint.measureText("Amount")), y + headerPaint.textSize, headerPaint)

        y += headerPaint.textSize + 8f
        canvas.drawLine(leftX, y, rightX, y, dividerPaint)
        y += 8f

        // Iterate transactions and paginate
        txns.forEachIndexed { _, t ->
            val note = t.note ?: ""
            val noteLines = if (note.isBlank()) 1 else {
                val approx = noteTextPaint.measureText(note)
                ceil(approx / colNoteWidth.toDouble()).toInt().coerceAtLeast(1)
            }
            val requiredHeight = rowMinHeight + (noteLines - 1) * noteLineHeight

            if (y + requiredHeight > pageHeight - margin - 30f) {
                // footer current page
                val footerPaint = Paint().apply { textSize = 9f; color = android.graphics.Color.DKGRAY }
                val pageNoText = "Page $pageIndex"
                val footerX = leftX + (usableWidth - footerPaint.measureText(pageNoText)) / 2f
                canvas.drawText(pageNoText, footerX, pageHeight - margin + 8f, footerPaint)

                // finish and start new page
                pdf.finishPage(page)
                pageIndex += 1
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas

                drawPageHeader(canvas, pageIndex)
                y = margin + headerBlockHeight

                // redraw totals area only on first page — subsequent pages should show minimal header and table header
                // draw table header on new page
                canvas.drawLine(leftX, y, rightX, y, dividerPaint)
                y += 12f
                canvas.drawText("Type", dirColLeft + 4f, y + headerPaint.textSize, headerPaint)
                canvas.drawText("Note", noteColLeft + 4f, y + headerPaint.textSize, headerPaint)
                canvas.drawText("Date", dateColLeft + (colDateWidth - headerPaint.measureText("Date")), y + headerPaint.textSize, headerPaint)
                canvas.drawText("Amount", amountColLeft + (colAmountWidth - headerPaint.measureText("Amount")), y + headerPaint.textSize, headerPaint)
                y += headerPaint.textSize + 8f
                canvas.drawLine(leftX, y, rightX, y, dividerPaint)
                y += 8f
            }

            // Draw row contents
            val isCredit = t.type == TxnType.CREDIT
            val dirText = if (isCredit) "You Received" else "You Paid"
            val amountStr = "₹${"%.2f".format(t.amount)}"
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t.date))

            // Type (left)
            canvas.drawText(dirText, dirColLeft + 4f, y + normalPaint.textSize, normalPaint)

            // Note (wrapped) in note column
            val noteStartY = y
            val noteDrawX = noteColLeft + 4f
            val wrappedEndY = drawTextWrapped(canvas, note, noteDrawX, noteStartY, noteTextPaint, colNoteWidth)

            // Date (right-aligned in date column)
            val dateX = dateColLeft + (colDateWidth - smallPaint.measureText(dateStr))
            canvas.drawText(dateStr, dateX, y + smallPaint.textSize, smallPaint)

            // Amount (right aligned)
            val amountX = amountColLeft + (colAmountWidth - normalPaint.measureText(amountStr))
            canvas.drawText(amountStr, amountX, y + normalPaint.textSize, normalPaint)

            // compute row height consumed (StaticLayout height if note multiline)
            val consumedHeight = if (note.isBlank()) rowMinHeight else (wrappedEndY - noteStartY).coerceAtLeast(rowMinHeight)
            // move y and draw divider
            y += consumedHeight
            canvas.drawLine(leftX, y, rightX, y, dividerPaint)
            y += 8f
        }

        // footer last page
        val footerPaint = Paint().apply { textSize = 9f; color = android.graphics.Color.DKGRAY }
        val pageNoText = "Page $pageIndex"
        val footerX = leftX + (usableWidth - footerPaint.measureText(pageNoText)) / 2f
        canvas.drawText(pageNoText, footerX, pageHeight - margin + 8f, footerPaint)

        // finish last page
        pdf.finishPage(page)

        // Save to MediaStore
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Hisab")
            }
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        if (uri == null) {
            pdf.close()
            return@withContext null
        }
        resolver.openOutputStream(uri).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
