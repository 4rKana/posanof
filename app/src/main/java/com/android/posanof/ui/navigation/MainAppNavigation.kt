package com.android.posanof.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.components.AdminPinDialog
import com.android.posanof.ui.credit.CreditManagementScreen
import com.android.posanof.ui.credit.CreditViewModel
import com.android.posanof.ui.product.ProductManagementScreen
import com.android.posanof.ui.report.ReportAndBackupScreen
import com.android.posanof.ui.transaction.POSScreen
import com.android.posanof.ui.transaction.POSTransactionViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val requiresAdminPin: Boolean = false) {
    data object POS : Screen("pos", "Kasir POS", Icons.Default.PointOfSale)
    data object Credit : Screen("credit", "Piutang Kasbon", Icons.AutoMirrored.Filled.ReceiptLong)
    data object MasterProduct : Screen("product", "Master Produk", Icons.Default.Inventory, requiresAdminPin = true)
    data object ReportBackup : Screen("report", "Laporan & Backup", Icons.Default.Assessment, requiresAdminPin = true)
}

/**
 * Komponen Utama Navigasi Aplikasi POSANOF dengan Material3 NavigationBar.
 */
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.POS) }
    var pendingScreen by remember { mutableStateOf<Screen?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        Screen.POS,
        Screen.Credit,
        Screen.MasterProduct,
        Screen.ReportBackup
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen.route == screen.route,
                        onClick = {
                            if (screen.requiresAdminPin && !authManager.isAdminSessionActive()) {
                                pendingScreen = screen
                                showPinDialog = true
                            } else {
                                currentScreen = screen
                            }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
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
                    POSScreen(
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
                        backupRepository = backupRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal Admin PIN Dialog jika mengakses screen terproteksi
    if (showPinDialog) {
        AdminPinDialog(
            authManager = authManager,
            onSuccess = {
                showPinDialog = false
                pendingScreen?.let {
                    currentScreen = it
                    pendingScreen = null
                }
            },
            onDismiss = {
                showPinDialog = false
                pendingScreen = null
            }
        )
    }
}
