package com.android.posanof.ui.transaction

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.ui.components.PosBackground
import com.android.posanof.ui.components.PosBadge
import com.android.posanof.ui.components.PosCard
import com.android.posanof.ui.components.PosInk
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosOutlineButton
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosPrimaryButton
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.components.PosWarnBg
import com.android.posanof.ui.components.PosWarnText
import com.android.posanof.ui.components.toRupiah

/** Lebar maksimum konten supaya tetap nyaman di tablet / layar lebar. */
private val ContentMaxWidth = 480.dp

/**
 * Layar Transaksi Berhasil (Figma layar 5).
 *
 * [rows] = pasangan (label, nilai) untuk kartu detail, sehingga layar yang sama
 * dipakai untuk transaksi Tunai maupun Kasbon.
 * [statusBadge] = lencana opsional di bawah total (mis. "Belum Lunas" untuk Kasbon).
 *
 * Konten bisa di-scroll, sedangkan tombol aksi selalu menempel di bawah,
 * jadi tetap terjangkau di HP kecil, mode landscape, atau ukuran huruf besar.
 */
@Composable
fun TransactionSuccessScreen(
    total: Long,
    rows: List<Pair<String, String>>,
    onSendWhatsApp: () -> Unit,
    onSavePdf: () -> Unit,
    onNewTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    statusBadge: String? = null
) {
    Column(modifier.fillMaxSize().background(PosBackground)) {
        // ---- Konten (scroll) ----
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                SuccessMark()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Transaksi Berhasil",
                    modifier = Modifier.semantics { heading() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosInk
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    total.toRupiah(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosPrimary,
                    textAlign = TextAlign.Center
                )
                if (statusBadge != null) {
                    Spacer(Modifier.height(8.dp))
                    PosBadge(statusBadge, PosWarnBg, PosWarnText)
                }
                Spacer(Modifier.height(20.dp))

                PosCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { (label, value) -> DetailLine(label, value) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Struk digital · tanpa kertas", fontSize = 12.sp, color = PosMuted)
                Spacer(Modifier.height(16.dp))
            }
        }

        // ---- Tombol aksi (menempel di bawah) ----
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PosPrimaryButton("Kirim Struk via WhatsApp", onSendWhatsApp)
                PosOutlineButton("Simpan sebagai PDF", onSavePdf)
                TextButton(onClick = onNewTransaction) {
                    Text("Transaksi Baru", color = PosPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Lingkaran centang dengan animasi muncul singkat. */
@Composable
private fun SuccessMark() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    Box(
        Modifier
            .size(88.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(PosSoft, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(56.dp).background(PosPrimary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = "Berhasil", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    // Satu baris dibaca TalkBack sebagai satu kalimat: "Metode, Tunai".
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top
    ) {
        Text(label, Modifier.weight(1f), fontSize = 12.sp, color = PosMuted)
        Text(
            value,
            Modifier.padding(start = 12.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PosInk,
            textAlign = TextAlign.End
        )
    }
}

// =====================================================================
// Struk WhatsApp
// =====================================================================

/**
 * Susun teks struk. [rows] urutan pertama dan kedua dianggap No. Transaksi dan Tanggal,
 * sisanya (metode, uang diterima, dst.) ditaruh setelah total.
 */
internal fun buildReceiptText(
    rows: List<Pair<String, String>>,
    itemsText: String,
    totalText: String
): String = buildString {
    appendLine("*STRUK PEMBELIAN*")
    rows.take(2).forEach { (label, value) -> appendLine("$label: $value") }
    if (itemsText.isNotBlank()) {
        appendLine()
        appendLine("Rincian:")
        appendLine(itemsText)
    }
    appendLine()
    appendLine("Total: $totalText")
    rows.drop(2).forEach { (label, value) -> appendLine("$label: $value") }
    appendLine()
    append("Terima kasih.")
}

/**
 * Buka WhatsApp dengan teks struk.
 * Kalau [phone] terisi (Kasbon), langsung ke chat nomor itu; kalau kosong (Tunai),
 * WhatsApp meminta pengguna memilih kontak.
 */
internal fun shareReceiptToWhatsApp(context: Context, phone: String, text: String) {
    val digits = phone.filter(Char::isDigit)
    val number = when {
        digits.isEmpty() -> ""
        digits.startsWith("0") -> "62" + digits.drop(1)
        digits.startsWith("62") -> digits
        digits.startsWith("8") -> "62$digits"
        else -> digits
    }
    val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(text)}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi yang bisa membuka WhatsApp.", Toast.LENGTH_SHORT).show()
    }
}

// =====================================================================
// PREVIEW
// =====================================================================
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TransactionSuccessTunaiPreview() {
    TransactionSuccessScreen(
        total = 31_500,
        rows = listOf(
            "No. Transaksi" to "#0126",
            "Tanggal" to "11 Okt 2026, 14.32",
            "Metode" to "Tunai",
            "Uang diterima" to "Rp 50.000",
            "Kembalian" to "Rp 18.500"
        ),
        onSendWhatsApp = {},
        onSavePdf = {},
        onNewTransaction = {}
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TransactionSuccessKasbonPreview() {
    TransactionSuccessScreen(
        total = 107_000,
        rows = listOf(
            "No. Transaksi" to "#0127",
            "Tanggal" to "11 Okt 2026, 14.40",
            "Metode" to "Kasbon",
            "Pelanggan" to "Bu Ratna",
            "Nominal hutang" to "Rp 107.000"
        ),
        statusBadge = "Belum Lunas",
        onSendWhatsApp = {},
        onSavePdf = {},
        onNewTransaction = {}
    )
}