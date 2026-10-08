package com.android.posanof

import com.android.posanof.data.local.entity.TransactionEntity
import com.android.posanof.data.local.relation.TransactionWithDetails
import com.android.posanof.util.ReportExporter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ReportExporterTest {

    @Test
    fun exportToExcel_generatesValidXlsxStreamWithoutAwtError() {
        val transactions = listOf(
            TransactionWithDetails(
                transaction = TransactionEntity(
                    id = 1,
                    date = System.currentTimeMillis(),
                    totalAmount = 50000.0,
                    paymentMethod = "TUNAI",
                    cashAmount = 100000.0,
                    changeAmount = 50000.0,
                    cashierName = "Kasir 1"
                ),
                details = emptyList()
            )
        )

        val outputStream = ByteArrayOutputStream()
        ReportExporter.exportToExcel(transactions, outputStream)

        val bytes = outputStream.toByteArray()
        assertTrue("Output stream should contain bytes", bytes.isNotEmpty())
    }
}
