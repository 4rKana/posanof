package com.android.posanof.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object SafeFileSaver {

    inline fun saveToDownloadsFallback(
        context: Context,
        fileName: String,
        mimeType: String,
        writer: (OutputStream) -> Unit
    ): String {
        val cacheFile = File(context.cacheDir, "temp_$fileName")
        try {
            FileOutputStream(cacheFile).use { stream ->
                writer(stream)
                stream.flush()
            }

            if (!cacheFile.exists() || cacheFile.length() <= 0) {
                throw IllegalStateException("Berkas hasil ekspor kosong atau gagal ditulis.")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/POSANOF")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Gagal membuat entri berkas di MediaStore Downloads.")

                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        cacheFile.inputStream().use { input ->
                            input.copyTo(stream)
                        }
                        stream.flush()
                    } ?: throw IllegalStateException("Gagal membuka output stream berkas.")

                    val pendingDoneValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, pendingDoneValues, null, null)

                    return "Download/POSANOF/$fileName"
                } catch (e: Exception) {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) {
                        // Ignore deletion error
                    }
                    throw e
                }
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "POSANOF").apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }
                val targetFile = File(dir, fileName)
                cacheFile.copyTo(targetFile, overwrite = true)
                if (!targetFile.exists() || targetFile.length() <= 0) {
                    throw IllegalStateException("Berkas hasil ekspor kosong atau gagal ditulis.")
                }
                return targetFile.absolutePath
            }
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }
}
