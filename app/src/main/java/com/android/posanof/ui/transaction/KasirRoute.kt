package com.android.posanof.ui.transaction

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.ui.transaction.scanner.POSScannerScreen

/**
 * Penghubung antara tampilan baru (KasirScreen) dan data asli
 * (POSTransactionViewModel + ProductRepository).
 *
 * SEMENTARA: tombol bar keranjang membuka POSScreen lama (tab Keranjang)
 * sampai layar Keranjang baru (layar 3 & 4) selesai dibuat.
 */
@Composable
fun KasirRoute(
    viewModel: POSTransactionViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val allProducts by productRepository.getAllProducts().collectAsState(initial = emptyList())

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    var showScanner by remember { mutableStateOf(false) }
    var showCart by rememberSaveable { mutableStateOf(false) }

    // Chip kategori diambil dari kategori yang benar-benar dipakai produk
    val categories = remember(allProducts) {
        listOf("Semua") + allProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val activeCategory = if (category in categories) category else "Semua"

    // Filter kategori + nama / barcode, lalu ubah ProductEntity -> KasirProduct
    val products = remember(query, activeCategory, allProducts) {
        allProducts
            .filter {
                (activeCategory == "Semua" || it.category == activeCategory) &&
                        (query.isBlank() ||
                                it.name.contains(query, ignoreCase = true) ||
                                (it.barcode?.contains(query, ignoreCase = true) == true))
            }
            .map {
                KasirProduct(
                    id = it.id.toLong(),
                    name = it.name,
                    price = it.price.toLong(),
                    stock = it.stock.toInt(),
                    lowStockAt = it.minStock.toInt()
                )
            }
    }

    if (showCart) {
        BackHandler { showCart = false }
        POSScreen(
            viewModel = viewModel,
            productRepository = productRepository,
            modifier = modifier,
            initialTab = 1 // langsung buka tab Keranjang
        )
    } else {
        KasirScreen(
            products = products,
            query = query,
            onQueryChange = { query = it },
            categories = categories,
            selectedCategory = activeCategory,
            onCategoryChange = { category = it },
            cartProductCount = cartItems.size,
            cartItemCount = cartItems.sumOf { it.quantity },
            cartTotal = totalAmount.toLong(),
            onProductClick = { kp ->
                val entity = allProducts.firstOrNull { it.id.toLong() == kp.id }
                when {
                    entity == null -> Unit
                    entity.stock <= 0 ->
                        Toast.makeText(context, "Stok ${entity.name} habis", Toast.LENGTH_SHORT).show()
                    else -> viewModel.addToCart(entity)
                }
            },
            onScanClick = { showScanner = true },
            onCartClick = { showCart = true },
            modifier = modifier
        )
    }

    // Pemindai barcode (memakai POSScannerScreen yang sudah ada)
    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                POSScannerScreen(
                    viewModel = viewModel,
                    productRepository = productRepository,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { showScanner = false },
                    onViewCart = {
                        showScanner = false
                        showCart = true
                    }
                )
            }
        }
    }
}