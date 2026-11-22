package com.android.posanof.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

data class QrisTlv(
    val tag: String,
    val length: Int,
    val value: String
)

object QrisGenerator {

    /**
     * Parsing payload EMVCo menjadi list pasangan Tag-Length-Value.
     */
    fun parseTlvList(payload: String): List<QrisTlv>? {
        val list = mutableListOf<QrisTlv>()
        var index = 0
        while (index < payload.length) {
            if (index + 4 > payload.length) return null
            val tag = payload.substring(index, index + 2)
            val lenStr = payload.substring(index + 2, index + 4)
            val len = lenStr.toIntOrNull() ?: return null
            if (index + 4 + len > payload.length) return null
            val value = payload.substring(index + 4, index + 4 + len)
            list.add(QrisTlv(tag, len, value))
            index += 4 + len
        }
        return list
    }

    /**
     * Memeriksa apakah payload tergolong QRIS valid (diawali "000201", memuat "5802ID", dan TLV valid).
     */
    fun isValidQrisPayload(payload: String): Boolean {
        if (!payload.startsWith("000201")) return false
        if (!payload.contains("5802ID")) return false
        val tlvList = parseTlvList(payload) ?: return false
        return tlvList.isNotEmpty()
    }

    /**
     * Mengambil nama merchant dari Tag "59".
     */
    fun extractMerchantName(payload: String): String? {
        val tlvList = parseTlvList(payload) ?: return null
        return tlvList.firstOrNull { it.tag == "59" }?.value
    }

    /**
     * Menghasilkan payload QRIS Dinamis dari payload Statis dan nominal transaksi.
     */
    fun generateDynamicQris(staticPayload: String, amount: Long): String {
        val tlvList = parseTlvList(staticPayload)?.toMutableList() ?: return staticPayload

        // Abaikan tag 63 (CRC) jika ada
        tlvList.removeAll { it.tag == "63" }

        // Ubah Tag 01 (Point of Initiation Method) dari 11 (Static) ke 12 (Dynamic)
        val tag01Index = tlvList.indexOfFirst { it.tag == "01" }
        if (tag01Index != -1) {
            tlvList[tag01Index] = QrisTlv("01", 2, "12")
        } else {
            tlvList.add(0, QrisTlv("01", 2, "12"))
        }

        // Sisipkan atau Ganti Tag 54 (Transaction Amount)
        if (amount > 0) {
            val amountStr = amount.toString()
            val amountTlv = QrisTlv("54", amountStr.length, amountStr)

            val tag54Index = tlvList.indexOfFirst { it.tag == "54" }
            if (tag54Index != -1) {
                tlvList[tag54Index] = amountTlv
            } else {
                // Letakkan sebelum Tag 58 (Country Code) jika ada
                val tag58Index = tlvList.indexOfFirst { it.tag == "58" }
                if (tag58Index != -1) {
                    tlvList.add(tag58Index, amountTlv)
                } else {
                    tlvList.add(amountTlv)
                }
            }
        }

        // Susun kembali string TLV
        val sb = StringBuilder()
        for (tlv in tlvList) {
            sb.append(tlv.tag)
            sb.append(tlv.length.toString().padStart(2, '0'))
            sb.append(tlv.value)
        }

        // Tambahkan Tag 6304 untuk header CRC
        sb.append("6304")

        // Hitung CRC-16/CCITT-FALSE
        val crcHex = calculateCrc16CcittFalse(sb.toString().toByteArray(Charsets.UTF_8))

        sb.append(crcHex)
        return sb.toString()
    }

    /**
     * Perhitungan CRC-16/CCITT-FALSE (Polynomial 0x1021, Initial 0xFFFF, MSB-First).
     */
    fun calculateCrc16CcittFalse(bytes: ByteArray): String {
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor polynomial) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }

        return String.format("%04X", crc)
    }

    /**
     * Membangun Bitmap QR Code dari string teks dengan ZXing QRCodeWriter.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * Membaca dan mendekode payload QRIS dari sebuah Bitmap menggunakan Google ML Kit.
     */
    fun decodeQrisFromBitmap(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val validQris = barcodes
                    .mapNotNull { it.rawValue }
                    .firstOrNull { isValidQrisPayload(it) }
                onResult(validQris)
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                scanner.close()
            }
    }
}
