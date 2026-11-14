package com.android.posanof.ui.transaction.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.ui.components.CameraScannerView
import com.android.posanof.ui.components.ManualProductSearchDialog
import com.android.posanof.ui.components.PosBadge
import com.android.posanof.ui.components.PosBorder
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosOutlineButton
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosPrimaryButton
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.components.PosTopBar
import com.android.posanof.ui.components.toRupiah
import com.android.posanof.ui.transaction.POSTransactionViewModel
import kotlinx.coroutines.launch

private val ScanBackground = Color(0xFF161D1B)
private val ScanAccent = Color(0xFF5EEAD4)

/** Barcode yang sama dalam rentang ini dianggap satu kali scan (mencegah tambah ganda). */
private const val SCAN_COOLDOWN_MS = 1500L

private class ScanGate {
    @Volatile var barcode: String = ""
    @Volatile var lastSeenAt: Long = 0L
}

/**
 * Layar Pindai Barcode (desain Figma layar 2).
 * Logika scan, izin kamera, dan pencarian manual tetap sama seperti sebelumnya.
 */
@Composable
fun POSScannerScreen(
    viewModel: POSTransactionViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onViewCart: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scanGate = remember { ScanGate() }

    var showManualDialog by remember { mutableStateOf(false) }
    var unresolvedBarcode by remember { mutableStateOf<String?>(null) }
    var detectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

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

    Column(modifier.fillMaxSize().background(ScanBackground)) {
        PosTopBar(title = "Pindai Barcode", onBack = onBack, dark = true)

        // ---- Area kamera ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraScannerView(
                    modifier = Modifier.fillMaxSize(),
                    showFrame = false,
                    onBarcodeScanned = { scannedBarcode ->
                        val now = System.currentTimeMillis()
                        val sameAsLast = scannedBarcode == scanGate.barcode &&
                                now - scanGate.lastSeenAt < SCAN_COOLDOWN_MS
                        scanGate.barcode = scannedBarcode
                        scanGate.lastSeenAt = now
                        if (sameAsLast) return@CameraScannerView

                        coroutineScope.launch {
                            val product = productRepository.findProductByBarcode(scannedBarcode)
                            if (product != null) {
                                viewModel.addToCart(product)
                                detectedProduct = product
                            } else {
                                // Barcode tidak terdaftar -> tampilkan pencarian manual
                                unresolvedBarcode = scannedBarcode
                                showManualDialog = true
                            }
                        }
                    }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScanFrame()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Arahkan kamera ke barcode produk",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Akses Kamera Diperlukan",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aplikasi membutuhkan izin kamera untuk memindai barcode produk.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    PosPrimaryButton(
                        text = "Berikan Izin Kamera",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        }

        // ---- Panel hasil scan ----
        ScanResultPanel(
            product = detectedProduct,
            onManualSearch = {
                unresolvedBarcode = null
                showManualDialog = true
            },
            onViewCart = onViewCart
        )
    }

    // Dialog Fallback Pencarian Manual
    if (showManualDialog) {
        ManualProductSearchDialog(
            scannedBarcode = unresolvedBarcode,
            productList = allProducts,
            onProductSelected = { selectedProduct ->
                viewModel.addToCart(selectedProduct)
                detectedProduct = selectedProduct
                showManualDialog = false
                unresolvedBarcode = null
            },
            onDismiss = {
                showManualDialog = false
                unresolvedBarcode = null
            }
        )
    }
}

@Composable
private fun ScanResultPanel(
    product: ProductEntity?,
    onManualSearch: () -> Unit,
    onViewCart: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(36.dp).height(4.dp).background(PosBorder, CircleShape))
            Spacer(Modifier.height(14.dp))

            if (product != null) {
                Column(Modifier.fillMaxWidth()) {
                    PosBadge("Terdeteksi")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            product.price.toLong().toRupiah(),
                            fontWeight = FontWeight.Bold,
                            color = PosPrimary
                        )
                    }
                    if (!product.barcode.isNullOrBlank()) {
                        Text("Barcode ${product.barcode}", fontSize = 11.sp, color = PosMuted)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(PosSoft.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = PosPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ditambahkan ke keranjang",
                            color = PosPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text("Belum ada barcode terdeteksi", color = PosMuted, fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PosOutlineButton("Cari manual", onManualSearch, Modifier.weight(1f))
                PosPrimaryButton("Lihat Keranjang", onViewCart, Modifier.weight(1f))
            }
        }
    }
}

/** Bingkai sudut hijau + garis scan di tengah, sesuai Figma. */
@Composable
private fun ScanFrame() {
    Canvas(Modifier.size(230.dp)) {
        val stroke = 4.dp.toPx()
        val len = 38.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(a: Offset, b: Offset) = drawLine(ScanAccent, a, b, stroke, StrokeCap.Round)

        // kiri-atas
        line(Offset(0f, 0f), Offset(len, 0f)); line(Offset(0f, 0f), Offset(0f, len))
        // kanan-atas
        line(Offset(w, 0f), Offset(w - len, 0f)); line(Offset(w, 0f), Offset(w, len))
        // kiri-bawah
        line(Offset(0f, h), Offset(len, h)); line(Offset(0f, h), Offset(0f, h - len))
        // kanan-bawah
        line(Offset(w, h), Offset(w - len, h)); line(Offset(w, h), Offset(w, h - len))
        // garis scan
        drawLine(ScanAccent, Offset(20.dp.toPx(), h / 2), Offset(w - 20.dp.toPx(), h / 2), 2.dp.toPx())
    }
}