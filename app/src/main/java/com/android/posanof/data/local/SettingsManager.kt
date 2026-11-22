package com.android.posanof.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class QrisMode { DYNAMIC, STATIC }

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

    private val _requirePinOnLaunch = MutableStateFlow(prefs.getBoolean("requirePinOnLaunch", false))
    val requirePinOnLaunch: StateFlow<Boolean> = _requirePinOnLaunch.asStateFlow()

    private val initialQrisMode = try {
        QrisMode.valueOf(prefs.getString("qrisMode", QrisMode.DYNAMIC.name) ?: QrisMode.DYNAMIC.name)
    } catch (_: Exception) {
        QrisMode.DYNAMIC
    }
    private val _qrisMode = MutableStateFlow(initialQrisMode)
    val qrisMode: StateFlow<QrisMode> = _qrisMode.asStateFlow()

    private val _qrisRawPayload = MutableStateFlow(prefs.getString("qrisRawPayload", "") ?: "")
    val qrisRawPayload: StateFlow<String> = _qrisRawPayload.asStateFlow()

    private val _qrisStaticImagePath = MutableStateFlow(prefs.getString("qrisStaticImagePath", "") ?: "")
    val qrisStaticImagePath: StateFlow<String> = _qrisStaticImagePath.asStateFlow()

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

    fun updateRequirePinOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean("requirePinOnLaunch", enabled).apply()
        _requirePinOnLaunch.value = enabled
    }

    fun updateQrisMode(mode: QrisMode) {
        prefs.edit().putString("qrisMode", mode.name).apply()
        _qrisMode.value = mode
    }

    fun updateQrisRawPayload(payload: String) {
        prefs.edit().putString("qrisRawPayload", payload).apply()
        _qrisRawPayload.value = payload
    }

    fun clearQrisRawPayload() {
        prefs.edit().remove("qrisRawPayload").apply()
        _qrisRawPayload.value = ""
    }

    fun updateQrisStaticImagePath(path: String) {
        prefs.edit().putString("qrisStaticImagePath", path).apply()
        _qrisStaticImagePath.value = path
    }

    fun clearQrisStaticImage(context: Context) {
        val currentPath = _qrisStaticImagePath.value
        if (currentPath.isNotBlank()) {
            try {
                val file = java.io.File(currentPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
        prefs.edit().remove("qrisStaticImagePath").apply()
        _qrisStaticImagePath.value = ""
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