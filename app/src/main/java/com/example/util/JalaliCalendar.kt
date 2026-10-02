package com.example.util

import java.util.Calendar

/**
 * Jalali (Persian/Shamsi) Calendar Conversion and Helper Utilities.
 */
object JalaliCalendar {

    data class JalaliDate(
        val year: Int,
        val month: Int, // 1 to 12
        val day: Int    // 1 to 31
    ) {
        override fun toString(): String {
            return String.format("%04d/%02d/%02d", year, month, day)
        }

        fun toPersianDigits(): String {
            return JalaliCalendar.toPersianDigits(toString())
        }

        fun getMonthName(): String {
            return monthNames.getOrElse(month - 1) { "" }
        }
    }

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val weekDays = listOf(
        "شنبه", "یکشنبه", "دوشنبه",
        "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun fromPersianDigits(input: String): String {
        var result = input
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in 0..9) {
            result = result.replace(persianDigits[i], englishDigits[i])
        }
        return result
    }

    fun getTodayJalali(): JalaliDate {
        val calendar = Calendar.getInstance()
        return gregorianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun millisToJalaliDate(millis: Long): JalaliDate {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return gregorianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isLeapJalaliYear(year: Int): Boolean {
        val matches = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)
        val mod = year % 33
        return matches.contains(mod)
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapJalaliYear(year)) 30 else 29
            else -> 30
        }
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gyNum = gy
        if (gyNum > 1600) {
            gyNum -= 1600
        }

        var gDayNo = 365 * gyNum + ((gyNum + 3) / 4) - ((gyNum + 99) / 100) + ((gyNum + 399) / 400)
        for (i in 1 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd - 1

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i + 1]) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= jDaysInMonth[i + 1]
        }

        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Calendar {
        var jyNum = jy - 979
        var jDayNo = 365 * jyNum + (jyNum / 33) * 8 + ((jyNum % 33) + 3) / 4

        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (i in 1 until jm) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd - 1

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gDayNo >= gDaysInMonth[gm]) {
            gDayNo -= gDaysInMonth[gm]
            gm++
        }

        val calendar = Calendar.getInstance()
        calendar.set(gy, gm, gDayNo + 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    /**
     * Gets day of week for a Jalali Date (0 = Saturday, 1 = Sunday, ..., 6 = Friday).
     */
    fun getDayOfWeek(jalaliDate: JalaliDate): Int {
        val cal = jalaliToGregorian(jalaliDate.year, jalaliDate.month, jalaliDate.day)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7
        return when (dayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    data class HijriDate(
        val year: Int,
        val month: Int, // 1 to 12
        val day: Int    // 1 to 30
    ) {
        fun getMonthName(): String = islamicMonthNames.getOrElse(month - 1) { "" }
        fun toPersianDigits(): String = JalaliCalendar.toPersianDigits("$day ${getMonthName()} $year")
        fun toShortPersianDigits(): String = JalaliCalendar.toPersianDigits("$day ${getMonthName()}")
        override fun toString(): String = String.format("%04d/%02d/%02d", year, month, day)
    }

    val islamicMonthNames = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
        "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    fun gregorianToJulianDay(gy: Int, gm: Int, gd: Int): Double {
        var y = gy
        var m = gm
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = (y / 100).toDouble()
        val b = 2 - a.toInt() + (a / 4).toInt()
        return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + gd + b - 1524.5
    }

    fun julianDayToHijri(jd: Double): HijriDate {
        val z = (jd + 0.5).toInt()
        val l = z - 1948440 + 10632
        val n = (l - 1) / 10631
        val l1 = l - 10631 * n + 354
        val j = ((10985 - l1) / 5316) * ((50 * l1) / 17719) + (l1 / 5670) * ((43 * l1) / 15238)
        val l2 = l1 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * l2) / 709
        val d = l2 - (709 * m) / 24
        val y = 30 * n + j - 30
        return HijriDate(y, m.coerceIn(1, 12), d.coerceIn(1, 30))
    }

    fun jalaliToHijri(jalaliDate: JalaliDate): HijriDate {
        val cal = jalaliToGregorian(jalaliDate.year, jalaliDate.month, jalaliDate.day)
        val jd = gregorianToJulianDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        return julianDayToHijri(jd)
    }
}
