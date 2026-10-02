package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.util.JalaliCalendar
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    initialWorkplace: String = "HOWZEH",
    onDismiss: () -> Unit,
    onConfirm: (workplace: String, title: String, tag: String, durationMins: Int, notes: String) -> Unit
) {
    var workplace by remember { mutableStateOf(initialWorkplace) }
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }
    var selectedTag by remember { mutableStateOf("#برنامه‌های_قرآنی") }
    var customTag by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val presetTags = listOf(
        "#برنامه‌های_قرآنی",
        "#امور_فرهنگی",
        "#تدریس",
        "#مطالعه_تدریس",
        "#پوستر_و_تبلیغات",
        "#جلسات_و_شوراها",
        "#پاسخگویی_شرعی",
        "#امور_اجتماعی"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت فعالیت جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workplace selector
                Text("محل انجام فعالیت:", style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = workplace == "HOWZEH",
                        onClick = { workplace = "HOWZEH" },
                        label = { Text("حوزه علمیه") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.weight(1f).testTag("dialog_workplace_howzeh")
                    )
                    FilterChip(
                        selected = workplace == "MOSQUE",
                        onClick = { workplace = "MOSQUE" },
                        label = { Text("مسجد") },
                        leadingIcon = { Icon(Icons.Default.Mosque, contentDescription = null) },
                        modifier = Modifier.weight(1f).testTag("dialog_workplace_mosque")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان فعالیت (مثلاً: آماده‌سازی پوستر شهدا)") },
                    modifier = Modifier.fillMaxWidth().testTag("activity_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("مدت زمان انجام (به دقیقه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("activity_duration_input"),
                    singleLine = true
                )

                Text("دسته / تگ فعالیت:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customTag,
                    onValueChange = {
                        customTag = it
                        if (it.isNotBlank()) selectedTag = if (it.startsWith("#")) it else "#$it"
                    },
                    label = { Text("تگ دلخواه جدید (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (workplace == "HOWZEH" && (selectedTag.contains("تدریس") || title.contains("تدریس"))) {
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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات تکمیلی") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationMins = durationText.toIntOrNull() ?: 30
                    val finalTag = if (customTag.isNotBlank()) {
                        if (customTag.startsWith("#")) customTag else "#$customTag"
                    } else selectedTag
                    onConfirm(workplace, title.ifBlank { "فعالیت روزانه" }, finalTag, durationMins, notes)
                },
                modifier = Modifier.testTag("submit_add_activity_button")
            ) {
                Text("ثبت فعالیت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddTaskReminderDialog(
    initialDate: com.example.util.JalaliCalendar.JalaliDate = com.example.util.JalaliCalendar.getTodayJalali(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, workplace: String, dueDateMillis: Long, jalaliDateStr: String, priority: String, tag: String, earlyReminderHours: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var workplace by remember { mutableStateOf("ALL") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var categoryTag by remember { mutableStateOf("#برنامه‌های_قرآنی") }

    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember { mutableIntStateOf(initialDate.day) }

    var selectedHour by remember { mutableIntStateOf(10) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var earlyReminderHours by remember { mutableIntStateOf(48) } // پیش‌فرض: ۲ روز قبل

    val taskTags = listOf(
        "#برنامه‌های_قرآنی", "#امور_فرهنگی", "#تدریس", "#مطالعه_تدریس",
        "#جلسات", "#مراسم_و_مناسبت‌ها", "#پاسخ_به_شبهات", "#تجهیزات", "#عمومی"
    )

    val earlyAlertOptions = listOf(
        Pair(0, "همان موقع (بدون پیش‌هشدار)"),
        Pair(1, "۱ ساعت قبل"),
        Pair(12, "۱۲ ساعت قبل"),
        Pair(24, "۱ روز قبل (۲۴ ساعت)"),
        Pair(48, "۲ روز قبل (۴۸ ساعت)"),
        Pair(72, "۳ روز قبل (۷۲ ساعت)")
    )

    val maxDays = com.example.util.JalaliCalendar.getDaysInJalaliMonth(selectedYear, selectedMonth)
    if (selectedDay > maxDays) selectedDay = maxDays

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد یادآور با آلارم و تقویم", fontWeight = FontWeight.Bold) },
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
                    label = { Text("عنوان کار یا یادآور") },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("جزییات و توضیحات") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Date Selection
                Text("تاریخ موعد در تقویم:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("روز ($selectedDay)", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = selectedDay.toFloat(),
                            onValueChange = { selectedDay = it.toInt() },
                            valueRange = 1f..maxDays.toFloat(),
                            steps = (maxDays - 2).coerceAtLeast(0)
                        )
                    }

                    // Month Selector
                    Column(modifier = Modifier.weight(1.2f)) {
                        val mName = com.example.util.JalaliCalendar.monthNames.getOrElse(selectedMonth - 1) { "" }
                        Text("ماه ($mName)", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = selectedMonth.toFloat(),
                            onValueChange = { selectedMonth = it.toInt() },
                            valueRange = 1f..12f,
                            steps = 10
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تاریخ انتخابی: $selectedDay ${com.example.util.JalaliCalendar.monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Time Selection
                Text("ساعت هشدار:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ساعت: ${String.format("%02d", selectedHour)}", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = selectedHour.toFloat(),
                            onValueChange = { selectedHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("دقیقه: ${String.format("%02d", selectedMinute)}", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = selectedMinute.toFloat(),
                            onValueChange = { selectedMinute = it.toInt() },
                            valueRange = 0f..55f,
                            steps = 10
                        )
                    }
                }

                // Pre-Alarm Option
                Text("پیش‌هشدار و آلارم زودهنگام سیستم:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    earlyAlertOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { earlyReminderHours = opt.first }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = earlyReminderHours == opt.first,
                                onClick = { earlyReminderHours = opt.first }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(opt.second, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Workplace
                Text("مربوط به:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = workplace == "HOWZEH",
                        onClick = { workplace = "HOWZEH" },
                        label = { Text("حوزه") }
                    )
                    FilterChip(
                        selected = workplace == "MOSQUE",
                        onClick = { workplace = "MOSQUE" },
                        label = { Text("مسجد") }
                    )
                    FilterChip(
                        selected = workplace == "ALL",
                        onClick = { workplace = "ALL" },
                        label = { Text("هر دو / عمومی") }
                    )
                }

                // Priority
                Text("اولویت:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = priority == "HIGH",
                        onClick = { priority = "HIGH" },
                        label = { Text("بالا") }
                    )
                    FilterChip(
                        selected = priority == "MEDIUM",
                        onClick = { priority = "MEDIUM" },
                        label = { Text("متوسط") }
                    )
                    FilterChip(
                        selected = priority == "LOW",
                        onClick = { priority = "LOW" },
                        label = { Text("پایین") }
                    )
                }

                // Tag Selection
                Text("دسته‌بندی، هشتگ و برچسب یادآور:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = categoryTag,
                    onValueChange = { categoryTag = it },
                    label = { Text("تایپ برچسب جدید یا هشتگ (مثال: تدریس_فقه یا پیگیری_شهرداری)") },
                    placeholder = { Text("#برچسب_دلخواه_شما") },
                    modifier = Modifier.fillMaxWidth().testTag("task_tag_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text("پیشنهادهای آماده یا انتخاب سریع:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val allSuggestedTags = listOf("#عمومی", "#حوزه", "#مسجد", "#تدریس", "#جلسه_شورا", "#فرهنگی", "#قرآنی", "#پیگیری", "#مطالعه", "#مالی")
                    items(allSuggestedTags) { tag ->
                        FilterChip(
                            selected = categoryTag == tag,
                            onClick = { categoryTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = com.example.util.JalaliCalendar.jalaliToGregorian(selectedYear, selectedMonth, selectedDay)
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                    cal.set(Calendar.MINUTE, selectedMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val dueDateMillis = cal.timeInMillis
                    val dateStr = String.format("%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay)

                    val cleanedTag = if (categoryTag.isNotBlank()) {
                        val trimmed = categoryTag.trim().replace(" ", "_")
                        if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                    } else "#عمومی"

                    onConfirm(
                        title.ifBlank { "یادآور کار" },
                        desc,
                        workplace,
                        dueDateMillis,
                        dateStr,
                        priority,
                        cleanedTag,
                        earlyReminderHours
                    )
                },
                modifier = Modifier.testTag("submit_add_task_button")
            ) {
                Text("ذخیره و تنظیم آلارم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddFinancialTransactionDialog(
    initialWorkplace: String = "HOWZEH",
    onDismiss: () -> Unit,
    onConfirm: (workplace: String, type: String, amount: Long, category: String, accountSource: String, desc: String, attachmentPath: String?, refNum: String?, partyName: String) -> Unit
) {
    var workplace by remember { mutableStateOf(initialWorkplace) }
    var type by remember { mutableStateOf("EXPENSE") } // INCOME / EXPENSE
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("برنامه‌های فرهنگی") }
    var accountSource by remember { mutableStateOf("صندوق اصلی") }
    var partyName by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var refNum by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> attachmentUri = uri }
    )

    val incomeCategories = listOf("شهریه و حق‌الزحمه", "نذورات و کمک‌های خیرین", "بودجه فرهنگی", "هدایا", "سایر ورودی‌ها")
    val expenseCategories = listOf("برنامه‌های فرهنگی و قرآنی", "خرید تجهیزات و ملزومات", "قبوض آب/برق/گاز", "پذیرایی و مناسبت‌ها", "کمک به نیازمندان", "سایر هزینه‌ها")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت سند مالی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workplace
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

                // Type Toggle (INCOME / EXPENSE)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = "EXPENSE"
                            category = expenseCategories.first()
                        },
                        colors = if (type == "EXPENSE") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f).testTag("financial_type_expense")
                    ) {
                        Text("هزینه (خروجی)")
                    }

                    Button(
                        onClick = {
                            type = "INCOME"
                            category = incomeCategories.first()
                        },
                        colors = if (type == "INCOME") ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f).testTag("financial_type_income")
                    ) {
                        Text("درآمد (ورودی)")
                    }
                }

                val parsedAmount = com.example.util.JalaliCalendar.fromPersianDigits(amountText).replace(",", "").toLongOrNull() ?: 0L

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val cleanDigits = com.example.util.JalaliCalendar.fromPersianDigits(input).filter { it.isDigit() }
                        if (cleanDigits.isBlank()) {
                            amountText = ""
                        } else {
                            val num = cleanDigits.toLongOrNull()
                            amountText = if (num != null) String.format(Locale.US, "%,d", num) else cleanDigits
                        }
                    },
                    label = { Text("مبلغ به ریال (جداکننده سه‌رقمی خودکار)") },
                    supportingText = {
                        if (parsedAmount > 0) {
                            Text(
                                text = "معادل: ${String.format(Locale.US, "%,d", parsedAmount / 10)} تومان  |  ${String.format(Locale.US, "%,d", parsedAmount)} ریال",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("financial_amount_input"),
                    singleLine = true
                )

                Text("سرفصل / محل مصرف:", style = MaterialTheme.typography.bodyMedium)
                val currentCategories = if (type == "INCOME") incomeCategories else expenseCategories
                Column {
                    currentCategories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { category = cat }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = category == cat, onClick = { category = cat })
                            Spacer(Modifier.width(8.dp))
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = accountSource,
                    onValueChange = { accountSource = it },
                    label = { Text("منبع / حساب (مثلاً: صندوق مسجد یا کارت سپه)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("طرف حساب / بانی / فروشگاه (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = refNum,
                    onValueChange = { refNum = it },
                    label = { Text("شماره پیگیری / شماره فاکتور (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات و بابت") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Attachment Section
                Text("پیوست تصویر رسید / فاکتور معتبر:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("attach_receipt_button")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (attachmentUri == null) "انتخاب تصویر رسید یا فاکتور" else "تغییر تصویر رسید انتخاب‌شده")
                }

                attachmentUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "رسید پیوست شده",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = com.example.util.JalaliCalendar.fromPersianDigits(amountText).replace(",", "").toLongOrNull() ?: 0L
                    onConfirm(workplace, type, amount, category, accountSource, desc, attachmentUri?.toString(), refNum, partyName)
                },
                modifier = Modifier.testTag("submit_financial_tx_button")
            ) {
                Text("ثبت در دفتر حسابرسی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun ImageViewerModal(
    imageUriStr: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تصویر رسید / فاکتور معتبر", fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = rememberAsyncImagePainter(imageUriStr),
                    contentDescription = "بزرگنمایی رسید",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
