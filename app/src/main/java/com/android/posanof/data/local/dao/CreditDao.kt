package com.android.posanof.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.android.posanof.data.local.entity.CreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCredit(credit: CreditEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCredits(credits: List<CreditEntity>)

    @Update
    suspend fun updateCredit(credit: CreditEntity)

    @Delete
    suspend fun deleteCredit(credit: CreditEntity)

    @Query("SELECT * FROM credit ORDER BY created_at DESC")
    fun getAllCredits(): Flow<List<CreditEntity>>

    @Query("SELECT * FROM credit")
    suspend fun getAllCreditsList(): List<CreditEntity>

    @Query("DELETE FROM credit")
    suspend fun deleteAllCredits()

    @Query("SELECT * FROM credit WHERE status = :status ORDER BY created_at DESC")
    fun getCreditsByStatus(status: String): Flow<List<CreditEntity>>

    @Query("SELECT * FROM credit WHERE id = :id")
    fun getCreditById(id: Int): Flow<CreditEntity?>

    @Query("SELECT * FROM credit WHERE id = :id")
    suspend fun findCreditById(id: Int): CreditEntity?

    @Query("SELECT * FROM credit WHERE transaction_id = :transactionId LIMIT 1")
    fun getCreditByTransactionId(transactionId: Int): Flow<CreditEntity?>
}
