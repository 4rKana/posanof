
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Layar pemindaian barcode POS.
 * Mencegah proses scan berjalan bersamaan dan menjaga dialog manual
 * agar tidak terganggu oleh hasil scan berikutnya.
 */
@Composable
fun POSScannerScreen(
    viewModel: POSTransactionViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showManualDialog by remember {
        mutableStateOf(false)
    }

    var unresolvedBarcode by remember {
        mutableStateOf<String?>(null)
    }

    // Pengaman agar beberapa callback scan tidak diproses bersamaan.
    val scanInProgress = remember {
        AtomicBoolean(false)
    }

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
            Toast.makeText(
                context,
                "Izin kamera diperlukan untuk memindai barcode.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val allProducts by productRepository
        .getAllProducts()
        .collectAsState(initial = emptyList())

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (hasCameraPermission) {
            CameraScannerView(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = { scannedBarcode ->
                    // Abaikan callback ketika pemindaian sebelumnya
                    // sedang diproses atau dialog manual terbuka.
                    if (
                        !showManualDialog &&
                        scanInProgress.compareAndSet(false, true)
                    ) {
                        coroutineScope.launch {
                            var waitingForManualSelection = false

                            try {
                                val product =
                                    productRepository.findProductByBarcode(
                                        scannedBarcode
                                    )

                                if (product != null) {
                                    viewModel.addToCart(product)

                                    Toast.makeText(
                                        context,
                                        "Ditambahkan: ${product.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    unresolvedBarcode = scannedBarcode
                                    showManualDialog = true

                                    // Kunci tetap aktif sampai dialog ditutup.
                                    waitingForManualSelection = true
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Gagal mencari produk. Silakan coba lagi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                if (!waitingForManualSelection) {
                                    scanInProgress.set(false)
                                }
                            }
                        }
                    }
                }
            )
        } else {
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

                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Berikan Izin Kamera")
                }
            }
        }

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
                    // Hentikan pemrosesan scan saat pencarian manual dibuka.
                    if (scanInProgress.compareAndSet(false, true)) {
                        unresolvedBarcode = null
                        showManualDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Barcode Rusak / Cari Produk Manual")
            }
        }

        if (showManualDialog) {
            ManualProductSearchDialog(
                scannedBarcode = unresolvedBarcode,
                productList = allProducts,
                onProductSelected = { selectedProduct ->
                    viewModel.addToCart(selectedProduct)

                    showManualDialog = false
                    unresolvedBarcode = null
                    scanInProgress.set(false)

                    Toast.makeText(
                        context,
                        "Ditambahkan: ${selectedProduct.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = {
                    showManualDialog = false
                    unresolvedBarcode = null
                    scanInProgress.set(false)
                }
            )
        }
    }
}
