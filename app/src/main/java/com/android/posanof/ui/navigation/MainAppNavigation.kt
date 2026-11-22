package com.android.posanof.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.credit.CreditManagementScreen
import com.android.posanof.ui.credit.CreditViewModel
import com.android.posanof.ui.product.ProductManagementScreen
import com.android.posanof.ui.report.DashboardScreen
import com.android.posanof.ui.report.TransactionHistoryScreen
import com.android.posanof.ui.report.BackupRestoreScreen
import com.android.posanof.ui.transaction.KasirRoute
import com.android.posanof.ui.transaction.POSTransactionViewModel


import com.android.posanof.ui.report.DashboardScreen
import com.android.posanof.ui.report.TransactionHistoryScreen
import com.android.posanof.ui.report.BackupRestoreScreen
import com.android.posanof.ui.settings.SettingsScreen
import com.android.posanof.data.local.SettingsManager

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val requiresAdminPin: Boolean = false) {
    data object POS : Screen("pos", "Kasir POS", Icons.Default.PointOfSale)
    data object Credit : Screen("credit", "Kasbon", Icons.AutoMirrored.Filled.ReceiptLong)
    data object Report : Screen("report", "Laporan", Icons.Default.Assessment, requiresAdminPin = true)
    data object Admin : Screen("admin", "Admin", Icons.Default.Inventory, requiresAdminPin = true)
}

/** Label pendek untuk bottom bar supaya tidak terpotong. */
private fun shortLabel(screen: Screen): String = when (screen) {
    Screen.POS -> "Kasir"
    Screen.Credit -> "Kasbon"
    Screen.Report -> "Laporan"
    Screen.Admin -> "Admin"
}

/**
 * Komponen Utama Navigasi Aplikasi POSANOF dengan Material3 NavigationBar.
 */
@Composable
fun MainAppNavigation(
    posViewModel: POSTransactionViewModel,
    creditViewModel: CreditViewModel,
    dashboardViewModel: com.android.posanof.ui.report.DashboardViewModel,
    productRepository: ProductRepository,
    transactionRepository: TransactionRepository,
    backupRepository: BackupRepository,
    authManager: AuthManager,
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.POS) }
    var pendingScreen by remember { mutableStateOf<Screen?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var explicitPinMode by remember { mutableStateOf<com.android.posanof.ui.components.PinMode?>(null) }
    
    // Untuk navigasi di tab admin
    var adminActiveTab by remember { mutableStateOf(0) } // 0: Master Data, 1: Pengaturan

    val navItems = listOf(
        Screen.POS,
        Screen.Credit,
        Screen.Report,
        Screen.Admin
    )

    var reportScreen by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                navItems.forEach { screen ->
                    val selected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (screen.requiresAdminPin && !authManager.isAdminSessionActive()) {
                                pendingScreen = screen
                                showPinDialog = true
                            } else {
                                currentScreen = screen
                            }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title, modifier = Modifier.size(22.dp)) },
                        label = {
                            Text(
                                shortLabel(screen),
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PosPrimary,
                            selectedTextColor = PosPrimary,
                            indicatorColor = PosSoft,
                            unselectedIconColor = PosMuted,
                            unselectedTextColor = PosMuted
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.POS -> {
                    KasirRoute(
                        viewModel = posViewModel,
                        productRepository = productRepository,
                        settingsManager = settingsManager,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.Credit -> {
                    CreditManagementScreen(
                        viewModel = creditViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.Report -> {
                    when (reportScreen) {
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                settingsManager = settingsManager,
                                onNavigateToHistory = { reportScreen = "history" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "history" -> {
                            TransactionHistoryScreen(
                                viewModel = dashboardViewModel,
                                onBack = { reportScreen = "dashboard" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "backup" -> {
                            BackupRestoreScreen(
                                backupRepository = backupRepository,
                                settingsManager = settingsManager,
                                onBack = { reportScreen = "dashboard" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                is Screen.Admin -> {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.SecondaryTabRow(selectedTabIndex = adminActiveTab) {
                            androidx.compose.material3.Tab(
                                selected = adminActiveTab == 0,
                                onClick = { adminActiveTab = 0 },
                                text = { Text("Master Data") }
                            )
                            androidx.compose.material3.Tab(
                                selected = adminActiveTab == 1,
                                onClick = { adminActiveTab = 1 },
                                text = { Text("Pengaturan") }
                            )
                        }
                        if (adminActiveTab == 0) {
                            ProductManagementScreen(
                                productRepository = productRepository,
                                settingsManager = settingsManager,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SettingsScreen(
                                settingsManager = settingsManager,
                                authManager = authManager,
                                onNavigateToChangePin = {
                                    explicitPinMode = com.android.posanof.ui.components.PinMode.CHANGE
                                    pendingScreen = Screen.Admin
                                    showPinDialog = true
                                },
                                onNavigateToBackupRestore = { 
                                    currentScreen = Screen.Report 
                                    reportScreen = "backup"
                                },
                                onLogoutAdmin = {
                                    android.widget.Toast.makeText(context, "Berhasil keluar Admin!", android.widget.Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.POS
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Full screen PIN Admin Screen
    if (showPinDialog) {
        val pinMode = explicitPinMode ?: if (!authManager.isPinSet()) com.android.posanof.ui.components.PinMode.CREATE else com.android.posanof.ui.components.PinMode.VERIFY
        com.android.posanof.ui.components.AdminPinScreen(
            authManager = authManager,
            mode = pinMode,
            onSuccess = {
                showPinDialog = false
                explicitPinMode = null
                pendingScreen?.let {
                    currentScreen = it
                    pendingScreen = null
                }
            },
            onCancel = {
                showPinDialog = false
                explicitPinMode = null
                pendingScreen = null
            }
        )
    }
}