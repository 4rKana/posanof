package com.android.posanof.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.ui.components.*

import com.android.posanof.data.local.SettingsManager

@Composable
fun AddEditProductScreen(
    product: ProductEntity?,
    settingsManager: SettingsManager,
    onSave: (name: String, price: Double, stock: Int, minStock: Int, barcode: String, category: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultMinStock by settingsManager.defaultMinStock.collectAsState()
    
    var nameInput by remember { mutableStateOf(product?.name.orEmpty()) }
    var priceInput by remember { mutableStateOf(product?.price?.toLong()?.toString().orEmpty()) }
    var stockInput by remember { mutableStateOf(product?.stock?.toString().orEmpty()) }
    var minStockInput by remember { mutableStateOf(product?.minStock?.toString() ?: defaultMinStock.toString()) }
    var barcodeInput by remember { mutableStateOf(product?.barcode.orEmpty()) }
    var categoryInput by remember { mutableStateOf(product?.category.orEmpty()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    val categories = listOf("Minuman", "Makanan", "Sembako", "Lainnya")
    
    if (showScanner) {
        CameraScannerView(
            onBarcodeScanned = { barcode ->
                barcodeInput = barcode
                showScanner = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PosBackground)
    ) {
        PosTopBar(
            title = if (product == null) "Tambah Produk" else "Edit Produk",
            onBack = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Nama produk", fontSize = 12.sp, color = PosMuted)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            val priceVal = priceInput.toDoubleOrNull() ?: 0.0
            val isPriceError = errorMessage != null && priceVal <= 0
            Text("Harga jual", fontSize = 12.sp, color = PosMuted)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = priceInput,
                onValueChange = { priceInput = it.filter { char -> char.isDigit() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = isPriceError,
                colors = OutlinedTextFieldDefaults.colors(
                    errorBorderColor = PosDanger
                )
            )
            if (isPriceError) {
                Text("Harga harus lebih dari 0", color = PosDanger, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Stok", fontSize = 12.sp, color = PosMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Stok minimum", fontSize = 12.sp, color = PosMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { minStockInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Barcode (opsional)", fontSize = 12.sp, color = PosMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Jenis Barang", fontSize = 12.sp, color = PosMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { option ->
                    FilterChip(
                        selected = categoryInput == option,
                        onClick = { categoryInput = if (categoryInput == option) "" else option },
                        label = { Text(option) }
                    )
                }
            }

            if (errorMessage != null && !isPriceError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage.orEmpty(), color = PosDanger, fontSize = 12.sp)
            }
        }
        
        Column(modifier = Modifier.padding(16.dp)) {
            PosPrimaryButton(
                text = "Simpan Produk",
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    val stock = stockInput.toIntOrNull() ?: -1
                    val minStock = minStockInput.toIntOrNull() ?: defaultMinStock

                    if (nameInput.isBlank()) {
                        errorMessage = "Nama produk wajib diisi."
                    } else if (price <= 0) {
                        errorMessage = "Harga harus lebih dari 0"
                    } else if (stock < 0) {
                        errorMessage = "Stok harus diisi angka valid."
                    } else {
                        onSave(nameInput.trim(), price, stock, minStock, barcodeInput.trim(), categoryInput)
                    }
                }
            )
            if (product != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PosDanger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosDanger),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) {
                    Text("Hapus Produk", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
