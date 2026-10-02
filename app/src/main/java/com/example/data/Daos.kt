package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PunchDao {
    @Query("SELECT * FROM punch_logs ORDER BY checkInTime DESC")
    fun getAllPunchLogs(): Flow<List<PunchLogEntity>>

    @Query("SELECT * FROM punch_logs ORDER BY checkInTime DESC")
    suspend fun getAllPunchLogsOnce(): List<PunchLogEntity>

    @Query("SELECT * FROM punch_logs WHERE workplace = :workplace ORDER BY checkInTime DESC")
    fun getPunchLogsByWorkplace(workplace: String): Flow<List<PunchLogEntity>>

    @Query("SELECT * FROM punch_logs WHERE checkOutTime IS NULL LIMIT 1")
    fun getActivePunch(): Flow<PunchLogEntity?>

    @Query("SELECT * FROM punch_logs WHERE jalaliDate = :dateStr")
    fun getPunchesForDate(dateStr: String): Flow<List<PunchLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPunch(punch: PunchLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(punches: List<PunchLogEntity>)

    @Update
    suspend fun updatePunch(punch: PunchLogEntity)

    @Delete
    suspend fun deletePunch(punch: PunchLogEntity)

    @Query("DELETE FROM punch_logs")
    suspend fun clearAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_logs ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY startTime DESC")
    suspend fun getAllActivitiesOnce(): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_logs WHERE workplace = :workplace ORDER BY startTime DESC")
    fun getActivitiesByWorkplace(workplace: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE jalaliDate = :dateStr")
    fun getActivitiesForDate(dateStr: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityLogEntity>)

    @Delete
    suspend fun deleteActivity(activity: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()
}

@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM task_reminders ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders ORDER BY isCompleted ASC, dueDate ASC")
    suspend fun getAllTasksOnce(): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE jalaliDateStr = :dateStr ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksForDate(dateStr: String): Flow<List<TaskReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskReminderEntity>)

    @Update
    suspend fun updateTask(task: TaskReminderEntity)

    @Delete
    suspend fun deleteTask(task: TaskReminderEntity)

    @Query("DELETE FROM task_reminders")
    suspend fun clearAll()
}

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<FinancialTransactionEntity>

    @Query("SELECT * FROM financial_transactions WHERE workplace = :workplace ORDER BY timestamp DESC")
    fun getTransactionsByWorkplace(workplace: String): Flow<List<FinancialTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<FinancialTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: FinancialTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: FinancialTransactionEntity)

    @Query("DELETE FROM financial_transactions")
    suspend fun clearAll()
}

@Dao
interface WorkplaceConfigDao {
    @Query("SELECT * FROM workplace_configs")
    fun getAllConfigs(): Flow<List<WorkplaceConfigEntity>>

    @Query("SELECT * FROM workplace_configs")
    suspend fun getAllConfigsOnce(): List<WorkplaceConfigEntity>

    @Query("SELECT * FROM workplace_configs WHERE workplace = :workplace LIMIT 1")
    suspend fun getConfig(workplace: String): WorkplaceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: WorkplaceConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<WorkplaceConfigEntity>)

    @Query("DELETE FROM workplace_configs")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}
