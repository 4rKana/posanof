package com.android.posanof.ui.credit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.posanof.data.local.entity.CreditEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Pengelolaan Piutang / Kasbon Pelanggan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditManagementScreen(
    viewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID")) }

    val unpaidCredits by viewModel.unpaidCredits.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val paymentResult by viewModel.paymentResult.collectAsState()

    var selectedCreditForPayment by remember { mutableStateOf<CreditEntity?>(null) }

    // Handle Payment Result Event
    when (val result = paymentResult) {
        is PaymentResult.Success -> {
            LaunchedEffect(result) {
                val statusText = if (result.isFullyPaid) "LUNAS!" else "Sebagian terbayar"
                Toast.makeText(
                    context,
                    "Pembayaran Kasbon ${result.customerName} berhasil ($statusText)",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetPaymentResult()
                selectedCreditForPayment = null
            }
        }

        is PaymentResult.Error -> {
            LaunchedEffect(result) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.resetPaymentResult()
            }
        }

        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Piutang Kasbon Pelanggan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
            // Field Pencarian Pelanggan
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Cari nama pelanggan atau no HP...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Total Piutang Belum Lunas
            val totalUnpaidAmount = remember(unpaidCredits) { unpaidCredits.sumOf { it.remainingDebt } }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Piutang Belum Lunas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${unpaidCredits.size} Transaksi Pelanggan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = currencyFormat.format(totalUnpaidAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List Kasbon Pelanggan
            if (unpaidCredits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "Tidak ada catatan piutang/kasbon belum lunas." else "Data pelanggan tidak ditemukan.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = unpaidCredits,
                        key = { it.id }
                    ) { credit ->
                        CreditItemCard(
                            credit = credit,
                            currencyFormat = currencyFormat,
                            dateFormatter = dateFormatter,
                            onPayClick = { selectedCreditForPayment = credit }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog Pembayaran Cicilan / Pelunasan Kasbon
    selectedCreditForPayment?.let { credit ->
        PayDebtDialog(
            credit = credit,
            currencyFormat = currencyFormat,
            onConfirmPayment = { amount ->
                viewModel.payDebt(credit.id, amount)
            },
            onDismiss = { selectedCreditForPayment = null }
        )
    }
}

@Composable
private fun CreditItemCard(
    credit: CreditEntity,
    currencyFormat: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onPayClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credit.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!credit.customerPhone.isNullOrEmpty()) {
                        Text(
                            text = "No. HP: ${credit.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Tanggal: ${dateFormatter.format(Date(credit.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(credit.remainingDebt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onPayClick) {
                        Text("Bayar / Lunas")
                    }
                }
            }
        }
    }
}

@Composable
private fun PayDebtDialog(
    credit: CreditEntity,
    currencyFormat: NumberFormat,
    onConfirmPayment: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Kasbon: ${credit.customerName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sisa Piutang: ${currencyFormat.format(credit.remainingDebt)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = paymentInput,
                    onValueChange = { paymentInput = it },
                    label = { Text("Nominal Pembayaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { paymentInput = credit.remainingDebt.toLong().toString() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Bayar Lunas Full (${currencyFormat.format(credit.remainingDebt)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = paymentInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirmPayment(amount)
                    }
                }
            ) {
                Text("Proses Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
