package com.android.posanof.ui.transaction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.ui.transaction.scanner.POSScannerScreen
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    viewModel: POSTransactionViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")) }
    fun formatRp(amount: Double): String = "Rp ${numberFormatter.format(amount.toLong())}"

    // --- Collect States dari ViewModel ---
    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val cashAmount by viewModel.cashAmount.collectAsState()
    val changeAmount by viewModel.changeAmount.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val checkoutResult by viewModel.checkoutResult.collectAsState()

    // --- State Katalog Produk & Pemindai ---
    val allProducts by productRepository.getAllProducts().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }

    // Tab terpilih khusus Mode Portrait HP (0 = Katalog Produk, 1 = Keranjang)
    var compactTabSelected by remember { mutableStateOf(0) }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isBlank()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        (it.barcode?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    // Modal Dialog Hasil Checkout
    when (val result = checkoutResult) {
        is CheckoutResult.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetCheckoutResult() },
                title = { Text("Transaksi Berhasil!", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("ID Transaksi: #${result.transactionId}")
                        if (selectedPaymentMethod == PaymentMethod.TUNAI) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kembalian: ${formatRp(result.changeAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Transaksi Kasbon dicatat (Belum Lunas).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.resetCheckoutResult() }) {
                        Text("Selesai / Transaksi Baru")
                    }
                }
            )
        }

        is CheckoutResult.Error -> {
            LaunchedEffect(result.message) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.resetCheckoutResult()
            }
        }

        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POS Kasir UMKM", fontWeight = FontWeight.Bold) },
                actions = {
                    // Tombol Pemindai Barcode
                    IconButton(onClick = { showCameraScanner = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode"
                        )
                    }
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text("Kosongkan Keranjang", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isCompact = maxWidth < 600.dp

            if (isCompact) {
                // =============================================================
                // LAYOUT RESPONSIF MODEREN: HP PORTRAIT (TAB SYSTEM)
                // =============================================================
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = compactTabSelected) {
                        Tab(
                            selected = compactTabSelected == 0,
                            onClick = { compactTabSelected = 0 },
                            text = { Text("Katalog Produk") }
                        )
                        Tab(
                            selected = compactTabSelected == 1,
                            onClick = { compactTabSelected = 1 },
                            text = {
                                val itemCount = cartItems.sumOf { it.quantity }
                                Text("Keranjang (${itemCount})")
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (compactTabSelected == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                ProductCatalogSection(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    filteredProducts = filteredProducts,
                                    allProducts = allProducts,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    formatRp = ::formatRp
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                CartAndPaymentSection(
                                    cartItems = cartItems,
                                    totalAmount = totalAmount,
                                    cashAmount = cashAmount,
                                    changeAmount = changeAmount,
                                    selectedPaymentMethod = selectedPaymentMethod,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    checkoutResult = checkoutResult,
                                    viewModel = viewModel,
                                    formatRp = ::formatRp
                                )
                            }
                        }
                    }

                    // Sticky Summary Bottom Bar di Tab Katalog
                    if (compactTabSelected == 0 && cartItems.isNotEmpty()) {
                        Surface(
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${cartItems.sumOf { it.quantity }} Item | ${formatRp(totalAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Button(
                                    onClick = { compactTabSelected = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Lihat Keranjang >")
                                }
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // LAYOUT DUAL PANEL: LANDSCAPE / TABLET / WIDE SCREEN
                // =============================================================
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        ProductCatalogSection(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            filteredProducts = filteredProducts,
                            allProducts = allProducts,
                            onAddToCart = { viewModel.addToCart(it) },
                            formatRp = ::formatRp
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        CartAndPaymentSection(
                            cartItems = cartItems,
                            totalAmount = totalAmount,
                            cashAmount = cashAmount,
                            changeAmount = changeAmount,
                            selectedPaymentMethod = selectedPaymentMethod,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            checkoutResult = checkoutResult,
                            viewModel = viewModel,
                            formatRp = ::formatRp
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog Pemindai Barcode Kamera
    if (showCameraScanner) {
        Dialog(
            onDismissRequest = { showCameraScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    POSScannerScreen(
                        viewModel = viewModel,
                        productRepository = productRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { showCameraScanner = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "X",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Seksi Katalog Produk
 */
@Composable
private fun ProductCatalogSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>,
    onAddToCart: (ProductEntity) -> Unit,
    formatRp: (Double) -> String
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Cari produk atau barcode...") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (filteredProducts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (allProducts.isEmpty()) "Belum ada data produk di database." else "Produk tidak ditemukan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = filteredProducts,
                key = { it.id }
            ) { product ->
                ProductGridItem(
                    product = product,
                    onAddToCart = { onAddToCart(product) },
                    formatRp = formatRp
                )
            }
        }
    }
}

/**
 * Seksi Keranjang Belanja dan Panel Pembayaran
 */
@Composable
private fun ColumnScope.CartAndPaymentSection(
    cartItems: List<CartItem>,
    totalAmount: Double,
    cashAmount: Double,
    changeAmount: Double,
    selectedPaymentMethod: PaymentMethod,
    customerName: String,
    customerPhone: String,
    checkoutResult: CheckoutResult,
    viewModel: POSTransactionViewModel,
    formatRp: (Double) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Keranjang Belanja",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${cartItems.sumOf { it.quantity }} Item",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keranjang masih kosong",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = cartItems,
                key = { it.product.id }
            ) { cartItem ->
                CartItemRow(
                    item = cartItem,
                    onIncrement = { viewModel.addToCart(cartItem.product) },
                    onDecrement = {
                        viewModel.updateQuantity(cartItem.product.id, cartItem.quantity - 1)
                    },
                    onRemove = { viewModel.removeFromCart(cartItem.product.id) },
                    formatRp = formatRp
                )
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Seksi Ringkasan Total & Pembayaran
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Belanja:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatRp(totalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Metode Pembayaran:", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPaymentMethod == PaymentMethod.TUNAI,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.TUNAI) },
                label = { Text("TUNAI", maxLines = 1) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedPaymentMethod == PaymentMethod.KASBON,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.KASBON) },
                label = { Text("KASBON", maxLines = 1) },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedPaymentMethod == PaymentMethod.TUNAI) {
            OutlinedTextField(
                value = if (cashAmount == 0.0) "" else cashAmount.toLong().toString(),
                onValueChange = { valValue ->
                    val parsed = valValue.toDoubleOrNull() ?: 0.0
                    viewModel.updateCashAmount(parsed)
                },
                label = { Text("Nominal Uang Tunai (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Chips Saran Uang Tunai
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.updateCashAmount(totalAmount) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text("Uang Pas", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                listOf(20000.0, 50000.0, 100000.0).forEach { suggested ->
                    if (suggested >= totalAmount) {
                        OutlinedButton(
                            onClick = { viewModel.updateCashAmount(suggested) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("${suggested.toInt() / 1000}k", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kembalian:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatRp(changeAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if ((cashAmount >= totalAmount) && totalAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }
        } else {
            OutlinedTextField(
                value = customerName,
                onValueChange = { name -> viewModel.updateCustomerInfo(name, customerPhone) },
                label = { Text("Nama Pelanggan *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { phone -> viewModel.updateCustomerInfo(customerName, phone) },
                label = { Text("No HP Pelanggan") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.checkout() },
            enabled = cartItems.isNotEmpty() && (checkoutResult !is CheckoutResult.Loading),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedPaymentMethod == PaymentMethod.KASBON) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        ) {
            if (checkoutResult is CheckoutResult.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (selectedPaymentMethod == PaymentMethod.KASBON) "Simpan Piutang Kasbon" else "Proses Pembayaran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Item Card Produk pada Grid
 */
@Composable
private fun ProductGridItem(
    product: ProductEntity,
    onAddToCart: () -> Unit,
    formatRp: (Double) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onAddToCart() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatRp(product.price),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.stock <= product.minStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddToCart,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    enabled = product.stock > 0
                ) {
                    Text("+", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Baris Item Keranjang Belanja
 */
@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    formatRp: (Double) -> String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = "@ ${formatRp(item.product.price)} = ${formatRp(item.subTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang")
                }
                Text(
                    text = item.quantity.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
