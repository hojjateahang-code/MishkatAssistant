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

    @Query("SELECT * FROM punch_logs WHERE workplace = :workplace ORDER BY checkInTime DESC")
    fun getPunchLogsByWorkplace(workplace: String): Flow<List<PunchLogEntity>>

    @Query("SELECT * FROM punch_logs WHERE checkOutTime IS NULL LIMIT 1")
    fun getActivePunch(): Flow<PunchLogEntity?>

    @Query("SELECT * FROM punch_logs WHERE jalaliDate = :dateStr")
    fun getPunchesForDate(dateStr: String): Flow<List<PunchLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPunch(punch: PunchLogEntity): Long

    @Update
    suspend fun updatePunch(punch: PunchLogEntity)

    @Delete
    suspend fun deletePunch(punch: PunchLogEntity)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_logs ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE workplace = :workplace ORDER BY startTime DESC")
    fun getActivitiesByWorkplace(workplace: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE jalaliDate = :dateStr")
    fun getActivitiesForDate(dateStr: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityLogEntity): Long

    @Delete
    suspend fun deleteActivity(activity: ActivityLogEntity)
}

@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM task_reminders ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders WHERE jalaliDateStr = :dateStr ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksForDate(dateStr: String): Flow<List<TaskReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskReminderEntity): Long

    @Update
    suspend fun updateTask(task: TaskReminderEntity)

    @Delete
    suspend fun deleteTask(task: TaskReminderEntity)
}

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE workplace = :workplace ORDER BY timestamp DESC")
    fun getTransactionsByWorkplace(workplace: String): Flow<List<FinancialTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: FinancialTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: FinancialTransactionEntity)
}

@Dao
interface WorkplaceConfigDao {
    @Query("SELECT * FROM workplace_configs")
    fun getAllConfigs(): Flow<List<WorkplaceConfigEntity>>

    @Query("SELECT * FROM workplace_configs WHERE workplace = :workplace LIMIT 1")
    suspend fun getConfig(workplace: String): WorkplaceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: WorkplaceConfigEntity)
}
