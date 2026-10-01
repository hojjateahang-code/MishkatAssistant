package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.data.PunchLogEntity
import com.example.ui.components.AddActivityDialog
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
    var showManualPunchDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    val todayStr = remember { JalaliCalendar.getTodayJalali().toString() }

    // Calculate Today's Total Minutes for Selected Workplace
    val todayPunchesForWorkplace = punches.filter {
        it.workplace == selectedWorkplaceTab && it.jalaliDate == todayStr
    }

    var presentMinutesToday by remember(todayPunchesForWorkplace, activePunch) {
        mutableLongStateOf(
            todayPunchesForWorkplace.sumOf { p ->
                val endTime = p.checkOutTime ?: System.currentTimeMillis()
                (endTime - p.checkInTime) / 60000
            }
        )
    }

    // Target daily minutes (default 240 mins / 4 hours for Howzeh, 180 mins / 3 hours for Mosque)
    val targetDailyMins = if (selectedWorkplaceTab == "HOWZEH") 240L else 180L
    val diffMins = presentMinutesToday - targetDailyMins
    val isDeficit = diffMins < 0

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner / Workplace Switcher
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
                        text = "امروز: ${JalaliCalendar.getTodayJalali().toPersianDigits()} - ${JalaliCalendar.monthNames[JalaliCalendar.getTodayJalali().month - 1]}",
                        style = MaterialTheme.typography.bodyMedium,
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

                    if (isActiveInThisWorkplace && activePunch != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "زمان ورود: ${timeFormat.format(Date(activePunch.checkInTime))}",
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
                            Icon(Icons.Default.Login, contentDescription = null)
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
                            Icon(Icons.Default.Logout, contentDescription = null)
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
                    Text("شاخص میزان حضور و محاسبه کسری / اضافه کار", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))

                    val presentHours = presentMinutesToday / 60
                    val presentMinsRemainder = presentMinutesToday % 60
                    val targetHours = targetDailyMins / 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("میزان حضور امروز:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("$presentHours ساعت و $presentMinsRemainder دقیقه", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("شاخص موظفی روزانه:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("$targetHours ساعت", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val progress = (presentMinutesToday.toFloat() / targetDailyMins.toFloat()).coerceIn(0f, 1f)
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
}

private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
