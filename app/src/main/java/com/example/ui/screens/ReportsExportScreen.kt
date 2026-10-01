package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLogEntity
import com.example.data.FinancialTransactionEntity
import com.example.data.PunchLogEntity
import com.example.util.PdfExcelExportHelper
import com.example.viewmodel.AppViewModel

@Composable
fun ReportsExportScreen(
    viewModel: AppViewModel,
    punches: List<PunchLogEntity>,
    activities: List<ActivityLogEntity>,
    transactions: List<FinancialTransactionEntity>
) {
    val context = LocalContext.current
    var selectedWorkplace by remember { mutableStateOf("HOWZEH") } // "HOWZEH" or "MOSQUE"

    val workplaceTransactions = transactions.filter { it.workplace == selectedWorkplace }
    val totalIncome = workplaceTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = workplaceTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        text = "بخش خروجی گزارش‌های منظم و دقیق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "تولید خروجی‌های PDF، فایل اکسل (CSV) و قابلیت پرینت مستقیم",
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
                            Text("گزارش حوزه")
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

        // Option 1: PDF Financial Statement
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "گزارش PDF صورت‌های مالی و حسابرسی",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "ایجاد فایل PDF رسمی صورت درآمدها، هزینه‌ها، سرفصل‌ها و مانده حساب جهت چاپ و ارائه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                            val pdfFile = PdfExcelExportHelper.generateFinancialPdf(
                                context = context,
                                workplaceTitle = title,
                                transactions = workplaceTransactions,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                balance = balance
                            )
                            if (pdfFile != null) {
                                PdfExcelExportHelper.shareFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در تولید فایل PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تولید PDF و اشتراک‌گذاری / پرینت")
                    }
                }
            }
        }

        // Option 2: Excel / CSV Activities Report
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "خروجی اکسل (CSV) ریز فعالیت‌های روزانه",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "شامل عنوان فعالیت‌ها، دسته‌بندی/تگ، مدت زمان دقیق به دقیقه و توضیحات برای نرم‌افزار اکسل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val csvFile = PdfExcelExportHelper.generateActivityCsv(context, activities)
                            if (csvFile != null) {
                                PdfExcelExportHelper.shareFile(context, csvFile, "text/csv")
                            } else {
                                Toast.makeText(context, "خطا در تولید فایل CSV", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("export_activity_excel_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("دریافت فایل اکسل ریز فعالیت‌ها")
                    }
                }
            }
        }

        // Option 3: Excel / CSV Attendance & Punches Report
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "خروجی اکسل (CSV) ساعت حضور و ترددها",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "شامل ساعت ورود، ساعت خروج و محاسبه مجموع دقیق حضور در محل کار به دقیقه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val csvFile = PdfExcelExportHelper.generatePunchCsv(context, punches)
                            if (csvFile != null) {
                                PdfExcelExportHelper.shareFile(context, csvFile, "text/csv")
                            } else {
                                Toast.makeText(context, "خطا در تولید فایل CSV", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().testTag("export_punch_excel_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("دریافت فایل اکسل کارکرد و حضور غیاب")
                    }
                }
            }
        }
    }
}
