package com.android.posanof.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.local.entity.TopProductSales
import com.android.posanof.data.local.relation.TransactionWithDetails
import com.android.posanof.data.repository.ProductRepository
import com.android.posanof.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardPeriod {
    HARIAN, MINGGUAN, BULANAN
}

data class DailySales(
    val dayName: String,
    val sales: Double
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DashboardPeriod.HARIAN)
    val selectedPeriod: StateFlow<DashboardPeriod> = _selectedPeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTransactions: StateFlow<List<TransactionWithDetails>> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getTimestampsForPeriod(period)
        transactionRepository.getTransactionsByDateRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalSales: StateFlow<Double> = currentTransactions.map { list ->
        list.sumOf { it.transaction.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val transactionCount: StateFlow<Int> = currentTransactions.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topProducts: StateFlow<List<TopProductSales>> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getTimestampsForPeriod(period)
        transactionRepository.getTopProducts(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = productRepository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val last7DaysSales: StateFlow<List<DailySales>> = _selectedPeriod.flatMapLatest { _ -> // update when period changes just in case, but really just last 7 days
        val (start, end) = getTimestampsForPeriod(DashboardPeriod.MINGGUAN)
        transactionRepository.getTransactionsByDateRange(start, end).map { transactions ->
            processLast7DaysSales(transactions)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updatePeriod(period: DashboardPeriod) {
        _selectedPeriod.value = period
    }

    private fun getTimestampsForPeriod(period: DashboardPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        
        when (period) {
            DashboardPeriod.HARIAN -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, end)
            }
            DashboardPeriod.MINGGUAN -> {
                // last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, end)
            }
            DashboardPeriod.BULANAN -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, end)
            }
        }
    }

    private fun processLast7DaysSales(transactions: List<TransactionWithDetails>): List<DailySales> {
        val format = SimpleDateFormat("EEE", Locale("id", "ID"))
        val calendar = Calendar.getInstance()
        
        val salesMap = mutableMapOf<String, Double>()
        // Initialize last 7 days with 0
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = format.format(cal.time).take(3)
            salesMap[dayName] = 0.0
        }
        
        transactions.forEach { t ->
            val dayName = format.format(Date(t.transaction.date)).take(3)
            if (salesMap.containsKey(dayName)) {
                salesMap[dayName] = (salesMap[dayName] ?: 0.0) + t.transaction.totalAmount
            }
        }
        
        return salesMap.map { DailySales(it.key, it.value) }
    }
}
