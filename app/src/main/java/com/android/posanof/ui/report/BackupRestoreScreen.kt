package com.android.posanof.ui.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.SettingsManager
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.ui.components.*
import com.android.posanof.util.SafeFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    backupRepository: BackupRepository,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var confirmRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val lastBackupTimestamp by settingsManager.lastBackupTimestamp.collectAsState()

    BackHandler(onBack = onBack)

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isProcessing = true
                val result = backupRepository.exportBackup(it)
                isProcessing = false
                result.fold(
                    onSuccess = { summary ->
                        settingsManager.updateLastBackupTimestamp(System.currentTimeMillis())
                        Toast.makeText(
                            context,
                            "Backup Berhasil Ditulis! (${summary.productCount} Produk, ${summary.transactionCount} Transaksi)",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "Gagal Backup: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            confirmRestoreUri = it
        }
    }

    val getContentRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            confirmRestoreUri = it
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
    ) {
        PosTopBar(
            title = "Backup dan Restore",
            onBack = onBack,
            trailing = {
                PosBadge("Admin", bg = Color(0xFFE0F2F1), fg = Color(0xFF00796B))
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PosCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backup Data", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PosInk)
                    Spacer(Modifier.height(4.dp))
                    Text("Terenkripsi AES-256, simpan ke penyimpanan", fontSize = 12.sp, color = PosMuted)
                    Spacer(Modifier.height(16.dp))
                    PosPrimaryButton(
                        text = if (isProcessing) "Memproses..." else "Backup Sekarang",
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            val fileName = "POSANOF_BACKUP_$timestamp.backup"
                            try {
                                createBackupLauncher.launch(fileName)
                            } catch (_: android.content.ActivityNotFoundException) {
                                coroutineScope.launch {
                                    isProcessing = true
                                    try {
                                        var backupSummary: com.android.posanof.data.repository.BackupSummary? = null
                                        val path = withContext(Dispatchers.IO) {
                                            SafeFileSaver.saveToDownloadsFallback(
                                                context = context,
                                                fileName = fileName,
                                                mimeType = "application/octet-stream"
                                            ) { stream ->
                                                val res = backupRepository.exportBackupToStream(stream)
                                                backupSummary = res.getOrNull()
                                            }
                                        }
                                        settingsManager.updateLastBackupTimestamp(System.currentTimeMillis())
                                        val summaryText = backupSummary?.let { " (${it.productCount} Produk, ${it.transactionCount} Transaksi)" } ?: ""
                                        Toast.makeText(context, "Backup berhasil tersimpan di: $path$summaryText", Toast.LENGTH_LONG).show()
                                    } catch (err: Exception) {
                                        Toast.makeText(context, "Gagal Backup: ${err.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing
                    )
                }
            }

            PosCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Restore Data", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PosInk)
                    Spacer(Modifier.height(4.dp))
                    Text("Pilih file .backup dari penyimpanan", fontSize = 12.sp, color = PosMuted)
                    Spacer(Modifier.height(16.dp))
                    PosOutlineButton(
                        text = "Pilih File Backup",
                        onClick = {
                            try {
                                openRestoreLauncher.launch(arrayOf("*/*"))
                            } catch (_: android.content.ActivityNotFoundException) {
                                try {
                                    getContentRestoreLauncher.launch("*/*")
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Tidak ada aplikasi pengelola berkas yang mendukung.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            PosWarningBox("Restore akan menimpa data saat ini. Pastikan backup terlebih dahulu.")
            
            val lastBackupText = if (lastBackupTimestamp == 0L) {
                "Belum pernah backup"
            } else {
                val format = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID"))
                "Backup terakhir: ${format.format(Date(lastBackupTimestamp))}"
            }
            Text(lastBackupText, fontSize = 12.sp, color = PosMuted)
        }
    }

    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestoreUri = null },
            title = { Text("Konfirmasi Restore Database", fontWeight = FontWeight.Bold, color = PosInk) },
            text = { Text("Restore akan MENIMPA (OVERWRITE) seluruh data lokal dengan data dari file backup. Lanjutkan?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            val result = backupRepository.importRestore(uri)
                            isProcessing = false
                            confirmRestoreUri = null
                            result.fold(
                                onSuccess = { summary ->
                                    Toast.makeText(
                                        context,
                                        "Restore Berhasil! (${summary.productCount} Produk, ${summary.transactionCount} Transaksi)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Gagal Restore: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Timpa & Restore", color = PosDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreUri = null }) {
                    Text("Batal", color = PosMuted)
                }
            }
        )
    }
}
