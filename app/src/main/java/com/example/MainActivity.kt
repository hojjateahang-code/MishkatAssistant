package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ActivityTrackerScreen
import com.example.ui.screens.AiThinkingAssistantScreen
import com.example.ui.screens.CalendarRemindersScreen
import com.example.ui.screens.DashboardAttendanceScreen
import com.example.ui.screens.FinancialAuditScreen
import com.example.ui.screens.ReportsExportScreen
import com.example.ui.theme.MishkatTheme
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MishkatTheme {
                val viewModel: AppViewModel = viewModel()
                MishkatMainApp(viewModel = viewModel)
            }
        }
    }
}

sealed class NavigationItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : NavigationItem("dashboard", "حضور و غیاب", Icons.Default.PunchClock)
    object Activities : NavigationItem("activities", "ریز فعالیت‌ها", Icons.Default.Assignment)
    object Calendar : NavigationItem("calendar", "تقویم و یادآور", Icons.Default.CalendarMonth)
    object Financial : NavigationItem("financial", "دفتر مالی", Icons.Default.AccountBalanceWallet)
    object Reports : NavigationItem("reports", "گزارش‌ها", Icons.Default.Summarize)
    object AiAssistant : NavigationItem("ai_assistant", "دستیار AI", Icons.Default.AutoAwesome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MishkatMainApp(viewModel: AppViewModel) {
    val navController = rememberNavController()

    val punches by viewModel.allPunches.collectAsStateWithLifecycle()
    val activePunch by viewModel.activePunch.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val activeTimerState by viewModel.activeTimerState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Notification Permission Request for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavigationItem.Dashboard.route

    val bottomNavItems = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Activities,
        NavigationItem.Calendar,
        NavigationItem.Financial,
        NavigationItem.Reports
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            NavigationItem.Dashboard.route -> "حضور غیاب و کسری کار"
                            NavigationItem.Activities.route -> "ریز فعالیت‌های روزانه"
                            NavigationItem.Calendar.route -> "تقویم فارسی و مناسبت‌ها"
                            NavigationItem.Financial.route -> "دفتر حسابرسی و اسناد مالی"
                            NavigationItem.Reports.route -> "گزارش‌های خروجی و پرینت"
                            NavigationItem.AiAssistant.route -> "دستیار هوشمند مشکاه"
                            else -> "دستیار مشکاه"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (currentRoute != NavigationItem.AiAssistant.route) {
                        IconButton(
                            onClick = { navController.navigate(NavigationItem.AiAssistant.route) },
                            modifier = Modifier.testTag("top_ai_assistant_icon")
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "دستیار هوشمند",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        modifier = Modifier.testTag("nav_${item.route}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute != NavigationItem.AiAssistant.route) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(NavigationItem.AiAssistant.route) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text("تحلیل AI") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("ai_assistant_fab")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationItem.Dashboard.route) {
                DashboardAttendanceScreen(
                    viewModel = viewModel,
                    punches = punches,
                    activePunch = activePunch,
                    onOpenAiAssistant = { navController.navigate(NavigationItem.AiAssistant.route) }
                )
            }

            composable(NavigationItem.Activities.route) {
                ActivityTrackerScreen(
                    viewModel = viewModel,
                    activities = activities,
                    activeTimerState = activeTimerState
                )
            }

            composable(NavigationItem.Calendar.route) {
                CalendarRemindersScreen(
                    viewModel = viewModel,
                    tasks = tasks,
                    selectedDate = selectedDate
                )
            }

            composable(NavigationItem.Financial.route) {
                FinancialAuditScreen(
                    viewModel = viewModel,
                    transactions = transactions
                )
            }

            composable(NavigationItem.Reports.route) {
                ReportsExportScreen(
                    viewModel = viewModel,
                    punches = punches,
                    activities = activities,
                    transactions = transactions
                )
            }

            composable(NavigationItem.AiAssistant.route) {
                AiThinkingAssistantScreen(
                    viewModel = viewModel,
                    messages = aiMessages,
                    isThinking = isAiThinking
                )
            }
        }
    }
}
