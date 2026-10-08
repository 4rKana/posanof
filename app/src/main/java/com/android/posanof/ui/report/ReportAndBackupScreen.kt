package com.android.posanof.ui.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.util.ReportExporter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Laporan Penjualan (Ekspor Excel & PDF) serta Backup & Restore Encrypted SAF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportAndBackupScreen(
    transactionRepository: TransactionRepository,
    backupRepository: BackupRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID")) }

    val transactionsWithDetails by transactionRepository.getAllTransactionsWithDetails()
        .collectAsState(initial = emptyList())

    var isProcessing by remember { mutableStateOf(false) }
    var confirmRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // --- SAF Launchers Ekspor Laporan ---
    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        ReportExporter.exportToExcel(transactionsWithDetails, stream)
                    }
                    Toast.makeText(context, "Laporan Excel berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal ekspor Excel: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        ReportExporter.exportToPdf(transactionsWithDetails, stream)
                    }
                    Toast.makeText(context, "Laporan PDF berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal ekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- SAF Launchers Backup & Restore ---
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan & Backup Data", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Section 1: Ekspor Laporan Penjualan (Paperless)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ekspor Laporan Penjualan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ekspor data riwayat transaksi dalam format Excel (.xlsx) atau PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createExcelLauncher.launch("Laporan_Penjualan_$timestamp.xlsx")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Excel (.xlsx)")
                        }

                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createPdfLauncher.launch("Laporan_Keuangan_$timestamp.pdf")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("PDF (.pdf)")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Backup & Restore Data
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Backup & Restore Database (AES-256)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enkripsi teramankan dengan AES-256 GCM untuk mencegah kebocoran data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createBackupLauncher.launch("POSANOF_BACKUP_$timestamp.backup")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Backup Data")
                        }

                        OutlinedButton(
                            onClick = { openRestoreLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Restore Data")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Riwayat Transaksi Penjualan (${transactionsWithDetails.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (transactionsWithDetails.isEmpty()) {
                Text(
                    text = "Belum ada transaksi penjualan yang dicatat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = transactionsWithDetails,
                        key = { it.transaction.id }
                    ) { item ->
                        val t = item.transaction
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "ID: #${t.id}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = currencyFormat.format(t.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Tanggal: ${dateFormatter.format(Date(t.date))} | Kasir: ${t.cashierName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Metode: ${t.paymentMethod} | Item: ${item.details.sumOf { it.quantity }} Pcs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Konfirmasi Restore Data
    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestoreUri = null },
            title = { Text("Konfirmasi Restore Database") },
            text = {
                Text("Restore akan MENIMPA (OVERWRITE) seluruh data lokal produk, transaksi, dan piutang saat ini dengan data dari file backup. Lanjutkan?")
            },
            confirmButton = {
                Button(
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
                    Text("Ya, Timpa & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreUri = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
