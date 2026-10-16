package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAssistantService
import com.example.data.ActivityLogEntity
import com.example.data.AppDatabase
import com.example.data.FinancialTransactionEntity
import com.example.data.PunchLogEntity
import com.example.data.TaskReminderEntity
import com.example.data.UserProfileEntity
import com.example.data.WorkplaceConfigEntity
import com.example.sync.BackupPackage
import com.example.sync.MinioSyncClient
import com.example.util.JalaliCalendar
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveTimerState(
    val workplace: String = "HOWZEH",
    val title: String = "",
    val categoryTag: String = "#عمومی",
    val startTime: Long = 0L,
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val punchDao = db.punchDao()
    private val activityDao = db.activityDao()
    private val taskDao = db.taskReminderDao()
    private val financialDao = db.financialDao()
    private val configDao = db.workplaceConfigDao()
    private val profileDao = db.userProfileDao()
    private val noteDao = db.noteDao()
    private val leaveDao = db.leaveDao()
    private val holidayDao = db.holidayDao()
    private val missionDao = db.missionDao()

    val allPunches = punchDao.getAllPunchLogs().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activePunch = punchDao.getActivePunch().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val allActivities = activityDao.getAllActivities().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allTasks = taskDao.getAllTasks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allTransactions = financialDao.getAllTransactions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val workplaceConfigs = configDao.getAllConfigs().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val userProfile = profileDao.getUserProfile().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val allNotes = noteDao.getAllNotes().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allLeaves = leaveDao.getAllLeaves().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allHolidays = holidayDao.getAllHolidays().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allMissions = missionDao.getAllMissions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )


    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage = _syncStatusMessage.asStateFlow()

    private var autoSyncJob: Job? = null

    fun triggerAutoCloudSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(3000) // 3 seconds debounce
            try {
                val punches = punchDao.getAllPunchLogsOnce()
                val activities = activityDao.getAllActivitiesOnce()
                val tasks = taskDao.getAllTasksOnce()
                val transactions = financialDao.getAllTransactionsOnce()
                val configs = configDao.getAllConfigsOnce()
                val profile = profileDao.getUserProfileOnce()
                val notes = noteDao.getAllNotesOnce()
                val leaves = leaveDao.getAllLeavesOnce()
                val holidays = holidayDao.getAllHolidaysOnce()
                val missions = missionDao.getAllMissionsOnce()

                val backup = BackupPackage(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                    punches = punches,
                    activities = activities,
                    tasks = tasks,
                    transactions = transactions,
                    configs = configs,
                    profile = profile,
                    notes = notes,
                    leaves = leaves,
                    holidays = holidays,
                    missions = missions
                )
                MinioSyncClient.uploadBackup(backup.toJsonString())

            } catch (e: Exception) {
                // Silently ignore background periodic upload
            }
        }
    }

    init {
        com.example.sync.ServerConfigManager.init(application)

        // Initial boot sync & connection check
        viewModelScope.launch {
            try {
                delay(1500) // slight delay for DB init
                _syncStatusMessage.value = "اتصال و بررسی سرور MinIO..."
                val downloadResult = MinioSyncClient.downloadLatestBackup()
                if (downloadResult.isSuccess) {
                    val jsonStr = downloadResult.getOrThrow()
                    val backup = BackupPackage.fromJsonString(jsonStr)
                    val punchesCount = punchDao.getAllPunchLogsOnce().size
                    val txCount = financialDao.getAllTransactionsOnce().size
                    val actCount = activityDao.getAllActivitiesOnce().size

                    if (punchesCount == 0 && txCount == 0 && actCount == 0) {
                        if (backup.punches.isNotEmpty() || backup.transactions.isNotEmpty() || backup.activities.isNotEmpty()) {
                            punchDao.insertAll(backup.punches)
                            activityDao.insertAll(backup.activities)
                            taskDao.insertAll(backup.tasks)
                            financialDao.insertAll(backup.transactions)
                            if (backup.configs.isNotEmpty()) configDao.insertAll(backup.configs)
                            backup.profile?.let { profileDao.insertOrUpdateProfile(it) }
                            _syncStatusMessage.value = "داده‌ها با موفقیت از سرور MinIO بازیابی شدند."
                        } else {
                            _syncStatusMessage.value = "اتصال برقرار شد - سرور آماده همگام‌سازی است."
                        }
                    } else {
                        _syncStatusMessage.value = "سرور ابری MinIO متصل و آماده است."
                    }
                } else {
                    _syncStatusMessage.value = "حالت آفلاین فعال است (سرور MinIO موقتاً در دسترس نیست)."
                }
            } catch (e: Exception) {
                _syncStatusMessage.value = "حالت آفلاین محلی فعال است."
            }

            // Periodic auto-sync loop (every 5 minutes)
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes
                try {
                    val punches = punchDao.getAllPunchLogsOnce()
                    val activities = activityDao.getAllActivitiesOnce()
                    val tasks = taskDao.getAllTasksOnce()
                    val transactions = financialDao.getAllTransactionsOnce()
                    val configs = configDao.getAllConfigsOnce()
                    val profile = profileDao.getUserProfileOnce()
                    val notes = noteDao.getAllNotesOnce()
                    val leaves = leaveDao.getAllLeavesOnce()
                    val holidays = holidayDao.getAllHolidaysOnce()
                    val missions = missionDao.getAllMissionsOnce()

                    if (punches.isNotEmpty() || activities.isNotEmpty() || transactions.isNotEmpty() || notes.isNotEmpty() || leaves.isNotEmpty() || missions.isNotEmpty()) {
                        val backup = BackupPackage(
                            version = 1,
                            timestamp = System.currentTimeMillis(),
                            jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                            punches = punches,
                            activities = activities,
                            tasks = tasks,
                            transactions = transactions,
                            configs = configs,
                            profile = profile,
                            notes = notes,
                            leaves = leaves,
                            holidays = holidays,
                            missions = missions
                        )
                        MinioSyncClient.uploadBackup(backup.toJsonString())
                    }

                } catch (e: Exception) {
                    // Silently ignore background periodic network issues
                }
            }
        }
    }

    // Selected Calendar Date
    private val _selectedDate = MutableStateFlow(JalaliCalendar.getTodayJalali())
    val selectedDate = _selectedDate.asStateFlow()

    fun setSelectedDate(date: JalaliCalendar.JalaliDate) {
        _selectedDate.value = date
    }

    // Active Activity Stopwatch State
    private val _activeTimerState = MutableStateFlow(ActiveTimerState())
    val activeTimerState = _activeTimerState.asStateFlow()

    private var timerJob: Job? = null

    fun startLiveTaskTimer(workplace: String, title: String, tag: String) {
        val now = System.currentTimeMillis()
        _activeTimerState.value = ActiveTimerState(
            workplace = workplace,
            title = title,
            categoryTag = tag,
            startTime = now,
            isRunning = true,
            elapsedSeconds = 0L
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_activeTimerState.value.isRunning) {
                delay(1000)
                _activeTimerState.value = _activeTimerState.value.copy(
                    elapsedSeconds = (System.currentTimeMillis() - _activeTimerState.value.startTime) / 1000
                )
            }
        }
    }

    fun stopAndSaveLiveTaskTimer(notes: String = "") {
        val state = _activeTimerState.value
        if (!state.isRunning) return

        val endTime = System.currentTimeMillis()
        val durationMins = ((endTime - state.startTime) / 60000).toInt().coerceAtLeast(1)
        val todayStr = JalaliCalendar.getTodayJalali().toString()

        viewModelScope.launch {
            activityDao.insertActivity(
                ActivityLogEntity(
                    workplace = state.workplace,
                    title = state.title.ifBlank { "فعالیت ثبت شده" },
                    categoryTag = state.categoryTag,
                    startTime = state.startTime,
                    endTime = endTime,
                    durationMinutes = durationMins,
                    jalaliDate = todayStr,
                    notes = notes
                )
            )
            _activeTimerState.value = ActiveTimerState()
            timerJob?.cancel()
            triggerAutoCloudSync()
        }
    }

    fun discardLiveTaskTimer() {
        _activeTimerState.value = ActiveTimerState()
        timerJob?.cancel()
    }

    // Punch Clock Actions
    fun checkIn(workplace: String, note: String = "") {
        val now = System.currentTimeMillis()
        val todayStr = JalaliCalendar.getTodayJalali().toString()
        viewModelScope.launch {
            punchDao.insertPunch(
                PunchLogEntity(
                    workplace = workplace,
                    checkInTime = now,
                    checkOutTime = null,
                    jalaliDate = todayStr,
                    note = note
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun checkOut(activePunchLog: PunchLogEntity, note: String = "") {
        val now = System.currentTimeMillis()
        val updatedNote = if (note.isBlank()) activePunchLog.note else note
        viewModelScope.launch {
            punchDao.updatePunch(
                activePunchLog.copy(
                    checkOutTime = now,
                    note = updatedNote
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun updatePunchNote(punch: PunchLogEntity, newNote: String) {
        viewModelScope.launch {
            punchDao.updatePunch(punch.copy(note = newNote.trim()))
            triggerAutoCloudSync()
        }
    }

    fun updatePunch(punch: PunchLogEntity) {
        viewModelScope.launch {
            punchDao.updatePunch(punch)
            triggerAutoCloudSync()
        }
    }

    fun addManualPunch(
        workplace: String,
        checkInTime: Long,
        checkOutTime: Long,
        jalaliDate: String,
        note: String
    ) {
        viewModelScope.launch {
            punchDao.insertPunch(
                PunchLogEntity(
                    workplace = workplace,
                    checkInTime = checkInTime,
                    checkOutTime = checkOutTime,
                    jalaliDate = jalaliDate,
                    note = note
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun deletePunch(punch: PunchLogEntity) {
        viewModelScope.launch {
            punchDao.deletePunch(punch)
            triggerAutoCloudSync()
        }
    }

    // Activity Actions
    fun addActivity(
        workplace: String,
        title: String,
        categoryTag: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
        jalaliDate: String,
        notes: String
    ) {
        viewModelScope.launch {
            activityDao.insertActivity(
                ActivityLogEntity(
                    workplace = workplace,
                    title = title,
                    categoryTag = categoryTag,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    jalaliDate = jalaliDate,
                    notes = notes
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun updateActivity(activity: ActivityLogEntity) {
        viewModelScope.launch {
            activityDao.insertActivity(activity) // OnConflict = REPLACE
            triggerAutoCloudSync()
        }
    }

    fun deleteActivity(activity: ActivityLogEntity) {
        viewModelScope.launch {
            activityDao.deleteActivity(activity)
            triggerAutoCloudSync()
        }
    }

    // Task & Reminder & Event Actions
    fun addTaskReminder(
        title: String,
        description: String,
        workplace: String,
        dueDate: Long,
        jalaliDateStr: String,
        priority: String,
        categoryTag: String,
        earlyReminderHours: Int = 0,
        isEvent: Boolean = false
    ) {
        viewModelScope.launch {
            val id = taskDao.insertTask(
                com.example.data.TaskReminderEntity(
                    title = title,
                    description = description,
                    workplace = workplace,
                    dueDate = dueDate,
                    jalaliDateStr = jalaliDateStr,
                    isCompleted = false,
                    priority = priority,
                    categoryTag = categoryTag,
                    earlyReminderHours = earlyReminderHours,
                    isEvent = isEvent
                )
            )
            if (!isEvent) {
                NotificationHelper.scheduleReminderWithEarlyAlert(
                    getApplication(),
                    id.toInt(),
                    "یادآور کار: $title",
                    description.ifBlank { "موعد رسیدگی به $title فرا رسیده است." },
                    dueDate,
                    earlyReminderHours
                )
            }
            triggerAutoCloudSync()
        }
    }

    fun updateTaskReminder(task: com.example.data.TaskReminderEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
            if (!task.isEvent) {
                NotificationHelper.scheduleReminderWithEarlyAlert(
                    getApplication(),
                    task.id,
                    "یادآور کار: ${task.title}",
                    task.description.ifBlank { "موعد رسیدگی به ${task.title} فرا رسیده است." },
                    task.dueDate,
                    task.earlyReminderHours
                )
            }
            triggerAutoCloudSync()
        }
    }

    fun toggleTaskCompletion(task: com.example.data.TaskReminderEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
            triggerAutoCloudSync()
        }
    }

    fun deleteTask(task: com.example.data.TaskReminderEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            NotificationHelper.cancelReminderWithEarlyAlert(getApplication(), task.id)
            triggerAutoCloudSync()
        }
    }

    // Note Actions
    fun addNote(
        title: String,
        content: String,
        folder: String = "عمومی",
        jalaliDate: String? = null,
        occasionTag: String = "",
        categoryTag: String = "#یادداشت"
    ) {
        viewModelScope.launch {
            noteDao.insertNote(
                com.example.data.NoteEntity(
                    title = title,
                    content = content,
                    folder = folder,
                    jalaliDate = jalaliDate,
                    occasionTag = occasionTag,
                    categoryTag = categoryTag,
                    updatedAt = System.currentTimeMillis()
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun updateNote(note: com.example.data.NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            triggerAutoCloudSync()
        }
    }

    fun deleteNote(note: com.example.data.NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            triggerAutoCloudSync()
        }
    }

    // Leave Actions
    fun addLeave(
        workplace: String,
        leaveType: String,
        jalaliDate: String,
        startHour: Int = 8,
        startMinute: Int = 0,
        endHour: Int = 12,
        endMinute: Int = 0,
        durationMinutes: Int = 240,
        reason: String = ""
    ) {
        viewModelScope.launch {
            leaveDao.insertLeave(
                com.example.data.LeaveEntity(
                    workplace = workplace,
                    leaveType = leaveType,
                    jalaliDate = jalaliDate,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    durationMinutes = durationMinutes,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun updateLeave(leave: com.example.data.LeaveEntity) {
        viewModelScope.launch {
            leaveDao.updateLeave(leave)
            triggerAutoCloudSync()
        }
    }

    fun deleteLeave(leave: com.example.data.LeaveEntity) {
        viewModelScope.launch {
            leaveDao.deleteLeave(leave)
            triggerAutoCloudSync()
        }
    }

    // Holiday Actions
    fun addHoliday(jalaliDate: String, title: String = "تعطیل رسمی") {
        viewModelScope.launch {
            holidayDao.insertHoliday(
                com.example.data.HolidayEntity(
                    jalaliDate = jalaliDate,
                    title = title,
                    isOffDay = true
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun deleteHoliday(holiday: com.example.data.HolidayEntity) {
        viewModelScope.launch {
            holidayDao.deleteHoliday(holiday)
            triggerAutoCloudSync()
        }
    }

    // Mission Actions
    fun addMission(
        workplace: String,
        title: String,
        jalaliDate: String,
        startHour: Int = 8,
        startMinute: Int = 0,
        endHour: Int = 12,
        endMinute: Int = 0,
        durationMinutes: Int = 240,
        description: String = ""
    ) {
        viewModelScope.launch {
            missionDao.insertMission(
                com.example.data.MissionEntity(
                    workplace = workplace,
                    title = title,
                    jalaliDate = jalaliDate,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    durationMinutes = durationMinutes,
                    description = description,
                    timestamp = System.currentTimeMillis()
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun updateMission(mission: com.example.data.MissionEntity) {
        viewModelScope.launch {
            missionDao.updateMission(mission)
            triggerAutoCloudSync()
        }
    }

    fun deleteMission(mission: com.example.data.MissionEntity) {
        viewModelScope.launch {
            missionDao.deleteMission(mission)
            triggerAutoCloudSync()
        }
    }


    // Financial Actions
    fun addFinancialTransaction(
        workplace: String,
        type: String,
        amount: Long,
        category: String,
        accountSource: String,
        jalaliDate: String,
        description: String,
        attachmentPath: String?,
        referenceNumber: String?,
        partyName: String = "",
        isVerified: Boolean = false
    ) {
        viewModelScope.launch {
            financialDao.insertTransaction(
                FinancialTransactionEntity(
                    workplace = workplace,
                    type = type,
                    amount = amount,
                    category = category,
                    accountSource = accountSource,
                    partyName = partyName,
                    jalaliDate = jalaliDate,
                    timestamp = System.currentTimeMillis(),
                    description = description,
                    attachmentPath = attachmentPath,
                    referenceNumber = referenceNumber,
                    isVerified = isVerified
                )
            )
            triggerAutoCloudSync()
        }
    }

    fun toggleTransactionVerification(transaction: FinancialTransactionEntity) {
        viewModelScope.launch {
            financialDao.updateTransaction(transaction.copy(isVerified = !transaction.isVerified))
            triggerAutoCloudSync()
        }
    }

    fun deleteTransaction(transaction: FinancialTransactionEntity) {
        viewModelScope.launch {
            financialDao.deleteTransaction(transaction)
            triggerAutoCloudSync()
        }
    }

    // Workplace Config Actions
    fun updateWorkplaceConfig(workplace: String, targetDailyMinutes: Int) {
        viewModelScope.launch {
            configDao.insertOrUpdateConfig(
                WorkplaceConfigEntity(
                    workplace = workplace,
                    targetDailyMinutes = targetDailyMinutes,
                    targetWeeklyMinutes = targetDailyMinutes * 5
                )
            )
            triggerAutoCloudSync()
        }
    }

    // User Profile Actions
    fun updateUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            profileDao.insertOrUpdateProfile(profile)
            triggerAutoCloudSync()
        }
    }

    fun updateHijriOffset(offsetDays: Int) {
        viewModelScope.launch {
            val current = profileDao.getUserProfileOnce() ?: UserProfileEntity()
            profileDao.insertOrUpdateProfile(current.copy(hijriOffsetDays = offsetDays))
            triggerAutoCloudSync()
        }
    }

    // MinIO Cloud Sync Actions (Offline-First)
    fun syncBackupToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        _isSyncing.value = true
        _syncStatusMessage.value = "در حال تجمیع داده‌ها و ارسال ایمن به MinIO..."
        viewModelScope.launch {
            try {
                val punches = punchDao.getAllPunchLogsOnce()
                val activities = activityDao.getAllActivitiesOnce()
                val tasks = taskDao.getAllTasksOnce()
                val transactions = financialDao.getAllTransactionsOnce()
                val configs = configDao.getAllConfigsOnce()
                val profile = profileDao.getUserProfileOnce()
                val notes = noteDao.getAllNotesOnce()
                val leaves = leaveDao.getAllLeavesOnce()
                val holidays = holidayDao.getAllHolidaysOnce()
                val missions = missionDao.getAllMissionsOnce()

                val backup = BackupPackage(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                    punches = punches,
                    activities = activities,
                    tasks = tasks,
                    transactions = transactions,
                    configs = configs,
                    profile = profile,
                    notes = notes,
                    leaves = leaves,
                    holidays = holidays,
                    missions = missions
                )
                val jsonString = backup.toJsonString()
                val result = MinioSyncClient.uploadBackup(jsonString)

                if (result.isSuccess) {
                    val msg = result.getOrNull() ?: "پشتیبان‌گیری ابری با موفقیت انجام شد."
                    _syncStatusMessage.value = msg
                    profile?.let {
                        profileDao.insertOrUpdateProfile(it.copy(lastSyncTimestamp = System.currentTimeMillis()))
                    }
                    onComplete(true, msg)
                } else {
                    val errMsg = result.exceptionOrNull()?.localizedMessage ?: "خطا در برقراری ارتباط با سرور MinIO"
                    _syncStatusMessage.value = errMsg
                    onComplete(false, errMsg)
                }
            } catch (e: Exception) {
                val errMsg = "خطا در عملیات پشتیبان‌گیری: ${e.localizedMessage}"
                _syncStatusMessage.value = errMsg
                onComplete(false, errMsg)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreBackupFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        _isSyncing.value = true
        _syncStatusMessage.value = "در حال دریافت فایل پشتیبان از سرور MinIO..."
        viewModelScope.launch {
            try {
                val result = MinioSyncClient.downloadLatestBackup()
                if (result.isSuccess) {
                    val jsonStr = result.getOrThrow()
                    val backup = BackupPackage.fromJsonString(jsonStr)

                    // Clear and restore tables
                    punchDao.clearAll()
                    punchDao.insertAll(backup.punches)

                    activityDao.clearAll()
                    activityDao.insertAll(backup.activities)

                    taskDao.clearAll()
                    taskDao.insertAll(backup.tasks)

                    financialDao.clearAll()
                    financialDao.insertAll(backup.transactions)

                    if (backup.configs.isNotEmpty()) {
                        configDao.clearAll()
                        configDao.insertAll(backup.configs)
                    }

                    if (backup.notes.isNotEmpty()) {
                        noteDao.clearAll()
                        noteDao.insertAll(backup.notes)
                    }

                    if (backup.leaves.isNotEmpty()) {
                        leaveDao.clearAll()
                        leaveDao.insertAll(backup.leaves)
                    }

                    if (backup.holidays.isNotEmpty()) {
                        holidayDao.clearAll()
                        holidayDao.insertAll(backup.holidays)
                    }

                    if (backup.missions.isNotEmpty()) {
                        missionDao.clearAll()
                        missionDao.insertAll(backup.missions)
                    }

                    backup.profile?.let {
                        profileDao.insertOrUpdateProfile(it.copy(lastSyncTimestamp = System.currentTimeMillis()))
                    }

                    val msg = "بازیابی اطلاعات از ابر با موفقیت انجام شد (${backup.punches.size} تردد، ${backup.transactions.size} سند مالی)."
                    _syncStatusMessage.value = msg
                    onComplete(true, msg)

                } else {
                    val errMsg = result.exceptionOrNull()?.localizedMessage ?: "خطا در دریافت اطلاعات از سرور MinIO"
                    _syncStatusMessage.value = errMsg
                    onComplete(false, errMsg)
                }
            } catch (e: Exception) {
                val errMsg = "خطا در بازیابی اطلاعات: ${e.localizedMessage}"
                _syncStatusMessage.value = errMsg
                onComplete(false, errMsg)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun runMinioDiagnostic(onResult: (MinioSyncClient.DiagnosticResult) -> Unit) {
        viewModelScope.launch {
            val res = MinioSyncClient.runConnectionDiagnostic()
            onResult(res)
        }
    }

    /**
     * Calculates deductions for Howzeh/Mosque:
     * - Teaching (تدریس): 60 minutes per session if performed (or logged duration)
     * - Study for teaching (مطالعه برای تدریس): exact logged duration
     * - Rest (استراحت): exact logged duration deducted from obligatory presence
     */
    data class DayDeductionDetails(
        val teachingMins: Int = 0,
        val studyMins: Int = 0,
        val restMins: Int = 0,
        val totalDeductionMins: Int = teachingMins + studyMins + restMins
    )

    fun getDeductionsForDate(dateStr: String, workplace: String = "HOWZEH"): DayDeductionDetails {
        val workplaceActs = allActivities.value.filter {
            (it.workplace == workplace || it.workplace == "ALL") && it.jalaliDate == dateStr
        }
        var teachingMins = 0
        var studyMins = 0
        var restMins = 0
        for (act in workplaceActs) {
            val isTeaching = act.categoryTag == "#تدریس" ||
                    (act.title.contains("تدریس") && !act.title.contains("مطالعه") && !act.title.contains("استراحت"))
            val isStudy = act.categoryTag == "#مطالعه_تدریس" ||
                    act.categoryTag == "#مطالعه" ||
                    (act.title.contains("مطالعه") && act.title.contains("تدریس"))
            val isRest = act.categoryTag == "#استراحت" || act.title.contains("استراحت")

            if (isTeaching) {
                teachingMins += if (act.durationMinutes > 0) act.durationMinutes else 60
            } else if (isStudy) {
                studyMins += act.durationMinutes
            } else if (isRest) {
                restMins += act.durationMinutes
            }
        }
        return DayDeductionDetails(
            teachingMins = teachingMins,
            studyMins = studyMins,
            restMins = restMins
        )
    }

    fun getTeachingAndStudyDeductions(dateStr: String): Pair<Int, Int> {
        val details = getDeductionsForDate(dateStr, "HOWZEH")
        return Pair(details.teachingMins, details.studyMins + details.restMins)
    }

    data class DayAttendanceCalculation(
        val jalaliDate: String,
        val workplace: String,
        val dayOfWeekName: String,
        val isFriday: Boolean,
        val isHoliday: Boolean,
        val holidayTitle: String?,
        val grossPresenceMinutes: Long,
        val teachingDeductionMinutes: Int,
        val studyDeductionMinutes: Int,
        val restDeductionMinutes: Int,
        val totalDeductionMinutes: Int,
        val leaveMinutes: Long,
        val missionMinutes: Long,
        val netEffectiveMinutes: Long,
        val targetDailyMinutes: Long,
        val diffMinutes: Long, // positive = overtime, negative = deficit
        val overtimeMinutes: Long,
        val deficitMinutes: Long,
        val firstCheckInTime: String = "-",
        val lastCheckOutTime: String = "-",
        val statusText: String = "عادی"
    )

    fun calculateDayAttendance(
        dateStr: String,
        workplace: String,
        punchesList: List<com.example.data.PunchLogEntity> = allPunches.value,
        activitiesList: List<com.example.data.ActivityLogEntity> = allActivities.value,
        leavesList: List<com.example.data.LeaveEntity> = allLeaves.value,
        missionsList: List<com.example.data.MissionEntity> = allMissions.value,
        holidaysList: List<com.example.data.HolidayEntity> = allHolidays.value,
        configsList: List<com.example.data.WorkplaceConfigEntity> = workplaceConfigs.value
    ): DayAttendanceCalculation {
        val isFri = com.example.util.JalaliCalendar.isFriday(dateStr)
        val dayName = com.example.util.JalaliCalendar.getWeekdayName(dateStr)
        val holiday = holidaysList.find { it.jalaliDate == dateStr && it.isOffDay }
        val isHol = isFri || (holiday != null)
        val holTitle = if (isFri) "تعطیل جمعه" else holiday?.title

        val baseTarget = configsList.firstOrNull { it.workplace == workplace }?.targetDailyMinutes?.toLong()
            ?: if (workplace == "HOWZEH") 240L else 180L

        // جمعه‌ها و روزهای تعطیل ثبت‌شده توقع موظفی ندارند
        val targetDaily = if (isHol) 0L else baseTarget

        val dayPunches = punchesList.filter { it.workplace == workplace && it.jalaliDate == dateStr }
        val grossPresence = dayPunches.sumOf { p ->
            val end = p.checkOutTime ?: System.currentTimeMillis()
            (end - p.checkInTime) / 60000
        }

        val dayActs = activitiesList.filter {
            (it.workplace == workplace || it.workplace == "ALL") && it.jalaliDate == dateStr
        }
        var teachingM = 0
        var studyM = 0
        var restM = 0
        for (act in dayActs) {
            val isTeaching = act.categoryTag == "#تدریس" ||
                    (act.title.contains("تدریس") && !act.title.contains("مطالعه") && !act.title.contains("استراحت"))
            val isStudy = act.categoryTag == "#مطالعه_تدریس" ||
                    act.categoryTag == "#مطالعه" ||
                    (act.title.contains("مطالعه") && act.title.contains("تدریس"))
            val isRest = act.categoryTag == "#استراحت" || act.title.contains("استراحت")

            if (isTeaching) {
                teachingM += if (act.durationMinutes > 0) act.durationMinutes else 60
            } else if (isStudy) {
                studyM += act.durationMinutes
            } else if (isRest) {
                restM += act.durationMinutes
            }
        }
        val totalDed = teachingM + studyM + restM

        val dayLeaves = leavesList.filter { it.workplace == workplace && it.jalaliDate == dateStr }
        val leaveM = dayLeaves.sumOf { it.durationMinutes.toLong() }

        val dayMissions = missionsList.filter { it.workplace == workplace && it.jalaliDate == dateStr }
        val missionM = dayMissions.sumOf { it.durationMinutes.toLong() }

        val netPhysical = maxOf(0L, grossPresence - totalDed)
        val netEffective = netPhysical + leaveM + missionM
        val diff = netEffective - targetDaily
        val overtime = if (diff > 0L) diff else 0L
        val deficit = if (diff < 0L) -diff else 0L

        val timeFmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val earliestPunch = dayPunches.minByOrNull { it.checkInTime }
        val activePunch = dayPunches.find { it.checkOutTime == null }
        val latestPunchWithCheckout = dayPunches.filter { it.checkOutTime != null }.maxByOrNull { it.checkOutTime ?: 0L }

        val firstInStr = earliestPunch?.let { timeFmt.format(java.util.Date(it.checkInTime)) }
            ?: if (leaveM > 0 || missionM > 0) "مرخصی/مأموریت" else "-"

        val lastOutStr = when {
            activePunch != null -> "در حال حضور"
            latestPunchWithCheckout?.checkOutTime != null -> timeFmt.format(java.util.Date(latestPunchWithCheckout.checkOutTime!!))
            leaveM > 0 || missionM > 0 -> "پایان مأموریت/مرخصی"
            else -> "-"
        }

        val status = when {
            isHol -> holTitle ?: "تعطیل"
            diff > 0 -> "مازاد (+${overtime / 60}س ${overtime % 60}د)"
            diff < 0 -> "کسری (-${deficit / 60}س ${deficit % 60}د)"
            targetDaily > 0 -> "تکمیل موظفی"
            else -> "عادی"
        }

        return DayAttendanceCalculation(
            jalaliDate = dateStr,
            workplace = workplace,
            dayOfWeekName = dayName,
            isFriday = isFri,
            isHoliday = isHol,
            holidayTitle = holTitle,
            grossPresenceMinutes = grossPresence,
            teachingDeductionMinutes = teachingM,
            studyDeductionMinutes = studyM,
            restDeductionMinutes = restM,
            totalDeductionMinutes = totalDed,
            leaveMinutes = leaveM,
            missionMinutes = missionM,
            netEffectiveMinutes = netEffective,
            targetDailyMinutes = targetDaily,
            diffMinutes = diff,
            overtimeMinutes = overtime,
            deficitMinutes = deficit,
            firstCheckInTime = firstInStr,
            lastCheckOutTime = lastOutStr,
            statusText = status
        )
    }


    // AI Assistant Thinking State
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "AI",
                text = "سلام و احترام! من دستیار هوشمند مشکاه هستم. می‌توانم تحلیل ساعت حضور، کسر کار، پیشنهاد برنامه‌های مناسبتی مسجد و حوزه، یا صورت حسابرسی مالی را برای شما انجام دهم. چه کمکی از دست من برمی‌آید؟"
            )
        )
    )
    val aiMessages = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    fun sendAiPrompt(userText: String) {
        if (userText.isBlank()) return

        val userMsg = AiChatMessage(sender = "USER", text = userText)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val contextSummary = buildContextSummary()
            val result = GeminiAssistantService.queryThinkingAssistant(userText, contextSummary)

            _isAiThinking.value = false
            val aiResponseText = result.getOrElse {
                "متأسفانه در دریافت پاسخ خطایی رخ داد: ${it.localizedMessage}"
            }

            val aiMsg = AiChatMessage(sender = "AI", text = aiResponseText)
            _aiMessages.value = _aiMessages.value + aiMsg
        }
    }

    private fun buildContextSummary(): String {
        val todayStr = JalaliCalendar.getTodayJalali().toString()
        val todayPunches = allPunches.value.filter { it.jalaliDate == todayStr }

        var howzehMins = 0
        var mosqueMins = 0
        for (p in todayPunches) {
            val end = p.checkOutTime ?: System.currentTimeMillis()
            val mins = ((end - p.checkInTime) / 60000).toInt()
            if (p.workplace == "HOWZEH") howzehMins += mins else mosqueMins += mins
        }

        val (teachingMins, studyMins) = getTeachingAndStudyDeductions(todayStr)
        val howzehNetMins = (howzehMins - (teachingMins + studyMins)).coerceAtLeast(0)

        val howzehTarget = workplaceConfigs.value.firstOrNull { it.workplace == "HOWZEH" }?.targetDailyMinutes ?: 240
        val mosqueTarget = workplaceConfigs.value.firstOrNull { it.workplace == "MOSQUE" }?.targetDailyMinutes ?: 180

        val howzehIncome = allTransactions.value.filter { it.workplace == "HOWZEH" && it.type == "INCOME" }.sumOf { it.amount }
        val howzehExpense = allTransactions.value.filter { it.workplace == "HOWZEH" && it.type == "EXPENSE" }.sumOf { it.amount }
        val mosqueIncome = allTransactions.value.filter { it.workplace == "MOSQUE" && it.type == "INCOME" }.sumOf { it.amount }
        val mosqueExpense = allTransactions.value.filter { it.workplace == "MOSQUE" && it.type == "EXPENSE" }.sumOf { it.amount }

        return """
            - تاریخ امروز: $todayStr
            - ساعت حضور ناخالص امروز حوزه: $howzehMins دقیقه
            - کسر بابت تدریس و مطالعه برای تدریس: ${teachingMins + studyMins} دقیقه ($teachingMins دقیقه تدریس + $studyMins دقیقه مطالعه)
            - حضور خالص محاسبه‌شده حوزه: $howzehNetMins دقیقه (شاخص موظفی: $howzehTarget دقیقه)
            - ساعت حضور امروز مسجد: $mosqueMins دقیقه (شاخص موظفی: $mosqueTarget دقیقه)
            - وضعیت مالی حوزه علمیه: کل درآمد $howzehIncome ریال | کل هزینه $howzehExpense ریال | مانده ${howzehIncome - howzehExpense} ریال
            - وضعیت مالی مسجد: کل درآمد $mosqueIncome ریال | کل هزینه $mosqueExpense ریال | مانده ${mosqueIncome - mosqueExpense} ریال
            - تعداد کارهای معوقه/یادآورها: ${allTasks.value.count { !it.isCompleted }} مورد
        """.trimIndent()
    }
}
