package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLogEntity
import com.example.data.HolidayEntity
import com.example.data.LeaveEntity
import com.example.data.MissionEntity
import com.example.data.NoteEntity
import com.example.data.PunchLogEntity
import com.example.data.TaskReminderEntity

import com.example.util.JalaliCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditPunchLogDialog(
    punch: PunchLogEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedPunch: PunchLogEntity) -> Unit,
    onDelete: (punch: PunchLogEntity) -> Unit
) {
    var workplace by remember { mutableStateOf(punch.workplace) }
    var jalaliDate by remember { mutableStateOf(punch.jalaliDate) }
    var note by remember { mutableStateOf(punch.note ?: "") }

    val calIn = remember(punch.checkInTime) { Calendar.getInstance().apply { timeInMillis = punch.checkInTime } }
    var inHourText by remember { mutableStateOf(calIn.get(Calendar.HOUR_OF_DAY).toString()) }
    var inMinText by remember { mutableStateOf(calIn.get(Calendar.MINUTE).toString()) }

    var hasCheckOut by remember { mutableStateOf(punch.checkOutTime != null) }
    val calOut = remember(punch.checkOutTime) {
        Calendar.getInstance().apply { timeInMillis = punch.checkOutTime ?: System.currentTimeMillis() }
    }
    var outHourText by remember { mutableStateOf(calOut.get(Calendar.HOUR_OF_DAY).toString()) }
    var outMinText by remember { mutableStateOf(calOut.get(Calendar.MINUTE).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش کامل ساعت و مشخصات تردد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workplace selector
                Text("محل انجام تردد:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = workplace == "HOWZEH",
                        onClick = { workplace = "HOWZEH" },
                        label = { Text("حوزه علمیه") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = workplace == "MOSQUE",
                        onClick = { workplace = "MOSQUE" },
                        label = { Text("مسجد") },
                        leadingIcon = { Icon(Icons.Default.Mosque, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Date
                OutlinedTextField(
                    value = jalaliDate,
                    onValueChange = { jalaliDate = it },
                    label = { Text("تاریخ جلالی (مثلاً 1405/07/03)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Check-in Time Inputs (Free exact numbers)
                Text("ساعت و دقیقه ورود:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inHourText,
                        onValueChange = { if (it.length <= 2) inHourText = it },
                        label = { Text("ساعت (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inMinText,
                        onValueChange = { if (it.length <= 2) inMinText = it },
                        label = { Text("دقیقه (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Check-out Time Inputs
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasCheckOut, onCheckedChange = { hasCheckOut = it })
                    Spacer(Modifier.width(6.dp))
                    Text("ساعت خروج ثبت شده است", fontWeight = FontWeight.SemiBold)
                }

                if (hasCheckOut) {
                    Text("ساعت و دقیقه خروج:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = outHourText,
                            onValueChange = { if (it.length <= 2) outHourText = it },
                            label = { Text("ساعت (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = outMinText,
                            onValueChange = { if (it.length <= 2) outMinText = it },
                            label = { Text("دقیقه (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات و نکات این تردد") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Delete Button inside dialog
                OutlinedButton(
                    onClick = { onDelete(punch); onDismiss() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("حذف این تردد")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateParts = jalaliDate.split("/")
                    val year = dateParts.getOrNull(0)?.toIntOrNull() ?: 1405
                    val month = dateParts.getOrNull(1)?.toIntOrNull() ?: 7
                    val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 1

                    val inH = inHourText.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val inM = inMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0

                    val gregCal = JalaliCalendar.jalaliToGregorian(year, month, day)
                    gregCal.set(Calendar.HOUR_OF_DAY, inH)
                    gregCal.set(Calendar.MINUTE, inM)
                    gregCal.set(Calendar.SECOND, 0)
                    val newCheckIn = gregCal.timeInMillis

                    val newCheckOut = if (hasCheckOut) {
                        val outH = outHourText.toIntOrNull()?.coerceIn(0, 23) ?: 12
                        val outM = outMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        val gregCalOut = JalaliCalendar.jalaliToGregorian(year, month, day)
                        gregCalOut.set(Calendar.HOUR_OF_DAY, outH)
                        gregCalOut.set(Calendar.MINUTE, outM)
                        gregCalOut.set(Calendar.SECOND, 0)
                        gregCalOut.timeInMillis
                    } else null

                    onConfirm(
                        punch.copy(
                            workplace = workplace,
                            checkInTime = newCheckIn,
                            checkOutTime = newCheckOut,
                            jalaliDate = jalaliDate,
                            note = note
                        )
                    )
                }
            ) {
                Text("ذخیره تغییرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditNoteDialog(
    initialNote: NoteEntity? = null,
    initialDate: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, folder: String, jalaliDate: String?, occasionTag: String, categoryTag: String) -> Unit,
    onDelete: ((NoteEntity) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var folder by remember { mutableStateOf(initialNote?.folder ?: "عمومی") }
    var customFolder by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(initialNote?.jalaliDate ?: initialDate ?: JalaliCalendar.getTodayJalali().toString()) }
    var linkToDate by remember { mutableStateOf(initialNote?.jalaliDate != null || initialDate != null) }
    var occasionTag by remember { mutableStateOf(initialNote?.occasionTag ?: "") }
    var categoryTag by remember { mutableStateOf(initialNote?.categoryTag ?: "#یادداشت") }

    val presetFolders = listOf("عمومی", "شخصی", "کاری", "حوزه علمیه", "مسجد", "مناسبت‌ها", "مطالعات", "ایده‌ها")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNote == null) "ثبت یادداشت جدید" else "ویرایش یادداشت", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یادداشت") },
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("متن و شرح یادداشت") },
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input"),
                    minLines = 4
                )

                // Folder selection
                Text("انتخاب پوشه / دسته‌بندی موضوعی:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetFolders.forEach { f ->
                        FilterChip(
                            selected = folder == f && customFolder.isBlank(),
                            onClick = { folder = f; customFolder = "" },
                            label = { Text(f) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customFolder,
                    onValueChange = {
                        customFolder = it
                        if (it.isNotBlank()) folder = it
                    },
                    label = { Text("ایجاد پوشه جدید (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Link to Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = linkToDate, onCheckedChange = { linkToDate = it })
                    Spacer(Modifier.width(6.dp))
                    Text("اتصال و نمایش در تاریخ مشخص تقویم", fontWeight = FontWeight.SemiBold)
                }

                if (linkToDate) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("تاریخ جلالی مرتبط (1405/07/03)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = occasionTag,
                    onValueChange = { occasionTag = it },
                    label = { Text("مناسبت یا رویداد مرتبط (مثال: شهادت امام علی / جلسه هیئت امنا)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = categoryTag,
                    onValueChange = { categoryTag = it },
                    label = { Text("هشتگ / برچسب (مثلاً #نکته_مهم)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (initialNote != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialNote); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف این یادداشت")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFolder = if (customFolder.isNotBlank()) customFolder.trim() else folder
                    val finalDate = if (linkToDate) dateStr.trim() else null
                    onConfirm(
                        title.ifBlank { "یادداشت جدید" },
                        content,
                        finalFolder,
                        finalDate,
                        occasionTag,
                        categoryTag
                    )
                },
                modifier = Modifier.testTag("submit_note_button")
            ) {
                Text("ذخیره یادداشت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditLeaveDialog(
    initialLeave: LeaveEntity? = null,
    initialDate: String = JalaliCalendar.getTodayJalali().toString(),
    onDismiss: () -> Unit,
    onConfirm: (workplace: String, leaveType: String, jalaliDate: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, durationMins: Int, reason: String) -> Unit,
    onDelete: ((LeaveEntity) -> Unit)? = null
) {
    var workplace by remember { mutableStateOf(initialLeave?.workplace ?: "HOWZEH") }
    var leaveType by remember { mutableStateOf(initialLeave?.leaveType ?: "HOURLY") } // HOURLY, DAILY, ESTEHAQY, ESTELAJI, BEDOON_HOQOOQ
    var jalaliDate by remember { mutableStateOf(initialLeave?.jalaliDate ?: initialDate) }

    var startHourText by remember { mutableStateOf((initialLeave?.startHour ?: 8).toString()) }
    var startMinText by remember { mutableStateOf((initialLeave?.startMinute ?: 0).toString()) }
    var endHourText by remember { mutableStateOf((initialLeave?.endHour ?: 12).toString()) }
    var endMinText by remember { mutableStateOf((initialLeave?.endMinute ?: 0).toString()) }

    var durationText by remember { mutableStateOf((initialLeave?.durationMinutes ?: 240).toString()) }
    var reason by remember { mutableStateOf(initialLeave?.reason ?: "") }

    val leaveTypes = listOf(
        Pair("HOURLY", "مرخصی ساعتی"),
        Pair("DAILY", "مرخصی روزانه (تمام موظفی)"),
        Pair("ESTEHAQY", "مرخصی استحقاقی"),
        Pair("ESTELAJI", "مرخصی استعلاجی"),
        Pair("BEDOON_HOQOOQ", "مرخصی بدون حقوق")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLeave == null) "ثبت درخواست / برگ مرخصی" else "ویرایش مرخصی", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workplace
                Text("محل ثبت مرخصی:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = workplace == "HOWZEH",
                        onClick = { workplace = "HOWZEH" },
                        label = { Text("حوزه علمیه") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = workplace == "MOSQUE",
                        onClick = { workplace = "MOSQUE" },
                        label = { Text("مسجد") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("نوع مرخصی:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Column {
                    leaveTypes.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    leaveType = item.first
                                    if (item.first == "DAILY" || item.first == "ESTEHAQY" || item.first == "ESTELAJI") {
                                        durationText = if (workplace == "HOWZEH") "240" else "180"
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = leaveType == item.first, onClick = { leaveType = item.first })
                            Spacer(Modifier.width(6.dp))
                            Text(item.second, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = jalaliDate,
                    onValueChange = { jalaliDate = it },
                    label = { Text("تاریخ مرخصی (1405/07/03)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (leaveType == "HOURLY") {
                    Text("زمان شروع مرخصی (ساعت و دقیقه):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHourText,
                            onValueChange = { if (it.length <= 2) startHourText = it },
                            label = { Text("ساعت شروع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startMinText,
                            onValueChange = { if (it.length <= 2) startMinText = it },
                            label = { Text("دقیقه شروع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("زمان پایان مرخصی (ساعت و دقیقه):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = endHourText,
                            onValueChange = { if (it.length <= 2) endHourText = it },
                            label = { Text("ساعت پایان") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endMinText,
                            onValueChange = { if (it.length <= 2) endMinText = it },
                            label = { Text("دقیقه پایان") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("مدت کل مرخصی (به دقیقه) - مثلا 240 برای 4 ساعت") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("علت یا توضیحات مرخصی") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (initialLeave != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialLeave); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف این مرخصی")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startH = startHourText.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val startM = startMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val endH = endHourText.toIntOrNull()?.coerceIn(0, 23) ?: 12
                    val endM = endMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val durMins = durationText.toIntOrNull() ?: 240

                    onConfirm(
                        workplace,
                        leaveType,
                        jalaliDate,
                        startH,
                        startM,
                        endH,
                        endM,
                        durMins,
                        reason
                    )
                },
                modifier = Modifier.testTag("submit_leave_button")
            ) {
                Text("ثبت مرخصی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditMissionDialog(
    initialMission: MissionEntity? = null,
    initialDate: String = JalaliCalendar.getTodayJalali().toString(),
    onDismiss: () -> Unit,
    onConfirm: (workplace: String, title: String, jalaliDate: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, durationMins: Int, description: String) -> Unit,
    onDelete: ((MissionEntity) -> Unit)? = null
) {
    var workplace by remember { mutableStateOf(initialMission?.workplace ?: "HOWZEH") }
    var title by remember { mutableStateOf(initialMission?.title ?: "") }
    var jalaliDate by remember { mutableStateOf(initialMission?.jalaliDate ?: initialDate) }

    var startHourText by remember { mutableStateOf((initialMission?.startHour ?: 8).toString()) }
    var startMinText by remember { mutableStateOf((initialMission?.startMinute ?: 0).toString()) }
    var endHourText by remember { mutableStateOf((initialMission?.endHour ?: 12).toString()) }
    var endMinText by remember { mutableStateOf((initialMission?.endMinute ?: 0).toString()) }

    var durationText by remember { mutableStateOf((initialMission?.durationMinutes ?: 240).toString()) }
    var description by remember { mutableStateOf(initialMission?.description ?: "") }

    val presetMissionTitles = listOf(
        "جلسه در مرکز مدیریت حوزه علمیه",
        "جلسه شورای هماهنگی و تبلیغات",
        "مأموریت فرهنگی و تبلیغی خارج از حوزه",
        "پیگیری امور اجرایی و عمرانی",
        "دیدار با مسئولین و نهادهای فرهنگی",
        "سفر کاری و همایش استانی"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMission == null) "ثبت مأموریت کاری (محاسبه به عنوان حضور)" else "ویرایش مأموریت", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ساعات مأموریت کاری خارج از مجموعه، به عنوان ساعات کارکرد موظفی معتبر محاسبه می‌گردد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text("محل مأموریت:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = workplace == "HOWZEH",
                        onClick = { workplace = "HOWZEH" },
                        label = { Text("حوزه علمیه") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = workplace == "MOSQUE",
                        onClick = { workplace = "MOSQUE" },
                        label = { Text("مسجد") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان مأموریت / جلسه") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("عناوین متداول:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetMissionTitles) { item ->
                        SuggestionChip(
                            onClick = { title = item },
                            label = { Text(item, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = jalaliDate,
                    onValueChange = { jalaliDate = it },
                    label = { Text("تاریخ مأموریت (1405/07/03)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("بازه زمانی مأموریت:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHourText,
                        onValueChange = { if (it.length <= 2) startHourText = it },
                        label = { Text("ساعت شروع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startMinText,
                        onValueChange = { if (it.length <= 2) startMinText = it },
                        label = { Text("دقیقه شروع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endHourText,
                        onValueChange = { if (it.length <= 2) endHourText = it },
                        label = { Text("ساعت پایان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endMinText,
                        onValueChange = { if (it.length <= 2) endMinText = it },
                        label = { Text("دقیقه پایان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("مدت مأموریت (به دقیقه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات و دستاورد مأموریت") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (initialMission != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialMission); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف این مأموریت")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sH = startHourText.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val sM = startMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val eH = endHourText.toIntOrNull()?.coerceIn(0, 23) ?: 12
                    val eM = endMinText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val dur = durationText.toIntOrNull() ?: 240
                    onConfirm(workplace, title.ifBlank { "مأموریت اداری" }, jalaliDate, sH, sM, eH, eM, dur, description)
                }
            ) {
                Text("ثبت مأموریت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditHolidayDialog(
    initialDate: String = JalaliCalendar.getTodayJalali().toString(),
    existingHoliday: HolidayEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (jalaliDate: String, title: String) -> Unit,
    onDelete: ((HolidayEntity) -> Unit)? = null
) {
    var jalaliDate by remember { mutableStateOf(existingHoliday?.jalaliDate ?: initialDate) }
    var title by remember { mutableStateOf(existingHoliday?.title ?: "تعطیل رسمی") }

    val presetHolidays = listOf(
        "تعطیل رسمی تقویمی",
        "شهادت امام رضا (ع)",
        "شهادت حضرت فاطمه (س)",
        "ارتحال امام خمینی (ره)",
        "تعطیلی به علت برودت هوا و یخبندان",
        "تعطیلی اداری اضطراری"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingHoliday == null) "ثبت روز تعطیل (معاف از موظفی)" else "ویرایش تعطیلی", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "در روزهای ثبت‌شده به عنوان تعطیل، ساعت موظفی صفر منظور شده و هیچ‌گونه کسری کاری متوجه شما نخواهد بود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = jalaliDate,
                    onValueChange = { jalaliDate = it },
                    label = { Text("تاریخ روز تعطیل (1405/07/03)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یا مناسبت تعطیلی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("عناوین پیشنهادی:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetHolidays) { item ->
                        SuggestionChip(
                            onClick = { title = item },
                            label = { Text(item, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (existingHoliday != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(existingHoliday); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف این روز تعطیل (بازگشت به روز عادی)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(jalaliDate, title.ifBlank { "تعطیل رسمی" })
                }
            ) {
                Text("ثبت تعطیلی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

