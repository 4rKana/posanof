
package com.android.posanof.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manager autentikasi Admin Posanof.
 * PIN Admin wajib tepat 6 digit angka.
 */
@Suppress("DEPRECATION")
class AuthManager private constructor(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences =
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    @Volatile
    private var isAdminAuthenticated: Boolean = false

    /**
     * Mengecek apakah PIN Admin sudah dibuat.
     * PIN lama yang bukan 6 digit dianggap tidak valid.
     */
    fun isPinSet(): Boolean {
        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null)

        if (!isValidPinFormat(storedPin.orEmpty())) {
            sharedPreferences.edit {
                remove(KEY_ADMIN_PIN)
            }
            isAdminAuthenticated = false
            return false
        }

        return true
    }

    /**
     * Menyimpan PIN Admin baru.
     * PIN wajib tepat 6 digit angka.
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
     * Memverifikasi PIN Admin.
     */
    fun verifyPin(inputPin: String): Boolean {
        if (!isValidPinFormat(inputPin)) {
            return false
        }

        val storedPin = sharedPreferences.getString(KEY_ADMIN_PIN, null)
            ?: return false

        if (!isValidPinFormat(storedPin)) {
            return false
        }

        val isCorrect = storedPin == inputPin

        if (isCorrect) {
            isAdminAuthenticated = true
        }

        return isCorrect
    }

    /**
     * Login Admin menggunakan PIN.
     */
    fun loginAdmin(inputPin: String): Boolean {
        return verifyPin(inputPin)
    }

    /**
     * Logout Admin.
     */
    fun logoutAdmin() {
        isAdminAuthenticated = false
    }

    /**
     * Mengecek status sesi Admin.
     */
    fun isAdminSessionActive(): Boolean {
        return isAdminAuthenticated
    }

    /**
     * Menghapus PIN Admin.
     */
    fun clearPin() {
        sharedPreferences.edit {
            remove(KEY_ADMIN_PIN)
        }

        isAdminAuthenticated = false
    }

    /**
     * Validasi PIN: tepat 6 digit angka.
     */
    private fun isValidPinFormat(pin: String): Boolean {
        return pin.length == PIN_LENGTH &&
                pin.all { it in '0'..'9' }
    }

    companion object {
        private const val PREFS_FILENAME = "posanof_secure_prefs"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
        private const val PIN_LENGTH = 6

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
