package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LeaveEntity
import com.example.data.NoteEntity
import com.example.data.TaskReminderEntity
import com.example.data.UserProfileEntity
import com.example.ui.components.AddOrEditLeaveDialog
import com.example.ui.components.AddOrEditNoteDialog
import com.example.ui.components.AddTaskReminderDialog
import com.example.util.JalaliCalendar
import com.example.util.OccasionsDatabase
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarRemindersScreen(
    viewModel: AppViewModel,
    tasks: List<TaskReminderEntity>,
    selectedDate: JalaliCalendar.JalaliDate
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val allLeaves by viewModel.allLeaves.collectAsStateWithLifecycle()

    val hijriOffset = userProfile?.hijriOffsetDays ?: 0
    var showHijriOffsetDialog by remember { mutableStateOf(false) }

    var currentYear by remember { mutableIntStateOf(selectedDate.year) }
    var currentMonth by remember { mutableIntStateOf(selectedDate.month) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isAddingEvent by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var showAddLeaveDialog by remember { mutableStateOf(false) }
    var leaveToEdit by remember { mutableStateOf<LeaveEntity?>(null) }

    var selectedFolderFilter by remember { mutableStateOf("همه پوشه‌ها") }
    var noteSearchQuery by remember { mutableStateOf("") }

    var calendarViewTab by remember { mutableIntStateOf(0) } // 0 = Selected Day, 1 = Monthly Events, 2 = Notes & Folders

    val daysInMonth = JalaliCalendar.getDaysInJalaliMonth(currentYear, currentMonth)
    val firstDayOfMonthJalali = JalaliCalendar.JalaliDate(currentYear, currentMonth, 1)
    val startDayOfWeek = JalaliCalendar.getDayOfWeek(firstDayOfMonthJalali) // 0 = Saturday

    val firstHijri = remember(currentYear, currentMonth, hijriOffset) {
        JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, 1), hijriOffset)
    }
    val lastHijri = remember(currentYear, currentMonth, daysInMonth, hijriOffset) {
        JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, daysInMonth), hijriOffset)
    }

    val selectedDayOccasions = remember(selectedDate, hijriOffset) {
        OccasionsDatabase.getOccasionsForDay(selectedDate, hijriOffset)
    }
    val selectedDayHijri = remember(selectedDate, hijriOffset) {
        JalaliCalendar.jalaliToHijri(selectedDate, hijriOffset)
    }

    val tasksForSelectedDate = tasks.filter { it.jalaliDateStr == selectedDate.toString() }
    val notesForSelectedDate = allNotes.filter { it.jalaliDate == selectedDate.toString() }
    val leavesForSelectedDate = allLeaves.filter { it.jalaliDate == selectedDate.toString() }

    // Monthly Events & Tasks
    val monthPrefix = String.format("%04d/%02d", currentYear, currentMonth)
    val monthlyTasks = tasks.filter { it.jalaliDateStr.startsWith(monthPrefix) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Jalali & Hijri Dual Month Header Navigation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth > 1) {
                                    currentMonth--
                                } else {
                                    currentMonth = 12
                                    currentYear--
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${JalaliCalendar.monthNames[currentMonth - 1]} ${JalaliCalendar.toPersianDigits(currentYear.toString())}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val hijriMonthText = if (firstHijri.month == lastHijri.month) {
                                "${firstHijri.getMonthName()} ${JalaliCalendar.toPersianDigits(firstHijri.year.toString())} هجری قمری"
                            } else {
                                "${firstHijri.getMonthName()} - ${lastHijri.getMonthName()} ${JalaliCalendar.toPersianDigits(firstHijri.year.toString())} هجری قمری"
                            }
                            Text(
                                text = hijriMonthText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentMonth < 12) {
                                    currentMonth++
                                } else {
                                    currentMonth = 1
                                    currentYear++
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Moon Sighting Adjuster Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showHijriOffsetDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.testTag("adjust_hijri_offset_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NightlightRound,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(6.dp))
                                val offsetLabel = when {
                                    hijriOffset == 0 -> "رویت هلال قمری: پیش‌فرض (۰ روز)"
                                    hijriOffset > 0 -> "رویت هلال قمری: +$hijriOffset روز جلوتر"
                                    else -> "رویت هلال قمری: ${Math.abs(hijriOffset)}- روز عقب‌تر"
                                }
                                Text(
                                    text = offsetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تنظیم",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Days of Week Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        JalaliCalendar.weekDays.forEach { dayName ->
                            Text(
                                text = dayName.take(2),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Calendar Grid (7 columns)
                    val totalGridCells = startDayOfWeek + daysInMonth
                    Column {
                        var dayCounter = 1
                        val rows = (totalGridCells + 6) / 7
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    if (cellIndex < startDayOfWeek || dayCounter > daysInMonth) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val day = dayCounter
                                        val isSelected = selectedDate.year == currentYear && selectedDate.month == currentMonth && selectedDate.day == day
                                        val isToday = JalaliCalendar.getTodayJalali().let {
                                            it.year == currentYear && it.month == currentMonth && it.day == day
                                        }

                                        val dayDate = JalaliCalendar.JalaliDate(currentYear, currentMonth, day)
                                        val occList = OccasionsDatabase.getOccasionsForDay(dayDate, hijriOffset)
                                        val hasOccasion = occList.isNotEmpty()
                                        val isHoliday = occList.any { it.isHoliday }

                                        val hDay = remember(currentYear, currentMonth, day, hijriOffset) {
                                            JalaliCalendar.jalaliToHijri(dayDate, hijriOffset).day
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    viewModel.setSelectedDate(JalaliCalendar.JalaliDate(currentYear, currentMonth, day))
                                                    calendarViewTab = 0
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = JalaliCalendar.toPersianDigits(day.toString()),
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isHoliday -> MaterialTheme.colorScheme.error
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = JalaliCalendar.toPersianDigits(hDay.toString()),
                                                    fontSize = 8.sp,
                                                    color = when {
                                                        isSelected -> Color.White.copy(alpha = 0.8f)
                                                        else -> Color.Gray
                                                    }
                                                )
                                                if (hasOccasion) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else if (isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                                                    )
                                                }
                                            }
                                        }
                                        dayCounter++
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // View Mode Switcher: 3 Tabs (Selected Day, Monthly Events, Notes & Folders)
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = calendarViewTab == 0,
                    onClick = { calendarViewTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = { Icon(Icons.Default.Today, contentDescription = null) },
                    modifier = Modifier.testTag("tab_selected_day")
                ) {
                    Text("روز (${selectedDate.day})")
                }
                SegmentedButton(
                    selected = calendarViewTab == 1,
                    onClick = { calendarViewTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null) },
                    modifier = Modifier.testTag("tab_monthly_events")
                ) {
                    Text("رویدادها")
                }
                SegmentedButton(
                    selected = calendarViewTab == 2,
                    onClick = { calendarViewTab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null) },
                    modifier = Modifier.testTag("tab_notes_folders")
                ) {
                    Text("یادداشت‌ها")
                }
            }
        }

        if (calendarViewTab == 0) {
            // SELECTED DAY VIEW
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مناسبت‌های امروز:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${selectedDate.toPersianDigits()}  |  ${selectedDayHijri.toPersianDigits()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))

                        if (selectedDayOccasions.isEmpty()) {
                            Text("هیچ مناسبت ملی یا مذهبی خاصی برای این روز ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            selectedDayOccasions.forEach { occ ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (occ.isReligious) Icons.Default.Mosque else Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (occ.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = occ.title + if (occ.isHoliday) " (تعطیل رسمی)" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (occ.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Task, Reminder & Events Header with Action Buttons
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ثبت‌های این روز (${tasksForSelectedDate.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { isAddingEvent = true; showAddTaskDialog = true },
                                modifier = Modifier.testTag("add_event_button")
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ثبت رویداد", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { isAddingEvent = false; showAddTaskDialog = true },
                                modifier = Modifier.testTag("add_reminder_fab")
                            ) {
                                Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ثبت یادآور", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { showAddNoteDialog = true },
                            label = { Text("+ یادداشت روز") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { showAddLeaveDialog = true },
                            label = { Text("+ ثبت مرخصی") },
                            leadingIcon = { Icon(Icons.Default.BeachAccess, contentDescription = null) }
                        )
                    }
                }
            }

            // Reminders / Events List
            if (tasksForSelectedDate.isEmpty()) {
                item {
                    Text(
                        "هیچ کار، یادآور یا رویدادی برای این تاریخ ثبت نشده است.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(tasksForSelectedDate, key = { "task_${it.id}" }) { task ->
                    TaskReminderItemCard(task = task, timeFormat = timeFormat, viewModel = viewModel)
                }
            }

            // Notes connected to this date
            if (notesForSelectedDate.isNotEmpty()) {
                item {
                    Text("یادداشت‌های متصل به این تاریخ (${notesForSelectedDate.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                items(notesForSelectedDate, key = { "note_${it.id}" }) { note ->
                    NoteItemCard(
                        note = note,
                        onEdit = { noteToEdit = it },
                        onDelete = { viewModel.deleteNote(it) }
                    )
                }
            }

            // Leaves registered for this date
            if (leavesForSelectedDate.isNotEmpty()) {
                item {
                    Text("مرخصی‌های ثبت‌شده در این تاریخ (${leavesForSelectedDate.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                items(leavesForSelectedDate, key = { "leave_${it.id}" }) { leave ->
                    LeaveItemCard(
                        leave = leave,
                        onDelete = { viewModel.deleteLeave(it) }
                    )
                }
            }

        } else if (calendarViewTab == 1) {
            // MONTHLY EVENTS & REMINDERS LIST VIEW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "لیست جامع رویدادها، مناسبت‌ها و یادآورهای ماه",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { isAddingEvent = false; showAddTaskDialog = true }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت یادآور")
                    }
                }
            }

            for (day in 1..daysInMonth) {
                val dayDate = JalaliCalendar.JalaliDate(currentYear, currentMonth, day)
                val dayStr = String.format("%04d/%02d/%02d", currentYear, currentMonth, day)
                val dayOccasions = OccasionsDatabase.getOccasionsForDay(dayDate, hijriOffset)
                val dayTasks = monthlyTasks.filter { it.jalaliDateStr == dayStr }

                if (dayOccasions.isNotEmpty() || dayTasks.isNotEmpty()) {
                    item(key = "day_$day") {
                        val hDay = JalaliCalendar.jalaliToHijri(dayDate, hijriOffset)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (dayOccasions.any { it.isHoliday }) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${JalaliCalendar.toPersianDigits(day.toString())} ${JalaliCalendar.monthNames[currentMonth - 1]}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = hDay.toShortPersianDigits(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }

                                    TextButton(onClick = {
                                        viewModel.setSelectedDate(dayDate)
                                        calendarViewTab = 0
                                    }) {
                                        Text("مشاهده جزئیات", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                dayOccasions.forEach { occ ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (occ.isReligious) Icons.Default.Mosque else Icons.Default.Event,
                                            contentDescription = null,
                                            tint = if (occ.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = occ.title + if (occ.isHoliday) " (تعطیل رسمی)" else "",
                                            fontWeight = if (occ.isHoliday) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (occ.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                dayTasks.forEach { task ->
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Icon(
                                            if (task.isEvent) Icons.Default.Event else Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (task.isEvent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${if (task.isEvent) "رویداد" else "یادآور"}: ${task.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (task.categoryTag.isNotBlank()) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = task.categoryTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 2: STANDALONE NOTES & FOLDERS SECTION (دفترچه یادداشت‌ها و پوشه‌ها)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "دفترچه یادداشت‌ها و پوشه‌ها",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { showAddNoteDialog = true },
                                modifier = Modifier.testTag("add_note_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("یادداشت جدید")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = noteSearchQuery,
                            onValueChange = { noteSearchQuery = it },
                            label = { Text("جستجو در متن و عنوان یادداشت‌ها...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        // Folders Filter Row
                        Text("دسته‌بندی و پوشه‌ها:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))

                        val allFolders = listOf("همه پوشه‌ها", "عمومی", "شخصی", "کاری", "حوزه علمیه", "مسجد", "مناسبت‌ها", "مطالعات", "ایده‌ها") +
                                allNotes.map { it.folder }.distinct().filter { it !in listOf("عمومی", "شخصی", "کاری", "حوزه علمیه", "مسجد", "مناسبت‌ها", "مطالعات", "ایده‌ها") }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allFolders) { folder ->
                                FilterChip(
                                    selected = selectedFolderFilter == folder,
                                    onClick = { selectedFolderFilter = folder },
                                    label = { Text(folder) }
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Notes List
            val filteredNotes = allNotes.filter { note ->
                val matchesFolder = (selectedFolderFilter == "همه پوشه‌ها" || note.folder == selectedFolderFilter)
                val matchesQuery = noteSearchQuery.isBlank() ||
                        note.title.contains(noteSearchQuery, ignoreCase = true) ||
                        note.content.contains(noteSearchQuery, ignoreCase = true) ||
                        note.folder.contains(noteSearchQuery, ignoreCase = true) ||
                        note.occasionTag.contains(noteSearchQuery, ignoreCase = true)
                matchesFolder && matchesQuery
            }

            if (filteredNotes.isEmpty()) {
                item {
                    Text(
                        "هیچ یادداشتی در این پوشه یافت نشد.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(filteredNotes, key = { "full_note_${it.id}" }) { note ->
                    NoteItemCard(
                        note = note,
                        onEdit = { noteToEdit = it },
                        onDelete = { viewModel.deleteNote(it) }
                    )
                }
            }
        }
    }

    // Add Task / Event Dialog
    if (showAddTaskDialog) {
        AddTaskReminderDialog(
            initialDate = selectedDate,
            initialIsEvent = isAddingEvent,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, workplace, dueDateMillis, jalaliDateStr, priority, tag, earlyReminderHours, isEvent ->
                showAddTaskDialog = false
                viewModel.addTaskReminder(
                    title = title,
                    description = desc,
                    workplace = workplace,
                    dueDate = dueDateMillis,
                    jalaliDateStr = jalaliDateStr,
                    priority = priority,
                    categoryTag = tag,
                    earlyReminderHours = earlyReminderHours,
                    isEvent = isEvent
                )
            }
        )
    }

    // Add / Edit Note Dialog
    if (showAddNoteDialog || noteToEdit != null) {
        AddOrEditNoteDialog(
            initialNote = noteToEdit,
            initialDate = selectedDate.toString(),
            onDismiss = {
                showAddNoteDialog = false
                noteToEdit = null
            },
            onConfirm = { title, content, folder, jalaliDate, occasionTag, categoryTag ->
                if (noteToEdit != null) {
                    viewModel.updateNote(
                        noteToEdit!!.copy(
                            title = title,
                            content = content,
                            folder = folder,
                            jalaliDate = jalaliDate,
                            occasionTag = occasionTag,
                            categoryTag = categoryTag
                        )
                    )
                } else {
                    viewModel.addNote(
                        title = title,
                        content = content,
                        folder = folder,
                        jalaliDate = jalaliDate,
                        occasionTag = occasionTag,
                        categoryTag = categoryTag
                    )
                }
                showAddNoteDialog = false
                noteToEdit = null
            },
            onDelete = { note ->
                viewModel.deleteNote(note)
                noteToEdit = null
            }
        )
    }

    // Add / Edit Leave Dialog
    if (showAddLeaveDialog || leaveToEdit != null) {
        AddOrEditLeaveDialog(
            initialLeave = leaveToEdit,
            initialDate = selectedDate.toString(),
            onDismiss = {
                showAddLeaveDialog = false
                leaveToEdit = null
            },
            onConfirm = { workplace, leaveType, jalaliDate, startH, startM, endH, endM, durMins, reason ->
                if (leaveToEdit != null) {
                    viewModel.updateLeave(
                        leaveToEdit!!.copy(
                            workplace = workplace,
                            leaveType = leaveType,
                            jalaliDate = jalaliDate,
                            startHour = startH,
                            startMinute = startM,
                            endHour = endH,
                            endMinute = endM,
                            durationMinutes = durMins,
                            reason = reason
                        )
                    )
                } else {
                    viewModel.addLeave(
                        workplace = workplace,
                        leaveType = leaveType,
                        jalaliDate = jalaliDate,
                        startHour = startH,
                        startMinute = startM,
                        endHour = endH,
                        endMinute = endM,
                        durationMinutes = durMins,
                        reason = reason
                    )
                }
                showAddLeaveDialog = false
                leaveToEdit = null
            },
            onDelete = { leave ->
                viewModel.deleteLeave(leave)
                leaveToEdit = null
            }
        )
    }

    // Moon Sighting Adjustment Dialog
    if (showHijriOffsetDialog) {
        var tempOffset by remember(hijriOffset) { mutableIntStateOf(hijriOffset) }
        val previewTodayHijri = JalaliCalendar.jalaliToHijri(JalaliCalendar.getTodayJalali(), tempOffset)

        AlertDialog(
            onDismissRequest = { showHijriOffsetDialog = false },
            title = { Text("تنظیم رویت هلال قمری (جابجایی ماه قمری)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "با توجه به رویت هلال ماه در کشور و اعلام دفتر مراجع عظام، می‌توانید تقویم قمری را تا ۲ روز به جلو یا عقب جابجا کنید.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(-2, -1, 0, 1, 2).forEach { offset ->
                            FilterChip(
                                selected = tempOffset == offset,
                                onClick = { tempOffset = offset },
                                label = { Text(if (offset > 0) "+$offset" else "$offset") }
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "پیش‌نمایش تاریخ قمری امروز: ${previewTodayHijri.toPersianDigits()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    userProfile?.let { viewModel.updateUserProfile(it.copy(hijriOffsetDays = tempOffset)) }
                    showHijriOffsetDialog = false
                }) {
                    Text("ذخیره تنظیمات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHijriOffsetDialog = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun TaskReminderItemCard(
    task: TaskReminderEntity,
    timeFormat: SimpleDateFormat,
    viewModel: AppViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (task.isEvent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (task.isEvent) "رویداد" else "یادآور",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isEvent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                when (task.workplace) {
                                    "HOWZEH" -> "حوزه"
                                    "MOSQUE" -> "مسجد"
                                    else -> "عمومی"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    if (task.categoryTag.isNotBlank() && task.categoryTag != "#عمومی") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = task.categoryTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!task.isEvent) {
                        val scheduledTimeStr = timeFormat.format(Date(task.dueDate))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = scheduledTimeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { viewModel.deleteTask(task) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun NoteItemCard(
    note: NoteEntity,
    onEdit: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "پوشه: ${note.folder}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (!note.jalaliDate.isNull_Blank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "تاریخ: ${note.jalaliDate}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = { onEdit(note) }) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش یادداشت", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(note) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف یادداشت", tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(text = note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (note.occasionTag.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = "مناسبت مرتبط: ${note.occasionTag}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeaveItemCard(
    leave: LeaveEntity,
    onDelete: (LeaveEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BeachAccess, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "مرخصی: ${
                            when (leave.leaveType) {
                                "DAILY" -> "روزانه کامل"
                                "ESTEHAQY" -> "استحقاقی"
                                "ESTELAJI" -> "استعلاجی"
                                "BEDOON_HOQOOQ" -> "بدون حقوق"
                                else -> "ساعتی"
                            }
                        } (${if (leave.workplace == "HOWZEH") "حوزه" else "مسجد"})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(text = "مدت: ${leave.durationMinutes / 60} ساعت و ${leave.durationMinutes % 60} دقیقه | تاریخ: ${leave.jalaliDate}", style = MaterialTheme.typography.bodySmall)
                if (leave.reason.isNotBlank()) {
                    Text(text = "علت: ${leave.reason}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
            }

            IconButton(onClick = { onDelete(leave) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف مرخصی", tint = Color.Gray)
            }
        }
    }
}

private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
