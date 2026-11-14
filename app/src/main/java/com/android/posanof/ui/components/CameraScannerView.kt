package com.android.posanof.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.android.posanof.scanner.BarcodeScannerAnalyzer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Komponen Compose CameraX Preview untuk pemindaian barcode.
 *
 * [showFrame] = true menampilkan kotak panduan bawaan di tengah.
 * Set false kalau layar pemanggil menggambar bingkainya sendiri (mis. POSScannerScreen).
 */
@Composable
fun CameraScannerView(
    modifier: Modifier = Modifier,
    showFrame: Boolean = true,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Selalu gunakan callback terbaru dari Compose.
    val currentOnBarcodeScanned = rememberUpdatedState(onBarcodeScanned)

    val previewView = remember(context) {
        PreviewView(context)
    }

    DisposableEffect(context, lifecycleOwner) {
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val isDisposed = AtomicBoolean(false)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        var cameraProvider: ProcessCameraProvider? = null
        var imageAnalysis: ImageAnalysis? = null
        var barcodeAnalyzer: BarcodeScannerAnalyzer? = null

        cameraProviderFuture.addListener(
            {
                if (isDisposed.get()) {
                    return@addListener
                }

                var analyzer: BarcodeScannerAnalyzer? = null
                var analysis: ImageAnalysis? = null

                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    if (isDisposed.get()) {
                        return@addListener
                    }

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    analyzer = BarcodeScannerAnalyzer(
                        onBarcodeDetected = { barcode ->
                            if (!isDisposed.get()) {
                                currentOnBarcodeScanned.value(barcode)
                            }
                        }
                    )
                    barcodeAnalyzer = analyzer

                    analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }

                    imageAnalysis = analysis

                    // Menggunakan CameraSelector default (Kamera Belakang)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    analysis?.clearAnalyzer()
                    analyzer?.close()
                    e.printStackTrace()
                }
            },
            mainExecutor
        )

        onDispose {
            isDisposed.set(true)

            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            barcodeAnalyzer?.close()

            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Frame panduan pemindaian di tengah layar (opsional).
        if (showFrame) {
            Box(
                modifier = Modifier
                    .size(260.dp, 160.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}