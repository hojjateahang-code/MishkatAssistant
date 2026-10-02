package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLogEntity
import com.example.ui.components.AddActivityDialog
import com.example.viewmodel.ActiveTimerState
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTrackerScreen(
    viewModel: AppViewModel,
    activities: List<ActivityLogEntity>,
    activeTimerState: ActiveTimerState
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWorkplaceFilter by remember { mutableStateOf("ALL") } // ALL, HOWZEH, MOSQUE
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    var newLiveTaskTitle by remember { mutableStateOf("") }
    var newLiveTaskTag by remember { mutableStateOf("#پوستر_و_تبلیغات") }
    var newLiveTaskWorkplace by remember { mutableStateOf("MOSQUE") }
    var showStartTimerDialog by remember { mutableStateOf(false) }

    val filteredActivities = activities.filter { act ->
        (selectedWorkplaceFilter == "ALL" || act.workplace == selectedWorkplaceFilter) &&
                (selectedTagFilter == null || act.categoryTag == selectedTagFilter)
    }

    val totalMinutesLogged = filteredActivities.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Activity Stopwatch Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (activeTimerState.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (activeTimerState.isRunning) "فعالیت در حال زمان‌گیری زنده" else "تایمر مستقیم ثبت فعالیت",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (activeTimerState.isRunning) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = activeTimerState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeTimerState.categoryTag} - ${if (activeTimerState.workplace == "HOWZEH") "حوزه علمیه" else "مسجد"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        val mins = activeTimerState.elapsedSeconds / 60
                        val secs = activeTimerState.elapsedSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.stopAndSaveLiveTaskTimer() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f).testTag("stop_live_timer_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("توقف و ثبت زمان")
                            }
                            OutlinedButton(
                                onClick = { viewModel.discardLiveTaskTimer() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("لغو")
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "می‌توانید همین حالا برای کارهایی مثل «آماده‌سازی پوستر شهدا» یا «کلاس درس» تایمر روشن کنید تا خودکار زمان محاسبه شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showStartTimerDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("start_live_timer_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("شروع تایمر زمان‌گیری زنده")
                        }
                    }
                }
            }
        }

        // Actions & Filters Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ریز فعالیت‌های روزانه", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_activity_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت فعالیت")
                    }
                }

                // Workplace Filter Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedWorkplaceFilter == "ALL",
                        onClick = { selectedWorkplaceFilter = "ALL" },
                        label = { Text("همه موارد") }
                    )
                    FilterChip(
                        selected = selectedWorkplaceFilter == "HOWZEH",
                        onClick = { selectedWorkplaceFilter = "HOWZEH" },
                        label = { Text("حوزه علمیه") }
                    )
                    FilterChip(
                        selected = selectedWorkplaceFilter == "MOSQUE",
                        onClick = { selectedWorkplaceFilter = "MOSQUE" },
                        label = { Text("مسجد") }
                    )
                }

                // Tag Filter Chips
                val tags = listOf("#برنامه‌های_قرآنی", "#پوستر_و_تبلیغات", "#امور_فرهنگی", "#تدریس", "#جلسات_و_شوراها")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTagFilter == null,
                            onClick = { selectedTagFilter = null },
                            label = { Text("همه تگ‌ها") }
                        )
                    }
                    items(tags) { tag ->
                        FilterChip(
                            selected = selectedTagFilter == tag,
                            onClick = { selectedTagFilter = if (selectedTagFilter == tag) null else tag },
                            label = { Text(tag) }
                        )
                    }
                }

                // Total Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مجموع زمان فعالیت‌های این بخش:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${totalMinutesLogged / 60} ساعت و ${totalMinutesLogged % 60} دقیقه",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // List of Activities
        if (filteredActivities.isEmpty()) {
            item {
                Text(
                    "هیچ فعالیتی مطابق فیلتر انتخاب شده یافت نشد.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(filteredActivities, key = { it.id }) { act ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = act.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { viewModel.deleteActivity(act) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(act.categoryTag, style = MaterialTheme.typography.bodySmall) }
                            )
                            Text(
                                text = if (act.workplace == "HOWZEH") "حوزه علمیه" else "مسجد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تاریخ: ${act.jalaliDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "مدت: ${act.durationMinutes} دقیقه (${act.durationMinutes / 60} ساعت)",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (act.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "توضیحات: ${act.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Activity Dialog
    if (showAddDialog) {
        AddActivityDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { workplace, title, tag, durationMins, notes ->
                showAddDialog = false
                val now = System.currentTimeMillis()
                viewModel.addActivity(
                    workplace = workplace,
                    title = title,
                    categoryTag = tag,
                    startTime = now - (durationMins * 60000L),
                    endTime = now,
                    durationMinutes = durationMins,
                    jalaliDate = com.example.util.JalaliCalendar.getTodayJalali().toString(),
                    notes = notes
                )
            }
        )
    }

    // Start Timer Modal Dialog
    if (showStartTimerDialog) {
        val categories = listOf(
            "تدریس (کسر ۱ ساعت از موظفی حوزه)",
            "مطالعه برای تدریس (کسر از موظفی حوزه)",
            "طراحی و تبلیغات",
            "امور قرآنی و حلقه‌ها",
            "مشاوره و پاسخ به شبهات",
            "امور اداری و اجرایی",
            "نماز جماعت و منبر",
            "سایر"
        )
        val suggestedHashtags = listOf(
            "#تدریس", "#مطالعه_تدریس", "#پوستر", "#تبلیغات",
            "#فقه", "#اصول", "#حلقه_صالحین", "#مشاوره", "#نماز_جماعت"
        )
        var selectedCategory by remember { mutableStateOf(categories[0]) }

        AlertDialog(
            onDismissRequest = { showStartTimerDialog = false },
            title = { Text("تنظیم زمان‌گیری زنده", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newLiveTaskTitle,
                        onValueChange = { newLiveTaskTitle = it },
                        label = { Text("عنوان فعالیت (مثلاً: تدریس فقه / مطالعه لمعه)") },
                        modifier = Modifier.fillMaxWidth().testTag("live_timer_title_input"),
                        singleLine = true
                    )

                    Text("محل انجام فعالیت:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newLiveTaskWorkplace == "HOWZEH",
                            onClick = { newLiveTaskWorkplace = "HOWZEH" },
                            label = { Text("حوزه علمیه") }
                        )
                        FilterChip(
                            selected = newLiveTaskWorkplace == "MOSQUE",
                            onClick = { newLiveTaskWorkplace = "MOSQUE" },
                            label = { Text("مسجد") }
                        )
                    }

                    Text("دسته‌بندی موضوعی:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        categories.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = cat
                                        if (cat.contains("تدریس") && !cat.contains("مطالعه")) {
                                            newLiveTaskTag = "#تدریس"
                                        } else if (cat.contains("مطالعه")) {
                                            newLiveTaskTag = "#مطالعه_تدریس"
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        selectedCategory = cat
                                        if (cat.contains("تدریس") && !cat.contains("مطالعه")) {
                                            newLiveTaskTag = "#تدریس"
                                        } else if (cat.contains("مطالعه")) {
                                            newLiveTaskTag = "#مطالعه_تدریس"
                                        }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(cat, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Text("هشتگ اختصاصی:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = newLiveTaskTag,
                        onValueChange = { newLiveTaskTag = it },
                        label = { Text("هشتگ دلخواه (مثال: #تدریس یا #پوستر)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestedHashtags) { h ->
                            FilterChip(
                                selected = newLiveTaskTag == h,
                                onClick = { newLiveTaskTag = h },
                                label = { Text(h, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (newLiveTaskWorkplace == "HOWZEH" && (newLiveTaskTag.contains("تدریس") || selectedCategory.contains("تدریس"))) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "توجه: در حوزه علمیه، مدت زمان تدریس (۱ ساعت) و مطالعه برای تدریس جزء ساعت موظفی حوزه محسوب نشده و از زمان حضور کسر می‌گردد.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartTimerDialog = false
                        viewModel.startLiveTaskTimer(
                            workplace = newLiveTaskWorkplace,
                            title = newLiveTaskTitle.ifBlank { "فعالیت در حال انجام" },
                            tag = newLiveTaskTag.ifBlank { "#عمومی" }
                        )
                    }
                ) {
                    Text("شروع تایمر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimerDialog = false }) { Text("انصراف") }
            }
        )
    }
}
