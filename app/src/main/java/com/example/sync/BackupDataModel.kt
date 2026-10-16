package com.example.sync

import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject

data class BackupPackage(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val jalaliDate: String = "",
    val punches: List<PunchLogEntity> = emptyList(),
    val activities: List<ActivityLogEntity> = emptyList(),
    val tasks: List<TaskReminderEntity> = emptyList(),
    val transactions: List<FinancialTransactionEntity> = emptyList(),
    val configs: List<WorkplaceConfigEntity> = emptyList(),
    val profile: UserProfileEntity? = null,
    val notes: List<NoteEntity> = emptyList(),
    val leaves: List<LeaveEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val missions: List<MissionEntity> = emptyList()
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("timestamp", timestamp)
        root.put("jalaliDate", jalaliDate)

        // Punches
        val punchesArr = JSONArray()
        punches.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("workplace", p.workplace)
            obj.put("checkInTime", p.checkInTime)
            if (p.checkOutTime != null) obj.put("checkOutTime", p.checkOutTime)
            obj.put("jalaliDate", p.jalaliDate)
            if (p.note != null) obj.put("note", p.note)
            punchesArr.put(obj)
        }
        root.put("punches", punchesArr)

        // Activities
        val actArr = JSONArray()
        activities.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("workplace", a.workplace)
            obj.put("title", a.title)
            obj.put("categoryTag", a.categoryTag)
            obj.put("startTime", a.startTime)
            obj.put("endTime", a.endTime)
            obj.put("durationMinutes", a.durationMinutes)
            obj.put("jalaliDate", a.jalaliDate)
            obj.put("notes", a.notes)
            actArr.put(obj)
        }
        root.put("activities", actArr)

        // Tasks
        val taskArr = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("workplace", t.workplace)
            obj.put("dueDate", t.dueDate)
            obj.put("jalaliDateStr", t.jalaliDateStr)
            obj.put("isCompleted", t.isCompleted)
            obj.put("priority", t.priority)
            obj.put("categoryTag", t.categoryTag)
            obj.put("earlyReminderHours", t.earlyReminderHours)
            obj.put("isEvent", t.isEvent)
            taskArr.put(obj)
        }
        root.put("tasks", taskArr)

        // Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("workplace", tx.workplace)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("category", tx.category)
            obj.put("accountSource", tx.accountSource)
            obj.put("partyName", tx.partyName)
            obj.put("jalaliDate", tx.jalaliDate)
            obj.put("timestamp", tx.timestamp)
            obj.put("description", tx.description)
            if (tx.attachmentPath != null) obj.put("attachmentPath", tx.attachmentPath)
            if (tx.referenceNumber != null) obj.put("referenceNumber", tx.referenceNumber)
            obj.put("isVerified", tx.isVerified)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        // Workplace Configs
        val cfgArr = JSONArray()
        configs.forEach { c ->
            val obj = JSONObject()
            obj.put("workplace", c.workplace)
            obj.put("targetDailyMinutes", c.targetDailyMinutes)
            obj.put("targetWeeklyMinutes", c.targetWeeklyMinutes)
            obj.put("defaultAccount", c.defaultAccount)
            cfgArr.put(obj)
        }
        root.put("configs", cfgArr)

        // Notes
        val notesArr = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("folder", n.folder)
            if (n.jalaliDate != null) obj.put("jalaliDate", n.jalaliDate)
            obj.put("occasionTag", n.occasionTag)
            obj.put("categoryTag", n.categoryTag)
            obj.put("updatedAt", n.updatedAt)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        // Leaves
        val leavesArr = JSONArray()
        leaves.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("workplace", l.workplace)
            obj.put("leaveType", l.leaveType)
            obj.put("jalaliDate", l.jalaliDate)
            obj.put("startHour", l.startHour)
            obj.put("startMinute", l.startMinute)
            obj.put("endHour", l.endHour)
            obj.put("endMinute", l.endMinute)
            obj.put("durationMinutes", l.durationMinutes)
            obj.put("reason", l.reason)
            obj.put("timestamp", l.timestamp)
            leavesArr.put(obj)
        }
        root.put("leaves", leavesArr)

        // Holidays
        val holArr = JSONArray()
        holidays.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("jalaliDate", h.jalaliDate)
            obj.put("title", h.title)
            obj.put("isOffDay", h.isOffDay)
            holArr.put(obj)
        }
        root.put("holidays", holArr)

        // Missions
        val misArr = JSONArray()
        missions.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("workplace", m.workplace)
            obj.put("title", m.title)
            obj.put("jalaliDate", m.jalaliDate)
            obj.put("startHour", m.startHour)
            obj.put("startMinute", m.startMinute)
            obj.put("endHour", m.endHour)
            obj.put("endMinute", m.endMinute)
            obj.put("durationMinutes", m.durationMinutes)
            obj.put("description", m.description)
            obj.put("timestamp", m.timestamp)
            misArr.put(obj)
        }
        root.put("missions", misArr)

        // User Profile
        profile?.let { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("fullName", p.fullName)
            pObj.put("roleTitle", p.roleTitle)
            pObj.put("phoneNumber", p.phoneNumber)
            pObj.put("email", p.email)
            pObj.put("notificationsEnabled", p.notificationsEnabled)
            pObj.put("autoSyncEnabled", p.autoSyncEnabled)
            pObj.put("lastSyncTimestamp", p.lastSyncTimestamp)
            pObj.put("hijriOffsetDays", p.hijriOffsetDays)
            root.put("profile", pObj)
        }

        return root.toString(2)
    }


    companion object {
        fun fromJsonString(jsonStr: String): BackupPackage {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val jalaliDate = root.optString("jalaliDate", "")

            // Punches
            val punchList = mutableListOf<PunchLogEntity>()
            val pArr = root.optJSONArray("punches")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    punchList.add(
                        PunchLogEntity(
                            id = o.optInt("id", 0),
                            workplace = o.optString("workplace", "HOWZEH"),
                            checkInTime = o.optLong("checkInTime"),
                            checkOutTime = if (o.has("checkOutTime")) o.optLong("checkOutTime") else null,
                            jalaliDate = o.optString("jalaliDate", ""),
                            note = if (o.has("note")) o.optString("note") else null
                        )
                    )
                }
            }

            // Activities
            val actList = mutableListOf<ActivityLogEntity>()
            val aArr = root.optJSONArray("activities")
            if (aArr != null) {
                for (i in 0 until aArr.length()) {
                    val o = aArr.getJSONObject(i)
                    actList.add(
                        ActivityLogEntity(
                            id = o.optInt("id", 0),
                            workplace = o.optString("workplace", "HOWZEH"),
                            title = o.optString("title", ""),
                            categoryTag = o.optString("categoryTag", "#عمومی"),
                            startTime = o.optLong("startTime"),
                            endTime = o.optLong("endTime"),
                            durationMinutes = o.optInt("durationMinutes", 0),
                            jalaliDate = o.optString("jalaliDate", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // Tasks
            val taskList = mutableListOf<TaskReminderEntity>()
            val tArr = root.optJSONArray("tasks")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    val o = tArr.getJSONObject(i)
                    taskList.add(
                        TaskReminderEntity(
                            id = o.optInt("id", 0),
                            title = o.optString("title", ""),
                            description = o.optString("description", ""),
                            workplace = o.optString("workplace", "ALL"),
                            dueDate = o.optLong("dueDate"),
                            jalaliDateStr = o.optString("jalaliDateStr", ""),
                            isCompleted = o.optBoolean("isCompleted", false),
                            priority = o.optString("priority", "MEDIUM"),
                            categoryTag = o.optString("categoryTag", "#عمومی"),
                            earlyReminderHours = o.optInt("earlyReminderHours", 0)
                        )
                    )
                }
            }

            // Transactions
            val txList = mutableListOf<FinancialTransactionEntity>()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    txList.add(
                        FinancialTransactionEntity(
                            id = o.optInt("id", 0),
                            workplace = o.optString("workplace", "HOWZEH"),
                            type = o.optString("type", "EXPENSE"),
                            amount = o.optLong("amount", 0L),
                            category = o.optString("category", ""),
                            accountSource = o.optString("accountSource", "صندوق اصلی"),
                            partyName = o.optString("partyName", ""),
                            jalaliDate = o.optString("jalaliDate", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            description = o.optString("description", ""),
                            attachmentPath = if (o.has("attachmentPath")) o.optString("attachmentPath") else null,
                            referenceNumber = if (o.has("referenceNumber")) o.optString("referenceNumber") else null,
                            isVerified = o.optBoolean("isVerified", false)
                        )
                    )
                }
            }

            // Configs
            val cfgList = mutableListOf<WorkplaceConfigEntity>()
            val cArr = root.optJSONArray("configs")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val o = cArr.getJSONObject(i)
                    cfgList.add(
                        WorkplaceConfigEntity(
                            workplace = o.optString("workplace", "HOWZEH"),
                            targetDailyMinutes = o.optInt("targetDailyMinutes", 240),
                            targetWeeklyMinutes = o.optInt("targetWeeklyMinutes", 1200),
                            defaultAccount = o.optString("defaultAccount", "صندوق اصلی")
                        )
                    )
                }
            }

            // Profile
            var profile: UserProfileEntity? = null
            val pObj = root.optJSONObject("profile")
            if (pObj != null) {
                profile = UserProfileEntity(
                    id = pObj.optInt("id", 1),
                    fullName = pObj.optString("fullName", ""),
                    roleTitle = pObj.optString("roleTitle", ""),
                    phoneNumber = pObj.optString("phoneNumber", ""),
                    email = pObj.optString("email", ""),
                    notificationsEnabled = pObj.optBoolean("notificationsEnabled", true),
                    autoSyncEnabled = pObj.optBoolean("autoSyncEnabled", false),
                    lastSyncTimestamp = pObj.optLong("lastSyncTimestamp", 0L),
                    hijriOffsetDays = pObj.optInt("hijriOffsetDays", 0)
                )
            }

            // Notes
            val noteList = mutableListOf<NoteEntity>()
            val nArr = root.optJSONArray("notes")
            if (nArr != null) {
                for (i in 0 until nArr.length()) {
                    val o = nArr.getJSONObject(i)
                    noteList.add(
                        NoteEntity(
                            id = o.optInt("id", 0),
                            title = o.optString("title", ""),
                            content = o.optString("content", ""),
                            folder = o.optString("folder", "عمومی"),
                            jalaliDate = if (o.has("jalaliDate")) o.optString("jalaliDate") else null,
                            occasionTag = o.optString("occasionTag", ""),
                            categoryTag = o.optString("categoryTag", "#یادداشت"),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Leaves
            val leaveList = mutableListOf<LeaveEntity>()
            val lArr = root.optJSONArray("leaves")
            if (lArr != null) {
                for (i in 0 until lArr.length()) {
                    val o = lArr.getJSONObject(i)
                    leaveList.add(
                        LeaveEntity(
                            id = o.optInt("id", 0),
                            workplace = o.optString("workplace", "HOWZEH"),
                            leaveType = o.optString("leaveType", "HOURLY"),
                            jalaliDate = o.optString("jalaliDate", ""),
                            startHour = o.optInt("startHour", 8),
                            startMinute = o.optInt("startMinute", 0),
                            endHour = o.optInt("endHour", 12),
                            endMinute = o.optInt("endMinute", 0),
                            durationMinutes = o.optInt("durationMinutes", 240),
                            reason = o.optString("reason", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Holidays
            val holList = mutableListOf<HolidayEntity>()
            val hArr = root.optJSONArray("holidays")
            if (hArr != null) {
                for (i in 0 until hArr.length()) {
                    val o = hArr.getJSONObject(i)
                    holList.add(
                        HolidayEntity(
                            id = o.optInt("id", 0),
                            jalaliDate = o.optString("jalaliDate", ""),
                            title = o.optString("title", "تعطیل رسمی"),
                            isOffDay = o.optBoolean("isOffDay", true)
                        )
                    )
                }
            }

            // Missions
            val misList = mutableListOf<MissionEntity>()
            val mArr = root.optJSONArray("missions")
            if (mArr != null) {
                for (i in 0 until mArr.length()) {
                    val o = mArr.getJSONObject(i)
                    misList.add(
                        MissionEntity(
                            id = o.optInt("id", 0),
                            workplace = o.optString("workplace", "HOWZEH"),
                            title = o.optString("title", ""),
                            jalaliDate = o.optString("jalaliDate", ""),
                            startHour = o.optInt("startHour", 8),
                            startMinute = o.optInt("startMinute", 0),
                            endHour = o.optInt("endHour", 12),
                            endMinute = o.optInt("endMinute", 0),
                            durationMinutes = o.optInt("durationMinutes", 240),
                            description = o.optString("description", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            return BackupPackage(
                version = version,
                timestamp = timestamp,
                jalaliDate = jalaliDate,
                punches = punchList,
                activities = actList,
                tasks = taskList,
                transactions = txList,
                configs = cfgList,
                profile = profile,
                notes = noteList,
                leaves = leaveList,
                holidays = holList,
                missions = misList
            )
        }
    }
}
