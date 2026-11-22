package com.android.posanof.data.repository

import com.android.posanof.data.local.dao.CreditDao
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.CreditHistoryEntity
import kotlinx.coroutines.flow.Flow

class CreditRepository(private val creditDao: CreditDao) {

    fun getAllCredits(): Flow<List<CreditEntity>> =
        creditDao.getAllCredits()

    fun getUnpaidCredits(): Flow<List<CreditEntity>> =
        creditDao.getCreditsByStatus("BELUM_LUNAS")

    fun getPaidCredits(): Flow<List<CreditEntity>> =
        creditDao.getCreditsByStatus("LUNAS")

    fun getCreditById(id: Int): Flow<CreditEntity?> =
        creditDao.getCreditById(id)

    fun getCreditHistoriesByCreditId(creditId: Int): Flow<List<CreditHistoryEntity>> =
        creditDao.getCreditHistoriesByCreditId(creditId)

    suspend fun findCreditById(id: Int): CreditEntity? =
        creditDao.findCreditById(id)

    suspend fun updateCredit(credit: CreditEntity) =
        creditDao.updateCredit(credit)

    suspend fun insertCreditHistory(history: CreditHistoryEntity) =
        creditDao.insertCreditHistory(history)

    suspend fun deleteCredit(credit: CreditEntity) =
        creditDao.deleteCredit(credit)
}
