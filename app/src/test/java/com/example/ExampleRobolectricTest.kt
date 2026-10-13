package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.DumpSerializer
import com.example.model.SectorData
import com.example.model.hexToByteArray
import com.example.model.toHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KeyMaster NFC", appName)
    }

    @Test
    fun `test dump serializer roundtrip with android json`() {
        val sampleSector = SectorData(
            sectorIndex = 0,
            blocks = listOf(
                "4A2B9C1D2A0804000102030405060708".hexToByteArray(),
                ByteArray(16),
                ByteArray(16),
                "FFFFFFFFFFFFFF078069FFFFFFFFFFFF".hexToByteArray()
            ),
            keyA = "FFFFFFFFFFFF",
            keyB = "FFFFFFFFFFFF",
            accessBits = "FF 07 80"
        )

        val dump = CardDump(
            uid = "4A:2B:9C:1D",
            atqa = "00 04",
            sak = "08",
            cardType = CardType.MIFARE_CLASSIC_1K,
            sectors = listOf(sampleSector)
        )

        val json = DumpSerializer.toJson(dump)
        assertNotNull(json)
        assertTrue(json.contains("4A:2B:9C:1D"))

        val restored = DumpSerializer.fromJson(json)
        assertEquals(dump.uid, restored.uid)
        assertEquals(dump.cardType, restored.cardType)
        assertEquals(1, restored.sectors.size)
        assertEquals("FFFFFFFFFFFF", restored.sectors[0].keyA)
        assertArrayEquals(dump.sectors[0].blocks[0], restored.sectors[0].blocks[0])
    }

    @Test
    fun `test emulation of chosen dump via ApduProcessor`() {
        val testUid = "12:34:56:78"
        val customDump = com.example.hce.EmulationManager.createDumpFromUid(testUid, CardType.MIFARE_CLASSIC_1K)
        com.example.hce.EmulationManager.startEmulation(customDump, "Офисный пропуск")

        assertTrue(com.example.hce.EmulationManager.isEmulationActive.value)
        assertEquals("Офисный пропуск", com.example.hce.EmulationManager.activeKeyName.value)
        assertEquals(testUid, com.example.hce.EmulationManager.activeDump.value?.uid)

        // 1. Reader sends GET UID: FF CA 00 00 00
        val getUidCmd = "FFCA000000".hexToByteArray()
        val getUidResp = com.example.hce.ApduProcessor.processCommand(getUidCmd)

        // Response should be 4 bytes UID + 90 00
        assertEquals(6, getUidResp.size)
        val respHex = getUidResp.toHex("")
        assertTrue("Expected response to start with 12345678, was $respHex", respHex.startsWith("12345678"))
        assertTrue("Expected response to end with 9000, was $respHex", respHex.endsWith("9000"))

        // 2. Reader sends SELECT AID
        val selectAidCmd = "00A4040007D2760000850101".hexToByteArray()
        val selectAidResp = com.example.hce.ApduProcessor.processCommand(selectAidCmd)
        assertEquals("9000", selectAidResp.toHex(""))
    }
}
