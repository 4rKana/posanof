package com.android.posanof.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "transaction_id"
    )
    val details: List<TransactionDetailEntity>
)
