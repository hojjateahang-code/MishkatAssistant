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

                val backup = BackupPackage(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                    punches = punches,
                    activities = activities,
                    tasks = tasks,
                    transactions = transactions,
                    configs = configs,
                    profile = profile
                )
                MinioSyncClient.uploadBackup(backup.toJsonString())
            } catch (e: Exception) {
                // Silently ignore background periodic upload
            }
        }
    }

    init {
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

                    if (punches.isNotEmpty() || activities.isNotEmpty() || transactions.isNotEmpty()) {
                        val backup = BackupPackage(
                            version = 1,
                            timestamp = System.currentTimeMillis(),
                            jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                            punches = punches,
                            activities = activities,
                            tasks = tasks,
                            transactions = transactions,
                            configs = configs,
                            profile = profile
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

    fun deleteActivity(activity: ActivityLogEntity) {
        viewModelScope.launch {
            activityDao.deleteActivity(activity)
            triggerAutoCloudSync()
        }
    }

    // Task & Reminder Actions
    fun addTaskReminder(
        title: String,
        description: String,
        workplace: String,
        dueDate: Long,
        jalaliDateStr: String,
        priority: String,
        categoryTag: String,
        earlyReminderHours: Int = 0
    ) {
        viewModelScope.launch {
            val id = taskDao.insertTask(
                TaskReminderEntity(
                    title = title,
                    description = description,
                    workplace = workplace,
                    dueDate = dueDate,
                    jalaliDateStr = jalaliDateStr,
                    isCompleted = false,
                    priority = priority,
                    categoryTag = categoryTag,
                    earlyReminderHours = earlyReminderHours
                )
            )
            NotificationHelper.scheduleReminderWithEarlyAlert(
                getApplication(),
                id.toInt(),
                "یادآور کار: $title",
                description.ifBlank { "موعد رسیدگی به $title فرا رسیده است." },
                dueDate,
                earlyReminderHours
            )
            triggerAutoCloudSync()
        }
    }

    fun toggleTaskCompletion(task: TaskReminderEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
            triggerAutoCloudSync()
        }
    }

    fun deleteTask(task: TaskReminderEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            NotificationHelper.cancelReminderWithEarlyAlert(getApplication(), task.id)
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

                val backup = BackupPackage(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    jalaliDate = JalaliCalendar.getTodayJalali().toString(),
                    punches = punches,
                    activities = activities,
                    tasks = tasks,
                    transactions = transactions,
                    configs = configs,
                    profile = profile
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
     * Calculates deductions for Howzeh:
     * - Teaching (تدریس): 60 minutes per session if performed
     * - Study for teaching (مطالعه برای تدریس): exact logged duration
     */
    fun getTeachingAndStudyDeductions(dateStr: String): Pair<Int, Int> {
        val howzehActivities = allActivities.value.filter { it.workplace == "HOWZEH" && it.jalaliDate == dateStr }
        var teachingMins = 0
        var studyMins = 0
        for (act in howzehActivities) {
            val isTeaching = act.categoryTag == "#تدریس" || (act.title.contains("تدریس") && !act.title.contains("مطالعه"))
            val isStudy = act.categoryTag == "#مطالعه_تدریس" || (act.title.contains("مطالعه") && act.title.contains("تدریس")) || act.categoryTag == "#مطالعه"
            if (isTeaching) {
                teachingMins += 60 // تدریس اگر محقق شود یکساعت است
            } else if (isStudy) {
                studyMins += act.durationMinutes // مطالعه هم هر چقدر زمانگیر نشان داد
            }
        }
        return Pair(teachingMins, studyMins)
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
