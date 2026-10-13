package com.example.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.model.CardDump
import com.example.model.hexToByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class WriteResult {
    data class Success(val message: String) : WriteResult()
    data class Error(val reason: String) : WriteResult()
}

class NfcCardWriter {

    suspend fun writeMifareBlock(
        tag: Tag,
        sectorIndex: Int,
        blockOffsetInSector: Int,
        newData16Bytes: ByteArray,
        keyHex: String,
        isKeyB: Boolean = false
    ): WriteResult = withContext(Dispatchers.IO) {
        val mfc = MifareClassic.get(tag) ?: return@withContext WriteResult.Error("Чип не поддерживает протокол MIFARE Classic")
        try {
            mfc.connect()
            val keyBytes = keyHex.hexToByteArray()
            val authed = if (isKeyB) {
                mfc.authenticateSectorWithKeyB(sectorIndex, keyBytes)
            } else {
                mfc.authenticateSectorWithKeyA(sectorIndex, keyBytes)
            }

            if (!authed) {
                return@withContext WriteResult.Error("Ошибка аутентификации сектора $sectorIndex ключом $keyHex")
            }

            val targetBlock = mfc.sectorToBlock(sectorIndex) + blockOffsetInSector
            mfc.writeBlock(targetBlock, newData16Bytes)

            WriteResult.Success("Блок $targetBlock сектора $sectorIndex успешно записан")
        } catch (e: Exception) {
            WriteResult.Error("Ошибка записи: ${e.message}")
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }

    suspend fun writeFullMifareDump(
        tag: Tag,
        dump: CardDump,
        writeBlock0Magic: Boolean = false,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): WriteResult = withContext(Dispatchers.IO) {
        val mfc = MifareClassic.get(tag) ?: return@withContext WriteResult.Error("Целевая карта не является MIFARE Classic")
        try {
            mfc.connect()
            val totalSectors = dump.sectors.size.coerceAtMost(mfc.sectorCount)
            var writtenBlocks = 0

            for (sec in dump.sectors) {
                if (sec.sectorIndex >= totalSectors) break
                onProgress(sec.sectorIndex + 1, totalSectors)

                val keysToTry = mutableListOf<ByteArray>()
                sec.keyA?.let { keysToTry.add(it.hexToByteArray()) }
                sec.keyB?.let { keysToTry.add(it.hexToByteArray()) }
                keysToTry.addAll(MifareKeys.DEFAULT_KEYS_BYTES)

                var authenticated = false
                for (k in keysToTry) {
                    if (mfc.authenticateSectorWithKeyA(sec.sectorIndex, k) ||
                        mfc.authenticateSectorWithKeyB(sec.sectorIndex, k)) {
                        authenticated = true
                        break
                    }
                }

                if (!authenticated) {
                    continue // Skip sector if unable to authenticate
                }

                val firstBlock = mfc.sectorToBlock(sec.sectorIndex)
                for (bIndex in sec.blocks.indices) {
                    val absoluteBlock = firstBlock + bIndex
                    if (absoluteBlock == 0 && !writeBlock0Magic) {
                        // Skip manufacturer block on regular cards
                        continue
                    }
                    try {
                        mfc.writeBlock(absoluteBlock, sec.blocks[bIndex])
                        writtenBlocks++
                    } catch (e: Exception) {
                        // Ignore single block errors (e.g. read-only block)
                    }
                }
            }

            WriteResult.Success("Запись завершена. Записано блоков: $writtenBlocks")
        } catch (e: Exception) {
            WriteResult.Error("Сбой при записи дампа: ${e.message}")
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }

    suspend fun writeUltralightPage(
        tag: Tag,
        pageIndex: Int,
        data4Bytes: ByteArray
    ): WriteResult = withContext(Dispatchers.IO) {
        val mfu = MifareUltralight.get(tag) ?: return@withContext WriteResult.Error("Чип не является MIFARE Ultralight/NTAG")
        try {
            mfu.connect()
            mfu.writePage(pageIndex, data4Bytes)
            WriteResult.Success("Страница $pageIndex успешно записана")
        } catch (e: Exception) {
            WriteResult.Error("Ошибка записи страницы: ${e.message}")
        } finally {
            try { mfu.close() } catch (_: Exception) {}
        }
    }

    suspend fun writeNdefMessage(
        tag: Tag,
        textRecord: String
    ): WriteResult = withContext(Dispatchers.IO) {
        try {
            val record = NdefRecord.createTextRecord("ru", textRecord)
            val message = NdefMessage(arrayOf(record))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    return@withContext WriteResult.Error("Метка заблокирована для записи")
                }
                if (ndef.maxSize < message.byteArrayLength) {
                    return@withContext WriteResult.Error("Недостаточно памяти на метке")
                }
                ndef.writeNdefMessage(message)
                return@withContext WriteResult.Success("NDEF запись успешно сохранена")
            }

            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                formatable.connect()
                formatable.format(message)
                return@withContext WriteResult.Success("Метка отформатирована и NDEF запись сохранена")
            }

            WriteResult.Error("Метка не поддерживает формат NDEF")
        } catch (e: Exception) {
            WriteResult.Error("Ошибка записи NDEF: ${e.message}")
        }
    }
}
