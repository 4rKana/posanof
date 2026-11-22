package com.android.posanof.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.CreditHistoryEntity
import com.android.posanof.data.repository.CreditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State hasil operasi pembayaran piutang/kasbon.
 */
sealed interface PaymentResult {
    data object Idle : PaymentResult
    data object Loading : PaymentResult
    data class Success(
        val customerName: String,
        val paidAmount: Double,
        val isFullyPaid: Boolean,
    ) : PaymentResult
    data class Error(val message: String) : PaymentResult
}

/**
 * ViewModel untuk modul Piutang/Kasbon Pelanggan.
 * Mengelola daftar piutang 'BELUM_LUNAS' dan alur pengurangan piutang/pelunasan.
 */
class CreditViewModel(
    private val creditRepository: CreditRepository,
) : ViewModel() {

    // --- State Search Query & Filter ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- State Flow Daftar Kasbon 'BELUM_LUNAS' ---
    val unpaidCredits: StateFlow<List<CreditEntity>> = combine(
        creditRepository.getAllCredits(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                        (it.customerPhone?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- State Results Operations ---
    private val _paymentResult = MutableStateFlow<PaymentResult>(PaymentResult.Idle)
    val paymentResult: StateFlow<PaymentResult> = _paymentResult.asStateFlow()

    fun getCreditHistories(creditId: Int) = creditRepository.getCreditHistoriesByCreditId(creditId)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetPaymentResult() {
        _paymentResult.value = PaymentResult.Idle
    }

    /**
     * Overload fungsi payDebt dengan ID bertipe Int.
     */
    fun payDebt(creditId: Int, paymentAmount: Double) {
        payDebt(creditId.toLong(), paymentAmount)
    }

    /**
     * Fungsi utama pembayaran piutang kasbon.
     * Mengurangi remaining_debt sesuai paymentAmount.
     * Jika remaining_debt mencapai 0, status otomatis diubah menjadi 'LUNAS'.
     *
     * @param creditId ID record piutang.
     * @param paymentAmount Jumlah pembayaran yang disetorkan.
     */
    fun payDebt(creditId: Long, paymentAmount: Double) {
        if (paymentAmount <= 0) {
            _paymentResult.value = PaymentResult.Error("Jumlah pembayaran harus lebih dari Rp 0.")
            return
        }

        _paymentResult.value = PaymentResult.Loading

        viewModelScope.launch {
            try {
                val credit = creditRepository.findCreditById(creditId.toInt())
                if (credit == null) {
                    _paymentResult.value = PaymentResult.Error("Data piutang tidak ditemukan.")
                    return@launch
                }

                if (credit.status == "LUNAS") {
                    _paymentResult.value = PaymentResult.Error("Piutang ini sudah LUNAS.")
                    return@launch
                }

                // Kalkulasi sisa piutang baru
                val newRemainingDebt = maxOf(0.0, credit.remainingDebt - paymentAmount)

                // Jika sisa piutang 0, ubah status menjadi 'LUNAS' secara otomatis
                val newStatus = if (newRemainingDebt <= 0.0) "LUNAS" else "BELUM_LUNAS"

                val updatedCredit = credit.copy(
                    remainingDebt = newRemainingDebt,
                    status = newStatus
                )

                androidx.room.RoomDatabase::class // just to let repository do transaction or we can just call repo methods in sequence (repo updateCredit and insertCreditHistory)
                creditRepository.updateCredit(updatedCredit)
                creditRepository.insertCreditHistory(
                    CreditHistoryEntity(
                        creditId = creditId.toInt(),
                        type = "PAYMENT",
                        amount = paymentAmount
                    )
                )

                _paymentResult.value = PaymentResult.Success(
                    customerName = credit.customerName,
                    paidAmount = paymentAmount,
                    isFullyPaid = (newStatus == "LUNAS")
                )
            } catch (e: Exception) {
                _paymentResult.value = PaymentResult.Error(
                    e.message ?: "Gagal memproses pembayaran piutang."
                )
            }
        }
    }
}
