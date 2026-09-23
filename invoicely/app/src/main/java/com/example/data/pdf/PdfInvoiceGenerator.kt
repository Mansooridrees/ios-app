package com.example.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.data.model.InvoiceDraft
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfInvoiceGenerator {

    // Standard A4 dimensions at 72 dpi: 595 x 842 points
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Generates a modern, print-ready PDF invoice.
     */
    fun generateInvoicePdf(context: Context, invoice: InvoiceDraft): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoiceOnCanvas(canvas, invoice)

        pdfDoc.finishPage(page)

        // Save into app's cache directory
        val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val cleanNumber = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val pdfFile = File(invoicesDir, "Invoice_${cleanNumber}.pdf")

        val outputStream = FileOutputStream(pdfFile)
        pdfDoc.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDoc.close()

        return pdfFile
    }

    private fun drawInvoiceOnCanvas(canvas: Canvas, invoice: InvoiceDraft) {
        if (invoice.companyName.equals("MASTER TOOLS", ignoreCase = true) ||
            invoice.templateType.equals("MASTER_TOOLS", ignoreCase = true) ||
            invoice.templateType.equals("MODERN", ignoreCase = true)) {
            drawModernInvoiceOnCanvas(canvas, invoice)
        } else if (invoice.templateType.equals("LAYOUT_1", ignoreCase = true) ||
            invoice.templateType.equals("CLASSIC_RECEIPT", ignoreCase = true)) {
            val finalInvoice = if (invoice.companyName.isBlank() || invoice.companyName.equals("Xp Computer", ignoreCase = true)) {
                invoice.copy(companyName = "XP Computers")
            } else invoice
            drawClassicSalesReceiptOnCanvas(canvas, finalInvoice)
        } else if (invoice.templateType.equals("LAYOUT_2", ignoreCase = true) ||
                   invoice.templateType.equals("MASTER_TECH", ignoreCase = true) ||
                   invoice.templateType.equals("LAYOUT_4", ignoreCase = true) ||
                   invoice.templateType.equals("FOUR", ignoreCase = true) ||
                   invoice.templateType.equals("4", ignoreCase = true)) {
            val finalInvoice = if (invoice.companyName.isBlank() ||
                invoice.companyName.equals("Global Tech Solutions", ignoreCase = true) ||
                invoice.companyName.equals("Xp Computer", ignoreCase = true)) {
                invoice.copy(
                    companyName = "Master Tech.",
                    headerTitle = "Sales Receipt",
                    companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
                    companyPhone1 = "MUSHARAF #03485577343",
                    companyPhone2 = "ISMAIL #03002698445"
                )
            } else invoice
            drawClassicSalesReceiptOnCanvas(canvas, finalInvoice)
        } else {
            drawModernInvoiceOnCanvas(canvas, invoice)
        }
    }

    /**
     * Renders the Classic Sales Receipt template matching XP Computers & Master Tech formats.
     */
    private fun drawClassicSalesReceiptOnCanvas(canvas: Canvas, invoice: InvoiceDraft) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Clean white background
        canvas.drawColor(Color.WHITE)

        // 1. Top Left: Company Title ("XP Computers" or "Master Tech")
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 24f
        paint.textAlign = Paint.Align.LEFT
        val defaultName = if (invoice.templateType.equals("LAYOUT_2", ignoreCase = true) ||
            invoice.templateType.equals("MASTER_TECH", ignoreCase = true)) {
            "Master Tech"
        } else {
            "XP Computers"
        }
        val titleText = invoice.companyName.ifBlank { defaultName }
        canvas.drawText(titleText, 40f, 62f, paint)

        // 2. Left Company Contact & Location Box
        val boxLeft = 38f
        val boxTop = 82f
        val boxRight = 210f
        val boxBottom = 175f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = Color.BLACK
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, paint)

        // Lines inside the company box
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT

        val addrRaw = invoice.companyAddress.ifBlank {
            "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET"
        }
        val addrLines = addrRaw.split(Regex("[\\n,]+")).map { it.trim().uppercase(Locale.US) }.filter { it.isNotBlank() }

        var lineY = boxTop + 16f
        for (line in addrLines.take(3)) {
            canvas.drawText(line, boxLeft + 7f, lineY, paint)
            lineY += 13f
        }

        val phone1 = invoice.companyPhone1.ifBlank { "MUSHARAF #03485577343" }
        val phone2 = invoice.companyPhone2.ifBlank { "ISMAIL #03002698445" }
        canvas.drawText(phone1, boxLeft + 7f, lineY + 3f, paint)
        canvas.drawText(phone2, boxLeft + 7f, lineY + 17f, paint)

        // 3. Top Right: "Sales Receipt" Title
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = 30f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        val headerTitle = invoice.headerTitle.ifBlank { "Sales Receipt" }
        canvas.drawText(headerTitle, 305f, 75f, paint)

        // Date & Receipt No.
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        paint.textSize = 13.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Date", 410f, 138f, paint)
        canvas.drawText(invoice.issueDate.ifBlank { "07-Feb-2026" }, 465f, 138f, paint)

        canvas.drawText("No.", 410f, 168f, paint)
        canvas.drawText(invoice.invoiceNumber.ifBlank { "27248" }, 485f, 168f, paint)

        // 4. "Sold To" and Customer
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.textSize = 13.5f
        canvas.drawText("Sold To", 70f, 205f, paint)

        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = 15f
        val customerName = invoice.clientName.ifBlank { "Cash" }
        canvas.drawText(customerName, 120f, 226f, paint)

        // 5. Receipt Table
        val tableLeft = 38f
        val tableRight = 557f
        val tableTop = 248f
        val tableBottom = 740f

        // Column Dividers
        val colDescRight = 350f
        val colQtyRight = 405f
        val colRateRight = 480f
        val colAmountRight = 557f

        // Table Borders
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.color = Color.BLACK
        // Outer boundary
        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, paint)
        // Header line
        canvas.drawLine(tableLeft, 280f, tableRight, 280f, paint)

        // Vertical column lines (extending all the way down)
        canvas.drawLine(colDescRight, tableTop, colDescRight, tableBottom, paint)
        canvas.drawLine(colQtyRight, tableTop, colQtyRight, tableBottom, paint)
        canvas.drawLine(colRateRight, tableTop, colRateRight, tableBottom, paint)

        // Header Column Labels
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = 13f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Discription", (tableLeft + colDescRight) / 2f, 272f, paint)
        canvas.drawText("Qty", (colDescRight + colQtyRight) / 2f, 272f, paint)
        canvas.drawText("Rate", (colQtyRight + colRateRight) / 2f, 272f, paint)
        canvas.drawText("Amount", (colRateRight + colAmountRight) / 2f, 272f, paint)

        // 6. Table Rows
        var rowY = 304f
        val rowHeight = 24f

        invoice.items.forEach { item ->
            // Item description
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            paint.textSize = 12f
            paint.textAlign = Paint.Align.LEFT
            val title = if (item.description.isNotBlank()) "${item.name} (${item.description})" else item.name
            canvas.drawText(title, tableLeft + 8f, rowY, paint)

            // Qty
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            paint.textAlign = Paint.Align.RIGHT
            val qtyText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
            canvas.drawText(qtyText, colQtyRight - 8f, rowY, paint)

            // Rate
            val rateText = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else String.format(Locale.US, "%.2f", item.unitPrice)
            canvas.drawText(rateText, colRateRight - 8f, rowY, paint)

            // Amount
            val amountText = if (item.total % 1.0 == 0.0) item.total.toInt().toString() else String.format(Locale.US, "%.2f", item.total)
            canvas.drawText(amountText, colAmountRight - 8f, rowY, paint)

            // Horizontal line below item row
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawLine(tableLeft, rowY + 8f, tableRight, rowY + 8f, paint)
            paint.style = Paint.Style.FILL

            rowY += rowHeight
        }

        // 7. Total Box (Bottom Right)
        val totalBoxLeft = 350f
        val totalBoxRight = 557f
        val totalBoxTop = tableBottom
        val totalBoxBottom = tableBottom + 40f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.color = Color.BLACK
        canvas.drawRect(totalBoxLeft, totalBoxTop, totalBoxRight, totalBoxBottom, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = 16f
        paint.textAlign = Paint.Align.RIGHT
        val totalStr = String.format(Locale.US, "Total  %s %.2f", invoice.currency, invoice.grandTotal)
        canvas.drawText(totalStr, totalBoxRight - 10f, totalBoxTop + 26f, paint)
    }

    private fun drawModernInvoiceOnCanvas(canvas: Canvas, invoice: InvoiceDraft) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Colors
        val primaryBlue = Color.parseColor("#0284C7")
        val primaryDark = Color.parseColor("#0369A1")
        val darkNavy = Color.parseColor("#0F172A")
        val slateGray = Color.parseColor("#64748B")
        val lightGray = Color.parseColor("#F8FAFC")
        val borderGray = Color.parseColor("#E2E8F0")

        // 1. Background clean white
        canvas.drawColor(Color.WHITE)

        // 2. Top Header Accent Bar
        paint.color = primaryBlue
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, paint)

        // 3. Company Title & Info (Top Left)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        paint.color = primaryBlue
        canvas.drawText(invoice.companyName.uppercase(Locale.US), 40f, 50f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = slateGray
        canvas.drawText(invoice.companyAddress, 40f, 66f, paint)
        canvas.drawText("Email: ${invoice.companyEmail}  •  Web: www.acmesolutions.com", 40f, 79f, paint)

        // 4. "INVOICE" Title & Number (Top Right)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        paint.color = darkNavy
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", (PAGE_WIDTH - 40).toFloat(), 50f, paint)

        paint.textSize = 10f
        paint.color = primaryDark
        canvas.drawText("# ${invoice.invoiceNumber}", (PAGE_WIDTH - 40).toFloat(), 68f, paint)

        // Status Badge (PAID / PENDING)
        val statusBgColor = if (invoice.status == "PAID") Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
        paint.color = statusBgColor
        val badgeRect = RectF((PAGE_WIDTH - 110).toFloat(), 76f, (PAGE_WIDTH - 40).toFloat(), 94f)
        canvas.drawRoundRect(badgeRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(invoice.status, badgeRect.centerX(), 88f, paint)

        // Top Divider Line
        paint.textAlign = Paint.Align.LEFT
        paint.color = borderGray
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 108f, (PAGE_WIDTH - 40).toFloat(), 108f, paint)

        // 5. Bill To & Invoice Meta Panel
        val panelY = 125f

        // Bill To
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = slateGray
        canvas.drawText("BILLED TO:", 40f, panelY, paint)

        paint.textSize = 12f
        paint.color = darkNavy
        canvas.drawText(invoice.clientName, 40f, panelY + 16f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = slateGray
        if (invoice.clientEmail.isNotBlank()) {
            canvas.drawText(invoice.clientEmail, 40f, panelY + 30f, paint)
        }
        if (invoice.clientAddress.isNotBlank()) {
            canvas.drawText(invoice.clientAddress, 40f, panelY + 44f, paint)
        }

        // Meta (Right Column)
        val metaX = 360f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = slateGray
        canvas.drawText("INVOICE SUMMARY:", metaX, panelY, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice Date:", metaX, panelY + 16f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = darkNavy
        canvas.drawText(invoice.issueDate, metaX + 80f, panelY + 16f, paint)

        // 6. Structured Items Table
        val tableY = panelY + 68f
        val tableWidth = (PAGE_WIDTH - 80).toFloat()

        // Header Background
        paint.color = lightGray
        canvas.drawRect(40f, tableY, 40f + tableWidth, tableY + 24f, paint)
        paint.color = borderGray
        canvas.drawLine(40f, tableY, 40f + tableWidth, tableY, paint)
        canvas.drawLine(40f, tableY + 24f, 40f + tableWidth, tableY + 24f, paint)

        // Table Header Columns
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = darkNavy
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("ITEM & DESCRIPTION", 50f, tableY + 16f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("QTY", 370f, tableY + 16f, paint)
        canvas.drawText("RATE", 445f, tableY + 16f, paint)
        canvas.drawText("AMOUNT", (PAGE_WIDTH - 50).toFloat(), tableY + 16f, paint)

        // Table Rows
        var curY = tableY + 24f
        val rowHeight = 24f

        invoice.items.forEachIndexed { index, item ->
            // Alternating fill
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(40f, curY, 40f + tableWidth, curY + rowHeight, paint)
            }

            paint.color = borderGray
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, curY + rowHeight, 40f + tableWidth, curY + rowHeight, paint)

            // Item Name & Description
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            paint.color = darkNavy
            val itemTitle = if (item.name.length > 35) item.name.substring(0, 32) + "..." else item.name
            canvas.drawText(itemTitle, 50f, curY + 15f, paint)

            if (item.description.isNotBlank()) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 7f
                paint.color = slateGray
                val desc = if (item.description.length > 50) item.description.substring(0, 47) + "..." else item.description
                canvas.drawText(desc, 180f, curY + 15f, paint)
            }

            // Qty
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = slateGray
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
            canvas.drawText(qtyStr, 370f, curY + 15f, paint)

            // Rate
            canvas.drawText(invoice.formatMoney(item.unitPrice), 445f, curY + 15f, paint)

            // Amount
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = darkNavy
            canvas.drawText(invoice.formatMoney(item.total), (PAGE_WIDTH - 50).toFloat(), curY + 15f, paint)

            curY += rowHeight
        }

        // 7. Summary & Payment Instructions Section
        val summaryY = curY + 18f

        // Left Side: Payment Terms & Bank Wire Instructions
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = slateGray
        canvas.drawText("PAYMENT INSTRUCTIONS", 40f, summaryY, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = slateGray
        canvas.drawText(invoice.paymentTerms, 40f, summaryY + 14f, paint)
        canvas.drawText("Bank: First National Commercial Bank", 40f, summaryY + 28f, paint)
        canvas.drawText("Account: 9876-5432-1098  •  Routing: 021000021", 40f, summaryY + 40f, paint)
        canvas.drawText("SWIFT / BIC: FNACBUSBX", 40f, summaryY + 52f, paint)
        canvas.drawText("Note: Please include invoice number in payment reference.", 40f, summaryY + 66f, paint)

        // Right Side: Financial Calculation Box
        val sumBoxX = 350f
        var sumLineY = summaryY

        val drawSummaryRow = { label: String, value: String, isBold: Boolean ->
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = if (isBold) darkNavy else slateGray
            canvas.drawText(label, sumBoxX, sumLineY, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = darkNavy
            canvas.drawText(value, (PAGE_WIDTH - 40).toFloat(), sumLineY, paint)
            sumLineY += 16f
        }

        drawSummaryRow("Subtotal:", invoice.formatMoney(invoice.subtotal), false)
        drawSummaryRow("Tax (${String.format(Locale.US, "%.1f", invoice.taxRate)}%):", invoice.formatMoney(invoice.taxAmount), false)
        if (invoice.discountAmount > 0) {
            drawSummaryRow("Discount:", "-${invoice.formatMoney(invoice.discountAmount)}", false)
        }

        // Grand Total Banner
        paint.color = primaryBlue
        val totalBannerRect = RectF(sumBoxX - 10f, sumLineY, (PAGE_WIDTH - 40).toFloat(), sumLineY + 28f)
        canvas.drawRoundRect(totalBannerRect, 4f, 4f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.WHITE
        canvas.drawText("TOTAL DUE:", sumBoxX, sumLineY + 18f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 12f
        canvas.drawText(invoice.formatMoney(invoice.grandTotal), (PAGE_WIDTH - 50).toFloat(), sumLineY + 18f, paint)

        sumLineY += 38f
        if (invoice.amountPaid > 0) {
            drawSummaryRow("Amount Paid:", invoice.formatMoney(invoice.amountPaid), false)
            drawSummaryRow("Balance Due:", invoice.formatMoney(invoice.balanceDue), true)
        }

        // 8. Bottom Footer
        val footerY = (PAGE_HEIGHT - 35).toFloat()
        paint.color = borderGray
        paint.strokeWidth = 0.5f
        canvas.drawLine(40f, footerY - 10f, (PAGE_WIDTH - 40).toFloat(), footerY - 10f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = slateGray
        canvas.drawText("Thank you for your business!  •  Questions? Contact support@acmesolutions.com", (PAGE_WIDTH / 2).toFloat(), footerY + 2f, paint)
    }

    /**
     * Renders a PDF file into a high-res Bitmap for live in-app preview.
     */
    fun renderPdfToBitmap(pdfFile: File, pageIndex: Int = 0): Bitmap? {
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) {
                pdfRenderer.close()
                fileDescriptor.close()
                return null
            }
            val page = pdfRenderer.openPage(pageIndex)

            // 2x scale for crisp mobile display
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates an Intent to share the PDF via WhatsApp, Email, Drive, etc.
     */
    fun createShareIntent(context: Context, pdfFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${pdfFile.nameWithoutExtension}")
            putExtra(Intent.EXTRA_TEXT, "Please find attached your invoice.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to view the PDF in external PDF reader app
     */
    fun createViewIntent(context: Context, pdfFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
