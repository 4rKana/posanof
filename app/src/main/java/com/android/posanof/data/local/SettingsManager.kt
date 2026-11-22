package com.android.posanof.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("posanof_settings", Context.MODE_PRIVATE)

    private val _storeName = MutableStateFlow(prefs.getString("storeName", "Toko UMKM POSANOF") ?: "Toko UMKM POSANOF")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _taxEnabled = MutableStateFlow(prefs.getBoolean("taxEnabled", false))
    val taxEnabled: StateFlow<Boolean> = _taxEnabled.asStateFlow()

    private val _defaultDiscountPercent = MutableStateFlow(prefs.getInt("defaultDiscountPercent", 0))
    val defaultDiscountPercent: StateFlow<Int> = _defaultDiscountPercent.asStateFlow()

    private val _defaultMinStock = MutableStateFlow(prefs.getInt("defaultMinStock", 10))
    val defaultMinStock: StateFlow<Int> = _defaultMinStock.asStateFlow()

    private val _cashierName = MutableStateFlow(prefs.getString("cashierName", "Kasir 1") ?: "Kasir 1")
    val cashierName: StateFlow<String> = _cashierName.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(prefs.getLong("lastBackupTimestamp", 0L))
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    fun updateStoreName(name: String) {
        prefs.edit().putString("storeName", name).apply()
        _storeName.value = name
    }

    fun updateTaxEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("taxEnabled", enabled).apply()
        _taxEnabled.value = enabled
    }

    fun updateDefaultDiscountPercent(percent: Int) {
        val validPercent = percent.coerceIn(0, 100)
        prefs.edit().putInt("defaultDiscountPercent", validPercent).apply()
        _defaultDiscountPercent.value = validPercent
    }

    fun updateDefaultMinStock(minStock: Int) {
        prefs.edit().putInt("defaultMinStock", minStock).apply()
        _defaultMinStock.value = minStock
    }

    fun updateCashierName(name: String) {
        prefs.edit().putString("cashierName", name).apply()
        _cashierName.value = name
    }

    fun updateLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong("lastBackupTimestamp", timestamp).apply()
        _lastBackupTimestamp.value = timestamp
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}