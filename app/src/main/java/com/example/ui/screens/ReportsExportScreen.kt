package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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

    val workplaceActivities = activities.filter { it.workplace == selectedWorkplace || it.workplace == "ALL" }
    val workplacePunches = punches.filter { it.workplace == selectedWorkplace }

    val howzehPunches = punches.filter { it.workplace == "HOWZEH" }
    val mosquePunches = punches.filter { it.workplace == "MOSQUE" }
    val howzehActivities = activities.filter { it.workplace == "HOWZEH" || it.workplace == "ALL" }
    val mosqueActivities = activities.filter { it.workplace == "MOSQUE" }

    val (teachingDeductionMins, studyDeductionMins) = remember(activities) {
        val todayStr = com.example.util.JalaliCalendar.getTodayJalali().toString()
        viewModel.getTeachingAndStudyDeductions(todayStr)
    }

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
                        text = "بخش خروجی گزارش‌های رسمی و اسناد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "تولید خروجی‌های PDF رسمی، فایل‌های اکسل (CSV) و آماده‌سازی جهت پرینت",
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

        // FEATURED: Combined Comprehensive PDF Report
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
                        text = "شامل جمع کل ساعات حضور در حوزه علمیه و مسجد، کسر تدریس و مطالعه، کارکرد خالص، جداول ورود و خروج و فعالیت‌ها همراه با جایگاه مهر و امضای رسمی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val pdfFile = PdfExcelExportHelper.generateCombinedComprehensivePdf(
                                context = context,
                                howzehPunches = howzehPunches,
                                mosquePunches = mosquePunches,
                                howzehActivities = howzehActivities,
                                mosqueActivities = mosqueActivities,
                                howzehTeachingDeductionMins = teachingDeductionMins,
                                howzehStudyDeductionMins = studyDeductionMins
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

        // Option 1: PDF Attendance & Punches
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
                            text = "گزارش PDF ساعت حضور و ترددها (${if (selectedWorkplace == "HOWZEH") "حوزه" else "مسجد"})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "تهیه برگه رسمی ورود و خروج، مدت زمان کل حضور و محاسبات تردد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                            val pdfFile = PdfExcelExportHelper.generateAttendancePdf(
                                context = context,
                                workplaceTitle = title,
                                punches = workplacePunches
                            )
                            if (pdfFile != null) {
                                PdfExcelExportHelper.shareFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در تولید فایل PDF حضور و غیاب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().testTag("export_attendance_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تولید PDF گزارش کارکرد و تردد")
                    }
                }
            }
        }

        // Option 2: PDF Activities Report
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "گزارش PDF ریز فعالیت‌های روزانه (${if (selectedWorkplace == "HOWZEH") "حوزه" else "مسجد"})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "لیست زمان‌بندی شده کارهای انجام شده، طراحی پوستر، تدریس، برنامه‌های قرآنی و فرهنگی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val title = if (selectedWorkplace == "HOWZEH") "حوزه علمیه" else "مسجد"
                            val pdfFile = PdfExcelExportHelper.generateActivitiesPdf(
                                context = context,
                                workplaceTitle = title,
                                activities = workplaceActivities
                            )
                            if (pdfFile != null) {
                                PdfExcelExportHelper.shareFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "خطا در تولید فایل PDF فعالیت‌ها", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_activities_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تولید PDF ریز فعالیت‌ها")
                    }
                }
            }
        }

        // Option 3: PDF Financial Statement
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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
                                Toast.makeText(context, "خطا در تولید فایل PDF مالی", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("export_financial_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تولید PDF صورت مالی و حسابرسی")
                    }
                }
            }
        }

        // Option 4: Excel / CSV Activities Report
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "خروجی اکسل (CSV) ریز فعالیت‌ها و ترددها",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "دریافت فایل داده‌های خام سازگار با نرم‌افزارهای اکسل و صفحات گسترده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csvFile = PdfExcelExportHelper.generateActivityCsv(context, workplaceActivities)
                                if (csvFile != null) {
                                    PdfExcelExportHelper.shareFile(context, csvFile, "text/csv")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اکسل فعالیت‌ها")
                        }

                        OutlinedButton(
                            onClick = {
                                val csvFile = PdfExcelExportHelper.generatePunchCsv(context, workplacePunches)
                                if (csvFile != null) {
                                    PdfExcelExportHelper.shareFile(context, csvFile, "text/csv")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اکسل ترددها")
                        }
                    }
                }
            }
        }
    }
}
