package com.android.posanof.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manager untuk keamanan dan autentikasi berbasis EncryptedSharedPreferences.
 * Secara default, aplikasi berada dalam 'Mode Kasir' tanpa login.
 * Memerlukan verifikasi PIN saat mengakses menu Admin / Master Data.
 */
@Suppress("DEPRECATION")
class AuthManager private constructor(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @Volatile
    private var isAdminAuthenticated: Boolean = false

    /**
     * Mengecek apakah PIN Admin sudah pernah dibuat/diatur sebelumnya.
     */
    fun isPinSet(): Boolean {
        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null)
        return !storedPin.isNullOrEmpty()
    }

    /**
     * Mengembalikan panjang PIN yang tersimpan.
     */
    fun getStoredPinLength(): Int {
        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null)
        return storedPin?.length ?: 4
    }

    /**
     * Menyimpan PIN Admin baru (harus 4-6 digit angka).
     * @param pin PIN baru yang akan disimpan.
     * @return Boolean true jika PIN valid (4-6 digit angka) dan berhasil disimpan.
     */
    fun savePin(pin: String): Boolean {
        if (!isValidPinFormat(pin)) {
            return false
        }
        sharedPreferences.edit {
            putString(KEY_ADMIN_PIN, pin)
        }
        return true
    }

    /**
     * Memverifikasi PIN input tanpa mengubah status sesi menjadi Admin.
     */
    fun verifyPinWithoutAdminElevation(inputPin: String): Boolean {
        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null) ?: return false
        return storedPin == inputPin
    }

    /**
     * Memverifikasi PIN input dengan PIN Admin yang tersimpan.
     * @param inputPin PIN yang dimasukkan pengguna.
     * @return Boolean true jika PIN cocok.
     */
    fun verifyPin(inputPin: String): Boolean {
        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null) ?: return false
        val isCorrect = storedPin == inputPin
        if (isCorrect) {
            isAdminAuthenticated = true
        }
        return isCorrect
    }

    /**
     * Mencoba login sebagai Admin dengan memasukkan PIN.
     */
    fun loginAdmin(inputPin: String): Boolean {
        return verifyPin(inputPin)
    }

    /**
     * Mengembalikan status sesi kembali ke Mode Kasir.
     */
    fun logoutAdmin() {
        isAdminAuthenticated = false
    }

    /**
     * Mengecek apakah sesi pengguna saat ini berada dalam Mode Admin yang terautentikasi.
     * Secara default bernilai false (Mode Kasir).
     */
    fun isAdminSessionActive(): Boolean {
        return isAdminAuthenticated
    }

    /**
     * Menghapus PIN tersimpan (misalnya untuk reset akun/debug).
     */
    fun clearPin() {
        sharedPreferences.edit {
            remove(KEY_ADMIN_PIN)
        }
        isAdminAuthenticated = false
    }

    /**
     * Validasi format PIN: harus 4 sampai 6 digit angka.
     */
    private fun isValidPinFormat(pin: String): Boolean {
        return (pin.length in 4..6) && pin.all { it.isDigit() }
    }

    companion object {
        private const val PREFS_FILENAME = "posanof_secure_prefs"
        private const val KEY_ADMIN_PIN = "key_admin_pin"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
