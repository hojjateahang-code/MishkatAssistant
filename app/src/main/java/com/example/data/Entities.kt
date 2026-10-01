package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "punch_logs")
data class PunchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workplace: String, // "HOWZEH" or "MOSQUE"
    val checkInTime: Long, // epoch millis
    val checkOutTime: Long? = null, // epoch millis
    val jalaliDate: String, // e.g., "1405/07/03"
    val note: String? = null
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workplace: String, // "HOWZEH" or "MOSQUE"
    val title: String, // e.g., "آماده‌سازی پوستر شهدا"
    val categoryTag: String, // e.g., "#برنامه‌های_قرآنی", "#امور_فرهنگی", "#تدریس", "#پوستر_و_تبلیغات"
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val jalaliDate: String,
    val notes: String = ""
)

@Entity(tableName = "task_reminders")
data class TaskReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val workplace: String = "ALL", // "HOWZEH", "MOSQUE", or "ALL"
    val dueDate: Long,
    val jalaliDateStr: String,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val categoryTag: String = "#عمومی"
)

@Entity(tableName = "financial_transactions")
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workplace: String, // "HOWZEH" or "MOSQUE"
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Long, // in Rials
    val category: String, // e.g., "حق‌الزحمه", "خرید تجهیزات", "نذورات و کمک‌های مردمی", "قبوض", "برنامه‌های فرهنگی"
    val accountSource: String = "صندوق اصلی",
    val jalaliDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val attachmentPath: String? = null, // Image URI or local file path
    val referenceNumber: String? = null // شماره پیگیری / شماره سند
)

@Entity(tableName = "workplace_configs")
data class WorkplaceConfigEntity(
    @PrimaryKey val workplace: String, // "HOWZEH" or "MOSQUE"
    val targetDailyMinutes: Int = 240, // Default 4 hours per day
    val targetWeeklyMinutes: Int = 1200 // Default 20 hours per week
)
