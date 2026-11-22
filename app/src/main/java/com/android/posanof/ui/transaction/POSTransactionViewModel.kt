
package com.android.posanof.ui.transaction

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity
import com.android.posanof.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Item keranjang belanja berisi produk dan kuantitasnya.
 */
data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
) {
    val subTotal: Double
        get() = product.price * quantity
}

/**
 * Metode pembayaran yang didukung.
 */
enum class PaymentMethod {
    TUNAI,
    KASBON
}

/**
 * State hasil eksekusi checkout/transaksi.
 */
sealed interface CheckoutResult {
    data object Idle : CheckoutResult
    data object Loading : CheckoutResult
    data class Success(
        val transactionId: Int,
        val changeAmount: Double
    ) : CheckoutResult

    data class Error(val message: String) : CheckoutResult
}

/**
 * ViewModel untuk mengelola alur Transaksi Penjualan (POS).
 */
class POSTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val adminPreferences: SharedPreferences
) : ViewModel() {

    // =========================================================================
    // STATEFLOW KERANJANG DAN KALKULASI
    // =========================================================================

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _subtotalAmount = MutableStateFlow(0.0)
    val subtotalAmount: StateFlow<Double> = _subtotalAmount.asStateFlow()

    private val _discountPercentage = MutableStateFlow(0.0)
    val discountPercentage: StateFlow<Double> = _discountPercentage.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _taxEnabled = MutableStateFlow(false)
    val taxEnabled: StateFlow<Boolean> = _taxEnabled.asStateFlow()

    private val _taxAmount = MutableStateFlow(0.0)
    val taxAmount: StateFlow<Double> = _taxAmount.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _cashAmount = MutableStateFlow(0.0)
    val cashAmount: StateFlow<Double> = _cashAmount.asStateFlow()

    private val _changeAmount = MutableStateFlow(0.0)
    val changeAmount: StateFlow<Double> = _changeAmount.asStateFlow()

    // =========================================================================
    // STATEFLOW PEMBAYARAN DAN PELANGGAN
    // =========================================================================

    private val _selectedPaymentMethod =
        MutableStateFlow(PaymentMethod.TUNAI)

    val selectedPaymentMethod: StateFlow<PaymentMethod> =
        _selectedPaymentMethod.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _cashierName = MutableStateFlow("Kasir 1")
    val cashierName: StateFlow<String> = _cashierName.asStateFlow()

    // =========================================================================
    // STATE RESULT CHECKOUT
    // =========================================================================

    private val _checkoutResult =
        MutableStateFlow<CheckoutResult>(CheckoutResult.Idle)

    val checkoutResult: StateFlow<CheckoutResult> =
        _checkoutResult.asStateFlow()

    init {
        refreshAdminSettings()
    }

    // =========================================================================
    // PENGATURAN ADMIN
    // =========================================================================

    /**
     * Membaca ulang pengaturan transaksi dari SharedPreferences Admin.
     */
    fun refreshAdminSettings() {
        val discount = adminPreferences
            .getString("default_discount", "0")
            ?.toDoubleOrNull()
            ?.coerceIn(0.0, 100.0)
            ?: 0.0

        val isTaxEnabled = adminPreferences
            .getBoolean("tax_enabled", false)

        _discountPercentage.value = discount
        _taxEnabled.value = isTaxEnabled

        recalculateTotals()
    }

    // =========================================================================
    // KELOLA KERANJANG BELANJA
    // =========================================================================

    /**
     * Menambahkan produk ke keranjang atau menambah quantity
     * jika produk sudah ada.
     */
    fun addToCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()

        val index = currentList.indexOfFirst {
            it.product.id == product.id
        }

        if (index >= 0) {
            val existingItem = currentList[index]
            val newQuantity = existingItem.quantity + 1

            if (newQuantity <= product.stock) {
                currentList[index] =
                    existingItem.copy(quantity = newQuantity)
            }
        } else {
            if (product.stock >= 1) {
                currentList.add(
                    CartItem(
                        product = product,
                        quantity = 1
                    )
                )
            }
        }

        _cartItems.value = currentList
        recalculateTotals()
    }

    /**
     * Memperbarui kuantitas item dalam keranjang.
     */
    fun updateQuantity(productId: Int, newQuantity: Int) {
        val currentList = _cartItems.value.toMutableList()

        val index = currentList.indexOfFirst {
            it.product.id == productId
        }

        if (index >= 0) {
            if (newQuantity <= 0) {
                currentList.removeAt(index)
            } else {
                val item = currentList[index]

                if (newQuantity <= item.product.stock) {
                    currentList[index] =
                        item.copy(quantity = newQuantity)
                }
            }

            _cartItems.value = currentList
            recalculateTotals()
        }
    }

    /**
     * Menghapus produk tertentu dari keranjang.
     */
    fun removeFromCart(productId: Int) {
        _cartItems.update { list ->
            list.filterNot {
                it.product.id == productId
            }
        }

        recalculateTotals()
    }

    /**
     * Mengosongkan seluruh keranjang belanja.
     */
    fun clearCart() {
        _cartItems.value = emptyList()
        recalculateTotals()
    }

    // =========================================================================
    // PEMBAYARAN DAN KALKULASI UANG
    // =========================================================================

    fun updateCashAmount(amount: Double) {
        _cashAmount.value = amount
        recalculateChange()
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun updateCustomerInfo(name: String, phone: String) {
        _customerName.value = name
        _customerPhone.value = phone
    }

    fun updateCashierName(name: String) {
        _cashierName.value = name
    }

    fun resetCheckoutResult() {
        _checkoutResult.value = CheckoutResult.Idle
    }

    /**
     * Menghitung subtotal, diskon, PPN, dan total akhir.
     *
     * Rumus:
     * Subtotal = jumlah seluruh subtotal produk
     * Diskon = Subtotal x Persentase Diskon
     * PPN = (Subtotal - Diskon) x 11%
     * Total = Subtotal - Diskon + PPN
     */
    private fun recalculateTotals() {
        val subtotal = _cartItems.value.sumOf {
            it.subTotal
        }

        val discountPercent = _discountPercentage.value

        val discount = subtotal * discountPercent / 100.0

        val subtotalAfterDiscount = subtotal - discount

        val tax = if (_taxEnabled.value) {
            subtotalAfterDiscount * 0.11
        } else {
            0.0
        }

        val total = subtotalAfterDiscount + tax

        _subtotalAmount.value = subtotal
        _discountAmount.value = discount
        _taxAmount.value = tax
        _totalAmount.value = total

        recalculateChange()
    }

    /**
     * Menghitung kembalian berdasarkan total akhir.
     */
    private fun recalculateChange() {
        val total = _totalAmount.value
        val cash = _cashAmount.value

        _changeAmount.value = if (cash >= total) {
            cash - total
        } else {
            0.0
        }
    }

    // =========================================================================
    // CHECKOUT TRANSAKSI
    // =========================================================================

    /**
     * Memproses checkout transaksi penjualan.
     *
     * TUNAI:
     * Menghitung kembalian berdasarkan total akhir.
     *
     * KASBON:
     * Membuat record piutang berdasarkan total akhir.
     *
     * Transaksi dan pengurangan stok tetap diproses
     * melalui repository.
     */
    fun checkout() {
        // Pastikan pengaturan Admin terbaru digunakan.
        refreshAdminSettings()

        val cart = _cartItems.value

        if (cart.isEmpty()) {
            _checkoutResult.value = CheckoutResult.Error(
                "Keranjang belanja kosong."
            )
            return
        }

        val total = _totalAmount.value
        val paymentMethod = _selectedPaymentMethod.value

        // Validasi pembayaran.
        if (paymentMethod == PaymentMethod.TUNAI) {
            val cash = _cashAmount.value

            if (cash < total) {
                _checkoutResult.value = CheckoutResult.Error(
                    "Uang tunai tidak mencukupi " +
                            "(Kurang Rp ${total - cash})."
                )
                return
            }
        } else if (paymentMethod == PaymentMethod.KASBON) {
            if (_customerName.value.isBlank()) {
                _checkoutResult.value = CheckoutResult.Error(
                    "Nama pelanggan wajib diisi untuk transaksi Kasbon."
                )
                return
            }
        }

        _checkoutResult.value = CheckoutResult.Loading

        viewModelScope.launch {
            val calculatedChange =
                if (paymentMethod == PaymentMethod.TUNAI) {
                    _cashAmount.value - total
                } else {
                    0.0
                }

            // 1. Header Transaction Entity.
            val transaction = TransactionEntity(
                date = System.currentTimeMillis(),
                totalAmount = total,
                paymentMethod = paymentMethod.name,
                cashAmount = if (
                    paymentMethod == PaymentMethod.TUNAI
                ) {
                    _cashAmount.value
                } else {
                    total
                },
                changeAmount = calculatedChange,
                cashierName = _cashierName.value.ifBlank {
                    "Kasir"
                }
            )

            // 2. Transaction Detail Entities.
            val details = cart.map { item ->
                TransactionDetailEntity(
                    transactionId = 0,
                    productId = item.product.id,
                    quantity = item.quantity,
                    priceAtPurchase = item.product.price,
                    subTotal = item.subTotal
                )
            }

            // 3. Record Credit Entity jika pembayaran KASBON.
            val credit = if (
                paymentMethod == PaymentMethod.KASBON
            ) {
                CreditEntity(
                    transactionId = 0,
                    customerName = _customerName.value.trim(),
                    customerPhone = _customerPhone.value.trim(),
                    remainingDebt = total,
                    status = "BELUM_LUNAS",
                    createdAt = System.currentTimeMillis()
                )
            } else {
                null
            }

            // 4. Eksekusi transaksi atomik di Room Database.
            val result = transactionRepository.executeSalesTransaction(
                transaction,
                details,
                credit
            )

            result.fold(
                onSuccess = { transactionId ->
                    clearCart()

                    _cashAmount.value = 0.0
                    _customerName.value = ""
                    _customerPhone.value = ""

                    _checkoutResult.value = CheckoutResult.Success(
                        transactionId = transactionId,
                        changeAmount = calculatedChange
                    )
                },
                onFailure = { throwable ->
                    _checkoutResult.value = CheckoutResult.Error(
                        throwable.message
                            ?: "Gagal memproses transaksi."
                    )
                }
            )
        }
    }
}
