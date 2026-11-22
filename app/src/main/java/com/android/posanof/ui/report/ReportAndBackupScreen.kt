
package com.android.posanof.ui.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import com.android.posanof.ui.credit.CreditViewModel
import com.android.posanof.util.ReportExporter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ReportPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
}

@Composable
fun ReportAndBackupScreen(
    transactionRepository: TransactionRepository,
    productRepository: ProductRepository,
    backupRepository: BackupRepository,
    creditViewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currency = remember {
        NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("id-ID")
        ).apply {
            maximumFractionDigits = 0
        }
    }

    val transactions by transactionRepository
        .getAllTransactionsWithDetails()
        .collectAsState(initial = emptyList())

    val products by productRepository
        .getAllProducts()
        .collectAsState(initial = emptyList())

    val lowStockProducts by productRepository
        .getLowStockProducts()
        .collectAsState(initial = emptyList())

    val credits by creditViewModel.allCredits.collectAsState()

    var selectedPeriod by remember {
        mutableStateOf(ReportPeriod.WEEKLY)
    }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now

    when (selectedPeriod) {
        ReportPeriod.DAILY -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        ReportPeriod.WEEKLY -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        ReportPeriod.MONTHLY -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
    }

    val periodStart = calendar.timeInMillis

    val filteredTransactions = transactions.filter {
        it.transaction.date >= periodStart &&
                it.transaction.date <= now
    }

    val dailyTransactions = if (selectedPeriod == ReportPeriod.DAILY) {
        filteredTransactions.sortedByDescending {
            it.transaction.date
        }
    } else {
        emptyList()
    }

    val totalSales = filteredTransactions.sumOf {
        it.transaction.totalAmount
    }

    val transactionCount = filteredTransactions.size

    val productNames = products.associate {
        it.id to it.name
    }

    val bestSellers = filteredTransactions
        .flatMap { it.details }
        .groupBy { it.productId }
        .map { (id, details) ->
            Triple(
                id,
                productNames[id] ?: "Produk #$id",
                details.sumOf { it.quantity }
            )
        }
        .sortedByDescending { it.third }
        .take(3)

    val chartData = when (selectedPeriod) {
        ReportPeriod.DAILY -> {
            (0..5).map { index ->
                val startCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.HOUR_OF_DAY, index * 4)
                }

                val endCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.HOUR_OF_DAY, (index + 1) * 4)
                }

                val start = startCalendar.timeInMillis
                val end = endCalendar.timeInMillis

                val amount = filteredTransactions
                    .filter {
                        it.transaction.date >= start &&
                                it.transaction.date < end
                    }
                    .sumOf {
                        it.transaction.totalAmount
                    }

                String.format("%02d", index * 4) to amount
            }
        }

        ReportPeriod.WEEKLY -> {
            (0..6).map { index ->
                val startCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.DAY_OF_YEAR, index)
                }

                val endCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.DAY_OF_YEAR, index + 1)
                }

                val start = startCalendar.timeInMillis
                val end = endCalendar.timeInMillis

                val amount = filteredTransactions
                    .filter {
                        it.transaction.date >= start &&
                                it.transaction.date < end
                    }
                    .sumOf {
                        it.transaction.totalAmount
                    }

                val label = SimpleDateFormat(
                    "EE",
                    Locale.forLanguageTag("id-ID")
                ).format(Date(start))
                    .replaceFirstChar { it.uppercase() }

                label to amount
            }
        }

        ReportPeriod.MONTHLY -> {
            val daysInMonth = calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

            val bucketCount = (daysInMonth + 6) / 7

            (0 until bucketCount).map { index ->
                val startCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.DAY_OF_MONTH, index * 7)
                }

                val endCalendar = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    add(Calendar.DAY_OF_MONTH, (index + 1) * 7)
                }

                val start = startCalendar.timeInMillis
                val end = endCalendar.timeInMillis

                val amount = filteredTransactions
                    .filter {
                        it.transaction.date >= start &&
                                it.transaction.date < end
                    }
                    .sumOf {
                        it.transaction.totalAmount
                    }

                val firstDay = index * 7 + 1
                val lastDay = minOf(firstDay + 6, daysInMonth)

                "$firstDay-$lastDay" to amount
            }
        }
    }

    val chartTitle = when (selectedPeriod) {
        ReportPeriod.DAILY -> "Penjualan Hari Ini"
        ReportPeriod.WEEKLY -> "Penjualan Minggu Ini"
        ReportPeriod.MONTHLY -> "Penjualan Bulan Ini"
    }

    val maxChartValue = chartData
        .maxOfOrNull { it.second }
        ?.coerceAtLeast(1.0) ?: 1.0

    // EXPORT EXCEL
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        uri?.let { target ->
            scope.launch {
                try {
                    context.contentResolver
                        .openOutputStream(target)
                        ?.use {
                            ReportExporter.exportToExcel(transactions, it)
                        }

                    Toast.makeText(
                        context,
                        "Laporan Excel berhasil disimpan!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Gagal ekspor Excel: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // EXPORT PDF
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { target ->
            scope.launch {
                try {
                    context.contentResolver
                        .openOutputStream(target)
                        ?.use {
                            ReportExporter.exportToPdf(transactions, it)
                        }

                    Toast.makeText(
                        context,
                        "Laporan PDF berhasil disimpan!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Gagal ekspor PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = Color(0xFFDDF3EF),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "PIN terverifikasi",
                            color = Color(0xFF087F75),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }
            }

            // FILTER PERIODE
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE7ECEA))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReportPeriod.values().forEach { period ->
                        val selected = selectedPeriod == period

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = if (selected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(9.dp),
                            onClick = {
                                selectedPeriod = period
                            }
                        ) {
                            Text(
                                period.label,
                                modifier = Modifier.padding(vertical = 9.dp),
                                textAlign = TextAlign.Center,
                                color = if (selected) {
                                    Color(0xFF087F75)
                                } else {
                                    Color(0xFF53615F)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        }
                    }
                }
            }

            // RINGKASAN PENJUALAN DAN TRANSAKSI
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Penjualan",
                        value = currency.format(totalSales),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Transaksi",
                        value = transactionCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // GRAFIK PENJUALAN
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            chartTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            chartData.forEachIndexed { index, item ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    val barHeight = (
                                            item.second / maxChartValue * 70f
                                            ).toFloat().coerceAtLeast(5f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(barHeight.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 6.dp,
                                                    topEnd = 6.dp
                                                )
                                            )
                                            .background(
                                                if (index == chartData.lastIndex) {
                                                    Color(0xFF087F75)
                                                } else {
                                                    Color(0xFF9CCFC8)
                                                }
                                            )
                                    )

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        item.first,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF65716F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIWAYAT TRANSAKSI HARIAN
            if (selectedPeriod == ReportPeriod.DAILY) {
                item {
                    Column {
                        Text(
                            "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Daftar transaksi hari ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (dailyTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                "Belum ada transaksi hari ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    dailyTransactions.forEach { item ->
                        item {
                            val transaction = item.transaction

                            val transactionDate = SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                Locale.forLanguageTag("id-ID")
                            ).format(Date(transaction.date))

                            val credit = credits.firstOrNull {
                                it.transactionId == transaction.id
                            }

                            val isKasbon = transaction.paymentMethod
                                .contains("KASBON", ignoreCase = true)

                            val paymentLabel = if (isKasbon) {
                                "KASBON - ${
                                    credit?.customerName
                                        ?: "Nama pelanggan tidak ditemukan"
                                }"
                            } else {
                                transaction.paymentMethod
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Transaksi #${transaction.id}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF172522)
                                        )

                                        Text(
                                            transactionDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF65716F)
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    item.details.forEach { detail ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${
                                                    productNames[detail.productId]
                                                        ?: "Produk #${detail.productId}"
                                                } x${detail.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(Modifier.padding(horizontal = 4.dp))

                                            Text(
                                                currency.format(detail.subTotal),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF65716F)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "Metode pembayaran",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF65716F)
                                            )

                                            Text(
                                                paymentLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isKasbon) {
                                                    Color(0xFFB7791F)
                                                } else {
                                                    Color(0xFF172522)
                                                }
                                            )
                                        }

                                        Spacer(Modifier.padding(horizontal = 4.dp))

                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                "Total",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF65716F)
                                            )

                                            Text(
                                                currency.format(transaction.totalAmount),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF087F75)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PERINGATAN STOK MENIPIS
            item {
                if (lowStockProducts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF2C9))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚠",
                                color = Color(0xFF9A4C00),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.padding(horizontal = 3.dp))

                            Text(
                                "${lowStockProducts.size} produk stok menipis",
                                color = Color(0xFF9A4C00),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            lowStockProducts.take(3).joinToString(", ") { it.name },
                            color = Color(0xFF8B5A20),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // PRODUK TERLARIS
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Produk terlaris",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(7.dp))

                        if (bestSellers.isEmpty()) {
                            Text(
                                "Belum ada data penjualan produk.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            bestSellers.forEachIndexed { index, product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${index + 1}. ${product.second}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        "${product.third} terjual",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF087F75),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // EKSPOR EXCEL DAN PDF
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val stamp = SimpleDateFormat(
                                "yyyyMMdd_HHmm",
                                Locale.getDefault()
                            ).format(Date())

                            excelLauncher.launch(
                                "Laporan_Penjualan_$stamp.xlsx"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF087F75)
                        )
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null
                        )

                        Spacer(Modifier.padding(3.dp))

                        Text(
                            "Ekspor Excel",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val stamp = SimpleDateFormat(
                                "yyyyMMdd_HHmm",
                                Locale.getDefault()
                            ).format(Date())

                            pdfLauncher.launch(
                                "Laporan_Penjualan_$stamp.pdf"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null
                        )

                        Spacer(Modifier.padding(3.dp))

                        Text(
                            "Ekspor PDF",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF53615F)
            )

            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172522)
            )
        }
    }
}
