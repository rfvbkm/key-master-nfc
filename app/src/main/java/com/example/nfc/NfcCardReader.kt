package com.example.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.PageData
import com.example.model.SectorData
import com.example.model.hexToByteArray
import com.example.model.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NfcCardReader {

    suspend fun readTag(
        tag: Tag,
        customKeys: List<String> = emptyList(),
        onProgress: (current: Int, total: Int, message: String) -> Unit = { _, _, _ -> }
    ): CardDump = withContext(Dispatchers.IO) {
        val uid = tag.id?.toHex(":") ?: "UNKNOWN"
        val rawTechs = tag.techList.map { it.substringAfterLast(".") }

        var atqa = ""
        var sak = ""
        var cardType = CardType.UNKNOWN

        // Extract NfcA info if present
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            atqa = nfcA.atqa?.toHex(" ") ?: ""
            sak = "%02X".format(nfcA.sak.toInt())
        }

        // Try MIFARE Classic
        val mifareClassic = MifareClassic.get(tag)
        if (mifareClassic != null) {
            return@withContext readMifareClassic(mifareClassic, uid, atqa, sak, rawTechs, customKeys, onProgress)
        }

        // Try MIFARE Ultralight / NTAG
        val mifareUltralight = MifareUltralight.get(tag)
        if (mifareUltralight != null) {
            return@withContext readMifareUltralight(mifareUltralight, uid, atqa, sak, rawTechs, onProgress)
        }

        // Try NDEF
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return@withContext readNdefOnly(ndef, uid, atqa, sak, rawTechs)
        }

        // Try ISO-DEP
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            cardType = CardType.ISO_DEP
        }

        CardDump(
            uid = uid,
            atqa = atqa,
            sak = sak,
            cardType = cardType,
            rawTechs = rawTechs
        )
    }

    private fun readMifareClassic(
        mfc: MifareClassic,
        uid: String,
        atqa: String,
        sak: String,
        rawTechs: List<String>,
        customKeys: List<String>,
        onProgress: (Int, Int, String) -> Unit
    ): CardDump {
        mfc.connect()
        try {
            val sectorCount = mfc.sectorCount
            val cardType = when (mfc.type) {
                MifareClassic.TYPE_CLASSIC -> if (sectorCount > 16) CardType.MIFARE_CLASSIC_4K else CardType.MIFARE_CLASSIC_1K
                else -> CardType.MIFARE_CLASSIC_1K
            }

            // Combine default and custom keys
            val keysToTry = (customKeys.map { it.hexToByteArray() } + MifareKeys.DEFAULT_KEYS_BYTES)
                .distinctBy { it.toHex() }

            val sectors = mutableListOf<SectorData>()

            for (sec in 0 until sectorCount) {
                onProgress(sec + 1, sectorCount, "Сектор $sec из $sectorCount...")
                var authenticated = false
                var foundKeyA: String? = null
                var foundKeyB: String? = null

                // Try Key A
                for (key in keysToTry) {
                    if (mfc.authenticateSectorWithKeyA(sec, key)) {
                        authenticated = true
                        foundKeyA = key.toHex()
                        break
                    }
                }

                // Also try Key B if not authenticated or to find both
                for (key in keysToTry) {
                    if (mfc.authenticateSectorWithKeyB(sec, key)) {
                        authenticated = true
                        foundKeyB = key.toHex()
                        break
                    }
                }

                if (authenticated) {
                    val blockCount = mfc.getBlockCountInSector(sec)
                    val firstBlock = mfc.sectorToBlock(sec)
                    val blocks = mutableListOf<ByteArray>()
                    var accessBits: String? = null

                    for (b in 0 until blockCount) {
                        try {
                            val data = mfc.readBlock(firstBlock + b)
                            blocks.add(data)
                            // Trailer block is the last block in sector
                            if (b == blockCount - 1 && data.size >= 9) {
                                val abBytes = data.sliceArray(6..8)
                                accessBits = abBytes.toHex(" ")
                            }
                        } catch (e: Exception) {
                            blocks.add(ByteArray(16))
                        }
                    }

                    sectors.add(
                        SectorData(
                            sectorIndex = sec,
                            blocks = blocks,
                            keyA = foundKeyA,
                            keyB = foundKeyB,
                            accessBits = accessBits,
                            isReadSuccess = true
                        )
                    )
                } else {
                    val blockCount = mfc.getBlockCountInSector(sec)
                    sectors.add(
                        SectorData(
                            sectorIndex = sec,
                            blocks = List(blockCount) { ByteArray(16) },
                            keyA = null,
                            keyB = null,
                            accessBits = null,
                            isReadSuccess = false
                        )
                    )
                }
            }

            return CardDump(
                uid = uid,
                atqa = atqa,
                sak = sak,
                cardType = cardType,
                sectors = sectors,
                rawTechs = rawTechs
            )
        } finally {
            try {
                mfc.close()
            } catch (_: Exception) {}
        }
    }

    private fun readMifareUltralight(
        mfu: MifareUltralight,
        uid: String,
        atqa: String,
        sak: String,
        rawTechs: List<String>,
        onProgress: (Int, Int, String) -> Unit
    ): CardDump {
        mfu.connect()
        try {
            val (pageCount, cardType) = when (mfu.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> 16 to CardType.MIFARE_ULTRALIGHT
                MifareUltralight.TYPE_ULTRALIGHT_C -> 48 to CardType.MIFARE_ULTRALIGHT
                else -> 45 to CardType.NTAG213 // Common default
            }

            val pages = mutableListOf<PageData>()
            // Ultralight readPages reads 4 pages (16 bytes) at once
            var p = 0
            while (p < pageCount) {
                onProgress(p + 1, pageCount, "Чтение страниц $p..${p + 3}...")
                try {
                    val batch = mfu.readPages(p)
                    for (i in 0 until 4) {
                        if (p + i < pageCount) {
                            val singlePage = batch.sliceArray((i * 4) until ((i + 1) * 4))
                            pages.add(PageData(p + i, singlePage))
                        }
                    }
                } catch (e: Exception) {
                    pages.add(PageData(p, ByteArray(4)))
                }
                p += 4
            }

            return CardDump(
                uid = uid,
                atqa = atqa,
                sak = sak,
                cardType = cardType,
                pages = pages,
                rawTechs = rawTechs
            )
        } finally {
            try {
                mfu.close()
            } catch (_: Exception) {}
        }
    }

    private fun readNdefOnly(
        ndef: Ndef,
        uid: String,
        atqa: String,
        sak: String,
        rawTechs: List<String>
    ): CardDump {
        ndef.connect()
        try {
            val message = ndef.ndefMessage
            val payloadText = message?.records?.joinToString("\n") { record ->
                String(record.payload, Charsets.UTF_8).filter { it in ' '..'~' || it in 'А'..'я' }
            }
            return CardDump(
                uid = uid,
                atqa = atqa,
                sak = sak,
                cardType = CardType.NDEF_TAG,
                ndefPayload = payloadText,
                rawTechs = rawTechs
            )
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Generates a realistic mock card dump for testing/demo in emulator environment.
     */
    fun createSampleDump(type: CardType = CardType.MIFARE_CLASSIC_1K): CardDump {
        val uid = "4A:2B:9C:1D"
        val atqa = "00 04"
        val sak = "08"
        val sectors = mutableListOf<SectorData>()

        for (s in 0 until 16) {
            val blocks = mutableListOf<ByteArray>()
            for (b in 0 until 4) {
                val blockBytes = ByteArray(16)
                if (s == 0 && b == 0) {
                    // Manufacturer block 0 (UID + BCC + Manufacturer data)
                    val uidBytes = "4A2B9C1D".hexToByteArray()
                    System.arraycopy(uidBytes, 0, blockBytes, 0, 4)
                    blockBytes[4] = (0x4A xor 0x2B xor 0x9C xor 0x1D).toByte() // BCC
                    blockBytes[5] = 0x08.toByte() // SAK
                    blockBytes[6] = 0x04.toByte() // ATQA
                    blockBytes[7] = 0x00.toByte()
                    // Random manufacturer bytes
                    for (i in 8 until 16) blockBytes[i] = (0x10 + i).toByte()
                } else if (b == 3) {
                    // Sector trailer: Key A (6B) + Access Bits (4B) + Key B (6B)
                    val keyA = "FFFFFFFFFFFF".hexToByteArray()
                    val accessBits = "FF078069".hexToByteArray()
                    val keyB = "FFFFFFFFFFFF".hexToByteArray()
                    System.arraycopy(keyA, 0, blockBytes, 0, 6)
                    System.arraycopy(accessBits, 0, blockBytes, 6, 4)
                    System.arraycopy(keyB, 0, blockBytes, 10, 6)
                } else {
                    // Data blocks
                    if (s == 1 && b == 0) {
                        // Access Control Card / Intercom sample data
                        val vizitCode = "VIZIT-CODE-7788".toByteArray(Charsets.US_ASCII)
                        System.arraycopy(vizitCode, 0, blockBytes, 0, vizitCode.size.coerceAtMost(16))
                    } else if (s == 2 && b == 0) {
                        // Office pass token
                        val passId = "PASS-ID-9042".toByteArray(Charsets.US_ASCII)
                        System.arraycopy(passId, 0, blockBytes, 0, passId.size.coerceAtMost(16))
                    }
                }
                blocks.add(blockBytes)
            }

            sectors.add(
                SectorData(
                    sectorIndex = s,
                    blocks = blocks,
                    keyA = "FFFFFFFFFFFF",
                    keyB = "FFFFFFFFFFFF",
                    accessBits = "FF 07 80",
                    isReadSuccess = true
                )
            )
        }

        return CardDump(
            uid = uid,
            atqa = atqa,
            sak = sak,
            cardType = CardType.MIFARE_CLASSIC_1K,
            sectors = sectors,
            rawTechs = listOf("NfcA", "MifareClassic", "NdefFormatable")
        )
    }
}
