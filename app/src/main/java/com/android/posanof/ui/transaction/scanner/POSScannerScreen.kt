package com.android.posanof.ui.transaction.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.ui.components.CameraScannerView
import com.android.posanof.ui.components.ManualProductSearchDialog
import com.android.posanof.ui.transaction.POSTransactionViewModel
import kotlinx.coroutines.launch

/**
 * Layar / Komponen Pemindaian Barcode POS.
 * Mengintegrasikan CameraX + ML Kit dengan penanganan Runtime Permission Kamera.
 */
@Composable
fun POSScannerScreen(
    viewModel: POSTransactionViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showManualDialog by remember { mutableStateOf(false) }
    var unresolvedBarcode by remember { mutableStateOf<String?>(null) }

    // --- State Runtime Permission Kamera ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin kamera diperlukan untuk memindai barcode.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val allProducts by productRepository.getAllProducts().collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // 1. Pemindai Kamera CameraX + ML Kit (jika izin diberikan)
            CameraScannerView(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = { scannedBarcode ->
                    coroutineScope.launch {
                        val product = productRepository.findProductByBarcode(scannedBarcode)
                        if (product != null) {
                            viewModel.addToCart(product)
                            Toast.makeText(
                                context,
                                "Ditambahkan: ${product.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Barcode tidak terdaftar / tidak ditemukan di DB -> Tampilkan dialog manual
                            unresolvedBarcode = scannedBarcode
                            showManualDialog = true
                        }
                    }
                }
            )
        } else {
            // Tampilan Peringatan Izin Kamera
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Akses Kamera Diperlukan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplikasi membutuhkan izin akses kamera untuk memindai barcode produk.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berikan Izin Kamera")
                }
            }
        }

        // 2. Tombol Cari Manual (jika barcode fisik rusak / tidak dapat dibaca kamera)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasCameraPermission) {
                Text(
                    text = "Arahkan kamera ke Barcode EAN-13 / QR Code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = {
                    unresolvedBarcode = null
                    showManualDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Barcode Rusak / Cari Produk Manual")
            }
        }

        // 3. Dialog Fallback Pencarian Manual
        if (showManualDialog) {
            ManualProductSearchDialog(
                scannedBarcode = unresolvedBarcode,
                productList = allProducts,
                onProductSelected = { selectedProduct ->
                    viewModel.addToCart(selectedProduct)
                    showManualDialog = false
                    unresolvedBarcode = null
                    Toast.makeText(
                        context,
                        "Ditambahkan: ${selectedProduct.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = {
                    showManualDialog = false
                    unresolvedBarcode = null
                }
            )
        }
    }
}
