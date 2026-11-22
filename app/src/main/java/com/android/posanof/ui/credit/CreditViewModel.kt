
package com.android.posanof.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.repository.CreditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class CreditViewModel(
    private val creditRepository: CreditRepository,
) : ViewModel() {

    // Semua data piutang, termasuk yang sudah lunas
    val allCredits: StateFlow<List<CreditEntity>> =
        creditRepository.getAllCredits().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State Search Query & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Daftar kasbon
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

    // State hasil operasi pembayaran
    private val _paymentResult = MutableStateFlow<PaymentResult>(PaymentResult.Idle)
    val paymentResult: StateFlow<PaymentResult> = _paymentResult.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetPaymentResult() {
        _paymentResult.value = PaymentResult.Idle
    }

    fun payDebt(creditId: Int, paymentAmount: Double) {
        payDebt(creditId.toLong(), paymentAmount)
    }

    fun payDebt(creditId: Long, paymentAmount: Double) {
        if (paymentAmount <= 0) {
            _paymentResult.value = PaymentResult.Error(
                "Jumlah pembayaran harus lebih dari Rp 0."
            )
            return
        }

        _paymentResult.value = PaymentResult.Loading

        viewModelScope.launch {
            try {
                val credit = creditRepository.findCreditById(creditId.toInt())

                if (credit == null) {
                    _paymentResult.value = PaymentResult.Error(
                        "Data piutang tidak ditemukan."
                    )
                    return@launch
                }

                if (credit.status == "LUNAS") {
                    _paymentResult.value = PaymentResult.Error(
                        "Piutang ini sudah LUNAS."
                    )
                    return@launch
                }

                val newRemainingDebt = maxOf(
                    0.0,
                    credit.remainingDebt - paymentAmount
                )

                val newStatus = if (newRemainingDebt <= 0.0) {
                    "LUNAS"
                } else {
                    "BELUM_LUNAS"
                }

                val updatedCredit = credit.copy(
                    remainingDebt = newRemainingDebt,
                    status = newStatus
                )

                creditRepository.updateCredit(updatedCredit)

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
