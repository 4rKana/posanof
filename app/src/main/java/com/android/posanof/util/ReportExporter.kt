package com.android.posanof.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.android.posanof.data.local.relation.TransactionWithDetails
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas untuk ekspor Laporan Penjualan & Keuangan ke format Excel (.xlsx) dan PDF.
 * Sesuai dengan spesifikasi paperless export pada PROJECT_CONTEXT.md.
 */
object ReportExporter {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))

    // =========================================================================
    // EXPORT TO EXCEL (.xlsx) APACHE POI
    // =========================================================================

    fun exportToExcel(
        transactions: List<TransactionWithDetails>,
        outputStream: OutputStream,
        title: String = "LAPORAN PENJUALAN UMKM (POSANOF)",
    ) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Laporan Penjualan")

            // Styles
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.getIndex()
            }

            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.DARK_BLUE.getIndex()
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            val boldStyle = workbook.createCellStyle().apply {
                setFont(workbook.createFont().apply { bold = true })
            }

            // Title Row
            val titleRow = sheet.createRow(0)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue(title)
            titleCell.cellStyle = boldStyle

            // Subtitle / Date
            val subTitleRow = sheet.createRow(1)
            subTitleRow.createCell(0).setCellValue("Tanggal Cetak: ${dateFormatter.format(Date())}")

            // Empty row
            sheet.createRow(2)

            // Table Header Row
            val headers = arrayOf(
                "No",
                "ID Transaksi",
                "Tanggal",
                "Kasir",
                "Metode Pembayaran",
                "Uang Tunai",
                "Kembalian",
                "Total (Rp)"
            )

            val headerRow = sheet.createRow(3)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            var rowIndex = 4
            var grandTotal = 0.0

            val colMaxLengths = headers.map { it.length }.toIntArray()

            transactions.forEachIndexed { index, item ->
                val t = item.transaction
                val row = sheet.createRow(rowIndex++)

                val noStr = (index + 1).toString()
                val idStr = t.id.toString()
                val dateStr = dateFormatter.format(Date(t.date))
                val cashierStr = t.cashierName
                val paymentStr = t.paymentMethod
                val cashStr = currencyFormat.format(t.cashAmount)
                val changeStr = currencyFormat.format(t.changeAmount)
                val totalStr = currencyFormat.format(t.totalAmount)

                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(t.id.toDouble())
                row.createCell(2).setCellValue(dateStr)
                row.createCell(3).setCellValue(cashierStr)
                row.createCell(4).setCellValue(paymentStr)
                row.createCell(5).setCellValue(t.cashAmount)
                row.createCell(6).setCellValue(t.changeAmount)
                row.createCell(7).setCellValue(t.totalAmount)

                colMaxLengths[0] = maxOf(colMaxLengths[0], noStr.length)
                colMaxLengths[1] = maxOf(colMaxLengths[1], idStr.length)
                colMaxLengths[2] = maxOf(colMaxLengths[2], dateStr.length)
                colMaxLengths[3] = maxOf(colMaxLengths[3], cashierStr.length)
                colMaxLengths[4] = maxOf(colMaxLengths[4], paymentStr.length)
                colMaxLengths[5] = maxOf(colMaxLengths[5], cashStr.length)
                colMaxLengths[6] = maxOf(colMaxLengths[6], changeStr.length)
                colMaxLengths[7] = maxOf(colMaxLengths[7], totalStr.length)

                grandTotal += t.totalAmount
            }

            // Summary Row
            val summaryRow = sheet.createRow(rowIndex + 1)
            val labelCell = summaryRow.createCell(6)
            val labelText = "TOTAL PENDAPATAN:"
            labelCell.setCellValue(labelText)
            labelCell.cellStyle = boldStyle
            colMaxLengths[6] = maxOf(colMaxLengths[6], labelText.length)

            val totalCell = summaryRow.createCell(7)
            totalCell.setCellValue(grandTotal)
            totalCell.cellStyle = boldStyle
            val grandTotalStr = currencyFormat.format(grandTotal)
            colMaxLengths[7] = maxOf(colMaxLengths[7], grandTotalStr.length)

            // Set column widths based on maximum character length
            // Note: sheet.autoSizeColumn relies on java.awt.font.FontRenderContext, which is not available in Android SDK.
            for (i in headers.indices) {
                val maxLen = colMaxLengths[i]
                val width = ((maxLen + 4) * 256).coerceIn(2000, 255 * 256)
                sheet.setColumnWidth(i, width)
            }

            workbook.write(outputStream)
            outputStream.flush()
        }
    }

    // =========================================================================
    // EXPORT TO PDF (ANDROID PDF DOCUMENT)
    // =========================================================================

    fun exportToPdf(
        transactions: List<TransactionWithDetails>,
        outputStream: OutputStream,
        storeName: String = "TOKO UMKM POSANOF",
        storeAddress: String = "Aplikasi Kasir POS Paperless"
    ) {
        val pdfDocument = PdfDocument()

        // Ukuran A4 standar (595 x 842 pt)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }

        var y = 40f

        // 1. Header Toko
        canvas.drawText(storeName, 40f, y, titlePaint)
        y += 18f
        canvas.drawText(storeAddress, 40f, y, subTitlePaint)
        y += 14f
        canvas.drawText("Dicetak pada: ${dateFormatter.format(Date())}", 40f, y, subTitlePaint)
        y += 20f

        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 25f

        // Title Laporan
        canvas.drawText("LAPORAN KEUANGAN PENJUALAN", 40f, y, titlePaint)
        y += 25f

        // Table Header
        val colNoX = 40f
        val colIdX = 75f
        val colDateX = 135f
        val colKasirX = 260f
        val colMethodX = 350f
        val colTotalX = 450f

        canvas.drawText("No", colNoX, y, headerPaint)
        canvas.drawText("ID", colIdX, y, headerPaint)
        canvas.drawText("Tanggal", colDateX, y, headerPaint)
        canvas.drawText("Kasir", colKasirX, y, headerPaint)
        canvas.drawText("Metode", colMethodX, y, headerPaint)
        canvas.drawText("Total", colTotalX, y, headerPaint)
        y += 8f

        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 18f

        var grandTotal = 0.0

        transactions.forEachIndexed { index, item ->
            val t = item.transaction
            grandTotal += t.totalAmount

            // Mencegah overflow halaman
            if (y > (pageHeight - 80)) {
                return@forEachIndexed
            }

            canvas.drawText((index + 1).toString(), colNoX, y, bodyPaint)
            canvas.drawText("#${t.id}", colIdX, y, bodyPaint)
            canvas.drawText(dateFormatter.format(Date(t.date)), colDateX, y, bodyPaint)
            canvas.drawText(t.cashierName.take(12), colKasirX, y, bodyPaint)
            canvas.drawText(t.paymentMethod, colMethodX, y, bodyPaint)
            canvas.drawText(currencyFormat.format(t.totalAmount), colTotalX, y, bodyPaint)

            y += 18f
        }

        y += 10f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 25f

        // Summary Total
        val totalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Total Transaksi: ${transactions.size}", 40f, y, totalPaint)
        y += 20f
        canvas.drawText("Total Pendapatan: ${currencyFormat.format(grandTotal)}", 40f, y, totalPaint)

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
        } finally {
            pdfDocument.close()
        }
    }
}
