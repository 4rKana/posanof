package com.android.posanof.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * ImageAnalysis.Analyzer menggunakan Google ML Kit Vision untuk membaca
 * Barcode EAN-13, EAN-8, QR Code, dan format standar lainnya dari CameraX.
 */
class BarcodeScannerAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
    private val cooldownMillis: Long = 1500L
) : ImageAnalysis.Analyzer {

    private var lastScanTimestamp: Long = 0L

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
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTimestamp < cooldownMillis) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val detectedBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
                if (detectedBarcode != null) {
                    val barcodeValue = detectedBarcode.rawValue.orEmpty()
                    lastScanTimestamp = System.currentTimeMillis()
                    onBarcodeDetected(barcodeValue)
                }
            }
            .addOnFailureListener {
                // Abaikan error pembacaan frame individual
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Menutup ML Kit Barcode Scanner client untuk membebaskan resource.
     */
    fun close() {
        try {
            scanner.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}