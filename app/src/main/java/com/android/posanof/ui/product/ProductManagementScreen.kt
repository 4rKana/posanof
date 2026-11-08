package com.android.posanof.ui.product

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Pilihan jenis barang di form produk. Ubah/tambah sesuai kebutuhan toko. */
private val ProductCategoryOptions = listOf("Minuman", "Makanan", "Sembako", "Lainnya")

/**
 * Layar Master Data Produk (Tambah, Edit, Hapus, Monitoring Stok).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    productRepository: ProductRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")) }

    val allProducts by productRepository.getAllProducts().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Data Produk", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedProductForEdit = null
                    showAddEditDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Field Pencarian Produk
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari produk atau barcode...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allProducts.isEmpty()) "Belum ada produk. Tekan tombol + untuk menambah." else "Produk tidak ditemukan.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredProducts,
                        key = { it.id }
                    ) { product ->
                        ProductItemRow(
                            product = product,
                            currencyFormat = currencyFormat,
                            onEdit = {
                                selectedProductForEdit = product
                                showAddEditDialog = true
                            },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
    }

    // Modal Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = selectedProductForEdit,
            onSave = { name, price, stock, minStock, barcode, category ->
                coroutineScope.launch {
                    if (selectedProductForEdit == null) {
                        val newProduct = ProductEntity(
                            name = name,
                            price = price,
                            stock = stock,
                            minStock = minStock,
                            barcode = barcode.ifBlank { null },
                            category = category
                        )
                        val result = productRepository.insertProduct(newProduct)
                        result.fold(
                            onSuccess = {
                                Toast.makeText(context, "Produk '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "Gagal menambah produk: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        val updatedProduct = selectedProductForEdit!!.copy(
                            name = name,
                            price = price,
                            stock = stock,
                            minStock = minStock,
                            barcode = barcode.ifBlank { null },
                            category = category
                        )
                        val result = productRepository.updateProduct(updatedProduct)
                        result.fold(
                            onSuccess = {
                                Toast.makeText(context, "Produk '$name' berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "Gagal memperbarui produk: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    showAddEditDialog = false
                    selectedProductForEdit = null
                }
            },
            onDismiss = {
                showAddEditDialog = false
                selectedProductForEdit = null
            }
        )
    }

    // Modal Konfirmasi Delete
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus produk '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = productRepository.deleteProduct(product)
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Produk terhapus", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    val msg = if (err is android.database.sqlite.SQLiteConstraintException) {
                                        "Produk '${product.name}' tidak dapat dihapus karena sudah memiliki riwayat transaksi penjualan."
                                    } else {
                                        "Gagal menghapus produk: ${err.message}"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                            productToDelete = null
                        }
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductItemRow(
    product: ProductEntity,
    currencyFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (product.category.isNotBlank()) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Harga: ${currencyFormat.format(product.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stok: ${product.stock} (Min. Stok: ${product.minStock})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock <= product.minStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!product.barcode.isNullOrEmpty()) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProductDialog(
    product: ProductEntity?,
    onSave: (name: String, price: Double, stock: Int, minStock: Int, barcode: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(product?.name.orEmpty()) }
    var priceInput by remember { mutableStateOf(product?.price?.toLong()?.toString().orEmpty()) }
    var stockInput by remember { mutableStateOf(product?.stock?.toString().orEmpty()) }
    var minStockInput by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var barcodeInput by remember { mutableStateOf(product?.barcode.orEmpty()) }
    var categoryInput by remember { mutableStateOf(product?.category.orEmpty()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Tambah Produk Baru" else "Edit Produk", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Jenis barang: tap lagi untuk membatalkan pilihan
                Text("Jenis Barang", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCategoryOptions.forEach { option ->
                        FilterChip(
                            selected = categoryInput == option,
                            onClick = { categoryInput = if (categoryInput == option) "" else option },
                            label = { Text(option) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Harga (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it },
                        label = { Text("Stok *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { minStockInput = it },
                        label = { Text("Min. Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("Kode Barcode (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    val stock = stockInput.toIntOrNull() ?: -1
                    val minStock = minStockInput.toIntOrNull() ?: 0

                    if (nameInput.isBlank()) {
                        errorMessage = "Nama produk wajib diisi."
                    } else if (price <= 0) {
                        errorMessage = "Harga harus lebih dari 0."
                    } else if (stock < 0) {
                        errorMessage = "Stok harus diisi angka valid."
                    } else {
                        onSave(nameInput.trim(), price, stock, minStock, barcodeInput.trim(), categoryInput)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}