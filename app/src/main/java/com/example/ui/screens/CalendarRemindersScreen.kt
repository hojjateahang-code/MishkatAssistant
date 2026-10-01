package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    val daysInMonth = JalaliCalendar.getDaysInJalaliMonth(currentYear, currentMonth)
    val firstDayOfMonthJalali = JalaliCalendar.JalaliDate(currentYear, currentMonth, 1)
    val startDayOfWeek = JalaliCalendar.getDayOfWeek(firstDayOfMonthJalali) // 0 = Saturday

    val selectedDayOccasions = remember(selectedDate) {
        OccasionsDatabase.getOccasionsForDate(selectedDate.month, selectedDate.day)
    }

    val tasksForSelectedDate = tasks.filter {
        it.jalaliDateStr == selectedDate.toString()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Jalali Month & Year Header Navigation
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

                        Text(
                            text = "${JalaliCalendar.monthNames[currentMonth - 1]} ${JalaliCalendar.toPersianDigits(currentYear.toString())}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

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

                                        val hasOccasion = OccasionsDatabase.getOccasionsForDate(currentMonth, day).isNotEmpty()
                                        val isHoliday = OccasionsDatabase.getOccasionsForDate(currentMonth, day).any { it.isHoliday }

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
                                                    fontSize = 13.sp
                                                )
                                                if (hasOccasion) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else MaterialTheme.colorScheme.secondary)
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

        // Selected Date Occasions Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مناسبت‌های امروز (${selectedDate.toPersianDigits()}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
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
                    text = "یادآورها و برنامه‌ریزی کارهای روز",
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

        // List of Reminders
        if (tasks.isEmpty()) {
            item {
                Text(
                    "هنوز هیچ کار یا یادآوری ثبت نشده است.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val priorityColor = when (task.priority) {
                                    "HIGH" -> MaterialTheme.colorScheme.error
                                    "MEDIUM" -> MaterialTheme.colorScheme.secondary
                                    else -> Color.Gray
                                }
                                Text(
                                    text = "اولویّت: ${
                                        when (task.priority) {
                                            "HIGH" -> "بالا"
                                            "MEDIUM" -> "متوسط"
                                            else -> "پایین"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = priorityColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskReminderDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, workplace, dueDateMillis, priority, tag ->
                showAddTaskDialog = false
                viewModel.addTaskReminder(
                    title = title,
                    description = desc,
                    workplace = workplace,
                    dueDate = dueDateMillis,
                    jalaliDateStr = selectedDate.toString(),
                    priority = priority,
                    categoryTag = tag
                )
            }
        )
    }
}
