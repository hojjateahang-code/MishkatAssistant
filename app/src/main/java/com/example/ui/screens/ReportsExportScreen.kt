package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ActivityLogEntity
import com.example.data.FinancialTransactionEntity
import com.example.data.PunchLogEntity
import com.example.util.JalaliCalendar
import com.example.util.PdfExcelExportHelper
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsExportScreen(
    viewModel: AppViewModel,
    punches: List<PunchLogEntity>,
    activities: List<ActivityLogEntity>,
    transactions: List<FinancialTransactionEntity>
) {
    val context = LocalContext.current
    var selectedWorkplace by remember { mutableStateOf("HOWZEH") } // "HOWZEH" or "MOSQUE"
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Daily Summary, 1 = Punches by Day, 2 = Activities, 3 = Financials

    // Date Range Presets: "LAST_7", "LAST_30", "CURRENT_MONTH", "ALL", "CUSTOM"
    var dateRangeFilter by remember { mutableStateOf("LAST_30") }
    var customStartDate by remember { mutableStateOf(JalaliCalendar.getDaysAgoJalali(30).toString()) }
    var customEndDate by remember { mutableStateOf(JalaliCalendar.getTodayJalali().toString()) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    val allLeaves by viewModel.allLeaves.collectAsStateWithLifecycle()
    val allMissions by viewModel.allMissions.collectAsStateWithLifecycle()
    val allHolidays by viewModel.allHolidays.collectAsStateWithLifecycle()
    val workplaceConfigs by viewModel.workplaceConfigs.collectAsStateWithLifecycle()

    val todayJalali = remember { JalaliCalendar.getTodayJalali() }
    val todayStr = remember { todayJalali.toString() }

    // Date filtering function
    fun isDateInRange(dateStr: String): Boolean {
        return when (dateRangeFilter) {
            "LAST_7" -> {
                val sevenDaysAgo = JalaliCalendar.getDaysAgoJalali(7).toString()
                JalaliCalendar.compareJalaliDates(dateStr, sevenDaysAgo) >= 0 &&
                        JalaliCalendar.compareJalaliDates(dateStr, todayStr) <= 0
            }
            "LAST_30" -> {
                val thirtyDaysAgo = JalaliCalendar.getDaysAgoJalali(30).toString()
                JalaliCalendar.compareJalaliDates(dateStr, thirtyDaysAgo) >= 0 &&
                        JalaliCalendar.compareJalaliDates(dateStr, todayStr) <= 0
            }
            "CURRENT_MONTH" -> {
                val firstDay = JalaliCalendar.getFirstDayOfCurrentJalaliMonth().toString()
                JalaliCalendar.compareJalaliDates(dateStr, firstDay) >= 0 &&
                        JalaliCalendar.compareJalaliDates(dateStr, todayStr) <= 0
            }
            "CUSTOM" -> {
                JalaliCalendar.compareJalaliDates(dateStr, customStartDate) >= 0 &&
                        JalaliCalendar.compareJalaliDates(dateStr, customEndDate) <= 0
            }
            else -> true // ALL
        }
    }

    val periodTitle = remember(dateRangeFilter, customStartDate, customEndDate) {
        when (dateRangeFilter) {
            "LAST_7" -> "۷ روز اخیر"
            "LAST_30" -> "۳۰ روز اخیر"
            "CURRENT_MONTH" -> "ماه جاری (${todayJalali.getMonthName()} ${todayJalali.year})"
            "CUSTOM" -> "از $customStartDate تا $customEndDate"
            else -> "همه زمان‌ها"
        }
    }

    // Filter workplace records
    val workplacePunches = punches.filter { it.workplace == selectedWorkplace && isDateInRange(it.jalaliDate) }
    val workplaceActivities = activities.filter { (it.workplace == selectedWorkplace || it.workplace == "ALL") && isDateInRange(it.jalaliDate) }
    val workplaceLeaves = allLeaves.filter { it.workplace == selectedWorkplace && isDateInRange(it.jalaliDate) }
    val workplaceMissions = allMissions.filter { it.workplace == selectedWorkplace && isDateInRange(it.jalaliDate) }
    val workplaceTransactions = transactions.filter { it.workplace == selectedWorkplace && isDateInRange(it.jalaliDate) }

    val totalIncome = workplaceTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = workplaceTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    // Distinct Dates in range for Daily Breakdown
    val distinctDates = remember(workplacePunches, workplaceLeaves, workplaceMissions, workplaceActivities, dateRangeFilter, customStartDate, customEndDate) {
        (workplacePunches.map { it.jalaliDate } +
                workplaceLeaves.map { it.jalaliDate } +
                workplaceMissions.map { it.jalaliDate } +
                workplaceActivities.map { it.jalaliDate } +
                listOf(todayStr)
                ).distinct()
            .filter { isDateInRange(it) }
            .sortedWith { d1, d2 -> JalaliCalendar.compareJalaliDates(d2, d1) } // descending
    }

    // Daily Attendance Calculations
    val dailyCalculations = remember(distinctDates, selectedWorkplace, workplacePunches, workplaceActivities, workplaceLeaves, workplaceMissions, allHolidays, workplaceConfigs) {
        distinctDates.map { d ->
            viewModel.calculateDayAttendance(
                dateStr = d,
                workplace = selectedWorkplace,
                punchesList = workplacePunches,
                activitiesList = workplaceActivities,
                leavesList = workplaceLeaves,
                missionsList = workplaceMissions,
                holidaysList = allHolidays,
                configsList = workplaceConfigs
            )
        }
    }

    // Grand Totals for the Selected Period
    val grandGrossMinutes = dailyCalculations.sumOf { it.grossPresenceMinutes }
    val grandTeachingDeductions = dailyCalculations.sumOf { it.teachingDeductionMinutes }
    val grandStudyDeductions = dailyCalculations.sumOf { it.studyDeductionMinutes }
    val grandRestDeductions = dailyCalculations.sumOf { it.restDeductionMinutes }
    val grandTotalDeductions = dailyCalculations.sumOf { it.totalDeductionMinutes.toLong() }
    val grandLeaveMinutes = dailyCalculations.sumOf { it.leaveMinutes }
    val grandMissionMinutes = dailyCalculations.sumOf { it.missionMinutes }
    val grandEffectiveMinutes = dailyCalculations.sumOf { it.netEffectiveMinutes }
    val grandTargetMinutes = dailyCalculations.sumOf { it.targetDailyMinutes }
    val grandOvertimeMinutes = dailyCalculations.sumOf { it.overtimeMinutes }
    val grandDeficitMinutes = dailyCalculations.sumOf { it.deficitMinutes }
    val netPeriodBalance = grandEffectiveMinutes - grandTargetMinutes

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workplace Switcher & Title Banner
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
                        text = "سامانه گزارش‌گیری رسمی، تجمیع روزانه و اسناد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "محاسبه دقیق کسورات تدریس و استراحت، مرخصی، مأموریت و تراز اضافه‌کاری/کسری",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedWorkplace == "HOWZEH",
                            onClick = { selectedWorkplace = "HOWZEH" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.School, contentDescription = null) }
                        ) {
                            Text("گزارش حوزه علمیه")
                        }
                        SegmentedButton(
                            selected = selectedWorkplace == "MOSQUE",
                            onClick = { selectedWorkplace = "MOSQUE" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.Mosque, contentDescription = null) }
                        ) {
                            Text("گزارش مسجد")
                        }
                    }
                }
            }
        }

        // Period & Date Range Filter Bar (Personalization)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("بازه زمانی گزارش:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = periodTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Quick filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = dateRangeFilter == "LAST_7",
                            onClick = { dateRangeFilter = "LAST_7" },
                            label = { Text("۷ روز اخیر", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = dateRangeFilter == "LAST_30",
                            onClick = { dateRangeFilter = "LAST_30" },
                            label = { Text("۳۰ روز اخیر", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = dateRangeFilter == "CURRENT_MONTH",
                            onClick = { dateRangeFilter = "CURRENT_MONTH" },
                            label = { Text("ماه جاری", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = dateRangeFilter == "ALL",
                            onClick = { dateRangeFilter = "ALL" },
                            label = { Text("همه زمان‌ها", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = dateRangeFilter == "CUSTOM",
                            onClick = {
                                dateRangeFilter = "CUSTOM"
                                showCustomRangeDialog = true
                            },
                            label = { Text("بازه سفارشی...", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Tabs: Daily Summary vs Punches by Day vs Activities vs Financials
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تجمیع روزانه", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("ریز ترددها", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("فعالیت‌ها", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("مالی", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // TAB 0: Daily Summary Breakdown (کارنامه و تجمیع روزانه)
        if (selectedTab == 0) {
            // Grand Period Summary Card
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
                            Text(
                                text = "کارنامه تجمیعی دوره ($periodTitle)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = {
                                    val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                                    val pdf = PdfExcelExportHelper.generateDailySummaryPdf(
                                        context = context,
                                        workplaceTitle = title,
                                        periodTitle = periodTitle,
                                        dailyCalculations = dailyCalculations
                                    )
                                    if (pdf != null) {
                                        PdfExcelExportHelper.shareFile(context, pdf, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "خطا در تولید PDF تجمیعی روزانه", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("خروجی PDF کارنامه", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("کل حضور فیزیکی:", style = MaterialTheme.typography.bodySmall)
                                    Text("${grandGrossMinutes / 60} ساعت و ${grandGrossMinutes % 60} دقیقه", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                                if (grandTotalDeductions > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("کسر تدریس، مطالعه و استراحت (-):", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB45309))
                                        Text("${grandTotalDeductions / 60}س ${grandTotalDeductions % 60}د", fontWeight = FontWeight.Bold, color = Color(0xFFB45309), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (grandLeaveMinutes + grandMissionMinutes > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("مرخصی و مأموریت موظفی (+):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0D9488))
                                        Text("${(grandLeaveMinutes + grandMissionMinutes) / 60}س ${(grandLeaveMinutes + grandMissionMinutes) % 60}د", fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("خالص حضور موظفی مؤثر:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${grandEffectiveMinutes / 60} ساعت و ${grandEffectiveMinutes % 60} دقیقه", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مجموع اضافه‌کاری (+):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                                    Text("${grandOvertimeMinutes / 60} ساعت و ${grandOvertimeMinutes % 60} دقیقه", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مجموع کسری کار (-):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    Text("${grandDeficitMinutes / 60} ساعت و ${grandDeficitMinutes % 60} دقیقه", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تراز خالص نهایی کارکرد:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    val isNetPlus = netPeriodBalance >= 0
                                    Text(
                                        text = if (isNetPlus) "+${netPeriodBalance / 60}س و ${netPeriodBalance % 60}د مازاد" else "-${Math.abs(netPeriodBalance) / 60}س و ${Math.abs(netPeriodBalance) % 60}د کسری",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNetPlus) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily List Items
            item {
                Text(
                    text = "ریز محاسبات به تفکیک روز (${dailyCalculations.size} روز):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (dailyCalculations.isEmpty()) {
                item {
                    Text("در این بازه زمانی هیچ داده‌ای یافت نشد.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(dailyCalculations, key = { it.jalaliDate }) { dayCalc ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (dayCalc.isHoliday) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${dayCalc.dayOfWeekName} ${dayCalc.jalaliDate}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (dayCalc.isHoliday) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = dayCalc.holidayTitle ?: "تعطیل",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val isOvertime = dayCalc.diffMinutes >= 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOvertime) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                                ) {
                                    val absH = Math.abs(dayCalc.diffMinutes) / 60
                                    val absM = Math.abs(dayCalc.diffMinutes) % 60
                                    Text(
                                        text = if (isOvertime) "+${absH}س ${absM}د (مازاد)" else "-${absH}س ${absM}د (کسری)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOvertime) Color(0xFF065F46) else Color(0xFF991B1B),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // First Check-in and Last Check-out row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ورود اولیه: ${dayCalc.firstCheckInTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "خروج نهایی: ${dayCalc.lastCheckOutTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "حضور فیزیکی: ${dayCalc.grossPresenceMinutes / 60}س ${dayCalc.grossPresenceMinutes % 60}د",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "موظفی: ${if (dayCalc.isHoliday) "۰ (تعطیل)" else "${dayCalc.targetDailyMinutes / 60}س"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (dayCalc.totalDeductionMinutes > 0 || dayCalc.leaveMinutes > 0 || dayCalc.missionMinutes > 0) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (dayCalc.totalDeductionMinutes > 0) {
                                        Text(
                                            text = "کسورات: ${dayCalc.totalDeductionMinutes} دقیقه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (dayCalc.leaveMinutes > 0) {
                                        Text(
                                            text = "مرخصی: ${dayCalc.leaveMinutes} دقیقه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0D9488)
                                        )
                                    }
                                    if (dayCalc.missionMinutes > 0) {
                                        Text(
                                            text = "مأموریت: ${dayCalc.missionMinutes} دقیقه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "خالص مؤثر: ${dayCalc.netEffectiveMinutes / 60}س ${dayCalc.netEffectiveMinutes % 60}د",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "وضعیت: ${dayCalc.statusText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dayCalc.isHoliday) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // سطر آخر: جمع کل موارد در گزارش
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سطر جمع کل گزارش (${dailyCalculations.size} روز)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (netPeriodBalance >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                ) {
                                    val absBal = Math.abs(netPeriodBalance)
                                    Text(
                                        text = if (netPeriodBalance >= 0) "تراز مثبت (+${absBal / 60}س ${absBal % 60}د)" else "کسری کل (-${absBal / 60}س ${absBal % 60}د)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("کل حضور ناخالص:", style = MaterialTheme.typography.bodySmall)
                                Text("${grandGrossMinutes / 60}س ${grandGrossMinutes % 60}د", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            if (grandTotalDeductions > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("کسورات تدریس و استراحت (-):", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB45309))
                                    Text("${grandTotalDeductions / 60}س ${grandTotalDeductions % 60}د", fontWeight = FontWeight.Bold, color = Color(0xFFB45309), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (grandLeaveMinutes + grandMissionMinutes > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مرخصی و مأموریت (+):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0D9488))
                                    Text("${(grandLeaveMinutes + grandMissionMinutes) / 60}س ${(grandLeaveMinutes + grandMissionMinutes) % 60}د", fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("خالص حضور مؤثر کل:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${grandEffectiveMinutes / 60}س ${grandEffectiveMinutes % 60}د", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مجموع ساعات موظفی دوره:", style = MaterialTheme.typography.bodySmall)
                                Text("${grandTargetMinutes / 60} ساعت", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مجموع کل اضافه‌کاری:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                                Text("${grandOvertimeMinutes / 60}س ${grandOvertimeMinutes % 60}د", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مجموع کل کسری کار:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("${grandDeficitMinutes / 60}س ${grandDeficitMinutes % 60}د", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: Detailed Punches (Grouped by Day - Collapsible)
        if (selectedTab == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ریز ترددها (دسته‌بندی‌شده بر اساس روز)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                            val pdf = PdfExcelExportHelper.generateAttendancePdf(
                                context = context,
                                workplaceTitle = title,
                                punches = workplacePunches,
                                leaves = workplaceLeaves,
                                missions = workplaceMissions,
                                totalDeductionMins = grandTotalDeductions.toInt(),
                                targetDailyMins = if (selectedWorkplace == "HOWZEH") 240L else 180L
                            )
                            if (pdf != null) {
                                PdfExcelExportHelper.shareFile(context, pdf, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در تولید PDF ترددها", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF ترددها", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            val punchesGroupedByDate = workplacePunches.groupBy { it.jalaliDate }
            if (punchesGroupedByDate.isEmpty()) {
                item {
                    Text("هیچ ترددی در این بازه زمانی ثبت نشده است.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                punchesGroupedByDate.forEach { (dateStr, dayPunches) ->
                    item(key = dateStr) {
                        var isExpanded by remember { mutableStateOf(dateStr == todayStr) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExpanded = !isExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${JalaliCalendar.getWeekdayName(dateStr)} $dateStr",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${dayPunches.size} نوبت تردد",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        dayPunches.forEach { punch ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        val inStr = timeFormat.format(Date(punch.checkInTime))
                                                        val outStr = if (punch.checkOutTime != null) timeFormat.format(Date(punch.checkOutTime)) else "حاضر"
                                                        val durMins = if (punch.checkOutTime != null) ((punch.checkOutTime - punch.checkInTime) / 60000) else 0L

                                                        Text(
                                                            text = "ورود: $inStr  |  خروج: $outStr",
                                                            fontWeight = FontWeight.SemiBold,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = "مدت: ${durMins / 60}س ${durMins % 60}د",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                        if (!punch.note.isNullOrBlank()) {
                                                            Text(
                                                                text = "یادداشت: ${punch.note}",
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
                }
            }
        }

        // TAB 2: Activities
        if (selectedTab == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ریز فعالیت‌ها (${workplaceActivities.size} فعالیت)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                            val pdf = PdfExcelExportHelper.generateActivitiesPdf(context, title, workplaceActivities)
                            if (pdf != null) {
                                PdfExcelExportHelper.shareFile(context, pdf, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در تولید PDF فعالیت‌ها", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF فعالیت‌ها", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (workplaceActivities.isEmpty()) {
                item {
                    Text("هیچ فعالیتی در این بازه زمانی ثبت نشده است.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(workplaceActivities, key = { it.id }) { act ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(act.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = act.categoryTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "تاریخ: ${act.jalaliDate}  |  مدت زمان: ${act.durationMinutes} دقیقه",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            if (act.notes.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "توضیحات: ${act.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: Financials
        if (selectedTab == 3) {
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
                            Text("خلاصه مالی و تراز حسابرسی", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = {
                                    val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                                    val pdf = PdfExcelExportHelper.generateFinancialPdf(
                                        context, title, workplaceTransactions, totalIncome, totalExpense, balance
                                    )
                                    if (pdf != null) {
                                        PdfExcelExportHelper.shareFile(context, pdf, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "خطا در تولید PDF مالی", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PDF مالی", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("کل درآمدها:", style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format(Locale.US, "%,d", totalIncome)} ریال", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("کل هزینه‌ها:", style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format(Locale.US, "%,d", totalExpense)} ریال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مانده حساب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format(Locale.US, "%,d", balance)} ریال",
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (workplaceTransactions.isEmpty()) {
                item {
                    Text("هیچ سند مالی در این بازه زمانی ثبت نشده است.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(workplaceTransactions, key = { it.id }) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${tx.jalaliDate} | ${tx.accountSource}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                if (tx.description.isNotBlank()) {
                                    Text(tx.description, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = "${if (tx.type == "INCOME") "+" else "-"}${String.format(Locale.US, "%,d", tx.amount)} ریال",
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "INCOME") Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // FEATURED: Combined Comprehensive PDF Report Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "گزارش جامع تلفیقی (حضور، غیاب و ریز فعالیت‌ها)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "شامل جمع کل ساعات حضور در حوزه علمیه و مسجد، کسر تدریس، مطالعه و استراحت، کارکرد خالص، جداول تردد و فعالیت‌ها با جایگاه مهر و امضای رسمی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val howzehPunches = punches.filter { it.workplace == "HOWZEH" && isDateInRange(it.jalaliDate) }
                            val mosquePunches = punches.filter { it.workplace == "MOSQUE" && isDateInRange(it.jalaliDate) }
                            val howzehActs = activities.filter { (it.workplace == "HOWZEH" || it.workplace == "ALL") && isDateInRange(it.jalaliDate) }
                            val mosqueActs = activities.filter { it.workplace == "MOSQUE" && isDateInRange(it.jalaliDate) }
                            val howzehLeaves = allLeaves.filter { it.workplace == "HOWZEH" && isDateInRange(it.jalaliDate) }.sumOf { it.durationMinutes.toLong() }
                            val howzehMissions = allMissions.filter { it.workplace == "HOWZEH" && isDateInRange(it.jalaliDate) }.sumOf { it.durationMinutes.toLong() }

                            val pdfFile = PdfExcelExportHelper.generateCombinedComprehensivePdf(
                                context = context,
                                howzehPunches = howzehPunches,
                                mosquePunches = mosquePunches,
                                howzehActivities = howzehActs,
                                mosqueActivities = mosqueActs,
                                howzehTeachingDeductionMins = grandTeachingDeductions,
                                howzehStudyDeductionMins = grandStudyDeductions,
                                howzehRestDeductionMins = grandRestDeductions,
                                howzehLeavesMins = howzehLeaves,
                                howzehMissionsMins = howzehMissions
                            )
                            if (pdfFile != null) {
                                PdfExcelExportHelper.shareFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در ایجاد گزارش جامع PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_combined_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تولید PDF گزارش جامع تلفیقی")
                    }
                }
            }
        }

        // CSV Excel Export Options
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("خروجی اکسل (CSV):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = PdfExcelExportHelper.generateActivityCsv(context, workplaceActivities)
                                if (csv != null) PdfExcelExportHelper.shareFile(context, csv, "text/csv")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اکسل فعالیت‌ها", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                val csv = PdfExcelExportHelper.generatePunchCsv(context, workplacePunches)
                                if (csv != null) PdfExcelExportHelper.shareFile(context, csv, "text/csv")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اکسل ترددها", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomRangeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("تعیین بازه زمانی دلخواه", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customStartDate,
                        onValueChange = { customStartDate = it },
                        label = { Text("از تاریخ (مثلاً 1405/06/01)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customEndDate,
                        onValueChange = { customEndDate = it },
                        label = { Text("تا تاریخ (مثلاً 1405/07/01)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCustomRangeDialog = false }) {
                    Text("اعمال بازه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }
}
