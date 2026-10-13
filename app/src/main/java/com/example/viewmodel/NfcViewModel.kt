package com.example.viewmodel

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CardDump
import com.example.model.CardType
import com.example.model.SectorData
import com.example.nfc.NfcCardReader
import com.example.nfc.NfcCardWriter
import com.example.nfc.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    data class Reading(val current: Int, val total: Int, val message: String) : ScanUiState()
    data class Success(val dump: CardDump) : ScanUiState()
    data class Error(val error: String) : ScanUiState()
}

sealed class WriteUiState {
    object Idle : WriteUiState()
    object WaitingForTag : WriteUiState()
    data class Writing(val current: Int, val total: Int) : WriteUiState()
    data class Success(val message: String) : WriteUiState()
    data class Error(val reason: String) : WriteUiState()
}

class NfcViewModel(application: Application) : AndroidViewModel(application) {

    private val reader = NfcCardReader()
    private val writer = NfcCardWriter()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _writeState = MutableStateFlow<WriteUiState>(WriteUiState.Idle)
    val writeState: StateFlow<WriteUiState> = _writeState.asStateFlow()

    private val _currentDump = MutableStateFlow<CardDump?>(null)
    val currentDump: StateFlow<CardDump?> = _currentDump.asStateFlow()

    private val _customKeys = MutableStateFlow<List<String>>(emptyList())
    val customKeys: StateFlow<List<String>> = _customKeys.asStateFlow()

    // Flag whether currently in "Write Mode" waiting for card to write to
    private val _isWritePending = MutableStateFlow(false)
    val isWritePending: StateFlow<Boolean> = _isWritePending.asStateFlow()

    private var pendingDumpToWrite: CardDump? = null
    private var writeMagicBlock0: Boolean = false

    fun setCustomKeys(keys: List<String>) {
        _customKeys.value = keys
    }

    fun setCurrentDump(dump: CardDump) {
        _currentDump.value = dump
    }

    /**
     * Called when a Tag is detected via NFC ReaderMode or Intent
     */
    fun onTagDiscovered(tag: Tag) {
        if (_isWritePending.value && pendingDumpToWrite != null) {
            executeWrite(tag, pendingDumpToWrite!!, writeMagicBlock0)
            return
        }

        // Otherwise Read tag
        viewModelScope.launch {
            _scanState.value = ScanUiState.Reading(0, 16, "Обнаружен чип. Чтение...")
            try {
                val dump = reader.readTag(tag, _customKeys.value) { current, total, msg ->
                    _scanState.value = ScanUiState.Reading(current, total, msg)
                }
                _currentDump.value = dump
                _scanState.value = ScanUiState.Success(dump)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error("Ошибка чтения карты: ${e.message}")
            }
        }
    }

    /**
     * Prepares writer to clone a dump on the next tapped tag
     */
    fun startWritingDump(dump: CardDump, allowMagicBlock0: Boolean = false) {
        pendingDumpToWrite = dump
        writeMagicBlock0 = allowMagicBlock0
        _isWritePending.value = true
        _writeState.value = WriteUiState.WaitingForTag
    }

    fun cancelWrite() {
        _isWritePending.value = false
        pendingDumpToWrite = null
        _writeState.value = WriteUiState.Idle
    }

    private fun executeWrite(tag: Tag, dump: CardDump, magicBlock0: Boolean) {
        viewModelScope.launch {
            _writeState.value = WriteUiState.Writing(0, dump.sectors.size)
            val result = writer.writeFullMifareDump(tag, dump, magicBlock0) { cur, tot ->
                _writeState.value = WriteUiState.Writing(cur, tot)
            }
            _isWritePending.value = false
            pendingDumpToWrite = null

            when (result) {
                is WriteResult.Success -> _writeState.value = WriteUiState.Success(result.message)
                is WriteResult.Error -> _writeState.value = WriteUiState.Error(result.reason)
            }
        }
    }

    fun writeNdefText(tag: Tag, text: String) {
        viewModelScope.launch {
            _writeState.value = WriteUiState.Writing(1, 1)
            val result = writer.writeNdefMessage(tag, text)
            when (result) {
                is WriteResult.Success -> _writeState.value = WriteUiState.Success(result.message)
                is WriteResult.Error -> _writeState.value = WriteUiState.Error(result.reason)
            }
        }
    }

    /**
     * Loads a realistic demo dump for rapid inspection and emulator testing
     */
    fun loadDemoDump(type: CardType = CardType.MIFARE_CLASSIC_1K) {
        val demo = reader.createSampleDump(type)
        _currentDump.value = demo
        _scanState.value = ScanUiState.Success(demo)
    }

    /**
     * Updates a single block in the current in-memory dump
     */
    fun updateBlockData(sectorIdx: Int, blockIdx: Int, newBytes: ByteArray) {
        val current = _currentDump.value ?: return
        val updatedSectors = current.sectors.map { sec ->
            if (sec.sectorIndex == sectorIdx) {
                val updatedBlocks = sec.blocks.toMutableList()
                if (blockIdx < updatedBlocks.size) {
                    updatedBlocks[blockIdx] = newBytes
                }
                sec.copy(blocks = updatedBlocks)
            } else sec
        }
        _currentDump.value = current.copy(sectors = updatedSectors)
    }

    fun resetScanState() {
        _scanState.value = ScanUiState.Idle
    }

    fun resetWriteState() {
        _writeState.value = WriteUiState.Idle
    }
}
