package com.android.posanof.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_history",
    foreignKeys = [
        ForeignKey(
            entity = CreditEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["credit_id"])
    ]
)
data class CreditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "credit_id")
    val creditId: Int,

    @ColumnInfo(name = "type")
    val type: String, // "DEBIT" or "PAYMENT"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
