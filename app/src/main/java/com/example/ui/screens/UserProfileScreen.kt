package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserProfileEntity
import com.example.sync.MinioSyncClient
import com.example.util.JalaliCalendar
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val workplaceConfigs by viewModel.workplaceConfigs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var isTestingMinio by remember { mutableStateOf(false) }
    var diagnosticResult by remember { mutableStateOf<MinioSyncClient.DiagnosticResult?>(null) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    val howzehConfig = workplaceConfigs.firstOrNull { it.workplace == "HOWZEH" }
    val mosqueConfig = workplaceConfigs.firstOrNull { it.workplace == "MOSQUE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = profile?.fullName ?: "کاربر محترم مشکاه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = profile?.roleTitle ?: "مدیر فرهنگی و فعال حوزوی/مسجد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = profile?.phoneNumber?.ifBlank { "ثبت نشده" } ?: "ثبت نشده",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = profile?.email?.ifBlank { "ثبت نشده" } ?: "ثبت نشده",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ویرایش اطلاعات هویتی")
                    }
                }
            }
        }

        // Workplace Target Hours Setting Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "شاخص ساعات موظفی روزانه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "تنظیم شاخص‌ها", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("حوزه علمیه", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                val howzehDaily = howzehConfig?.targetDailyMinutes ?: 240
                                Text("${howzehDaily / 60} ساعت و ${howzehDaily % 60} دقیقه در روز", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مسجد", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                val mosqueDaily = mosqueConfig?.targetDailyMinutes ?: 180
                                Text("${mosqueDaily / 60} ساعت و ${mosqueDaily % 60} دقیقه در روز", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // MinIO Cloud Sync & Backup Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "همگام‌سازی ابری (MinIO S3)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AWS SigV4",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "سرور: ${MinioSyncClient.getEndpoint()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "باکت: ${MinioSyncClient.getBucket()} | مسیر: ${MinioSyncClient.getPrefix()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    val lastSyncTime = profile?.lastSyncTimestamp ?: 0L
                    if (lastSyncTime > 0) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        Text(
                            text = "آخرین همگام‌سازی موفق: ${sdf.format(Date(lastSyncTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "تاکنون پشتیبانی در ابر ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    if (syncStatusMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = syncStatusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isSyncing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("در حال پردازش عملیات شبکه ابری...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.syncBackupToCloud { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_to_minio_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("پشتیبان‌گیری در ابر")
                            }

                            OutlinedButton(
                                onClick = { showRestoreConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_from_minio_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("بازیابی از ابر")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = {
                                isTestingMinio = true
                                viewModel.runMinioDiagnostic { result ->
                                    isTestingMinio = false
                                    diagnosticResult = result
                                    showDiagnosticDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("minio_diagnostic_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTestingMinio) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("در حال بررسی سرور و امضای AWS SigV4...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("تست، عیب‌یابی و پینگ اتصال MinIO")
                            }
                        }
                    }
                }
            }
        }

        // Architecture Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "معماری ذخیره‌سازی آفلاین‌محور (Offline-First)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "برنامه مشکاه به صورت کاملاً مستقل و بومی روی دیتابیس گوشی شما کار می‌کند. هیچ وابستگی دائمی به اینترنت وجود ندارد. در صورت اتصال، داده‌های شما با الگوریتم HMAC-SHA256 در سرور MinIO پشتیبان‌گیری امن می‌گردند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(profile?.fullName ?: "") }
        var roleInput by remember { mutableStateOf(profile?.roleTitle ?: "") }
        var phoneInput by remember { mutableStateOf(profile?.phoneNumber ?: "") }
        var emailInput by remember { mutableStateOf(profile?.email ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("ویرایش پروفایل شخصی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("نام و نام خانوادگی / عنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("عنوان مسئولیت / سمت") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("شماره تماس") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("رایانامه (ایمیل)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = profile ?: UserProfileEntity()
                        viewModel.updateUserProfile(
                            current.copy(
                                fullName = nameInput.trim(),
                                roleTitle = roleInput.trim(),
                                phoneNumber = phoneInput.trim(),
                                email = emailInput.trim()
                            )
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Workplace Config Hours Dialog (Supports 30-min increments)
    if (showConfigDialog) {
        var howzehMins by remember(howzehConfig) { mutableIntStateOf(howzehConfig?.targetDailyMinutes ?: 240) }
        var mosqueMins by remember(mosqueConfig) { mutableIntStateOf(mosqueConfig?.targetDailyMinutes ?: 180) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("تنظیم ساعات موظفی روزانه (گام‌های ۳۰ دقیقه‌ای)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Howzeh
                    val hH = howzehMins / 60
                    val hM = howzehMins % 60
                    val hText = if (hM > 0) "$hH ساعت و $hM دقیقه (${hH + 0.5f} ساعت)" else "$hH ساعت"
                    Text("ساعات موظفی حوزه علمیه: $hText", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { if (howzehMins >= 60) howzehMins -= 30 }) {
                            Text("- ۳۰ دقیقه")
                        }
                        FilledTonalButton(onClick = { if (howzehMins <= 720) howzehMins += 30 }) {
                            Text("+ ۳۰ دقیقه")
                        }
                    }
                    Slider(
                        value = howzehMins.toFloat(),
                        onValueChange = {
                            howzehMins = (Math.round(it / 30f) * 30).toInt().coerceIn(30, 720)
                        },
                        valueRange = 30f..720f,
                        steps = 22
                    )

                    HorizontalDivider()

                    // Mosque
                    val mH = mosqueMins / 60
                    val mM = mosqueMins % 60
                    val mText = if (mM > 0) "$mH ساعت و $mM دقیقه (${mH + 0.5f} ساعت)" else "$mH ساعت"
                    Text("ساعات موظفی مسجد: $mText", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { if (mosqueMins >= 60) mosqueMins -= 30 }) {
                            Text("- ۳۰ دقیقه")
                        }
                        FilledTonalButton(onClick = { if (mosqueMins <= 720) mosqueMins += 30 }) {
                            Text("+ ۳۰ دقیقه")
                        }
                    }
                    Slider(
                        value = mosqueMins.toFloat(),
                        onValueChange = {
                            mosqueMins = (Math.round(it / 30f) * 30).toInt().coerceIn(30, 720)
                        },
                        valueRange = 30f..720f,
                        steps = 22
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkplaceConfig("HOWZEH", howzehMins)
                        viewModel.updateWorkplaceConfig("MOSQUE", mosqueMins)
                        showConfigDialog = false
                    }
                ) {
                    Text("اعمال تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // MinIO Connection Diagnostic Modal
    if (showDiagnosticDialog && diagnosticResult != null) {
        val res = diagnosticResult!!
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (res.isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("نتیجه عیب‌یابی اتصال به MinIO", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (res.isSuccess) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res.message,
                            fontWeight = FontWeight.Bold,
                            color = if (res.isSuccess) Color(0xFF065F46) else Color(0xFF991B1B),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text("آدرس سرور: ${res.endpoint}", style = MaterialTheme.typography.bodySmall)
                    Text("باکت هدف: ${res.bucket}", style = MaterialTheme.typography.bodySmall)
                    Text("زمان پاسخگویی (Latency): ${res.latencyMs} میلی‌ثانیه", style = MaterialTheme.typography.bodySmall)
                    Text("کد وضعیت HTTP: ${if (res.httpCode != -1) res.httpCode else "عدم دسترسی شبکه"}", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text("جزئیات سیستمی پاسخ:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res.details,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDiagnosticDialog = false }) {
                    Text("متوجه شدم")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("تایید بازیابی اطلاعات از سرور") },
            text = {
                Text("توجه: با بازیابی اطلاعات از سرور MinIO، داده‌های فعلی شما با آخرین نسخه پشتیبان سرور جایگزین خواهند شد. آیا ادامه می‌دهید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreBackupFromCloud { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، بازیابی کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
