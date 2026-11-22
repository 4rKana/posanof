
package com.android.posanof.ui.transaction

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.ui.components.PosBackground
import com.android.posanof.ui.components.PosBorder
import com.android.posanof.ui.components.PosCard
import com.android.posanof.ui.components.PosDanger
import com.android.posanof.ui.components.PosInk
import com.android.posanof.ui.components.PosMuted
import com.android.posanof.ui.components.PosPrimary
import com.android.posanof.ui.components.PosPrimaryButton
import com.android.posanof.ui.components.PosSegmented
import com.android.posanof.ui.components.PosSoft
import com.android.posanof.ui.components.PosTextField
import com.android.posanof.ui.components.PosTopBar
import com.android.posanof.ui.components.PosWarningBox
import com.android.posanof.ui.components.toRupiah

// =====================================================================
// ROUTE: MENGHUBUNGKAN CARTSCREEN DENGAN POSTRANSACTIONVIEWMODEL
// =====================================================================

@Composable
fun CartRoute(
    viewModel: POSTransactionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val items by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotalAmount.collectAsState()
    val discountPercentage by viewModel.discountPercentage.collectAsState()
    val discount by viewModel.discountAmount.collectAsState()
    val tax by viewModel.taxAmount.collectAsState()
    val taxEnabled by viewModel.taxEnabled.collectAsState()
    val total by viewModel.totalAmount.collectAsState()

    val cashAmount by viewModel.cashAmount.collectAsState()
    val changeAmount by viewModel.changeAmount.collectAsState()
    val method by viewModel.selectedPaymentMethod.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val checkoutResult by viewModel.checkoutResult.collectAsState()

    // Baca pengaturan Admin terbaru setiap kali halaman keranjang dibuka.
    LaunchedEffect(Unit) {
        viewModel.refreshAdminSettings()
    }

    // Snapshot data sebelum checkout karena ViewModel akan mengosongkan keranjang.
    var snapSubtotal by rememberSaveable { mutableStateOf(0.0) }
    var snapDiscount by rememberSaveable { mutableStateOf(0.0) }
    var snapDiscountPercentage by rememberSaveable { mutableStateOf(0.0) }
    var snapTax by rememberSaveable { mutableStateOf(0.0) }
    var snapTaxEnabled by rememberSaveable { mutableStateOf(false) }
    var snapTotal by rememberSaveable { mutableStateOf(0.0) }
    var snapCash by rememberSaveable { mutableStateOf(0.0) }
    var snapIsCash by rememberSaveable { mutableStateOf(true) }
    var snapName by rememberSaveable { mutableStateOf("") }
    var snapPhone by rememberSaveable { mutableStateOf("") }
    var snapTime by rememberSaveable { mutableStateOf(0L) }
    var snapItems by rememberSaveable { mutableStateOf("") }

    val result = checkoutResult

    if (result is CheckoutResult.Success) {
        val finish = {
            viewModel.resetCheckoutResult()
            onBack()
        }

        BackHandler(onBack = finish)

        val rows = mutableListOf<Pair<String, String>>()

        rows.add(
            "No. Transaksi" to
                    ("#" + result.transactionId.toString().padStart(4, '0'))
        )
        rows.add("Tanggal" to formatReceiptDate(snapTime))

        rows.add("Subtotal" to snapSubtotal.toLong().toRupiah())

        if (snapDiscount > 0.0) {
            rows.add(
                "Diskon (${snapDiscountPercentage.toLong()}%)" to
                        ("-" + snapDiscount.toLong().toRupiah())
            )
        }

        if (snapTaxEnabled) {
            rows.add("PPN (11%)" to snapTax.toLong().toRupiah())
        }

        rows.add("Total" to snapTotal.toLong().toRupiah())

        if (snapIsCash) {
            rows.add("Metode" to "Tunai")
            rows.add("Uang diterima" to snapCash.toLong().toRupiah())
            rows.add(
                "Kembalian" to result.changeAmount.toLong().toRupiah()
            )
        } else {
            rows.add("Metode" to "Kasbon")

            if (snapName.isNotBlank()) {
                rows.add("Pelanggan" to snapName)
            }

            rows.add("Nominal hutang" to snapTotal.toLong().toRupiah())
        }

        TransactionSuccessScreen(
            total = snapTotal.toLong(),
            rows = rows,
            statusBadge = if (snapIsCash) null else "Belum Lunas",
            onSendWhatsApp = {
                val text = buildReceiptText(
                    rows,
                    snapItems,
                    snapTotal.toLong().toRupiah()
                )

                shareReceiptToWhatsApp(context, snapPhone, text)
            },
            onSavePdf = {
                Toast.makeText(
                    context,
                    "Simpan PDF: segera hadir",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onNewTransaction = finish,
            modifier = modifier
        )
    } else {
        CartScreen(
            items = items,
            subtotal = subtotal,
            discountPercentage = discountPercentage,
            discount = discount,
            tax = tax,
            taxEnabled = taxEnabled,
            total = total,
            cashAmount = cashAmount,
            changeAmount = changeAmount,
            method = method,
            customerName = customerName,
            customerPhone = customerPhone,
            isProcessing = result is CheckoutResult.Loading,
            onBack = onBack,
            onQuantityChange = viewModel::updateQuantity,
            onClearCart = viewModel::clearCart,
            onMethodChange = viewModel::updatePaymentMethod,
            onCashChange = viewModel::updateCashAmount,
            onCustomerChange = viewModel::updateCustomerInfo,
            onCheckout = {
                snapSubtotal = subtotal
                snapDiscount = discount
                snapDiscountPercentage = discountPercentage
                snapTax = tax
                snapTaxEnabled = taxEnabled
                snapTotal = total
                snapCash = cashAmount
                snapIsCash = method == PaymentMethod.TUNAI
                snapName = customerName.trim()
                snapPhone = customerPhone.trim()
                snapTime = System.currentTimeMillis()

                snapItems = items.joinToString("\n") {
                    "${it.quantity}x ${it.product.name} - " +
                            "${it.subTotal.toLong().toRupiah()}"
                }

                viewModel.checkout()
            },
            modifier = modifier
        )
    }

    if (result is CheckoutResult.Error) {
        LaunchedEffect(result.message) {
            Toast.makeText(
                context,
                result.message,
                Toast.LENGTH_LONG
            ).show()

            viewModel.resetCheckoutResult()
        }
    }
}

private fun formatReceiptDate(millis: Long): String =
    SimpleDateFormat(
        "dd MMM yyyy, HH.mm",
        Locale.forLanguageTag("id-ID")
    ).format(Date(millis))

// =====================================================================
// KERANJANG: TUNAI / KASBON
// =====================================================================

@Composable
fun CartScreen(
    items: List<CartItem>,
    subtotal: Double,
    discountPercentage: Double,
    discount: Double,
    tax: Double,
    taxEnabled: Boolean,
    total: Double,
    cashAmount: Double,
    changeAmount: Double,
    method: PaymentMethod,
    customerName: String,
    customerPhone: String,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onQuantityChange: (productId: Int, newQuantity: Int) -> Unit,
    onClearCart: () -> Unit,
    onMethodChange: (PaymentMethod) -> Unit,
    onCashChange: (Double) -> Unit,
    onCustomerChange: (name: String, phone: String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCash = method == PaymentMethod.TUNAI
    val totalLong = total.toLong()
    val segmentIndex = if (isCash) 0 else 1

    val onSegmentSelect: (Int) -> Unit = {
        onMethodChange(
            if (it == 0) PaymentMethod.TUNAI
            else PaymentMethod.KASBON
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
    ) {
        PosTopBar(
            title = "Keranjang",
            onBack = onBack,
            trailing = {
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClearCart) {
                        Text(
                            "Kosongkan",
                            color = PosDanger,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )

        if (items.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        tint = PosMuted,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Keranjang masih kosong",
                        color = PosMuted,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = onBack) {
                        Text(
                            "Kembali ke Kasir",
                            color = PosPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCash) {
                    // ---------- TUNAI ----------

                    PosCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(horizontal = 12.dp)
                        ) {
                            items.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(color = PosBorder)
                                }

                                CartLineRow(item, onQuantityChange)
                            }
                        }
                    }

                    // RINCIAN PEMBAYARAN
                    PosCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryRow(
                                "Subtotal",
                                subtotal.toLong().toRupiah()
                            )

                            SummaryRow(
                                "Diskon (${discountPercentage.toLong()}%)",
                                if (discount > 0.0) {
                                    "-${discount.toLong().toRupiah()}"
                                } else {
                                    discount.toLong().toRupiah()
                                }
                            )

                            SummaryRow(
                                if (taxEnabled) "PPN (11%)" else "Pajak",
                                tax.toLong().toRupiah()
                            )

                            HorizontalDivider(
                                color = PosBorder,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            SummaryRow(
                                "Total",
                                totalLong.toRupiah(),
                                bold = true
                            )
                        }
                    }

                    PosSegmented(
                        listOf("Tunai", "Kasbon"),
                        segmentIndex,
                        onSegmentSelect
                    )

                    PosTextField(
                        label = "Uang diterima",
                        value = if (cashAmount == 0.0) {
                            ""
                        } else {
                            cashAmount.toLong().toString()
                        },
                        onValueChange = {
                            onCashChange(
                                it.filter(Char::isDigit)
                                    .toDoubleOrNull() ?: 0.0
                            )
                        },
                        placeholder = "Rp 0",
                        keyboardType = KeyboardType.Number
                    )

                    // UANG PAS + 2 NOMINAL TERDEKAT
                    val suggestions = listOf(
                        20_000L,
                        50_000L,
                        100_000L,
                        200_000L
                    )
                        .filter { it >= totalLong }
                        .take(2)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickCashChip(
                            "Uang pas",
                            Modifier.weight(1f)
                        ) {
                            onCashChange(total)
                        }

                        suggestions.forEach { nominal ->
                            QuickCashChip(
                                nominal.toRupiah().removePrefix("Rp "),
                                Modifier.weight(1f)
                            ) {
                                onCashChange(nominal.toDouble())
                            }
                        }
                    }

                    val shortfall = total - cashAmount
                    val isShort = cashAmount > 0.0 && shortfall > 0.0

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isShort) {
                                    Color(0xFFFDE8E6)
                                } else {
                                    PosSoft
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isShort) "Kurang" else "Kembalian",
                            modifier = Modifier.weight(1f),
                            color = PosInk,
                            fontSize = 13.sp
                        )

                        Text(
                            (if (isShort) shortfall else changeAmount)
                                .toLong()
                                .toRupiah(),
                            color = if (isShort) PosDanger else PosPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // ---------- KASBON ----------

                    PosCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${items.size} produk · " +
                                        "${items.sumOf { it.quantity }} item",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )

                            Text(
                                "Rincian tagihan",
                                fontSize = 11.sp,
                                color = PosMuted
                            )

                            HorizontalDivider(color = PosBorder)

                            SummaryRow(
                                "Subtotal",
                                subtotal.toLong().toRupiah()
                            )

                            SummaryRow(
                                "Diskon (${discountPercentage.toLong()}%)",
                                if (discount > 0.0) {
                                    "-${discount.toLong().toRupiah()}"
                                } else {
                                    discount.toLong().toRupiah()
                                }
                            )

                            SummaryRow(
                                if (taxEnabled) "PPN (11%)" else "Pajak",
                                tax.toLong().toRupiah()
                            )

                            HorizontalDivider(
                                color = PosBorder,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            SummaryRow(
                                "Total hutang",
                                totalLong.toRupiah(),
                                bold = true
                            )
                        }
                    }

                    PosSegmented(
                        listOf("Tunai", "Kasbon"),
                        segmentIndex,
                        onSegmentSelect
                    )

                    PosTextField(
                        label = "Nama pelanggan",
                        value = customerName,
                        onValueChange = {
                            onCustomerChange(it, customerPhone)
                        },
                        placeholder = "Contoh: Bu Ratna"
                    )

                    PosTextField(
                        label = "No. HP / WhatsApp (opsional)",
                        value = customerPhone,
                        onValueChange = {
                            onCustomerChange(customerName, it)
                        },
                        placeholder = "0812 3456 7890",
                        keyboardType = KeyboardType.Phone
                    )

                    ReadOnlyField(
                        "Nominal hutang",
                        totalLong.toRupiah()
                    )

                    PosWarningBox(
                        "Dicatat sebagai piutang pelanggan.\n" +
                                "Stok otomatis berkurang setelah disimpan."
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            Box(Modifier.padding(16.dp)) {
                if (isCash) {
                    PosPrimaryButton(
                        text = if (isProcessing) {
                            "Memproses..."
                        } else {
                            "Bayar ${totalLong.toRupiah()}"
                        },
                        onClick = onCheckout,
                        enabled = cashAmount >= total && !isProcessing
                    )
                } else {
                    PosPrimaryButton(
                        text = if (isProcessing) {
                            "Memproses..."
                        } else {
                            "Simpan Kasbon"
                        },
                        onClick = onCheckout,
                        enabled = customerName.isNotBlank() && !isProcessing
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    item: CartItem,
    onQuantityChange: (Int, Int) -> Unit
) {
    val product = item.product

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = PosInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${product.price.toLong().toRupiah()} / pcs",
                fontSize = 11.sp,
                color = PosMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        StepButton(
            Icons.Default.Remove,
            filled = false,
            enabled = true
        ) {
            onQuantityChange(product.id, item.quantity - 1)
        }

        Text(
            "${item.quantity}",
            Modifier.widthIn(min = 28.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )

        StepButton(
            Icons.Default.Add,
            filled = true,
            enabled = item.quantity < product.stock
        ) {
            onQuantityChange(product.id, item.quantity + 1)
        }

        Text(
            item.subTotal.toLong()
                .toRupiah()
                .removePrefix("Rp "),
            Modifier
                .widthIn(min = 64.dp)
                .padding(start = 6.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(7.dp)

    Box(
        Modifier
            .size(26.dp)
            .clip(shape)
            .background(if (filled) PosSoft else Color.White)
            .border(1.dp, PosBorder, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) {
                PosPrimary
            } else {
                PosMuted.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.weight(1f),
            fontSize = if (bold) 14.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) PosInk else PosMuted
        )

        Text(
            value,
            fontSize = if (bold) 14.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = PosInk
        )
    }
}

@Composable
private fun QuickCashChip(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, PosBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PosInk
        )
    }
}

@Composable
private fun ReadOnlyField(
    label: String,
    value: String
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 11.sp,
            color = PosPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PosSoft.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                value,
                fontSize = 14.sp,
                color = PosInk
            )
        }
    }
}

// =====================================================================
// PREVIEW
// =====================================================================

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CartScreenPreview() {
    val items = listOf(
        CartItem(
            ProductEntity(
                id = 1,
                name = "Teh Botol 450ml",
                price = 4_000.0,
                stock = 48
            ),
            2
        ),
        CartItem(
            ProductEntity(
                id = 2,
                name = "Mie Instan Goreng",
                price = 3_500.0,
                stock = 120
            ),
            5
        ),
        CartItem(
            ProductEntity(
                id = 3,
                name = "Kopi Sachet",
                price = 2_000.0,
                stock = 6
            ),
            3
        )
    )

    val subtotal = items.sumOf { it.subTotal }
    val discountPercentage = 10.0
    val discount = subtotal * discountPercentage / 100.0
    val tax = (subtotal - discount) * 0.11
    val total = subtotal - discount + tax

    var method by remember {
        mutableStateOf(PaymentMethod.TUNAI)
    }

    var cash by remember {
        mutableStateOf(50_000.0)
    }

    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    CartScreen(
        items = items,
        subtotal = subtotal,
        discountPercentage = discountPercentage,
        discount = discount,
        tax = tax,
        taxEnabled = true,
        total = total,
        cashAmount = cash,
        changeAmount = if (cash >= total) cash - total else 0.0,
        method = method,
        customerName = name,
        customerPhone = phone,
        isProcessing = false,
        onBack = {},
        onQuantityChange = { _, _ -> },
        onClearCart = {},
        onMethodChange = { method = it },
        onCashChange = { cash = it },
        onCustomerChange = { n, p ->
            name = n
            phone = p
        },
        onCheckout = {}
    )
}
