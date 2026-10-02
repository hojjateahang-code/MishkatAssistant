package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PunchLogEntity
import com.example.util.JalaliCalendar
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAttendanceScreen(
    viewModel: AppViewModel,
    punches: List<PunchLogEntity>,
    activePunch: PunchLogEntity?,
    onOpenAiAssistant: () -> Unit
) {
    var selectedWorkplaceTab by remember { mutableStateOf("HOWZEH") } // "HOWZEH" or "MOSQUE"
    var showAdjustTargetDialog by remember { mutableStateOf(false) }

    val workplaceConfigs by viewModel.workplaceConfigs.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()

    val todayJalali = remember { JalaliCalendar.getTodayJalali() }
    val todayHijri = remember(todayJalali) { JalaliCalendar.jalaliToHijri(todayJalali) }
    val todayStr = remember { todayJalali.toString() }

    // Dynamic Target Daily Minutes from Room Database
    val currentConfig = workplaceConfigs.firstOrNull { it.workplace == selectedWorkplaceTab }
    val targetDailyMins = currentConfig?.targetDailyMinutes?.toLong()
        ?: if (selectedWorkplaceTab == "HOWZEH") 240L else 180L

    // Calculate Today's Total Gross Minutes for Selected Workplace
    val todayPunchesForWorkplace = punches.filter {
        it.workplace == selectedWorkplaceTab && it.jalaliDate == todayStr
    }

    val grossPresentMinutesToday = todayPunchesForWorkplace.sumOf { p ->
        val endTime = p.checkOutTime ?: System.currentTimeMillis()
        (endTime - p.checkInTime) / 60000
    }

    // Howzeh Teaching and Study Deductions
    val (teachingDeductionMins, studyDeductionMins) = remember(activities, todayStr, selectedWorkplaceTab) {
        if (selectedWorkplaceTab == "HOWZEH") {
            viewModel.getTeachingAndStudyDeductions(todayStr)
        } else {
            Pair(0, 0)
        }
    }
    val totalDeductionMins = teachingDeductionMins + studyDeductionMins
    val netPresentMinutesToday = maxOf(0L, grossPresentMinutesToday - totalDeductionMins)

    val diffMins = netPresentMinutesToday - targetDailyMins
    val isDeficit = diffMins < 0

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner / Workplace Switcher & Dual Calendar (Shamsi + Hijri)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "دستیار ساماندهی شغلی و حضور غیاب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "امروز: ${todayJalali.toPersianDigits()} (${todayJalali.getMonthName()})  |  ${todayHijri.toPersianDigits()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Segmented Control
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedWorkplaceTab == "HOWZEH",
                            onClick = { selectedWorkplaceTab = "HOWZEH" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier.testTag("tab_howzeh")
                        ) {
                            Text("حوزه علمیه")
                        }
                        SegmentedButton(
                            selected = selectedWorkplaceTab == "MOSQUE",
                            onClick = { selectedWorkplaceTab = "MOSQUE" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.Mosque, contentDescription = null) },
                            modifier = Modifier.testTag("tab_mosque")
                        ) {
                            Text("مسجد")
                        }
                    }
                }
            }
        }

        // Active Check-In & Punch Clock Card
        item {
            val isActiveInThisWorkplace = activePunch != null && activePunch.workplace == selectedWorkplaceTab

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActiveInThisWorkplace) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isActiveInThisWorkplace) Color(0xFF10B981) else Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isActiveInThisWorkplace) "در حال حاضر: حاضر در محل کار" else "وضعیت: عدم حضور ثبت‌شده",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (isActiveInThisWorkplace) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "زمان ورود: ${timeFormat.format(Date(activePunch?.checkInTime ?: 0L))}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (activePunch == null) {
                        Button(
                            onClick = { viewModel.checkIn(selectedWorkplaceTab) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("punch_check_in_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ثبت ورود الان به ${if (selectedWorkplaceTab == "HOWZEH") "حوزه علمیه" else "مسجد"}", fontWeight = FontWeight.Bold)
                        }
                    } else if (isActiveInThisWorkplace) {
                        Button(
                            onClick = { viewModel.checkOut(activePunch) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("punch_check_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ثبت خروج از ${if (selectedWorkplaceTab == "HOWZEH") "حوزه علمیه" else "مسجد"}", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Active in the OTHER workplace
                        OutlinedButton(
                            onClick = {
                                viewModel.checkOut(activePunch)
                                viewModel.checkIn(selectedWorkplaceTab)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("انتقال و ثبت ورود به ${if (selectedWorkplaceTab == "HOWZEH") "حوزه علمیه" else "مسجد"}")
                        }
                    }
                }
            }
        }

        // Attendance Target & Deficit / Overtime Metrics Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شاخص موظفی و کسری / اضافه کار",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(
                            onClick = { showAdjustTargetDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تنظیم شاخص", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val netHours = netPresentMinutesToday / 60
                    val netMinsRemainder = netPresentMinutesToday % 60
                    val targetHours = targetDailyMins / 60
                    val targetMinsRemainder = targetDailyMins % 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("حضور خالص احتسابی:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                "$netHours ساعت و $netMinsRemainder دقیقه",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("شاخص موظفی روزانه:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            val targetStr = if (targetMinsRemainder > 0) {
                                "$targetHours ساعت و $targetMinsRemainder دقیقه"
                            } else {
                                "$targetHours ساعت"
                            }
                            Text(
                                targetStr,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Howzeh Teaching and Study Deductions Breakdown
                    if (selectedWorkplaceTab == "HOWZEH" && totalDeductionMins > 0) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "کسورات آموزشی حوزه علمیه:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "• کل حضور فیزیکی: ${grossPresentMinutesToday / 60} ساعت و ${grossPresentMinutesToday % 60} دقیقه",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (teachingDeductionMins > 0) {
                                    Text(
                                        "• کسر تدریس (هر جلسه ۱ ساعت): ${teachingDeductionMins / 60} ساعت و ${teachingDeductionMins % 60} دقیقه",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (studyDeductionMins > 0) {
                                    Text(
                                        "• کسر مطالعه اختصاصی تدریس: ${studyDeductionMins / 60} ساعت و ${studyDeductionMins % 60} دقیقه",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    "زمان تدریس و مطالعه برای تدریس جزء ساعت موظفی حوزه محسوب نمی‌گردد.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val progress = if (targetDailyMins > 0) {
                        (netPresentMinutesToday.toFloat() / targetDailyMins.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (isDeficit) MaterialTheme.colorScheme.secondary else Color(0xFF10B981)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Deficit / Overtime Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDeficit) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeficit) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDeficit) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                        Spacer(Modifier.width(8.dp))
                        val absDiffHours = Math.abs(diffMins) / 60
                        val absDiffMins = Math.abs(diffMins) % 60
                        Text(
                            text = if (isDeficit) {
                                "کسری حضور امروز: $absDiffHours ساعت و $absDiffMins دقیقه"
                            } else {
                                "اضافه کار امروز: $absDiffHours ساعت و $absDiffMins دقیقه"
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isDeficit) Color(0xFF991B1B) else Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        // Section Title: History of Punches
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاریخچه ترددهای ثبت‌شده",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onOpenAiAssistant) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "تحلیل هوشمند", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        val historyList = punches.filter { it.workplace == selectedWorkplaceTab }
        if (historyList.isEmpty()) {
            item {
                Text(
                    "هنوز ترددی برای این محل کار ثبت نشده است.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(historyList, key = { it.id }) { punch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تاریخ: ${punch.jalaliDate}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            val inStr = timeFormat.format(Date(punch.checkInTime))
                            val outStr = if (punch.checkOutTime != null) timeFormat.format(Date(punch.checkOutTime)) else "در حال حضور"
                            Text(
                                text = "ورود: $inStr  |  خروج: $outStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!punch.note.isNull_Blank()) {
                                Text(
                                    text = "یادداشت: ${punch.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deletePunch(punch) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // Adjust Target Hours Dialog (with 30-min steps)
    if (showAdjustTargetDialog) {
        var tempMinutes by remember(targetDailyMins) { mutableIntStateOf(targetDailyMins.toInt()) }

        AlertDialog(
            onDismissRequest = { showAdjustTargetDialog = false },
            title = {
                Text(
                    text = "تنظیم شاخص موظفی ${if (selectedWorkplaceTab == "HOWZEH") "حوزه علمیه" else "مسجد"}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val h = tempMinutes / 60
                    val m = tempMinutes % 60
                    val textDisplay = if (m > 0) "$h ساعت و $m دقیقه (${h + 0.5f} ساعت)" else "$h ساعت"

                    Text(
                        text = textDisplay,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (tempMinutes >= 60) tempMinutes -= 30
                            }
                        ) {
                            Text("- ۳۰ دقیقه")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (tempMinutes <= 720) tempMinutes += 30
                            }
                        ) {
                            Text("+ ۳۰ دقیقه")
                        }
                    }

                    Slider(
                        value = tempMinutes.toFloat(),
                        onValueChange = {
                            // Snap to nearest 30 minutes
                            val rounded = (Math.round(it / 30f) * 30).toInt().coerceIn(30, 720)
                            tempMinutes = rounded
                        },
                        valueRange = 30f..720f,
                        steps = 22 // 23 possible values in steps of 30 mins
                    )

                    Text(
                        text = "تغییرات بلافاصله در کل برنامه، صفحه اصلی و محاسبات کسری/مازاد حضور اعمال می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkplaceConfig(selectedWorkplaceTab, tempMinutes)
                        showAdjustTargetDialog = false
                    }
                ) {
                    Text("اعمال و ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustTargetDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
