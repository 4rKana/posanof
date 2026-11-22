package com.android.posanof.ui.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.SettingsManager
import com.android.posanof.ui.components.*
import com.android.posanof.util.SafeFileSaver
import com.android.posanof.util.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    settingsManager: SettingsManager,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val period by viewModel.selectedPeriod.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val last7DaysSales by viewModel.last7DaysSales.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val currentTransactions by viewModel.currentTransactions.collectAsState()
    val storeName by settingsManager.storeName.collectAsState()

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        withContext(Dispatchers.IO) {
                            ReportExporter.exportToExcel(currentTransactions, stream, title = "LAPORAN PENJUALAN $storeName")
                        }
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
                        withContext(Dispatchers.IO) {
                            ReportExporter.exportToPdf(currentTransactions, stream, storeName = storeName)
                        }
                    }
                    Toast.makeText(context, "Laporan PDF berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal ekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PosInk)
            PosBadge("PIN terverifikasi", bg = Color(0xFFE8F5E9), fg = Color(0xFF2E7D32))
        }

        val options = listOf("Harian", "Mingguan", "Bulanan")
        PosSegmented(
            options = options,
            selectedIndex = period.ordinal,
            onSelect = { viewModel.updatePeriod(DashboardPeriod.entries[it]) }
        )

        if (period == DashboardPeriod.BULANAN) {
            MonthYearBar(
                month = selectedMonth,
                year = selectedYear,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosCard(Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Penjualan", fontSize = 12.sp, color = PosMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        totalSales.toLong().toRupiah(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosInk
                    )
                }
            }
            PosCard(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToHistory)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Transaksi", fontSize = 12.sp, color = PosMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        transactionCount.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosInk
                    )
                }
            }
        }

        PosCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Penjualan 7 hari terakhir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PosInk)
                Spacer(Modifier.height(16.dp))
                
                if (last7DaysSales.isNotEmpty()) {
                    val maxSales = last7DaysSales.maxOfOrNull { it.sales } ?: 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysSales.forEach { day ->
                            val ratio = if (maxSales > 0) (day.sales / maxSales).toFloat() else 0f
                            val isMax = day.sales == maxSales && maxSales > 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .weight(1f, fill = false)
                                        .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (isMax) PosPrimary else PosSoft)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(day.dayName, fontSize = 10.sp, color = PosMuted)
                            }
                        }
                    }
                }
            }
        }

        if (lowStockProducts.isNotEmpty()) {
            val productNames = lowStockProducts.take(3).joinToString(", ") { it.name }
            val suffix = if (lowStockProducts.size > 3) ", ..." else ""
            PosWarningBox("${lowStockProducts.size} produk stok menipis\n$productNames$suffix")
        }

        if (topProducts.isNotEmpty()) {
            PosCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Produk terlaris", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PosInk)
                    Spacer(Modifier.height(12.dp))
                    topProducts.forEachIndexed { index, product ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${index + 1}. ${product.name}", fontSize = 12.sp, color = PosInk, modifier = Modifier.weight(1f))
                            Text("${product.totalQty} terjual", fontSize = 12.sp, color = PosPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PosPrimaryButton(
                text = "Ekspor Excel",
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val fileName = "Laporan_Penjualan_$timestamp.xlsx"
                    try {
                        createExcelLauncher.launch(fileName)
                    } catch (_: android.content.ActivityNotFoundException) {
                        coroutineScope.launch {
                            try {
                                val path = withContext(Dispatchers.IO) {
                                    SafeFileSaver.saveToDownloadsFallback(
                                        context = context,
                                        fileName = fileName,
                                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    ) { stream ->
                                        ReportExporter.exportToExcel(currentTransactions, stream, title = "LAPORAN PENJUALAN $storeName")
                                    }
                                }
                                Toast.makeText(context, "Tersimpan di: $path", Toast.LENGTH_LONG).show()
                            } catch (err: Exception) {
                                Toast.makeText(context, "Gagal ekspor Excel: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            PosOutlineButton(
                text = "Ekspor PDF",
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val fileName = "Laporan_Keuangan_$timestamp.pdf"
                    try {
                        createPdfLauncher.launch(fileName)
                    } catch (_: android.content.ActivityNotFoundException) {
                        coroutineScope.launch {
                            try {
                                val path = withContext(Dispatchers.IO) {
                                    SafeFileSaver.saveToDownloadsFallback(
                                        context = context,
                                        fileName = fileName,
                                        mimeType = "application/pdf"
                                    ) { stream ->
                                        ReportExporter.exportToPdf(currentTransactions, stream, storeName = storeName)
                                    }
                                }
                                Toast.makeText(context, "Tersimpan di: $path", Toast.LENGTH_LONG).show()
                            } catch (err: Exception) {
                                Toast.makeText(context, "Gagal ekspor PDF: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun MonthYearBar(
    month: Int,
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    val monthText = "${monthNames.getOrElse(month - 1) { "" }} $year"

    PosCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Sebelumnya", tint = PosInk)
            }
            Text(monthText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PosInk)
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Berikutnya", tint = PosInk)
            }
        }
    }
}
