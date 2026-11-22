package com.android.posanof

import com.android.posanof.util.QrisGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrisGeneratorTest {

    // Valid EMVCo QRIS Payload sample
    // Tag 59: "TOKO POSANOF" (12 chars) -> 5912TOKO POSANOF
    private val sampleStaticPayload = "00020101021126580014ID.LINKAJA.WWW01189360091100020088920215200826102188925204581253033605802ID5912TOKO POSANOF6007BANDUNG61054013262070703A0163041234"

    @Test
    fun testIsValidQrisPayload() {
        assertTrue(QrisGenerator.isValidQrisPayload(sampleStaticPayload))
        assertFalse(QrisGenerator.isValidQrisPayload("12345"))
        assertFalse(QrisGenerator.isValidQrisPayload("0002015802US"))
    }

    @Test
    fun testExtractMerchantName() {
        val merchantName = QrisGenerator.extractMerchantName(sampleStaticPayload)
        assertEquals("TOKO POSANOF", merchantName)
    }

    @Test
    fun testGenerateDynamicQrisAndTag54() {
        val amount = 50000L
        val dynamicPayload = QrisGenerator.generateDynamicQris(sampleStaticPayload, amount)

        // 1. Tag 01 should change from 11 to 12
        assertTrue(dynamicPayload.contains("010212"))

        // 2. Tag 54 (Transaction Amount) should be 540550000
        assertTrue(dynamicPayload.contains("540550000"))

        // 3. Payload should end with valid 4-character hex CRC
        assertTrue(dynamicPayload.contains("6304"))
        val crcStr = dynamicPayload.takeLast(4)
        assertEquals(4, crcStr.length)

        // 4. Verify the CRC calculation over payload without CRC
        val payloadWithoutCrc = dynamicPayload.dropLast(4)
        val recalculatedCrc = QrisGenerator.calculateCrc16CcittFalse(payloadWithoutCrc.toByteArray(Charsets.UTF_8))
        assertEquals(crcStr, recalculatedCrc)
    }

    @Test
    fun testCrc16CcittFalseCalculation() {
        // Test CRC calculation for "123456789" (Standard CCITT-FALSE CRC is 29B1)
        val testData = "123456789".toByteArray(Charsets.UTF_8)
        val crc = QrisGenerator.calculateCrc16CcittFalse(testData)
        assertEquals("29B1", crc)
    }
}
