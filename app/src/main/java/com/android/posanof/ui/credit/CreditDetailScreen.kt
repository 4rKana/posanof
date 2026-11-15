package com.android.posanof.ui.credit

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.ui.components.PosBackground
import com.android.posanof.ui.components.PosBadge
import com.android.posanof.ui.components.PosBorder
import com.android.posanof.ui.components.PosDanger
import com.android.posanof.ui.components.PosInk
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.components.PosTextField
import com.android.posanof.ui.components.PosWarnBg
import com.android.posanof.ui.components.PosWarnText
import com.android.posanof.ui.components.toRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreditDetailScreen(
    credit: CreditEntity,
    viewModel: CreditViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val paymentResult by viewModel.paymentResult.collectAsState()
    var input by remember { mutableStateOf("") }

    var rawInput by remember { mutableStateOf("") }
    val displayInput = remember(rawInput) {
        if (rawInput.isBlank()) "" else rawInput.toLongOrNull()?.toRupiah() ?: rawInput
    }

    val remaining = credit.remainingDebt
    val amount = rawInput.toDoubleOrNull() ?: 0.0
    val exceeds = amount > remaining
    val canSubmit = amount > 0.0 && amount <= remaining && paymentResult !is PaymentResult.Loading
    val isPaidOff = remaining <= 0.0 || credit.status.equals("LUNAS", ignoreCase = true)

    // Handle Toast Hasil Pembayaran
    when (val result = paymentResult) {
        is PaymentResult.Success -> {
            LaunchedEffect(result) {
                Toast.makeText(context, "Pembayaran berhasil dicatat!", Toast.LENGTH_SHORT).show()
                viewModel.resetPaymentResult()
                input = ""
            }
        }
        is PaymentResult.Error -> {
            LaunchedEffect(result) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.resetPaymentResult()
            }
        }
        else -> Unit
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Top App Bar (Header) ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = PosInk)
            }
            Text(
                text = credit.customerName,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PosInk
            )
            if (isPaidOff) {
                PosBadge("Lunas", Color(0xFFE8F5E9), Color(0xFF2E7D32))
            } else {
                PosBadge("Belum Lunas", PosWarnBg, PosWarnText)
            }
        }

        Column(Modifier.padding(16.dp)) {
            // ---- Kartu Sisa Hutang & No HP ----
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PosBorder)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sisa hutang", fontSize = 13.sp, color = PosMuted)
                        Text(
                            text = if (!credit.customerPhone.isNullOrBlank()) maskPhone(credit.customerPhone) else "Tanpa nomor HP",
                            fontSize = 13.sp,
                            color = PosMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = remaining.toLong().toRupiah(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosInk
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Riwayat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PosInk)
            Spacer(Modifier.height(10.dp))

            // ---- Riwayat Transaksi / Kasbon ----
            HistoryItem(
                title = "Kasbon baru",
                date = formatKasbonDate(credit.createdAt),
                amount = "+ " + credit.remainingDebt.toLong().toRupiah(),
                isDebit = true
            )

            Spacer(Modifier.height(20.dp))

            // ---- Form Nominal Pembayaran ----
            if (!isPaidOff) {
                Spacer(Modifier.height(16.dp))

                // Gunakan Column dengan spacing yang kecil agar teks label rapat ke kotak inputnya
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nominal pembayaran",
                        fontSize = 13.sp,
                        color = PosMuted,
                        fontWeight = FontWeight.Medium
                    )

                    PosTextField(
                        label = "",
                        value = rawInput,
                        onValueChange = { inputStr ->
                            rawInput = inputStr.filter(Char::isDigit)
                        },
                        placeholder = "Rp 0",
                        keyboardType = KeyboardType.Number
                    )
                }

                if (exceeds) {
                    Spacer(Modifier.height(4.dp))
                    Text("Melebihi sisa hutang.", color = PosDanger, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = { input = remaining.toLong().toString() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Lunasi (${remaining.toLong().toRupiah()})", color = PosPrimary)
                }

                Spacer(Modifier.height(12.dp))

                // ---- Tombol Terima Pembayaran ----
                Button(
                    onClick = { viewModel.payDebt(credit.id, amount) },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PosPrimary,
                        disabledContainerColor = PosPrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (paymentResult is PaymentResult.Loading) "Memproses..." else "Terima Pembayaran",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- Tombol Ingatkan via WhatsApp ----
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PosPrimary),
                onClick = {
                    val phone = credit.customerPhone.orEmpty().filter(Char::isDigit)
                    if (phone.isBlank()) {
                        Toast.makeText(context, "Pelanggan tidak memiliki nomor HP.", Toast.LENGTH_SHORT).show()
                    } else {
                        val formattedPhone = if (phone.startsWith("0")) "62" + phone.drop(1) else phone
                        val message = "Halo ${credit.customerName}, mengingatkan sisa kasbon Anda sebesar ${remaining.toLong().toRupiah()}. Terima kasih."
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(message)}"))
                        context.startActivity(intent)
                    }
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ingatkan via WhatsApp",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PosPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(title: String, date: String, amount: String, isDebit: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isDebit) PosWarnBg else Color(0xFFE8F5E9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isDebit) PosWarnText else Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PosInk)
                Text(date, fontSize = 11.sp, color = PosMuted)
            }
            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDebit) PosDanger else Color(0xFF2E7D32)
            )
        }
    }
}

private fun maskPhone(phone: String?): String {
    val digits = phone.orEmpty().filter(Char::isDigit)
    return when {
        digits.isEmpty() -> "Tanpa nomor HP"
        digits.length >= 8 -> "${digits.take(4)} •••• ${digits.takeLast(4)}"
        else -> digits
    }
}

private fun formatKasbonDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(millis))