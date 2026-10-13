package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoVaultManager
import com.example.db.AppDatabase
import com.example.db.CardEntity
import com.example.db.DictionaryKeyEntity
import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.DumpSerializer
import com.example.model.SectorData
import com.example.model.hexToByteArray
import com.example.model.toHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.cardsDao()

    val savedCards: StateFlow<List<CardEntity>> = dao.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dictionaryKeys: StateFlow<List<DictionaryKeyEntity>> = dao.getAllDictionaryKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatus() {
        _statusMessage.value = null
    }

    /**
     * Encrypts and saves card dump to local database with Android KeyStore
     */
    fun saveCard(
        dump: CardDump,
        name: String,
        category: String = "Общие",
        notes: String = "",
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val json = DumpSerializer.toJson(dump)
                val encrypted = CryptoVaultManager.encrypt(json)

                val entity = CardEntity(
                    name = name.ifBlank { "Ключ ${dump.uid}" },
                    tagType = dump.cardType.displayName,
                    uidHex = dump.uid,
                    category = category,
                    notes = notes,
                    encryptedPayloadBase64 = encrypted.encryptedBase64,
                    ivBase64 = encrypted.ivBase64
                )
                val id = dao.insertCard(entity)
                _statusMessage.value = "Ключ '$name' сохранен и зашифрован AES-GCM"
                onSaved(id)
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    /**
     * Decrypts and returns card dump by entity
     */
    fun decryptCard(entity: CardEntity): CardDump? {
        return try {
            val decryptedJson = CryptoVaultManager.decrypt(
                entity.encryptedPayloadBase64,
                entity.ivBase64
            )
            DumpSerializer.fromJson(decryptedJson)
        } catch (e: Exception) {
            _statusMessage.value = "Ошибка расшифровки ключа: ${e.message}"
            null
        }
    }

    fun deleteCard(entity: CardEntity) {
        viewModelScope.launch {
            dao.deleteCard(entity)
            _statusMessage.value = "Ключ '${entity.name}' удален"
        }
    }

    // Dictionary Keys
    fun addDictionaryKey(keyHex: String, description: String = "Пользовательский ключ") {
        val clean = keyHex.replace(" ", "").uppercase()
        if (clean.length != 12) {
            _statusMessage.value = "Ключ MIFARE должен содержать ровно 6 байт (12 HEX символов)"
            return
        }
        viewModelScope.launch {
            dao.insertKey(
                DictionaryKeyEntity(
                    keyHex = clean,
                    description = description,
                    isCustom = true
                )
            )
            _statusMessage.value = "Ключ $clean добавлен в словарь"
        }
    }

    fun deleteDictionaryKey(key: DictionaryKeyEntity) {
        viewModelScope.launch {
            dao.deleteKey(key)
        }
    }

    /**
     * Imports dump from MCT format text
     */
    fun importFromMct(mctText: String, name: String, category: String = "Импорт"): CardDump? {
        try {
            val lines = mctText.lines().map { it.trim() }.filter { it.isNotBlank() }
            val sectors = mutableListOf<SectorData>()
            var currentSector = -1
            var currentBlocks = mutableListOf<ByteArray>()

            for (line in lines) {
                if (line.startsWith("+Sector:", ignoreCase = true)) {
                    if (currentSector >= 0) {
                        sectors.add(
                            SectorData(
                                sectorIndex = currentSector,
                                blocks = currentBlocks,
                                keyA = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 6) currentBlocks.last().sliceArray(0..5).toHex() else null,
                                keyB = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 16) currentBlocks.last().sliceArray(10..15).toHex() else null,
                                accessBits = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 9) currentBlocks.last().sliceArray(6..8).toHex(" ") else null
                            )
                        )
                    }
                    currentSector = line.substringAfter(":").trim().toIntOrNull() ?: (currentSector + 1)
                    currentBlocks = mutableListOf()
                } else if (!line.startsWith("#") && !line.startsWith("+")) {
                    val bytes = line.hexToByteArray()
                    if (bytes.size == 16) {
                        currentBlocks.add(bytes)
                    }
                }
            }
            if (currentSector >= 0 && currentBlocks.isNotEmpty()) {
                sectors.add(
                    SectorData(
                        sectorIndex = currentSector,
                        blocks = currentBlocks,
                        keyA = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 6) currentBlocks.last().sliceArray(0..5).toHex() else null,
                        keyB = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 16) currentBlocks.last().sliceArray(10..15).toHex() else null,
                        accessBits = if (currentBlocks.isNotEmpty() && currentBlocks.last().size >= 9) currentBlocks.last().sliceArray(6..8).toHex(" ") else null
                    )
                )
            }

            val uid = if (sectors.isNotEmpty() && sectors[0].blocks.isNotEmpty()) {
                sectors[0].blocks[0].sliceArray(0..3).toHex(":")
            } else "IMPORTED"

            val dump = CardDump(
                uid = uid,
                cardType = if (sectors.size > 16) CardType.MIFARE_CLASSIC_4K else CardType.MIFARE_CLASSIC_1K,
                sectors = sectors
            )
            saveCard(dump, name, category)
            return dump
        } catch (e: Exception) {
            _statusMessage.value = "Ошибка разбора MCT файла: ${e.message}"
            return null
        }
    }
}
