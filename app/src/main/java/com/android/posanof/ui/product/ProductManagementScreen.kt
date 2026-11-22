package com.android.posanof.ui.product

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.ui.components.*
import kotlinx.coroutines.launch
import java.util.Locale

import com.android.posanof.data.local.SettingsManager

@Composable
fun ProductManagementScreen(
    productRepository: ProductRepository,
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allProducts by productRepository.getAllProducts().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var showSearchScanner by remember { mutableStateOf(false) }
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
    
    if (showSearchScanner) {
        BarcodeScannerOverlay(
            onBarcodeScanned = { barcode ->
                searchQuery = barcode
                showSearchScanner = false
            },
            onBack = { showSearchScanner = false }
        )
    } else if (showAddEditDialog) {
        BackHandler { showAddEditDialog = false }
        AddEditProductScreen(
            product = selectedProductForEdit,
            settingsManager = settingsManager,
            onSave = { name, price, stock, minStock, barcode, category ->
                coroutineScope.launch {
                    if (selectedProductForEdit == null) {
                        val newProduct = ProductEntity(
                            name = name,
                            price = price,
                            stock = stock,
                            minStock = minStock,
                            barcode = if (barcode.isBlank()) null else barcode,
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
                            barcode = if (barcode.isBlank()) null else barcode,
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
            onDelete = {
                productToDelete = selectedProductForEdit
            },
            onDismiss = {
                showAddEditDialog = false
                selectedProductForEdit = null
            }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PosBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Master Data",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosInk,
                    modifier = Modifier.weight(1f)
                )
                PosBadge(text = "Admin", bg = Color(0xFFE0F2F1), fg = Color(0xFF00796B))
            }

            // Search Bar (rounded, white) + scan button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = PosMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = PosInk),
                        cursorBrush = SolidColor(PosPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Cari produk atau barcode", color = PosMuted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = PosMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PosPrimary)
                        .clickable { showSearchScanner = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Scan barcode", tint = Color.White)
                }
            }
            
            // List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductItemRow(
                        product = product,
                        onEdit = {
                            selectedProductForEdit = product
                            showAddEditDialog = true
                        }
                    )
                }
            }
            
            Box(Modifier.padding(16.dp)) {
                PosPrimaryButton(
                    text = "+ Tambah Produk",
                    onClick = {
                        selectedProductForEdit = null
                        showAddEditDialog = true
                    }
                )
            }
        }
    }
    
    // Modal Konfirmasi Delete
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus produk '${product.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = productRepository.deleteProduct(product)
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Produk terhapus", Toast.LENGTH_SHORT).show()
                                    showAddEditDialog = false
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
                    Text("Hapus", color = PosDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal", color = PosMuted)
                }
            }
        )
    }
}

@Composable
private fun ProductItemRow(
    product: ProductEntity,
    onEdit: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock
    val bgColor = if (isLowStock) PosWarnBg else Color.White
    val borderColor = if (isLowStock) Color(0xFFFFD54F) else PosBorder
    val stockTextColor = if (isLowStock) PosWarnText else PosMuted
    val stockText = if (isLowStock) "Stok ${product.stock} · menipis" else "Stok ${product.stock}"

    PosCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), 
        color = bgColor,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PosInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.price.toLong().toRupiah(),
                        fontSize = 12.sp,
                        color = PosInk,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = PosMuted
                    )
                    Text(
                        text = stockText,
                        fontSize = 12.sp,
                        color = stockTextColor,
                        fontWeight = if (isLowStock) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = PosSoft,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PosPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
