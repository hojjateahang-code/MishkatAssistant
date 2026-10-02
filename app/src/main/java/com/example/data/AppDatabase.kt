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
        WorkplaceConfigEntity::class,
        UserProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun punchDao(): PunchDao
    abstract fun activityDao(): ActivityDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun financialDao(): FinancialDao
    abstract fun workplaceConfigDao(): WorkplaceConfigDao
    abstract fun userProfileDao(): UserProfileDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
            // Workplace configs default (4 hours / 240 mins for Howzeh, 3 hours / 180 mins for Mosque)
            db.workplaceConfigDao().insertOrUpdateConfig(
                WorkplaceConfigEntity(workplace = "HOWZEH", targetDailyMinutes = 240, targetWeeklyMinutes = 1200)
            )
            db.workplaceConfigDao().insertOrUpdateConfig(
                WorkplaceConfigEntity(workplace = "MOSQUE", targetDailyMinutes = 180, targetWeeklyMinutes = 900)
            )

            // User Profile - starts with clean empty fields (default zero data)
            db.userProfileDao().insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    fullName = "",
                    roleTitle = "",
                    phoneNumber = "",
                    email = "",
                    notificationsEnabled = true,
                    autoSyncEnabled = true
                )
            )

            // All activity logs, punches, financial transactions, and reminders start at clean zero!
        }
    }
}
