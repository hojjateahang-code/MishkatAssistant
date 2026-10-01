package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.util.JalaliCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PunchLogEntity::class,
        ActivityLogEntity::class,
        TaskReminderEntity::class,
        FinancialTransactionEntity::class,
        WorkplaceConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun punchDao(): PunchDao
    abstract fun activityDao(): ActivityDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun financialDao(): FinancialDao
    abstract fun workplaceConfigDao(): WorkplaceConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mishkat_assistant_db"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val todayStr = JalaliCalendar.getTodayJalali().toString()
            val now = System.currentTimeMillis()

            // Workplace configs
            db.workplaceConfigDao().insertOrUpdateConfig(
                WorkplaceConfigEntity(workplace = "HOWZEH", targetDailyMinutes = 240, targetWeeklyMinutes = 1200)
            )
            db.workplaceConfigDao().insertOrUpdateConfig(
                WorkplaceConfigEntity(workplace = "MOSQUE", targetDailyMinutes = 180, targetWeeklyMinutes = 900)
            )

            // Sample Punches
            val morningCheckIn = now - (5 * 3600 * 1000) // 5 hours ago
            val morningCheckOut = now - (1 * 3600 * 1000) // 1 hour ago
            db.punchDao().insertPunch(
                PunchLogEntity(
                    workplace = "HOWZEH",
                    checkInTime = morningCheckIn,
                    checkOutTime = morningCheckOut,
                    jalaliDate = todayStr,
                    note = "حضور در کلاس‌های بامداد و مباحثه فقه"
                )
            )

            // Sample Activities
            db.activityDao().insertActivity(
                ActivityLogEntity(
                    workplace = "MOSQUE",
                    title = "آماده‌سازی پوستر شهدا",
                    categoryTag = "#پوستر_و_تبلیغات",
                    startTime = now - (3 * 3600 * 1000),
                    endTime = now - (2 * 3600 * 1000),
                    durationMinutes = 60,
                    jalaliDate = todayStr,
                    notes = "طراحی پوستر گرافیکی یادواره شهدای محله جهت چاپ و نصب"
                )
            )

            db.activityDao().insertActivity(
                ActivityLogEntity(
                    workplace = "MOSQUE",
                    title = "برگزاری جلسه حلقه‌های صالحین و برنامه قرآنی",
                    categoryTag = "#برنامه‌های_قرآنی",
                    startTime = now - (2 * 3600 * 1000),
                    endTime = now - (30 * 60 * 1000),
                    durationMinutes = 90,
                    jalaliDate = todayStr,
                    notes = "تلاوت یک صفحه قرآن و بیان نکات تفسیری برای جوانان"
                )
            )

            db.activityDao().insertActivity(
                ActivityLogEntity(
                    workplace = "HOWZEH",
                    title = "تدریس درس اصول فقه و تصحیح اوراق",
                    categoryTag = "#تدریس",
                    startTime = now - (6 * 3600 * 1000),
                    endTime = now - (4 * 3600 * 1000),
                    durationMinutes = 120,
                    jalaliDate = todayStr,
                    notes = "تدریس مبحث امر و نهی برای طلاب پایه سوم"
                )
            )

            // Sample Task Reminders
            db.taskReminderDao().insertTask(
                TaskReminderEntity(
                    title = "پیگیری خرید سیستم صوت جدید مسجد",
                    description = "تماس با فروشگاه تجهیزات صوتی و دریافت پیش‌فاکتور",
                    workplace = "MOSQUE",
                    dueDate = now + (24 * 3600 * 1000),
                    jalaliDateStr = todayStr,
                    isCompleted = false,
                    priority = "HIGH",
                    categoryTag = "#تجهیزات"
                )
            )

            db.taskReminderDao().insertTask(
                TaskReminderEntity(
                    title = "تنظیم جزوه امتحانی درس کلام",
                    description = "جمع‌بندی مباحث نهایی برای طلاب حوزه",
                    workplace = "HOWZEH",
                    dueDate = now + (48 * 3600 * 1000),
                    jalaliDateStr = todayStr,
                    isCompleted = false,
                    priority = "MEDIUM",
                    categoryTag = "#آموزش"
                )
            )

            // Sample Financial Transactions
            // Howzeh Financial Ledger
            db.financialDao().insertTransaction(
                FinancialTransactionEntity(
                    workplace = "HOWZEH",
                    type = "INCOME",
                    amount = 35000000, // 35 Million Rials
                    category = "شهریه و حق‌الزحمه تدریس",
                    accountSource = "حساب سپه حوزه",
                    jalaliDate = todayStr,
                    description = "واریز حق‌الزحمه تدریس ماه جاری",
                    referenceNumber = "TRX-98213"
                )
            )

            db.financialDao().insertTransaction(
                FinancialTransactionEntity(
                    workplace = "HOWZEH",
                    type = "EXPENSE",
                    amount = 4500000,
                    category = "تهیه کتاب و منابع درسی",
                    accountSource = "صندوق شخصی",
                    jalaliDate = todayStr,
                    description = "خرید کتاب‌های جدید فقهی و منابع پژوهشی",
                    referenceNumber = "INV-3310"
                )
            )

            // Mosque Financial Ledger
            db.financialDao().insertTransaction(
                FinancialTransactionEntity(
                    workplace = "MOSQUE",
                    type = "INCOME",
                    amount = 120000000, // 120 Million Rials
                    category = "نذورات و کمک‌های خیرین",
                    accountSource = "صندوق بانیان مسجد",
                    jalaliDate = todayStr,
                    description = "کمک خیرین محترم برای مراسم میلاد پیامبر (ص)",
                    referenceNumber = "DONATION-441"
                )
            )

            db.financialDao().insertTransaction(
                FinancialTransactionEntity(
                    workplace = "MOSQUE",
                    type = "EXPENSE",
                    amount = 28000000,
                    category = "برنامه‌های فرهنگی و قرآنی",
                    accountSource = "حساب جاری مسجد",
                    jalaliDate = todayStr,
                    description = "خرید جوایز مسابقه قرآنی و پذیرایی برنامه‌ها",
                    referenceNumber = "REC-88219"
                )
            )
        }
    }
}
