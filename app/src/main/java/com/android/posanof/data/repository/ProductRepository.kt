package com.android.posanof.data.repository

import com.android.posanof.data.local.dao.ProductDao
import com.android.posanof.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {

    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getLowStockProducts(): Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    fun searchProductsByName(query: String): Flow<List<ProductEntity>> =
        productDao.searchProductsByName(query)

    suspend fun findProductByBarcode(barcode: String): ProductEntity? =
        productDao.findProductByBarcode(barcode)

    suspend fun insertProduct(product: ProductEntity): Result<Long> = try {
        val id = productDao.insertProduct(product)
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProduct(product: ProductEntity): Result<Unit> = try {
        productDao.updateProduct(product)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteProduct(product: ProductEntity): Result<Unit> = try {
        productDao.deleteProduct(product)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
