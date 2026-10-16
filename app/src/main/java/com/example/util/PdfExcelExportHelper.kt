package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
        canvas.drawText("کل درآمدها: ${String.format(Locale.US, "%,d", totalIncome)} ریال", 30f, 135f, paint)
        canvas.drawText("کل هزینه‌ها: ${String.format(Locale.US, "%,d", totalExpense)} ریال", 220f, 135f, paint)
        paint.color = if (balance >= 0) Color.rgb(16, 185, 129) else Color.RED
        canvas.drawText("مانده حساب: ${String.format(Locale.US, "%,d", balance)} ریال", 410f, 135f, paint)

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
            if (y > 780f) break
            canvas.drawText(tx.jalaliDate, 30f, y + 12f, paint)
            canvas.drawText(if (tx.type == "INCOME") "درآمد" else "هزینه", 110f, y + 12f, paint)
            canvas.drawText(String.format(Locale.US, "%,d", tx.amount), 170f, y + 12f, paint)

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

    fun generateActivitiesPdf(
        context: Context,
        workplaceTitle: String,
        activities: List<ActivityLogEntity>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Header Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(30, 64, 175) // Royal Blue
        }
        canvas.drawRect(20f, 20f, 575f, 75f, headerPaint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        canvas.drawText("گزارش ریز فعالیت‌های شغلی و فرهنگی - $workplaceTitle", 30f, 52f, paint)

        // Summary
        val totalMinutes = activities.sumOf { it.durationMinutes }
        val boxPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(20f, 85f, 575f, 135f, boxPaint)

        paint.color = Color.BLACK
        paint.textSize = 11f
        canvas.drawText("تعداد کل فعالیت‌ها: ${activities.size} مورد", 30f, 115f, paint)
        canvas.drawText("مجموع زمان انجام: ${totalMinutes / 60} ساعت و ${totalMinutes % 60} دقیقه", 250f, 115f, paint)

        // Table Header
        var y = 155f
        val tableHeaderPaint = Paint().apply { color = Color.rgb(226, 232, 240) }
        canvas.drawRect(20f, y, 575f, y + 25f, tableHeaderPaint)

        paint.textSize = 10f
        canvas.drawText("تاریخ", 30f, y + 17f, paint)
        canvas.drawText("عنوان فعالیت", 100f, y + 17f, paint)
        canvas.drawText("دسته / تگ", 240f, y + 17f, paint)
        canvas.drawText("مدت (دقیقه)", 360f, y + 17f, paint)
        canvas.drawText("توضیحات تکمیلی", 440f, y + 17f, paint)

        y += 30f
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        for (act in activities) {
            if (y > 780f) break
            canvas.drawText(act.jalaliDate, 30f, y + 12f, paint)
            val titleTr = if (act.title.length > 20) act.title.take(18) + ".." else act.title
            canvas.drawText(titleTr, 100f, y + 12f, paint)
            canvas.drawText(act.categoryTag, 240f, y + 12f, paint)
            canvas.drawText("${act.durationMinutes} دقیقه", 360f, y + 12f, paint)
            val noteTr = if (act.notes.length > 20) act.notes.take(18) + ".." else act.notes
            canvas.drawText(noteTr, 440f, y + 12f, paint)

            canvas.drawLine(20f, y + 20f, 575f, y + 20f, linePaint)
            y += 28f
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Activities_Report_${System.currentTimeMillis()}.pdf")
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

    fun generateAttendancePdf(
        context: Context,
        workplaceTitle: String,
        punches: List<PunchLogEntity>,
        leaves: List<com.example.data.LeaveEntity> = emptyList(),
        missions: List<com.example.data.MissionEntity> = emptyList(),
        totalDeductionMins: Int = 0,
        targetDailyMins: Long = 240L
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Header Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // Emerald Green
        }
        canvas.drawRect(20f, 20f, 575f, 75f, headerPaint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        canvas.drawText("گزارش ریز تردد و حضور و غیاب - $workplaceTitle", 30f, 52f, paint)

        val totalPunchMins = punches.sumOf { p ->
            val end = p.checkOutTime ?: System.currentTimeMillis()
            (end - p.checkInTime) / 60000
        }
        val totalLeaveMins = leaves.sumOf { it.durationMinutes.toLong() }
        val totalMissionMins = missions.sumOf { it.durationMinutes.toLong() }
        val netEffectiveMins = maxOf(0L, totalPunchMins - totalDeductionMins) + totalLeaveMins + totalMissionMins

        val boxPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(20f, 85f, 575f, 160f, boxPaint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("• کل حضور فیزیکی: ${totalPunchMins / 60} ساعت و ${totalPunchMins % 60} دقیقه", 30f, 105f, paint)
        canvas.drawText("• کسورات (تدریس/مطالعه/استراحت): ${totalDeductionMins / 60} ساعت و ${totalDeductionMins % 60} دقیقه", 280f, 105f, paint)
        canvas.drawText("• مرخصی و مأموریت: ${(totalLeaveMins + totalMissionMins) / 60} ساعت و ${(totalLeaveMins + totalMissionMins) % 60} دقیقه", 30f, 128f, paint)
        paint.color = Color.rgb(16, 185, 129)
        canvas.drawText("• خالص حضور موظفی مؤثر: ${netEffectiveMins / 60} ساعت و ${netEffectiveMins % 60} دقیقه", 280f, 128f, paint)

        paint.color = Color.DKGRAY
        canvas.drawText("تاریخ تنظیم: ${JalaliCalendar.getTodayJalali().toPersianDigits()}  |  تعداد کل ترددها: ${punches.size} نوبت", 30f, 150f, paint)

        var y = 175f
        val tableHeaderPaint = Paint().apply { color = Color.rgb(226, 232, 240) }
        canvas.drawRect(20f, y, 575f, y + 25f, tableHeaderPaint)

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("تاریخ", 30f, y + 17f, paint)
        canvas.drawText("ساعت ورود", 110f, y + 17f, paint)
        canvas.drawText("ساعت خروج", 200f, y + 17f, paint)
        canvas.drawText("مدت حضور", 300f, y + 17f, paint)
        canvas.drawText("یادداشت و توضیحات تردد", 410f, y + 17f, paint)

        y += 30f
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        for (p in punches) {
            if (y > 780f) break
            canvas.drawText(p.jalaliDate, 30f, y + 12f, paint)
            val inStr = sdf.format(Date(p.checkInTime))
            val outStr = if (p.checkOutTime != null) sdf.format(Date(p.checkOutTime)) else "در حال حضور"
            val mins = if (p.checkOutTime != null) ((p.checkOutTime - p.checkInTime) / 60000) else 0L

            canvas.drawText(inStr, 110f, y + 12f, paint)
            canvas.drawText(outStr, 200f, y + 12f, paint)
            canvas.drawText("${mins / 60} ساعت و ${mins % 60} دقیقه", 300f, y + 12f, paint)
            val safeNote = p.note ?: ""
            val noteTr = if (safeNote.length > 20) safeNote.take(18) + ".." else safeNote
            canvas.drawText(noteTr, 410f, y + 12f, paint)

            canvas.drawLine(20f, y + 20f, 575f, y + 20f, linePaint)
            y += 28f
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Attendance_Report_${System.currentTimeMillis()}.pdf")
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

    /**
     * Dedicated Daily Breakdown Report PDF (گزارش کارنامه و تجمیع روزانه)
     * Lists each day with:
     * Date, Weekday, Gross Presence, Deductions (Teaching/Rest), Leave/Mission, Net Duty Presence, Target, Balance (Overtime/Deficit).
     */
    fun generateDailySummaryPdf(
        context: Context,
        workplaceTitle: String,
        periodTitle: String,
        dailyCalculations: List<com.example.viewmodel.AppViewModel.DayAttendanceCalculation>
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

        // Header Banner
        val headerPaint = Paint().apply {
            color = Color.rgb(2, 132, 199) // Light Blue / Ocean
        }
        canvas.drawRect(20f, 20f, 575f, 75f, headerPaint)

        paint.color = Color.WHITE
        paint.textSize = 17f
        canvas.drawText("کارنامه و گزارش تجمیعی کارکرد روزانه - $workplaceTitle", 30f, 52f, paint)

        // Summary Calculations
        val grandGross = dailyCalculations.sumOf { it.grossPresenceMinutes }
        val grandDeduction = dailyCalculations.sumOf { it.totalDeductionMinutes.toLong() }
        val grandLeaves = dailyCalculations.sumOf { it.leaveMinutes }
        val grandMissions = dailyCalculations.sumOf { it.missionMinutes }
        val grandEffective = dailyCalculations.sumOf { it.netEffectiveMinutes }
        val grandOvertime = dailyCalculations.sumOf { it.overtimeMinutes }
        val grandDeficit = dailyCalculations.sumOf { it.deficitMinutes }
        val workdaysCount = dailyCalculations.count { !it.isHoliday }
        val holidaysCount = dailyCalculations.count { it.isHoliday }

        val boxPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(20f, 85f, 575f, 165f, boxPaint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("• بازه گزارش: $periodTitle ($workdaysCount روز کاری  |  $holidaysCount روز تعطیل)", 30f, 105f, paint)
        canvas.drawText("• کل حضور ناخالص: ${grandGross / 60}س ${grandGross % 60}د", 340f, 105f, paint)

        paint.color = Color.rgb(180, 83, 9)
        canvas.drawText("• کسر تدریس، مطالعه و استراحت: ${grandDeduction / 60}س ${grandDeduction % 60}د", 30f, 125f, paint)
        paint.color = Color.rgb(13, 148, 136)
        canvas.drawText("• مرخصی و مأموریت: ${(grandLeaves + grandMissions) / 60}س ${(grandLeaves + grandMissions) % 60}د", 340f, 125f, paint)

        paint.color = Color.rgb(2, 132, 199)
        canvas.drawText("• خالص حضور موظفی مؤثر: ${grandEffective / 60}س ${grandEffective % 60}د", 30f, 145f, paint)
        paint.color = Color.rgb(16, 185, 129)
        canvas.drawText("• مجموع اضافه‌کاری: ${grandOvertime / 60}س ${grandOvertime % 60}د", 230f, 145f, paint)
        paint.color = Color.RED
        canvas.drawText("• مجموع کسری: ${grandDeficit / 60}س ${grandDeficit % 60}د", 410f, 145f, paint)

        // Table Header
        var y = 175f
        val tableHeaderPaint = Paint().apply { color = Color.rgb(226, 232, 240) }
        canvas.drawRect(20f, y, 575f, y + 22f, tableHeaderPaint)

        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("ردیف", 24f, y + 15f, paint)
        canvas.drawText("تاریخ و روز", 48f, y + 15f, paint)
        canvas.drawText("ورود اولیه", 120f, y + 15f, paint)
        canvas.drawText("خروج نهایی", 168f, y + 15f, paint)
        canvas.drawText("ساعت حضور", 222f, y + 15f, paint)
        canvas.drawText("کسری", 285f, y + 15f, paint)
        canvas.drawText("اضافه‌کار", 345f, y + 15f, paint)
        canvas.drawText("موظفی", 408f, y + 15f, paint)
        canvas.drawText("وضعیت نهایی", 465f, y + 15f, paint)

        y += 24f
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var rowIdx = 1
        paint.typeface = Typeface.DEFAULT
        for (item in dailyCalculations) {
            if (y > 720f) break

            paint.color = Color.DKGRAY
            paint.textSize = 8f
            canvas.drawText(rowIdx.toString(), 24f, y + 11f, paint)

            val dayLabel = "${item.dayOfWeekName} ${item.jalaliDate.takeLast(5)}"
            paint.color = if (item.isHoliday) Color.rgb(220, 38, 38) else Color.BLACK
            canvas.drawText(dayLabel, 48f, y + 11f, paint)

            paint.color = Color.DKGRAY
            canvas.drawText(item.firstCheckInTime, 120f, y + 11f, paint)
            canvas.drawText(item.lastCheckOutTime, 168f, y + 11f, paint)

            paint.color = Color.rgb(2, 132, 199)
            val effH = item.netEffectiveMinutes / 60
            val effM = item.netEffectiveMinutes % 60
            canvas.drawText("${effH}س ${effM}د", 222f, y + 11f, paint)

            // کسری
            if (item.deficitMinutes > 0) {
                paint.color = Color.RED
                canvas.drawText("${item.deficitMinutes / 60}س ${item.deficitMinutes % 60}د", 285f, y + 11f, paint)
            } else {
                paint.color = Color.GRAY
                canvas.drawText("-", 285f, y + 11f, paint)
            }

            // اضافه‌کار
            if (item.overtimeMinutes > 0) {
                paint.color = Color.rgb(16, 185, 129)
                canvas.drawText("${item.overtimeMinutes / 60}س ${item.overtimeMinutes % 60}د", 345f, y + 11f, paint)
            } else {
                paint.color = Color.GRAY
                canvas.drawText("-", 345f, y + 11f, paint)
            }

            paint.color = Color.DKGRAY
            val targetStr = if (item.isHoliday) "تعطیل" else "${item.targetDailyMinutes / 60}س"
            canvas.drawText(targetStr, 408f, y + 11f, paint)

            // وضعیت
            paint.color = when {
                item.isHoliday -> Color.rgb(220, 38, 38)
                item.overtimeMinutes > 0 -> Color.rgb(16, 185, 129)
                item.deficitMinutes > 0 -> Color.RED
                else -> Color.DKGRAY
            }
            val statusDisplay = if (item.statusText.length > 14) item.statusText.take(13) + ".." else item.statusText
            canvas.drawText(statusDisplay, 465f, y + 11f, paint)

            canvas.drawLine(20f, y + 15f, 575f, y + 15f, linePaint)
            y += 17f
            rowIdx++
        }

        // سطر آخر: جمع کل موارد در جدول
        val totalRowPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(20f, y, 575f, y + 22f, totalRowPaint)
        paint.color = Color.BLACK
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 8.5f
        canvas.drawText("جمع کل:", 48f, y + 15f, paint)
        canvas.drawText("-", 120f, y + 15f, paint)
        canvas.drawText("-", 168f, y + 15f, paint)

        paint.color = Color.rgb(2, 132, 199)
        canvas.drawText("${grandEffective / 60}س ${grandEffective % 60}د", 222f, y + 15f, paint)

        paint.color = Color.RED
        canvas.drawText("${grandDeficit / 60}س ${grandDeficit % 60}د", 285f, y + 15f, paint)

        paint.color = Color.rgb(16, 185, 129)
        canvas.drawText("${grandOvertime / 60}س ${grandOvertime % 60}د", 345f, y + 15f, paint)

        paint.color = Color.DKGRAY
        val grandTarget = dailyCalculations.sumOf { it.targetDailyMinutes }
        canvas.drawText("${grandTarget / 60}س", 408f, y + 15f, paint)

        val grandBalance = grandEffective - grandTarget
        paint.color = if (grandBalance >= 0) Color.rgb(16, 185, 129) else Color.RED
        canvas.drawText(if (grandBalance >= 0) "مازاد کل" else "کسری کل", 465f, y + 15f, paint)

        canvas.drawLine(20f, y + 22f, 575f, y + 22f, linePaint)

        // Official Signature Box
        y = 780f
        canvas.drawLine(20f, y, 575f, y, linePaint)
        paint.textSize = 10f
        paint.color = Color.BLACK
        canvas.drawText("مهر و امضای مسئول مربوطه", 80f, y + 25f, paint)
        canvas.drawText("امضای شخص گزارش‌دهنده", 360f, y + 25f, paint)

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Daily_Summary_Report_${System.currentTimeMillis()}.pdf")
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


    fun generateCombinedComprehensivePdf(
        context: Context,
        howzehPunches: List<PunchLogEntity>,
        mosquePunches: List<PunchLogEntity>,
        howzehActivities: List<ActivityLogEntity>,
        mosqueActivities: List<ActivityLogEntity>,
        howzehTeachingDeductionMins: Int = 0,
        howzehStudyDeductionMins: Int = 0,
        howzehRestDeductionMins: Int = 0,
        howzehLeavesMins: Long = 0L,
        howzehMissionsMins: Long = 0L
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Header
        val headerPaint = Paint().apply { color = Color.rgb(67, 56, 202) } // Indigo
        canvas.drawRect(20f, 20f, 575f, 75f, headerPaint)

        paint.color = Color.WHITE
        paint.textSize = 16f
        canvas.drawText("گزارش جامع تلفیقی عملکرد، حضور و غیاب و ریز فعالیت‌ها", 30f, 52f, paint)

        val howzehGrossMins = howzehPunches.sumOf { p ->
            val end = p.checkOutTime ?: System.currentTimeMillis()
            (end - p.checkInTime) / 60000
        }
        val mosqueMins = mosquePunches.sumOf { p ->
            val end = p.checkOutTime ?: System.currentTimeMillis()
            (end - p.checkInTime) / 60000
        }
        val howzehTotalDeductions = howzehTeachingDeductionMins + howzehStudyDeductionMins + howzehRestDeductionMins
        val howzehNetMins = maxOf(0L, howzehGrossMins - howzehTotalDeductions) + howzehLeavesMins + howzehMissionsMins

        // Summary Box
        val boxPaint = Paint().apply { color = Color.rgb(243, 244, 246) }
        canvas.drawRect(20f, 85f, 575f, 175f, boxPaint)

        paint.color = Color.BLACK
        paint.textSize = 11f
        canvas.drawText("• کل حضور فیزیکی در حوزه: ${howzehGrossMins / 60} ساعت و ${howzehGrossMins % 60} دقیقه", 30f, 110f, paint)
        canvas.drawText("• کسر تدریس، مطالعه و استراحت: ${howzehTotalDeductions / 60} ساعت و ${howzehTotalDeductions % 60} دقیقه", 300f, 110f, paint)
        paint.color = Color.rgb(67, 56, 202)
        canvas.drawText("• خالص موظفی مؤثر حوزه: ${howzehNetMins / 60} ساعت و ${howzehNetMins % 60} دقیقه", 30f, 135f, paint)
        paint.color = Color.rgb(16, 185, 129)
        canvas.drawText("• جمع کل حضور در مسجد: ${mosqueMins / 60} ساعت و ${mosqueMins % 60} دقیقه", 300f, 135f, paint)


        paint.color = Color.DKGRAY
        paint.textSize = 10f
        canvas.drawText("تعداد فعالیت‌های ثبت‌شده: ${howzehActivities.size + mosqueActivities.size} فعالیت  |  کل ترددها: ${howzehPunches.size + mosquePunches.size} نوبت", 30f, 160f, paint)

        // Attendance Table Header
        var y = 195f
        val tableHeaderPaint = Paint().apply { color = Color.rgb(226, 232, 240) }
        canvas.drawRect(20f, y, 575f, y + 22f, tableHeaderPaint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        canvas.drawText("خلاصه ترددهای ثبت‌شده (ورود و خروج)", 30f, y + 15f, paint)

        y += 26f
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val allPunches = (howzehPunches + mosquePunches).sortedByDescending { it.checkInTime }.take(8)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (p in allPunches) {
            val place = if (p.workplace == "HOWZEH") "حوزه" else "مسجد"
            val inStr = sdf.format(Date(p.checkInTime))
            val outStr = if (p.checkOutTime != null) sdf.format(Date(p.checkOutTime)) else "حاضر"
            val mins = if (p.checkOutTime != null) ((p.checkOutTime - p.checkInTime) / 60000) else 0L

            canvas.drawText("$place | ${p.jalaliDate} | ورود: $inStr خروج: $outStr | مدت: ${mins / 60}س ${mins % 60}د", 30f, y + 10f, paint)
            canvas.drawLine(20f, y + 15f, 575f, y + 15f, linePaint)
            y += 20f
        }

        // Activities Table Header
        y += 10f
        canvas.drawRect(20f, y, 575f, y + 22f, tableHeaderPaint)
        canvas.drawText("خلاصه ریز فعالیت‌های انجام‌شده", 30f, y + 15f, paint)

        y += 26f
        val allActs = (howzehActivities + mosqueActivities).sortedByDescending { it.startTime }.take(8)
        for (act in allActs) {
            val place = if (act.workplace == "HOWZEH") "حوزه" else "مسجد"
            val tTr = if (act.title.length > 25) act.title.take(23) + ".." else act.title
            canvas.drawText("$place | ${act.jalaliDate} | $tTr | ${act.categoryTag} | ${act.durationMinutes} دقیقه", 30f, y + 10f, paint)
            canvas.drawLine(20f, y + 15f, 575f, y + 15f, linePaint)
            y += 20f
        }

        // Official Signature Box
        y = 750f
        canvas.drawLine(20f, y, 575f, y, linePaint)
        paint.textSize = 10f
        canvas.drawText("مهر و امضای مسئول حوزه علمیه", 60f, y + 30f, paint)
        canvas.drawText("مهر و امضای امام جماعت / هیئت امنای مسجد", 330f, y + 30f, paint)

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Combined_Report_${System.currentTimeMillis()}.pdf")
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
