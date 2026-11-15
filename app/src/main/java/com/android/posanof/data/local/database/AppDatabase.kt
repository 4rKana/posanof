package com.android.posanof.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.posanof.data.local.dao.CreditDao
import com.android.posanof.data.local.dao.ProductDao
import com.android.posanof.data.local.dao.TransactionDao
import com.android.posanof.data.local.entity.CreditEntity
import com.android.posanof.data.local.entity.ProductEntity
import com.android.posanof.data.local.entity.TransactionDetailEntity
import com.android.posanof.data.local.entity.TransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionDetailEntity::class,
        CreditEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditDao(): CreditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Versi 1 -> 2: tambah kolom kategori (jenis barang) pada tabel "product".
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "posanof_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}