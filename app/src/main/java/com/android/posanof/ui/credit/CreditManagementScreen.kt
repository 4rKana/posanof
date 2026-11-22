package com.android.posanof.ui.credit

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

// Enum untuk filter status kasbon sesuai Figma
enum class CreditFilterTab {
    SEMUA, BELUM_LUNAS, LUNAS
}

@Composable
fun CreditManagementScreen(
    viewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val credits by viewModel.unpaidCredits.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCreditId by remember { mutableStateOf<Int?>(null) }
    var selectedTab by remember { mutableStateOf(CreditFilterTab.SEMUA) }

    val selectedCredit = remember(selectedCreditId, credits) { credits.find { it.id == selectedCreditId } }

    selectedCredit?.let { credit ->
        CreditDetailScreen(
            credit = credit,
            viewModel = viewModel,
            onBackClick = { selectedCreditId = null },
            modifier = modifier
        )
    } ?: KasbonListContent(
        credits = credits,
        searchQuery = searchQuery,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onSearchChange = viewModel::setSearchQuery,
        onCreditClick = { selectedCreditId = it.id },
        modifier = modifier
    )
}

@Composable
fun KasbonListContent(
    credits: List<CreditEntity>,
    searchQuery: String,
    selectedTab: CreditFilterTab,
    onTabSelected: (CreditFilterTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onCreditClick: (CreditEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter data berdasarkan Tab yang dipilih dan Search Query
    val filteredCredits = remember(credits, searchQuery, selectedTab) {
        credits.filter { credit ->
            val matchesSearch = credit.customerName.contains(searchQuery, ignoreCase = true) ||
                    (credit.customerPhone?.contains(searchQuery, ignoreCase = true) == true)
            val matchesTab = when (selectedTab) {
                CreditFilterTab.SEMUA -> true
                CreditFilterTab.BELUM_LUNAS -> credit.remainingDebt > 0
                CreditFilterTab.LUNAS -> credit.remainingDebt <= 0
            }
            matchesSearch && matchesTab
        }
    }

    val total = remember(credits) { credits.sumOf { it.remainingDebt } }
    val customerCount = remember(credits) {
        credits.filter { it.remainingDebt > 0.0 && !it.status.equals("LUNAS", ignoreCase = true) }.map { it.customerName.trim().lowercase() }.distinct().size
    }
    val isSearching = searchQuery.isNotBlank()

    Column(modifier.fillMaxSize().background(PosBackground)) {
        // ---- Header ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Kasbon",
                modifier = Modifier.weight(1f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PosInk
            )
            PosBadge("$customerCount pelanggan")
        }

        // ---- Total piutang ----
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PosPrimary)
                .padding(16.dp)
        ) {
            Text(
                if (isSearching) "Total hasil pencarian" else "Total piutang aktif",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                total.toLong().toRupiah(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---- Filter Tabs (Sesuai Figma: Semua, Belum Lunas, Lunas) ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                text = "Semua",
                selected = selectedTab == CreditFilterTab.SEMUA,
                onClick = { onTabSelected(CreditFilterTab.SEMUA) }
            )
            FilterChipItem(
                text = "Belum Lunas",
                selected = selectedTab == CreditFilterTab.BELUM_LUNAS,
                onClick = { onTabSelected(CreditFilterTab.BELUM_LUNAS) }
            )
            FilterChipItem(
                text = "Lunas",
                selected = selectedTab == CreditFilterTab.LUNAS,
                onClick = { onTabSelected(CreditFilterTab.LUNAS) }
            )
        }

        Spacer(Modifier.height(12.dp))
        KasbonSearchBar(searchQuery, onSearchChange, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))

        // ---- Daftar ----
        if (filteredCredits.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.MoneyOff, null, tint = PosMuted, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (isSearching) "Data pelanggan tidak ditemukan."
                        else "Tidak ada data kasbon pada filter ini.",
                        color = PosMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCredits, key = { it.id }) { credit ->
                    CreditRow(credit) { onCreditClick(credit) }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        color = if (selected) PosPrimary else Color.White,
        border = BorderStroke(1.dp, if (selected) PosPrimary else PosBorder),
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else PosInk
        )
    }
}

@Composable
private fun CreditRow(credit: CreditEntity, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val isPaidOff = credit.remainingDebt <= 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                onClickLabel = "Terima pembayaran ${credit.customerName}",
                onClick = onClick
            ),
        shape = shape,
        color = Color.White,
        border = BorderStroke(1.dp, PosBorder)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    credit.customerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PosInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    maskPhone(credit.customerPhone),
                    fontSize = 11.sp,
                    color = PosMuted,
                    maxLines = 1
                )
                Text(
                    formatKasbonDate(credit.createdAt),
                    fontSize = 11.sp,
                    color = PosMuted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (isPaidOff) {
                    // Badge hijau untuk lunas ala Figma
                    PosBadge("Lunas", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                } else {
                    PosBadge("Belum Lunas", PosWarnBg, PosWarnText)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    credit.remainingDebt.toLong().toRupiah(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PosInk,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun KasbonSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = PosMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = PosInk),
            cursorBrush = SolidColor(PosPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Cari nama atau no. HP", color = PosMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Hapus pencarian",
                tint = PosMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun PayDebtDialog(
    credit: CreditEntity,
    isProcessing: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val remaining = credit.remainingDebt
    val amount = input.toDoubleOrNull() ?: 0.0
    val exceeds = amount > remaining
    val canSubmit = amount > 0.0 && !exceeds && !isProcessing

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Terima Pembayaran", fontWeight = FontWeight.Bold, color = PosInk) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    credit.customerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = PosInk
                )
                Text(
                    "Kasbon sejak ${formatKasbonDate(credit.createdAt)}",
                    fontSize = 11.sp,
                    color = PosMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PosSoft.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sisa hutang", Modifier.weight(1f), fontSize = 13.sp, color = PosInk)
                    Text(
                        remaining.toLong().toRupiah(),
                        fontWeight = FontWeight.Bold,
                        color = PosPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
                PosTextField(
                    label = "Nominal pembayaran",
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit) },
                    placeholder = "Rp 0",
                    keyboardType = KeyboardType.Number
                )
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount) }, enabled = canSubmit) {
                Text(
                    if (isProcessing) "Memproses..." else "Terima Pembayaran",
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSubmit) PosPrimary else PosMuted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = PosMuted) }
        }
    )
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun KasbonListPreview() {
    val now = System.currentTimeMillis()
    val sample = listOf(
        CreditEntity(
            id = 1, transactionId = 1, customerName = "Bu Ratna", customerPhone = "081234563456",
            remainingDebt = 250_000.0, status = "BELUM_LUNAS", createdAt = now
        ),
        CreditEntity(
            id = 2, transactionId = 2, customerName = "Pak Dedi", customerPhone = "085798769021",
            remainingDebt = 135_000.0, status = "BELUM_LUNAS", createdAt = now
        ),
        CreditEntity(
            id = 3, transactionId = 3, customerName = "Teh Sari", customerPhone = "",
            remainingDebt = 100_000.0, status = "BELUM_LUNAS", createdAt = now
        ),
        CreditEntity(
            id = 4, transactionId = 4, customerName = "Pak Omar", customerPhone = "",
            remainingDebt = 0.0, status = "LUNAS", createdAt = now
        )
    )
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(CreditFilterTab.SEMUA) }

    KasbonListContent(
        credits = sample,
        searchQuery = query,
        selectedTab = tab,
        onTabSelected = { tab = it },
        onSearchChange = { query = it },
        onCreditClick = {}
    )
}