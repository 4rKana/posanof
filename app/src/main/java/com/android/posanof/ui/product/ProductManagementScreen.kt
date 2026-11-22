
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val ProductCategoryOptions =
    listOf("Minuman", "Makanan", "Sembako", "Lainnya")

private val ProductPrimary = Color(0xFF087F75)
private val ProductBackground = Color(0xFFF4F6F4)
private val ProductText = Color(0xFF172522)
private val ProductMuted = Color(0xFF71807D)
private val ProductWarningBackground = Color(0xFFFFF3C9)
private val ProductWarningText = Color(0xFF9A4C00)
private val ProductDanger = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    productRepository: ProductRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("id-ID")
        ).apply {
            maximumFractionDigits = 0
        }
    }

    val allProducts by productRepository
        .getAllProducts()
        .collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditScreen by remember { mutableStateOf(false) }

    var selectedProductForEdit by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    var productToDelete by remember {
        mutableStateOf<ProductEntity?>(null)
    }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isBlank()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        (it.barcode?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true)
            }
        }
    }

    // HALAMAN EDIT PRODUK
    if (showEditScreen && selectedProductForEdit != null) {
        ProductEditScreen(
            product = selectedProductForEdit!!,
            onBack = {
                showEditScreen = false
                selectedProductForEdit = null
            },
            onSave = { name, price, stock, minStock, barcode ->
                val currentProduct = selectedProductForEdit!!

                coroutineScope.launch {
                    val updatedProduct = currentProduct.copy(
                        name = name,
                        price = price,
                        stock = stock,
                        minStock = minStock,
                        barcode = barcode.ifBlank { null },
                        category = currentProduct.category
                    )

                    val result =
                        productRepository.updateProduct(updatedProduct)

                    result.fold(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Produk '$name' berhasil diperbarui",
                                Toast.LENGTH_SHORT
                            ).show()

                            showEditScreen = false
                            selectedProductForEdit = null
                        },
                        onFailure = { err ->
                            Toast.makeText(
                                context,
                                "Gagal memperbarui produk: ${err.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            },
            onDelete = {
                productToDelete = selectedProductForEdit
            },
            modifier = modifier
        )
    } else {
        // HALAMAN DAFTAR PRODUK
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = ProductBackground,
            bottomBar = {
                Surface(
                    color = ProductBackground,
                    shadowElevation = 4.dp
                ) {
                    Button(
                        onClick = {
                            showAddDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 10.dp,
                                bottom = 12.dp
                            )
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProductPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text(
                            "Tambah Produk",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Master Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ProductText
                    )

                    Surface(
                        color = Color(0xFFDDF3EF),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Admin",
                            color = ProductPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Cari produk atau barcode",
                            color = ProductMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = ProductMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProductPrimary,
                        unfocusedBorderColor = Color(0xFFD5DEDA),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(12.dp))

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allProducts.isEmpty()) {
                                "Belum ada produk.\nTekan Tambah Produk untuk memulai."
                            } else {
                                "Produk tidak ditemukan."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ProductMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
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
                                    showEditScreen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG TAMBAH PRODUK
    if (showAddDialog) {
        AddProductDialog(
            onSave = { name, price, stock, minStock, barcode, category ->
                coroutineScope.launch {
                    val newProduct = ProductEntity(
                        name = name,
                        price = price,
                        stock = stock,
                        minStock = minStock,
                        barcode = barcode.ifBlank { null },
                        category = category
                    )

                    val result =
                        productRepository.insertProduct(newProduct)

                    result.fold(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Produk '$name' berhasil ditambahkan",
                                Toast.LENGTH_SHORT
                            ).show()

                            showAddDialog = false
                        },
                        onFailure = { err ->
                            Toast.makeText(
                                context,
                                "Gagal menambah produk: ${err.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            },
            onDismiss = {
                showAddDialog = false
            }
        )
    }

    // KONFIRMASI HAPUS PRODUK
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = {
                productToDelete = null
            },
            title = {
                Text("Hapus Produk")
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus produk '${product.name}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result =
                                productRepository.deleteProduct(product)

                            result.fold(
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Produk terhapus",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showEditScreen = false
                                    selectedProductForEdit = null
                                },
                                onFailure = { err ->
                                    val message =
                                        if (err is android.database.sqlite.SQLiteConstraintException) {
                                            "Produk '${product.name}' tidak dapat dihapus karena sudah memiliki riwayat transaksi penjualan."
                                        } else {
                                            "Gagal menghapus produk: ${err.message}"
                                        }

                                    Toast.makeText(
                                        context,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )

                            productToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProductDanger
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        productToDelete = null
                    }
                ) {
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
    onEdit: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) {
                ProductWarningBackground
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 6.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProductText
                )

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencyFormat.format(product.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLowStock) {
                            ProductWarningText
                        } else {
                            ProductMuted
                        }
                    )

                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProductMuted
                    )

                    Text(
                        text = "Stok ${product.stock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLowStock) {
                            ProductWarningText
                        } else {
                            ProductMuted
                        },
                        fontWeight = if (isLowStock) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )

                    if (isLowStock) {
                        Text(
                            text = " · menipis",
                            style = MaterialTheme.typography.bodySmall,
                            color = ProductWarningText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit produk",
                    tint = ProductPrimary
                )
            }
        }
    }
}

// HALAMAN EDIT PRODUK FULL SCREEN
@Composable
private fun ProductEditScreen(
    product: ProductEntity,
    onBack: () -> Unit,
    onSave: (
        name: String,
        price: Double,
        stock: Int,
        minStock: Int,
        barcode: String
    ) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember(product.id) {
        mutableStateOf(product.name)
    }

    var priceInput by remember(product.id) {
        mutableStateOf(product.price.toLong().toString())
    }

    var stockInput by remember(product.id) {
        mutableStateOf(product.stock.toString())
    }

    var minStockInput by remember(product.id) {
        mutableStateOf(product.minStock.toString())
    }

    var barcodeInput by remember(product.id) {
        mutableStateOf(product.barcode.orEmpty())
    }

    var errorMessage by remember(product.id) {
        mutableStateOf<String?>(null)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ProductBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val price = priceInput.toDoubleOrNull() ?: 0.0
                        val stock = stockInput.toIntOrNull() ?: -1
                        val minStock = minStockInput.toIntOrNull() ?: -1

                        when {
                            nameInput.isBlank() -> {
                                errorMessage = "Nama produk wajib diisi."
                            }

                            price <= 0 -> {
                                errorMessage = "Harga harus lebih dari 0."
                            }

                            stock < 0 -> {
                                errorMessage = "Stok harus diisi angka valid."
                            }

                            minStock < 0 -> {
                                errorMessage = "Stok minimum tidak valid."
                            }

                            else -> {
                                errorMessage = null
                                onSave(
                                    nameInput.trim(),
                                    price,
                                    stock,
                                    minStock,
                                    barcodeInput.trim()
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProductPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Simpan Produk",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = ProductDanger
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ProductDanger
                    )
                ) {
                    Text(
                        "Hapus Produk",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = ProductText
                    )
                }

                Text(
                    text = "Edit Produk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProductText
                )
            }

            EditFieldLabel("Nama produk")

            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    errorMessage = null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )

            Spacer(Modifier.height(8.dp))

            EditFieldLabel("Harga jual")

            OutlinedTextField(
                value = priceInput,
                onValueChange = {
                    priceInput = it
                    errorMessage = null
                },
                singleLine = true,
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    EditFieldLabel("Stok")

                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = {
                            stockInput = it
                            errorMessage = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = editFieldColors()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    EditFieldLabel("Stok minimum")

                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = {
                            minStockInput = it
                            errorMessage = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = editFieldColors()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            EditFieldLabel("Barcode (opsional)")

            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = errorMessage.orEmpty(),
                    color = ProductDanger,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EditFieldLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = ProductMuted,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun editFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ProductPrimary,
        unfocusedBorderColor = Color(0xFFD5DEDA),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedTextColor = ProductText,
        unfocusedTextColor = ProductText
    )

// DIALOG TAMBAH PRODUK
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
    onSave: (
        name: String,
        price: Double,
        stock: Int,
        minStock: Int,
        barcode: String,
        category: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }
    var minStockInput by remember { mutableStateOf("5") }
    var barcodeInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Tambah Produk Baru",
                fontWeight = FontWeight.Bold
            )
        },
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

                Spacer(Modifier.height(8.dp))

                Text(
                    "Jenis Barang",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCategoryOptions.forEach { option ->
                        FilterChip(
                            selected = categoryInput == option,
                            onClick = {
                                categoryInput =
                                    if (categoryInput == option) "" else option
                            },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Harga (Rp) *") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it },
                        label = { Text("Stok *") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { minStockInput = it },
                        label = { Text("Min. Stok") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("Kode Barcode (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    val stock = stockInput.toIntOrNull() ?: -1
                    val minStock = minStockInput.toIntOrNull() ?: 0

                    when {
                        nameInput.isBlank() -> {
                            errorMessage = "Nama produk wajib diisi."
                        }

                        price <= 0 -> {
                            errorMessage = "Harga harus lebih dari 0."
                        }

                        stock < 0 -> {
                            errorMessage = "Stok harus diisi angka valid."
                        }

                        else -> {
                            onSave(
                                nameInput.trim(),
                                price,
                                stock,
                                minStock,
                                barcodeInput.trim(),
                                categoryInput
                            )
                        }
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
