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
import kotlinx.coroutines.flow.combine
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

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTransactions: StateFlow<List<TransactionWithDetails>> = combine(_selectedPeriod, _selectedMonth, _selectedYear) { period, month, year ->
        Triple(period, month, year)
    }.flatMapLatest { (period, month, year) ->
        val (start, end) = getTimestampsForPeriod(period, month, year)
        transactionRepository.getTransactionsByDateRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalSales: StateFlow<Double> = currentTransactions.map { list ->
        list.sumOf { it.transaction.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val transactionCount: StateFlow<Int> = currentTransactions.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topProducts: StateFlow<List<TopProductSales>> = combine(_selectedPeriod, _selectedMonth, _selectedYear) { period, month, year ->
        Triple(period, month, year)
    }.flatMapLatest { (period, month, year) ->
        val (start, end) = getTimestampsForPeriod(period, month, year)
        transactionRepository.getTopProducts(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = productRepository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val last7DaysSales: StateFlow<List<DailySales>> = _selectedPeriod.flatMapLatest { _ -> // update when period changes just in case, but really just last 7 days
        val (start, end) = getTimestampsForPeriod(DashboardPeriod.MINGGUAN, _selectedMonth.value, _selectedYear.value)
        transactionRepository.getTransactionsByDateRange(start, end).map { transactions ->
            processLast7DaysSales(transactions)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updatePeriod(period: DashboardPeriod) {
        _selectedPeriod.value = period
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun updateMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month.coerceIn(1, 12)
        _selectedYear.value = year
    }

    private fun getTimestampsForPeriod(period: DashboardPeriod, month: Int, year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        when (period) {
            DashboardPeriod.HARIAN -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                
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
                
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                
                return Pair(start, end)
            }
            DashboardPeriod.BULANAN -> {
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                
                val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, lastDay)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                
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
