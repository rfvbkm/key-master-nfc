package com.example.hce

import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.SectorData
import com.example.model.hexToByteArray
import com.example.model.toHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApduLogEntry(
    val timestamp: String,
    val commandHex: String,
    val responseHex: String,
    val description: String
)

data class KeyPreset(
    val id: String,
    val title: String,
    val description: String,
    val uidHex: String,
    val cardType: CardType,
    val category: String
)

object EmulationManager {

    private val _isEmulationActive = MutableStateFlow(false)
    val isEmulationActive: StateFlow<Boolean> = _isEmulationActive.asStateFlow()

    private val _activeDump = MutableStateFlow<CardDump?>(null)
    val activeDump: StateFlow<CardDump?> = _activeDump.asStateFlow()

    private val _activeKeyName = MutableStateFlow("Ключ не выбран")
    val activeKeyName: StateFlow<String> = _activeKeyName.asStateFlow()

    private val _interactionCount = MutableStateFlow(0)
    val interactionCount: StateFlow<Int> = _interactionCount.asStateFlow()

    private val _logs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val logs: StateFlow<List<ApduLogEntry>> = _logs.asStateFlow()

    val presets: List<KeyPreset> = listOf(
        KeyPreset(
            id = "preset_vizit",
            title = "Домофон Визит / Цифрал",
            description = "Стандартный 4-байтовый MIFARE UID для домофонных панелей",
            uidHex = "12:34:56:78",
            cardType = CardType.MIFARE_CLASSIC_1K,
            category = "Домофон"
        ),
        KeyPreset(
            id = "preset_turnstile",
            title = "Офисный турникет СКУД",
            description = "Пропускная система Perco / Sigur / Parsec",
            uidHex = "A1:B2:C3:D4",
            cardType = CardType.MIFARE_CLASSIC_1K,
            category = "Офис"
        ),
        KeyPreset(
            id = "preset_barrier",
            title = "Шлагбаум и паркинг",
            description = "RFID идентификатор въезда на придомовую территорию",
            uidHex = "DE:AD:BE:EF",
            cardType = CardType.MIFARE_CLASSIC_1K,
            category = "Шлагбаум"
        ),
        KeyPreset(
            id = "preset_ntag_badge",
            title = "NFC Бейдж сотрудника",
            description = "NTAG215 чип (7-байтовый UID, NDEF)",
            uidHex = "04:A1:7E:29:4F:88:80",
            cardType = CardType.NTAG215,
            category = "Пропуск"
        ),
        KeyPreset(
            id = "preset_apartment",
            title = "Ключ от подъезда ТСЖ",
            description = "Дубликат ключа подъездной двери",
            uidHex = "5E:9B:31:C7",
            cardType = CardType.MIFARE_CLASSIC_1K,
            category = "Квартира"
        )
    )

    fun startEmulation(dump: CardDump, name: String) {
        _activeDump.value = dump
        _activeKeyName.value = name.ifBlank { "Ключ ${dump.uid}" }
        _isEmulationActive.value = true
        addLog(
            "START",
            "ACTIVE",
            "Режим ключа активирован: '$name' (UID: ${dump.uid})"
        )
    }

    fun startEmulationFromUid(uidHex: String, name: String, cardType: CardType = CardType.MIFARE_CLASSIC_1K) {
        val dump = createDumpFromUid(uidHex, cardType)
        startEmulation(dump, name)
    }

    fun createDumpFromUid(uidHex: String, cardType: CardType = CardType.MIFARE_CLASSIC_1K): CardDump {
        val clean = uidHex.replace(":", "").replace(" ", "")
        val uidBytes = clean.hexToByteArray()

        val block0 = ByteArray(16)
        if (uidBytes.isNotEmpty()) {
            val copyLen = uidBytes.size.coerceAtMost(block0.size)
            System.arraycopy(uidBytes, 0, block0, 0, copyLen)
            if (uidBytes.size >= 4) {
                // Calculate BCC (Block Check Character = XOR of 4 UID bytes)
                block0[4] = (block0[0].toInt() xor block0[1].toInt() xor block0[2].toInt() xor block0[3].toInt()).toByte()
            }
            if (copyLen > 5) {
                block0[5] = 0x08.toByte() // SAK
                block0[6] = 0x04.toByte() // ATQA LSB
                block0[7] = 0x00.toByte() // ATQA MSB
            }
        }

        val dummySectors = listOf(
            SectorData(
                sectorIndex = 0,
                blocks = listOf(block0, ByteArray(16), ByteArray(16), ByteArray(16)),
                keyA = "FFFFFFFFFFFF",
                keyB = "FFFFFFFFFFFF"
            )
        )

        return CardDump(
            uid = if (clean.length == 8) clean.chunked(2).joinToString(":") else uidHex,
            atqa = "0004",
            sak = "08",
            cardType = cardType,
            sectors = dummySectors
        )
    }

    fun stopEmulation() {
        _isEmulationActive.value = false
        addLog("STOP", "IDLE", "Эмуляция ключа выключена")
    }

    fun logInteraction(cmdHex: String, respHex: String, desc: String) {
        _interactionCount.value = _interactionCount.value + 1
        addLog(cmdHex, respHex, desc)
    }

    private fun addLog(cmd: String, resp: String, desc: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = ApduLogEntry(time, cmd, resp, desc)
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) current.removeAt(current.size - 1)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _interactionCount.value = 0
    }

    fun simulateReaderCommand(commandHex: String) {
        val bytes = commandHex.hexToByteArray()
        ApduProcessor.processCommand(bytes)
    }
}
