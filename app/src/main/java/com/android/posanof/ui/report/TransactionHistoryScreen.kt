package com.android.posanof.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.relation.TransactionWithDetails
import com.android.posanof.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionHistoryScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val period by viewModel.selectedPeriod.collectAsState()
    val transactions by viewModel.currentTransactions.collectAsState()

    val format = SimpleDateFormat("HH.mm", Locale("id", "ID"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
    ) {
        PosTopBar(title = "Riwayat Transaksi", onBack = onBack)

        val options = listOf("Hari ini", "Minggu ini", "Bulan ini")
        Box(modifier = Modifier.padding(16.dp)) {
            PosSegmented(
                options = options,
                selectedIndex = period.ordinal,
                onSelect = { viewModel.updatePeriod(DashboardPeriod.entries[it]) }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.transaction.id }) { t ->
                val trans = t.transaction
                val dateStr = format.format(Date(trans.date))
                
                PosCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Transaksi #${trans.id.toString().padStart(4, '0')}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PosInk
                            )
                            Spacer(Modifier.height(4.dp))
                            val methodPart = if (trans.paymentMethod.equals("KASBON", ignoreCase = true)) {
                                "Kasbon"
                            } else {
                                "Tunai"
                            }
                            Text(
                                text = "$dateStr · $methodPart",
                                fontSize = 12.sp,
                                color = PosMuted
                            )
                        }
                        Text(
                            trans.totalAmount.toLong().toRupiah(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosInk
                        )
                    }
                }
            }
        }
    }
}
