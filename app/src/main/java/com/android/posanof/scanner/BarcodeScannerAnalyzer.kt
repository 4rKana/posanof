
package com.android.posanof.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analyzer CameraX menggunakan Google ML Kit untuk membaca barcode.
 *
 * Mencegah pemrosesan frame secara bersamaan, mengurangi scan berulang,
 * dan memilih barcode dengan area deteksi terbesar.
 */
class BarcodeScannerAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private var lastDetectedBarcode: String? = null
    private var consecutiveEmptyFrames = 0

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed.get()) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (isClosed.get()) {
                        return@addOnSuccessListener
                    }

                    // Pilih barcode dengan area bounding box terbesar.
                    val detectedBarcode = barcodes
                        .filter { !it.rawValue.isNullOrBlank() }
                        .maxByOrNull { barcode ->
                            val box = barcode.boundingBox

                            if (box == null) {
                                0L
                            } else {
                                box.width().toLong() * box.height().toLong()
                            }
                        }

                    val barcodeValue = detectedBarcode?.rawValue

                    if (barcodeValue == null) {
                        consecutiveEmptyFrames++

                        // Reset hanya setelah lima frame berhasil diproses
                        // tanpa barcode yang dapat dibaca.
                        if (consecutiveEmptyFrames >= 5) {
                            lastDetectedBarcode = null
                        }
                    } else {
                        consecutiveEmptyFrames = 0

                        // Barcode yang sama tidak dikirim berulang kali
                        // selama masih terdeteksi.
                        if (barcodeValue != lastDetectedBarcode) {
                            lastDetectedBarcode = barcodeValue

                            try {
                                onBarcodeDetected(barcodeValue)
                            } catch (exception: Exception) {
                                // Cegah error callback menghentikan analyzer.
                                exception.printStackTrace()
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Kegagalan satu frame tidak langsung mereset
                    // barcode terakhir yang terdeteksi.
                    exception.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing.set(false)
                }
        } catch (exception: Exception) {
            imageProxy.close()
            isProcessing.set(false)
            exception.printStackTrace()
        }
    }

    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            scanner.close()
        }
    }
}
