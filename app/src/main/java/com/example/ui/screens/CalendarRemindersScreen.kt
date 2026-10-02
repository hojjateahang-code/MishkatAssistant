package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.TaskReminderEntity
import com.example.ui.components.AddTaskReminderDialog
import com.example.util.JalaliCalendar
import com.example.util.OccasionsDatabase
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRemindersScreen(
    viewModel: AppViewModel,
    tasks: List<TaskReminderEntity>,
    selectedDate: JalaliCalendar.JalaliDate
) {
    var currentYear by remember { mutableIntStateOf(selectedDate.year) }
    var currentMonth by remember { mutableIntStateOf(selectedDate.month) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var calendarViewTab by remember { mutableIntStateOf(0) } // 0 = Selected Day, 1 = Monthly Events & Reminders

    val daysInMonth = JalaliCalendar.getDaysInJalaliMonth(currentYear, currentMonth)
    val firstDayOfMonthJalali = JalaliCalendar.JalaliDate(currentYear, currentMonth, 1)
    val startDayOfWeek = JalaliCalendar.getDayOfWeek(firstDayOfMonthJalali) // 0 = Saturday

    val firstHijri = remember(currentYear, currentMonth) {
        JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, 1))
    }
    val lastHijri = remember(currentYear, currentMonth, daysInMonth) {
        JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, daysInMonth))
    }

    val selectedDayOccasions = remember(selectedDate) {
        OccasionsDatabase.getOccasionsForDate(selectedDate.month, selectedDate.day)
    }
    val selectedDayHijri = remember(selectedDate) {
        JalaliCalendar.jalaliToHijri(selectedDate)
    }

    val tasksForSelectedDate = tasks.filter {
        it.jalaliDateStr == selectedDate.toString()
    }

    // Monthly Events & Tasks
    val monthPrefix = String.format("%04d/%02d", currentYear, currentMonth)
    val monthlyTasks = tasks.filter { it.jalaliDateStr.startsWith(monthPrefix) }

    val monthlyOccasionsByDay = remember(currentMonth, daysInMonth) {
        (1..daysInMonth).map { day ->
            val occs = OccasionsDatabase.getOccasionsForDate(currentMonth, day)
            day to occs
        }.filter { it.second.isNotEmpty() }
    }

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

                    Spacer(Modifier.height(12.dp))

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

                    // Calendar Grid (7 columns) with Hijri sub-digits
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

                                        val occList = OccasionsDatabase.getOccasionsForDate(currentMonth, day)
                                        val hasOccasion = occList.isNotEmpty()
                                        val isHoliday = occList.any { it.isHoliday }

                                        // Corresponding Hijri Day
                                        val hDay = remember(currentYear, currentMonth, day) {
                                            JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, day)).day
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

        // View Mode Switcher: Selected Day vs Monthly Events
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = calendarViewTab == 0,
                    onClick = { calendarViewTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.Today, contentDescription = null) },
                    modifier = Modifier.testTag("tab_selected_day")
                ) {
                    Text("روز انتخابی (${selectedDate.day})")
                }
                SegmentedButton(
                    selected = calendarViewTab == 1,
                    onClick = { calendarViewTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null) },
                    modifier = Modifier.testTag("tab_monthly_events")
                ) {
                    Text("رویدادهای ماهانه (${JalaliCalendar.monthNames[currentMonth - 1]})")
                }
            }
        }

        if (calendarViewTab == 0) {
            // SELECTED DAY VIEW

            // Selected Date Occasions Card with Hijri representation
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

            // Task & Reminders Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "یادآورها و برنامه‌ریزی کارهای روز (${tasksForSelectedDate.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.testTag("add_reminder_fab")
                    ) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("یادآور جدید")
                    }
                }
            }

            // List of Reminders for Selected Date
            if (tasksForSelectedDate.isEmpty()) {
                item {
                    Text(
                        "هنوز هیچ کار یا یادآوری برای این تاریخ ثبت نشده است.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(tasksForSelectedDate, key = { it.id }) { task ->
                    TaskReminderItemCard(task = task, timeFormat = timeFormat, viewModel = viewModel)
                }
            }
        } else {
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
                    Button(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت یادآور")
                    }
                }
            }

            // Iterate 1..daysInMonth
            for (day in 1..daysInMonth) {
                val dayStr = String.format("%04d/%02d/%02d", currentYear, currentMonth, day)
                val dayOccasions = OccasionsDatabase.getOccasionsForDate(currentMonth, day)
                val dayTasks = monthlyTasks.filter { it.jalaliDateStr == dayStr }

                if (dayOccasions.isNotEmpty() || dayTasks.isNotEmpty()) {
                    item(key = "day_$day") {
                        val hDay = JalaliCalendar.jalaliToHijri(JalaliCalendar.JalaliDate(currentYear, currentMonth, day))
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
                                        viewModel.setSelectedDate(JalaliCalendar.JalaliDate(currentYear, currentMonth, day))
                                        calendarViewTab = 0
                                    }) {
                                        Text("مشاهده جزئیات", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                // Occasions
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

                                // Tasks of this day
                                dayTasks.forEach { task ->
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "یادآور: ${task.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        if (task.earlyReminderHours > 0) {
                                            Text(
                                                text = "(هشدار: ${task.earlyReminderHours / 24} روز قبل)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Reminder Modal Dialog
    if (showAddTaskDialog) {
        AddTaskReminderDialog(
            initialDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, workplace, dueDateMillis, jalaliDateStr, priority, tag, earlyReminderHours ->
                showAddTaskDialog = false
                viewModel.addTaskReminder(
                    title = title,
                    description = desc,
                    workplace = workplace,
                    dueDate = dueDateMillis,
                    jalaliDateStr = jalaliDateStr,
                    priority = priority,
                    categoryTag = tag,
                    earlyReminderHours = earlyReminderHours
                )
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
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
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

                    if (task.earlyReminderHours > 0) {
                        val hoursOrDays = if (task.earlyReminderHours >= 24) {
                            "${task.earlyReminderHours / 24} روز قبل"
                        } else {
                            "${task.earlyReminderHours} ساعت قبل"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "آلارم: $hoursOrDays",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
