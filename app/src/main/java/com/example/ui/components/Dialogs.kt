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
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, workplace: String, dueDateMillis: Long, priority: String, tag: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var workplace by remember { mutableStateOf("ALL") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var categoryTag by remember { mutableStateOf("#عمومی") }

    var selectedHour by remember { mutableStateOf(10) }
    var selectedMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد یادآور / کار جدید", fontWeight = FontWeight.Bold) },
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

                Text("مربوط به:", style = MaterialTheme.typography.bodyMedium)
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

                Text("اولیت:", style = MaterialTheme.typography.bodyMedium)
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.HOUR_OF_DAY, 1) // Default 1 hour from now
                    val dueDateMillis = cal.timeInMillis
                    onConfirm(title.ifBlank { "یادآور کار" }, desc, workplace, dueDateMillis, priority, categoryTag)
                },
                modifier = Modifier.testTag("submit_add_task_button")
            ) {
                Text("ذخیره یادآور")
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
    onConfirm: (workplace: String, type: String, amount: Long, category: String, accountSource: String, desc: String, attachmentPath: String?, refNum: String?) -> Unit
) {
    var workplace by remember { mutableStateOf(initialWorkplace) }
    var type by remember { mutableStateOf("EXPENSE") } // INCOME / EXPENSE
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("برنامه‌های فرهنگی") }
    var accountSource by remember { mutableStateOf("صندوق اصلی") }
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

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ به ریال (مثال: ۵۰۰۰۰۰۰)") },
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
                    val amount = amountText.toLongOrNull() ?: 0L
                    onConfirm(workplace, type, amount, category, accountSource, desc, attachmentUri?.toString(), refNum)
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
