package com.android.posanof.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.android.posanof.data.local.database.AppDatabase
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.CreditHistoryEntity
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupSummary(
    val productCount: Int,
    val transactionCount: Int,
    val transactionDetailCount: Int,
    val creditCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Repository untuk ekspor (backup) dan impor (restore) data Room Database.
 * Menyediakan enkripsi & dekripsi AES-256 (GCM) dengan pengerjaan atomik di Room.
 * Terintegrasi dengan Storage Access Framework (SAF).
 */
class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val creditDao = database.creditDao()

    suspend fun exportBackupToStream(
        outputStream: OutputStream,
        secretPassphrase: String = DEFAULT_PASSPHRASE
    ): Result<BackupSummary> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val products = productDao.getAllProductsList()
            val transactions = transactionDao.getAllTransactionsList()
            val details = transactionDao.getAllTransactionDetailsList()
            val credits = creditDao.getAllCreditsList()
            val creditHistories = creditDao.getAllCreditHistoriesList()

            // 1. Buat struktur JSON
            val rootJson = JSONObject().apply {
                put("app", APP_IDENTIFIER)
                put("version", BACKUP_VERSION)
                put("exported_at", System.currentTimeMillis())

                put("products", JSONArray().apply {
                    products.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("price", p.price)
                            put("stock", p.stock)
                            put("barcode", p.barcode ?: JSONObject.NULL)
                            put("min_stock", p.minStock)
                            put("created_at", p.createdAt)
                            put("category", p.category)
                        })
                    }
                })

                put("transactions", JSONArray().apply {
                    transactions.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("date", t.date)
                            put("total_amount", t.totalAmount)
                            put("payment_method", t.paymentMethod)
                            put("cash_amount", t.cashAmount)
                            put("change_amount", t.changeAmount)
                            put("cashier_name", t.cashierName)
                        })
                    }
                })

                put("transaction_details", JSONArray().apply {
                    details.forEach { d ->
                        put(JSONObject().apply {
                            put("id", d.id)
                            put("transaction_id", d.transactionId)
                            put("product_id", d.productId)
                            put("quantity", d.quantity)
                            put("price_at_purchase", d.priceAtPurchase)
                            put("sub_total", d.subTotal)
                        })
                    }
                })

                put("credits", JSONArray().apply {
                    credits.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("transaction_id", c.transactionId)
                            put("customer_name", c.customerName)
                            put("customer_phone", c.customerPhone ?: JSONObject.NULL)
                            put("remaining_debt", c.remainingDebt)
                            put("status", c.status)
                            put("created_at", c.createdAt)
                        })
                    }
                })

                put("credit_histories", JSONArray().apply {
                    creditHistories.forEach { ch ->
                        put(JSONObject().apply {
                            put("id", ch.id)
                            put("credit_id", ch.creditId)
                            put("type", ch.type)
                            put("amount", ch.amount)
                            put("created_at", ch.createdAt)
                        })
                    }
                })
            }

            val jsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)

            // 2. Enkripsi AES-256 (GCM)
            val encryptedBytes = encryptData(jsonBytes, secretPassphrase)

            // 3. Tulis file ke Stream
            outputStream.write(encryptedBytes)
            outputStream.flush()

            val summary = BackupSummary(
                productCount = products.size,
                transactionCount = transactions.size,
                transactionDetailCount = details.size,
                creditCount = credits.size
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Membaca Room DB, mengubah ke format JSON, mengenkripsi dengan AES-256 (GCM),
     * dan menyimpan file ke eksternal storage via SAF.
     */
    suspend fun exportBackup(
        destinationUri: Uri,
        secretPassphrase: String = DEFAULT_PASSPHRASE
    ): Result<BackupSummary> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream: OutputStream ->
                exportBackupToStream(outputStream, secretPassphrase)
            } ?: throw IllegalStateException("Tidak dapat membuka file tujuan untuk menulis backup.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Membaca file .backup via SAF, mendekripsi dengan AES-256 (GCM),
     * dan melakukan overwrite atomik ke Room Database lokal.
     */
    suspend fun importRestore(
        sourceUri: Uri,
        secretPassphrase: String = DEFAULT_PASSPHRASE
    ): Result<BackupSummary> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Baca seluruh byte file backup
            val encryptedBytes = context.contentResolver.openInputStream(sourceUri)?.use { inputStream: InputStream ->
                inputStream.readBytes()
            } ?: throw IllegalStateException("File backup tidak dapat dibuka.")

            if (encryptedBytes.size < (SALT_SIZE_BYTES + IV_SIZE_BYTES)) {
                throw IllegalArgumentException("File backup tidak valid atau rusak (ukuran terlalu kecil).")
            }

            // 2. Dekripsi file dengan AES-256 (GCM)
            val decryptedBytes = try {
                decryptData(encryptedBytes, secretPassphrase)
            } catch (_: Exception) {
                throw IllegalArgumentException("Gagal mendekripsi file backup. Kunci/Password salah atau file korup/terkait modifikasi.")
            }

            // 3. Parsing JSON & verifikasi identitas file
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val rootJson = JSONObject(jsonString)

            val appIdentifier = rootJson.optString("app")
            if (appIdentifier != APP_IDENTIFIER) {
                throw IllegalArgumentException("Format file backup tidak cocok dengan aplikasi POSANOF.")
            }

            // Extract Products
            val productsList = mutableListOf<ProductEntity>()
            val productsArray = rootJson.getJSONArray("products")
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                productsList.add(
                    ProductEntity(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        price = obj.getDouble("price"),
                        stock = obj.getInt("stock"),
                        barcode = if (obj.isNull("barcode")) null else obj.getString("barcode"),
                        minStock = obj.optInt("min_stock", 0),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        category = obj.optString("category", "")
                    )
                )
            }

            // Extract Transactions
            val transactionsList = mutableListOf<TransactionEntity>()
            val transactionsArray = rootJson.getJSONArray("transactions")
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                transactionsList.add(
                    TransactionEntity(
                        id = obj.getInt("id"),
                        date = obj.getLong("date"),
                        totalAmount = obj.getDouble("total_amount"),
                        paymentMethod = obj.getString("payment_method"),
                        cashAmount = obj.getDouble("cash_amount"),
                        changeAmount = obj.getDouble("change_amount"),
                        cashierName = obj.getString("cashier_name")
                    )
                )
            }

            // Extract Transaction Details
            val detailsList = mutableListOf<TransactionDetailEntity>()
            val detailsArray = rootJson.getJSONArray("transaction_details")
            for (i in 0 until detailsArray.length()) {
                val obj = detailsArray.getJSONObject(i)
                detailsList.add(
                    TransactionDetailEntity(
                        id = obj.getInt("id"),
                        transactionId = obj.getInt("transaction_id"),
                        productId = obj.getInt("product_id"),
                        quantity = obj.getInt("quantity"),
                        priceAtPurchase = obj.getDouble("price_at_purchase"),
                        subTotal = obj.getDouble("sub_total")
                    )
                )
            }

            // Extract Credits
            val creditsList = mutableListOf<CreditEntity>()
            val creditsArray = rootJson.getJSONArray("credits")
            for (i in 0 until creditsArray.length()) {
                val obj = creditsArray.getJSONObject(i)
                creditsList.add(
                    CreditEntity(
                        id = obj.getInt("id"),
                        transactionId = obj.getInt("transaction_id"),
                        customerName = obj.getString("customer_name"),
                        customerPhone = if (obj.isNull("customer_phone")) null else obj.getString("customer_phone"),
                        remainingDebt = obj.getDouble("remaining_debt"),
                        status = obj.getString("status"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }

            // Extract Credit Histories
            val creditHistoriesList = mutableListOf<CreditHistoryEntity>()
            val creditHistoriesArray = rootJson.optJSONArray("credit_histories")
            if (creditHistoriesArray != null) {
                for (i in 0 until creditHistoriesArray.length()) {
                    val obj = creditHistoriesArray.getJSONObject(i)
                    creditHistoriesList.add(
                        CreditHistoryEntity(
                            id = obj.getInt("id"),
                            creditId = obj.getInt("credit_id"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 4. Overwrite Atomik ke Room Database (@Transaction)
            database.withTransaction {
                creditDao.deleteAllCreditHistories()
                transactionDao.deleteAllTransactionDetails()
                creditDao.deleteAllCredits()
                transactionDao.deleteAllTransactions()
                productDao.deleteAllProducts()

                productDao.insertProducts(productsList)
                transactionDao.insertTransactions(transactionsList)
                transactionDao.insertTransactionDetails(detailsList)
                creditDao.insertCredits(creditsList)
                creditDao.insertCreditHistories(creditHistoriesList)
            }

            val summary = BackupSummary(
                productCount = productsList.size,
                transactionCount = transactionsList.size,
                transactionDetailCount = detailsList.size,
                creditCount = creditsList.size
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // AES-256 GCM ENCRYPTION & DECRYPTION HELPERS
    // =========================================================================

    private fun encryptData(data: ByteArray, passphrase: String): ByteArray {
        val random = SecureRandom()

        val salt = ByteArray(SALT_SIZE_BYTES)
        random.nextBytes(salt)

        val iv = ByteArray(IV_SIZE_BYTES)
        random.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(data)

        // Structure: [SALT (16B)] + [IV (12B)] + [CIPHERTEXT]
        return salt + iv + ciphertext
    }

    private fun decryptData(encryptedBytes: ByteArray, passphrase: String): ByteArray {
        val salt = encryptedBytes.copyOfRange(0, SALT_SIZE_BYTES)
        val iv = encryptedBytes.copyOfRange(SALT_SIZE_BYTES, SALT_SIZE_BYTES + IV_SIZE_BYTES)
        val ciphertext = encryptedBytes.copyOfRange(SALT_SIZE_BYTES + IV_SIZE_BYTES, encryptedBytes.size)

        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATION_COUNT, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private const val APP_IDENTIFIER = "posanof"
        private const val BACKUP_VERSION = 2
        private const val DEFAULT_PASSPHRASE = "POSANOF_OFFLINE_SECURE_BACKUP_2026"

        private const val SALT_SIZE_BYTES = 16
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ITERATION_COUNT = 10000
    }
}
