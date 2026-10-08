package com.android.posanof.data.repository

import androidx.room.withTransaction
import com.android.posanof.data.local.database.AppDatabase
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity
import com.android.posanof.data.local.relation.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val productDao = database.productDao()
    private val creditDao = database.creditDao()

    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    /**
     * Menyimpan transaksi penjualan beserta rinciannya, mengurangi stok produk,
     * dan mencatat piutang (kasbon) jika metode pembayaran adalah KASBON.
     * Semua operasi dijalankan secara atomik dalam satu transaksi Room Database (@Transaction).
     */
    suspend fun executeSalesTransaction(
        transaction: TransactionEntity,
        details: List<TransactionDetailEntity>,
        credit: CreditEntity? = null,
    ): Result<Int> {
        return try {
            database.withTransaction {
                // 1. Kurangi stok produk terlebih dahulu & cek ketersediaan
                for (detail in details) {
                    val updatedRows = productDao.decreaseStock(detail.productId, detail.quantity)
                    if (updatedRows == 0) {
                        throw IllegalStateException("Stok produk tidak mencukupi untuk ID produk: ${detail.productId}")
                    }
                }

                // 2. Simpan header transaksi
                val transactionId = transactionDao.insertTransaction(transaction).toInt()

                // 3. Simpan rincian transaksi dengan transaction_id yang baru didapat
                val updatedDetails = details.map { it.copy(transactionId = transactionId) }
                transactionDao.insertTransactionDetails(updatedDetails)

                // 4. Jika metode KASBON, simpan record piutang pada tabel credit
                if (credit != null) {
                    val updatedCredit = credit.copy(transactionId = transactionId)
                    creditDao.insertCredit(updatedCredit)
                }

                Result.success(transactionId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
