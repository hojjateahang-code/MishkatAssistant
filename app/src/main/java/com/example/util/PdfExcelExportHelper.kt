package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.ActivityLogEntity
import com.example.data.FinancialTransactionEntity
import com.example.data.PunchLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExcelExportHelper {

    fun generateFinancialPdf(
        context: Context,
        workplaceTitle: String,
        transactions: List<FinancialTransactionEntity>,
        totalIncome: Long,
        totalExpense: Long,
        balance: Long
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        var y = 40f

        // Header Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(13, 148, 136) // Deep Teal
        }
        canvas.drawRect(20f, 20f, 575f, 75f, headerPaint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        canvas.drawText("صورت حسابرسی مالی - $workplaceTitle", 30f, 52f, paint)

        // Date
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        canvas.drawText("تاریخ تنظیم گزارش: ${sdf.format(Date())}", 350f, 92f, paint)

        // Summary Box
        val boxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }
        canvas.drawRect(20f, 105f, 575f, 160f, boxPaint)

        paint.color = Color.BLACK
        paint.textSize = 11f
        canvas.drawText("کل درآمدها: $totalIncome ریال", 30f, 135f, paint)
        canvas.drawText("کل هزینه‌ها: $totalExpense ریال", 220f, 135f, paint)
        paint.color = if (balance >= 0) Color.rgb(16, 185, 129) else Color.RED
        canvas.drawText("مانده حساب: $balance ریال", 410f, 135f, paint)

        // Table Header
        y = 185f
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
        }
        canvas.drawRect(20f, y, 575f, y + 25f, tableHeaderPaint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("تاریخ", 30f, y + 17f, paint)
        canvas.drawText("نوع", 110f, y + 17f, paint)
        canvas.drawText("مبلغ (ریال)", 170f, y + 17f, paint)
        canvas.drawText("سرفصل / محل مصرف", 270f, y + 17f, paint)
        canvas.drawText("توضیحات / پیگیری", 430f, y + 17f, paint)

        y += 30f

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        for (tx in transactions) {
            if (y > 780f) break // Page overflow protection
            canvas.drawText(tx.jalaliDate, 30f, y + 12f, paint)
            canvas.drawText(if (tx.type == "INCOME") "درآمد" else "هزینه", 110f, y + 12f, paint)
            canvas.drawText("${tx.amount}", 170f, y + 12f, paint)

            val categoryTruncated = if (tx.category.length > 20) tx.category.take(18) + ".." else tx.category
            canvas.drawText(categoryTruncated, 270f, y + 12f, paint)

            val descTruncated = if (tx.description.length > 20) tx.description.take(18) + ".." else tx.description
            canvas.drawText(descTruncated, 430f, y + 12f, paint)

            canvas.drawLine(20f, y + 20f, 575f, y + 20f, linePaint)
            y += 28f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Financial_Report_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    fun generateActivityCsv(
        context: Context,
        activities: List<ActivityLogEntity>
    ): File? {
        val file = File(context.cacheDir, "Activities_Report_${System.currentTimeMillis()}.csv")
        try {
            val writer = file.bufferedWriter(Charsets.UTF_8)
            // UTF-8 BOM for Excel Persian compatibility
            writer.write("\uFEFF")
            writer.write("شناسه,محل کار,عنوان فعالیت,دسته/تگ,تاریخ,مدت (دقیقه),توضیحات\n")
            for (act in activities) {
                val workplaceText = if (act.workplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                val line = "${act.id},\"$workplaceText\",\"${act.title}\",\"${act.categoryTag}\",\"${act.jalaliDate}\",${act.durationMinutes},\"${act.notes}\"\n"
                writer.write(line)
            }
            writer.flush()
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generatePunchCsv(
        context: Context,
        punches: List<PunchLogEntity>
    ): File? {
        val file = File(context.cacheDir, "Attendance_Report_${System.currentTimeMillis()}.csv")
        try {
            val writer = file.bufferedWriter(Charsets.UTF_8)
            writer.write("\uFEFF")
            writer.write("شناسه,محل کار,تاریخ,زمان ورود,زمان خروج,مدت حضور (دقیقه),یادداشت\n")
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            for (p in punches) {
                val workplaceText = if (p.workplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                val inStr = sdf.format(Date(p.checkInTime))
                val outStr = if (p.checkOutTime != null) sdf.format(Date(p.checkOutTime)) else "در حال حضور"
                val duration = if (p.checkOutTime != null) ((p.checkOutTime - p.checkInTime) / 60000).toInt() else 0
                val line = "${p.id},\"$workplaceText\",\"${p.jalaliDate}\",\"$inStr\",\"$outStr\",$duration,\"${p.note ?: ""}\"\n"
                writer.write(line)
            }
            writer.flush()
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش"))
    }
}
