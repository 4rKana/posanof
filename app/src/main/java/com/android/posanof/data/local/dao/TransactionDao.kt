package com.android.posanof.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity
import com.android.posanof.data.local.relation.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionDetails(details: List<TransactionDetailEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM `transaction` ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transaction`")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transaction_detail")
    suspend fun getAllTransactionDetailsList(): List<TransactionDetailEntity>

    @Query("DELETE FROM transaction_detail")
    suspend fun deleteAllTransactionDetails()

    @Query("DELETE FROM `transaction`")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    fun getTransactionById(id: Int): Flow<TransactionEntity?>

    @Query("SELECT * FROM transaction_detail WHERE transaction_id = :transactionId")
    fun getTransactionDetails(transactionId: Int): Flow<List<TransactionDetailEntity>>

    @Transaction
    @Query("SELECT * FROM `transaction` WHERE id = :transactionId")
    fun getTransactionWithDetails(transactionId: Int): Flow<TransactionWithDetails?>

    @Transaction
    @Query("SELECT * FROM `transaction` ORDER BY date DESC")
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>
}
