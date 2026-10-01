package com.example.util

data class Occasion(
    val month: Int, // 1..12
    val day: Int,   // 1..31
    val title: String,
    val isHoliday: Boolean = false,
    val isReligious: Boolean = false
)

object OccasionsDatabase {

    val occasions = listOf(
        // فروردین
        Occasion(1, 1, "عید نوروز - آغاز سال نو", isHoliday = true),
        Occasion(1, 2, "عید نوروز", isHoliday = true),
        Occasion(1, 3, "عید نوروز", isHoliday = true),
        Occasion(1, 4, "عید نوروز", isHoliday = true),
        Occasion(1, 12, "روز جمهوری اسلامی ایران", isHoliday = true),
        Occasion(1, 13, "روز طبیعت (سیزده بدر)", isHoliday = true),
        Occasion(1, 18, "روز سلامتی و بهداشت"),

        // اردیبهشت
        Occasion(2, 2, "تاسیس سپاه پاسداران انقلاب اسلامی"),
        Occasion(2, 9, "روز شوراها"),
        Occasion(2, 11, "روز جهانی کار و کارگر"),
        Occasion(2, 12, "روز معلم و شهادت استاد مرتضی مطهری"),
        Occasion(2, 24, "لغو امتیاز تنباکو به فتوی آیت‌الله شیرازی"),

        // خرداد
        Occasion(3, 3, "فتح خرمشهر - روز مقاومت، ایثار و پیروزی"),
        Occasion(3, 14, "رحلت امام خمینی (ره)", isHoliday = true),
        Occasion(3, 15, "قیام خونین ۱۵ خرداد", isHoliday = true),
        Occasion(3, 27, "تشکیل جهاد سازندگی"),

        // تیر
        Occasion(4, 1, "روز اصناف"),
        Occasion(4, 7, "شهادت آیت‌الله دکتر بهشتی و ۷۲ تن از یاران امام"),
        Occasion(4, 8, "روز مبارزه با سلاح‌های شیمیایی و میکروبی"),
        Occasion(4, 11, "شهادت آیت‌الله صدوقی (چهارمین شهید محراب)"),
        Occasion(4, 21, "روز عفاف و حجاب"),

        // مرداد
        Occasion(5, 5, "سالروز اقامه اولین نماز جمعه پس از پیروزی انقلاب"),
        Occasion(5, 8, "روز بزرگداشت شیخ شهاب‌الدین سهروردی"),
        Occasion(5, 14, "صدور فرمان مشروطیت"),
        Occasion(5, 26, "سالروز ورود آزادگان سرافراز به میهن"),

        // شهریور
        Occasion(6, 1, "روز پزشک - بزرگداشت ابن سینا"),
        Occasion(6, 2, "آغاز هفته دولت"),
        Occasion(6, 8, "شهادت شهیدان رجایی و باهنر - روز مبارزه با تروریسم"),
        Occasion(6, 17, "قیام ۱۷ شهریور"),
        Occasion(6, 27, "روز شعر و ادب فارسی - بزرگداشت شهریار"),
        Occasion(6, 31, "آغاز هفته دفاع مقدس"),

        // مهر
        Occasion(7, 1, "آغاز سال تحصیلی و زنگ مهر"),
        Occasion(7, 7, "روز آتش‌نشانی و ایمنی"),
        Occasion(7, 8, "روز بزرگداشت مولوی"),
        Occasion(7, 13, "روز نیروی انتظامی"),
        Occasion(7, 20, "روز بزرگداشت حافظ"),

        // آبان
        Occasion(8, 8, "روز نوجوان و بسیج دانش‌آموزی - شهادت محمدحسین فهمیده"),
        Occasion(8, 13, "روز ملی مبارزه با استکبار جهانی - روز دانش‌آموز"),
        Occasion(8, 24, "روز کتاب، کتابخوانی و کتابدار - بزرگداشت علامه طباطبایی"),

        // آذر
        Occasion(9, 5, "روز بسیج مستضعفان"),
        Occasion(9, 7, "روز نیروی دریایی - شهادت دکتر شهریاری"),
        Occasion(9, 9, "روز آذر - روز پرستار و بهورز"),
        Occasion(9, 10, "روز مجلس - شهادت آیت‌الله مدرس"),
        Occasion(9, 16, "روز دانشجو"),
        Occasion(9, 27, "روز وحدت حوزه و دانشگاه - شهادت آیت‌الله مفتح"),
        Occasion(9, 30, "شب یلدا"),

        // دی
        Occasion(10, 9, "روز بصیرت و میثاق امت با ولایت"),
        Occasion(10, 13, "شهادت سردار سپهبد حاج قاسم سلیمانی - روز جهانی مقاومت"),
        Occasion(10, 20, "شهادت میرزا تقی‌خان امیرکبیر"),
        Occasion(10, 26, "فرار شاه خائن از ایران"),

        // بهمن
        Occasion(11, 12, "بازگشت امام خمینی (ره) به میهن و آغاز دهه فجر"),
        Occasion(11, 19, "روز نیروی هوایی"),
        Occasion(11, 22, "پیروزی انقلاب اسلامی ایران و سقوط رژیم پهلوی", isHoliday = true),
        Occasion(11, 29, "قیام مردم تبریز"),

        // اسفند
        Occasion(12, 14, "روز احسان و نیکوکاری - روز درختکاری"),
        Occasion(12, 15, "روز درختکاری و هفته منابع طبیعی"),
        Occasion(12, 22, "روز بزرگداشت شهدا - تاسیس بنیاد شهید"),
        Occasion(12, 29, "روز ملی شدن صنعت نفت ایران", isHoliday = true),

        // مناسبت‌های مذهبی شاخص (تقریبی/مناسبت شناور در سال)
        Occasion(1, 10, "مبعث حضرت رسول اکرم (ص)", isReligious = true, isHoliday = true),
        Occasion(1, 15, "ولادت حضرت قائم (عج) - نیمه شعبان", isReligious = true, isHoliday = true),
        Occasion(3, 21, "شهادت حضرت علی (ع) - لیله القدر", isReligious = true, isHoliday = true),
        Occasion(4, 1, "عید سعید فطر", isReligious = true, isHoliday = true),
        Occasion(4, 25, "شهادت امام جعفر صادق (ع)", isReligious = true, isHoliday = true),
        Occasion(6, 10, "عید سعید قربان", isReligious = true, isHoliday = true),
        Occasion(6, 18, "عید سعید غدیر خم", isReligious = true, isHoliday = true),
        Occasion(7, 9, "تاسوعای حسینی", isReligious = true, isHoliday = true),
        Occasion(7, 10, "عاشورای حسینی", isReligious = true, isHoliday = true),
        Occasion(8, 20, "اربعین حسینی", isReligious = true, isHoliday = true),
        Occasion(8, 28, "رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isReligious = true, isHoliday = true),
        Occasion(8, 30, "شهادت امام رضا (ع)", isReligious = true, isHoliday = true),
        Occasion(12, 3, "شهادت حضرت فاطمه زهرا (س)", isReligious = true, isHoliday = true),
        Occasion(12, 13, "ولادت حضرت علی (ع) - روز پدر", isReligious = true, isHoliday = true)
    )

    fun getOccasionsForDate(month: Int, day: Int): List<Occasion> {
        return occasions.filter { it.month == month && it.day == day }
    }
}