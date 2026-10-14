package com.android.posanof.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.ui.platform.LocalContext


sealed class Screen(val route: String, val title: String, val icon: ImageVector, val requiresAdminPin: Boolean = false) {
    data object POS : Screen("pos", "Kasir POS", Icons.Default.PointOfSale)
    data object Credit : Screen("credit", "Piutang Kasbon", Icons.AutoMirrored.Filled.ReceiptLong)
    data object MasterProduct : Screen("product", "Master Produk", Icons.Default.Inventory, requiresAdminPin = true)
    data object ReportBackup : Screen("report", "Laporan & Backup", Icons.Default.Assessment, requiresAdminPin = true)
    data object Logout : Screen("logout", "Keluar", Icons.Default.Logout)
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
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.POS) }
    var pendingScreen by remember { mutableStateOf<Screen?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false)}

    val navItems = listOf(
        Screen.POS,
        Screen.Credit,
        Screen.MasterProduct,
        Screen.ReportBackup,
        Screen.Logout
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { screen ->
                    NavigationBarItem(
                        selected = if (screen is Screen.Logout) false else currentScreen.route == screen.route,
                        onClick = {
                            if (screen is Screen.Logout) {
                                showLogoutDialog = true
                            } else if (screen.requiresAdminPin && !authManager.isAdminSessionActive()) {
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

                is Screen.Logout -> {

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

    // Dialog Konfirmasi Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Apakah Anda yakin ingin mengakhiri sesi admin ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authManager.logoutAdmin()
                        android.widget.Toast.makeText(context, "Berhasil keluar Admin!", android.widget.Toast.LENGTH_SHORT).show()
                        currentScreen = Screen.POS
                    }
                ) {
                    Text("Ya, Keluar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
