package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.FinancialTransactionEntity
import com.example.ui.components.AddFinancialTransactionDialog
import java.util.Locale
import com.example.ui.components.ImageViewerModal
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAuditScreen(
    viewModel: AppViewModel,
    transactions: List<FinancialTransactionEntity>
) {
    var selectedWorkplaceTab by remember { mutableStateOf("HOWZEH") } // HOWZEH vs MOSQUE
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

    val workplaceTransactions = transactions.filter { it.workplace == selectedWorkplaceTab }

    val totalIncome = workplaceTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = workplaceTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner & Workplace Selector Tab
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
                        text = "دفتر حسابرسی و اسناد مالی مجزا",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "مدیریت کامل ورودی، خروجی، محل مصرف و اسناد پیوست",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedWorkplaceTab == "HOWZEH",
                            onClick = { selectedWorkplaceTab = "HOWZEH" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier.testTag("financial_tab_howzeh")
                        ) {
                            Text("مالی حوزه علمیه")
                        }
                        SegmentedButton(
                            selected = selectedWorkplaceTab == "MOSQUE",
                            onClick = { selectedWorkplaceTab = "MOSQUE" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.Mosque, contentDescription = null) },
                            modifier = Modifier.testTag("financial_tab_mosque")
                        ) {
                            Text("مالی مسجد")
                        }
                    }
                }
            }
        }

        // Ledger Balance Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("خلاصه دفتر حسابرسی ${if (selectedWorkplaceTab == "HOWZEH") "حوزه علمیه" else "مسجد"}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("مجموع درآمدها (ورودی):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${String.format(Locale.US, "%,d", totalIncome)} ریال", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("مجموع هزینه‌ها (خروجی):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${String.format(Locale.US, "%,d", totalExpense)} ریال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مانده نهایی حساب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${String.format(Locale.US, "%,d", balance)} ریال",
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }

        // Section Title & Add Transaction Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دفتر ثبت اسناد و تراکنش‌ها",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_financial_tx_fab")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("ثبت سند جدید")
                }
            }
        }

        // List of Transactions
        if (workplaceTransactions.isEmpty()) {
            item {
                Text(
                    "هیچ سند یا تراکنش مالی ثبت نشده است.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(workplaceTransactions, key = { it.id }) { tx ->
                val isIncome = tx.type == "INCOME"

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isIncome) "درآمد (ورودی)" else "هزینه (خروجی)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "${String.format(Locale.US, "%,d", tx.amount)} ریال",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text("سرفصل / محل مصرف: ${tx.category}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("منبع / حساب: ${tx.accountSource}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                        if (tx.partyName.isNotBlank()) {
                            Text("طرف حساب / بانی: ${tx.partyName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }

                        if (!tx.referenceNumber.isNull_Blank()) {
                            Text("شماره سند/پیگیری: ${tx.referenceNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        if (tx.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("بابت / توضیحات: ${tx.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Audit Verification Status & Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleTransactionVerification(tx) }
                                .background(if (tx.isVerified) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (tx.isVerified) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "وضعیت تایید حسابرس",
                                tint = if (tx.isVerified) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (tx.isVerified) "تایید شده توسط حسابرس" else "در انتظار بررسی حسابرس (کلیک جهت تایید)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tx.isVerified) Color(0xFF10B981) else Color.Gray,
                                fontWeight = if (tx.isVerified) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تاریخ: ${tx.jalaliDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tx.attachmentPath != null) {
                                    OutlinedButton(
                                        onClick = { viewingReceiptPath = tx.attachmentPath },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("مشاهده رسید پیوست", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFinancialTransactionDialog(
            initialWorkplace = selectedWorkplaceTab,
            onDismiss = { showAddDialog = false },
            onConfirm = { workplace, type, amount, category, accountSource, desc, attachmentPath, refNum, partyName ->
                showAddDialog = false
                viewModel.addFinancialTransaction(
                    workplace = workplace,
                    type = type,
                    amount = amount,
                    category = category,
                    accountSource = accountSource,
                    jalaliDate = com.example.util.JalaliCalendar.getTodayJalali().toString(),
                    description = desc,
                    attachmentPath = attachmentPath,
                    referenceNumber = refNum,
                    partyName = partyName,
                    isVerified = false
                )
            }
        )
    }

    viewingReceiptPath?.let { path ->
        ImageViewerModal(
            imageUriStr = path,
            onDismiss = { viewingReceiptPath = null }
        )
    }
}

private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
