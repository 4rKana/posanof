package com.android.posanof.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.ui.components.*

/** Model UI khusus layar ini. Mapping dari entity Room kamu di ViewModel. */
data class KasirProduct(
    val id: Long,
    val name: String,
    val price: Long,
    val stock: Int,
    /** Stok <= nilai ini ditampilkan sebagai badge kuning (isi dari minStock produk). */
    val lowStockAt: Int = LOW_STOCK_THRESHOLD
)

/** Stok <= angka ini ditampilkan sebagai badge kuning. */
private const val LOW_STOCK_THRESHOLD = 10

private val CartBarColor = Color(0xFF10201D)

@Composable
fun KasirScreen(
    products: List<KasirProduct>,
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    cartProductCount: Int,
    cartItemCount: Int,
    cartTotal: Long,
    cartQuantities: Map<Long, Int>,
    onProductClick: (KasirProduct) -> Unit,
    onDecreaseClick: (KasirProduct) -> Unit,
    onScanClick: () -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(PosBackground)) {
        Column(Modifier.fillMaxSize()) {

            // ---- Header ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kasir",
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosInk
                )
                PosBadge("Mode Kasir")
            }

            // ---- Search + tombol scan ----
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(query, onQueryChange, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PosPrimary)
                        .clickable(onClick = onScanClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Scan barcode", tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))
            PosFilterChips(categories, selectedCategory, onCategoryChange)
            Spacer(Modifier.height(12.dp))

            // ---- Grid produk ----
            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Produk tidak ditemukan", color = PosMuted, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 2.dp,
                        // ruang supaya item terakhir tidak tertutup bar keranjang
                        bottom = if (cartItemCount > 0) 88.dp else 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { p ->
                        val cartQty = cartQuantities[p.id] ?: 0
                        ProductCard(
                            product = p,
                            cartQty = cartQty,
                            onAdd = { onProductClick(p) },
                            onDecrease = { onDecreaseClick(p) }
                        )
                    }
                }
            }
        }

        // ---- Bar keranjang (mengambang di bawah) ----
        if (cartItemCount > 0) {
            CartBar(
                productCount = cartProductCount,
                itemCount = cartItemCount,
                total = cartTotal,
                onClick = onCartClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
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
                        Text("Cari nama produk", color = PosMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: KasirProduct,
    cartQty: Int,
    onAdd: () -> Unit,
    onDecrease: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val hasItem = cartQty > 0
    val borderStroke = if (hasItem) BorderStroke(1.5.dp, PosPrimary) else BorderStroke(1.dp, PosBorder)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onAdd),
        shape = shape,
        color = Color.White,
        border = borderStroke
    ) {
        Column(Modifier.padding(10.dp)) {
            // placeholder gambar produk (ganti dengan AsyncImage kalau sudah ada foto)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(PosSoft.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PosInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    product.price.toRupiah(),
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosPrimary,
                    maxLines = 1
                )
                if (!hasItem) {
                    if (product.stock <= product.lowStockAt && product.stock > 0) {
                        PosBadge("Stok ${product.stock}", PosWarnBg, PosWarnText)
                    } else if (product.stock > product.lowStockAt) {
                        Text("Stok ${product.stock}", fontSize = 10.sp, color = PosMuted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Kontrol Kuantitas (Height 30.dp)
            if (hasItem) {
                val canAddMore = cartQty < product.stock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, PosBorder, RoundedCornerShape(8.dp))
                            .clickable(onClick = onDecrease),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Kurang",
                            tint = PosPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$cartQty",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosInk,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canAddMore) PosPrimary else PosPrimary.copy(alpha = 0.4f))
                            .clickable(enabled = canAddMore, onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                val isAvailable = product.stock > 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAvailable) PosSoft else PosBorder)
                        .clickable(enabled = isAvailable, onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAvailable) "+ Tambah" else "Stok Habis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) PosPrimary else PosMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun CartBar(
    productCount: Int,
    itemCount: Int,
    total: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CartBarColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$productCount produk · $itemCount item",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(total.toRupiah(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// =====================================================================
// PREVIEW (data sama persis dengan Figma)
// =====================================================================
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun KasirScreenPreview() {
    val all = listOf(
        KasirProduct(1, "Teh Botol 450ml", 4_000, 48),
        KasirProduct(2, "Mie Instan Goreng", 3_500, 120),
        KasirProduct(3, "Kopi Sachet", 2_000, 6),
        KasirProduct(4, "Air Mineral 600ml", 3_000, 64),
        KasirProduct(5, "Gula Pasir 1 kg", 16_000, 22),
        KasirProduct(6, "Minyak Goreng 1 L", 18_500, 3),
        KasirProduct(7, "Telur 1 kg", 28_000, 15),
        KasirProduct(8, "Roti Tawar", 14_500, 9)
    )
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }

    Scaffold(
        bottomBar = { PosBottomBar(currentRoute = "pos", onNavigate = {}) }
    ) { pad ->
        KasirScreen(
            products = all.filter { it.name.contains(query, ignoreCase = true) },
            query = query,
            onQueryChange = { query = it },
            categories = listOf("Semua", "Minuman", "Makanan", "Sembako"),
            selectedCategory = category,
            onCategoryChange = { category = it },
            cartProductCount = 3,
            cartItemCount = 10,
            cartTotal = 31_500,
            cartQuantities = mapOf(1L to 2, 2L to 5, 3L to 3),
            onProductClick = {},
            onDecreaseClick = {},
            onScanClick = {},
            onCartClick = {},
            modifier = Modifier.padding(pad)
        )
    }
}
