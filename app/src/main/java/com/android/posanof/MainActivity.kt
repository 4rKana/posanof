package com.android.posanof

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.android.posanof.data.local.database.AppDatabase
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.CreditRepository
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.credit.CreditViewModel
import com.android.posanof.ui.navigation.MainAppNavigation
import com.android.posanof.ui.theme.PosanofTheme
import com.android.posanof.ui.transaction.POSTransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.android.posanof.data.local.SettingsManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Inisialisasi Database, Auth, dan Repositories
        val database = AppDatabase.getInstance(this)
        val authManager = AuthManager.getInstance(this)
        val settingsManager = SettingsManager.getInstance(this)

        val productRepository = ProductRepository(database.productDao())
        val transactionRepository = TransactionRepository(database)
        val creditRepository = CreditRepository(database.creditDao())
        val backupRepository = BackupRepository(this, database)

        // 2. Inisialisasi Data Demo UMKM jika Database masih kosong
        lifecycleScope.launch {
            val currentProducts = productRepository.getAllProducts().first()
            if (currentProducts.isEmpty()) {
                seedDemoProducts(productRepository)
            }
        }

        // 3. Inisialisasi ViewModels
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(POSTransactionViewModel::class.java)) {
                    return POSTransactionViewModel(transactionRepository, settingsManager) as T
                }
                if (modelClass.isAssignableFrom(CreditViewModel::class.java)) {
                    return CreditViewModel(creditRepository) as T
                }
                if (modelClass.isAssignableFrom(com.android.posanof.ui.report.DashboardViewModel::class.java)) {
                    return com.android.posanof.ui.report.DashboardViewModel(transactionRepository, productRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val provider = androidx.lifecycle.ViewModelProvider(this, factory)
        val posViewModel = provider.get(POSTransactionViewModel::class.java)
        val creditViewModel = provider.get(CreditViewModel::class.java)
        val dashboardViewModel = provider.get(com.android.posanof.ui.report.DashboardViewModel::class.java)

        setContent {
            PosanofTheme {
                MainAppNavigation(
                    posViewModel = posViewModel,
                    creditViewModel = creditViewModel,
                    dashboardViewModel = dashboardViewModel,
                    productRepository = productRepository,
                    transactionRepository = transactionRepository,
                    backupRepository = backupRepository,
                    authManager = authManager,
                    settingsManager = settingsManager
                )
            }
        }
    }

    private suspend fun seedDemoProducts(repository: ProductRepository) {
        val demoProducts = listOf(
            ProductEntity(name = "Kopi Hitam Tubruk 100g", price = 12000.0, stock = 50, barcode = "89910010001", minStock = 5),
            ProductEntity(name = "Beras Premium 5kg", price = 75000.0, stock = 20, barcode = "89910010002", minStock = 3),
            ProductEntity(name = "Minyak Goreng 1L", price = 18000.0, stock = 30, barcode = "89910010003", minStock = 5),
            ProductEntity(name = "Gula Pasir 1kg", price = 16000.0, stock = 40, barcode = "89910010004", minStock = 5),
            ProductEntity(name = "Mie Instan Goreng", price = 3500.0, stock = 100, barcode = "89910010005", minStock = 10),
            ProductEntity(name = "Sabun Mandi Batang 80g", price = 4500.0, stock = 60, barcode = "89910010006", minStock = 10)
        )
        demoProducts.forEach { repository.insertProduct(it) }
    }
}
