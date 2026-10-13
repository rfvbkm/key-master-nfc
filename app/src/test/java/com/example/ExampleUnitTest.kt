package com.example

import com.example.model.CardDump
import com.example.model.DumpSerializer
import com.example.model.SectorData
import com.example.model.hexToByteArray
import com.example.model.toHex
import com.example.nfc.MifareKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testHexConversion() {
        val originalBytes = byteArrayOf(0x4A, 0x2B, 0x9C.toByte(), 0x1D)
        val hexString = originalBytes.toHex()
        assertEquals("4A2B9C1D", hexString)

        val parsedBytes = hexString.hexToByteArray()
        assertArrayEquals(originalBytes, parsedBytes)

        val spacedHex = originalBytes.toHex(" ")
        assertEquals("4A 2B 9C 1D", spacedHex)
    }

    @Test
    fun testAccessBitsDecoder_DefaultTransport() {
        val transportAccessBytes = byteArrayOf(0xFF.toByte(), 0x07, 0x80.toByte())
        val cond = MifareKeys.decodeAccessBits(transportAccessBytes)

        assertTrue(cond.isDefaultOrInsecure)
        assertTrue(cond.block0Permission.contains("Транспортный режим"))
    }

    @Test
    fun testMctFormatExport() {
        val dump = CardDump(
            uid = "11:22:33:44",
            sectors = listOf(
                SectorData(
                    sectorIndex = 0,
                    blocks = listOf("11223344000000000000000000000000".hexToByteArray())
                )
            )
        )

        val mctText = DumpSerializer.toMctFormat(dump)
        assertTrue(mctText.contains("+Sector: 0"))
        assertTrue(mctText.contains("11223344000000000000000000000000"))
    }
}
