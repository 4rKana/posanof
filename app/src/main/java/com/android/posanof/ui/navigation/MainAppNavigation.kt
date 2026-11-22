
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.admin.AdminScreen
import com.android.posanof.ui.components.AdminPinScreen
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.credit.CreditManagementScreen
import com.android.posanof.ui.credit.CreditViewModel
import com.android.posanof.ui.product.ProductManagementScreen
import com.android.posanof.ui.report.ReportAndBackupScreen
import com.android.posanof.ui.transaction.KasirRoute
import com.android.posanof.ui.transaction.POSTransactionViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val requiresAdminPin: Boolean = false
) {
    data object POS : Screen(
        "pos",
        "Kasir POS",
        Icons.Default.PointOfSale
    )

    data object Credit : Screen(
        "credit",
        "Piutang Kasbon",
        Icons.AutoMirrored.Filled.ReceiptLong
    )

    data object MasterProduct : Screen(
        "product",
        "Master Produk",
        Icons.Default.Inventory,
        requiresAdminPin = true
    )

    data object ReportBackup : Screen(
        "report",
        "Laporan & Backup",
        Icons.Default.Assessment,
        requiresAdminPin = true
    )

    data object Admin : Screen(
        "admin",
        "Admin",
        Icons.Default.Settings,
        requiresAdminPin = true
    )
}

private fun shortLabel(screen: Screen): String = when (screen) {
    Screen.POS -> "Kasir"
    Screen.Credit -> "Kasbon"
    Screen.MasterProduct -> "Produk"
    Screen.ReportBackup -> "Laporan"
    Screen.Admin -> "Admin"
}

@Composable
fun MainAppNavigation(
    posViewModel: POSTransactionViewModel,
    creditViewModel: CreditViewModel,
    productRepository: ProductRepository,
    transactionRepository: TransactionRepository,
    backupRepository: BackupRepository,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.POS)
    }

    var pendingScreen by remember {
        mutableStateOf<Screen?>(null)
    }

    var showPinScreen by remember {
        mutableStateOf(false)
    }

    val navItems = listOf(
        Screen.POS,
        Screen.Credit,
        Screen.MasterProduct,
        Screen.ReportBackup,
        Screen.Admin
    )

    Scaffold(
        bottomBar = {
            if (!showPinScreen) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { screen ->
                        val selected =
                            currentScreen.route == screen.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (
                                    screen.requiresAdminPin &&
                                    !authManager.isAdminSessionActive()
                                ) {
                                    pendingScreen = screen
                                    showPinScreen = true
                                } else {
                                    currentScreen = screen
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = shortLabel(screen),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    }
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
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showPinScreen) {
                        Modifier
                    } else {
                        Modifier.padding(innerPadding)
                    }
                )
        ) {
            when (currentScreen) {
                is Screen.POS -> {
                    KasirRoute(
                        viewModel = posViewModel,
                        productRepository = productRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.Credit -> {
                    CreditManagementScreen(
                        viewModel = creditViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.MasterProduct -> {
                    ProductManagementScreen(
                        productRepository = productRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.ReportBackup -> {
                    ReportAndBackupScreen(
                        transactionRepository = transactionRepository,
                        productRepository = productRepository,
                        backupRepository = backupRepository,
                        creditViewModel = creditViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is Screen.Admin -> {
                    AdminScreen(
                        authManager = authManager,
                        backupRepository = backupRepository,
                        onLogout = {
                            authManager.logoutAdmin()
                            currentScreen = Screen.POS
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (showPinScreen) {
                AdminPinScreen(
                    authManager = authManager,
                    onSuccess = {
                        showPinScreen = false

                        pendingScreen?.let {
                            currentScreen = it
                            pendingScreen = null
                        }
                    },
                    onDismiss = {
                        showPinScreen = false
                        pendingScreen = null
                    }
                )
            }
        }
    }
}
