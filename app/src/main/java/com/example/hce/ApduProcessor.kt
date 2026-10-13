package com.example.hce

import com.example.model.hexToByteArray
import com.example.model.toHex

object ApduProcessor {

    // Status Words (SW1 SW2)
    val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val SW_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    val SW_NOT_ACTIVE = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    // Standard Type 4 Tag Capability Container (CC) File (15 bytes)
    val CC_FILE = byteArrayOf(
        0x00, 0x0F, // CCLEN (15 bytes)
        0x20,       // Mapping Version 2.0
        0x00, 0x3B, // MLe (max read 59 bytes)
        0x00, 0x34, // MLc (max write 52 bytes)
        0x04,       // T (NDEF File Control TLV)
        0x06,       // L (6 bytes)
        0xE1.toByte(), 0x04, // File Identifier (E1 04)
        0x00, 0xFF.toByte(), // Max NDEF size (255 bytes)
        0x00,       // Read access without security
        0x00        // Write access without security
    )

    fun processCommand(commandApdu: ByteArray?): ByteArray {
        if (commandApdu == null || commandApdu.isEmpty()) return SW_ERROR

        if (!EmulationManager.isEmulationActive.value) {
            val cmdHex = commandApdu.toHex(" ")
            EmulationManager.logInteraction(
                cmdHex,
                SW_NOT_ACTIVE.toHex(" "),
                "Запрос отклонен: эмуляция выключена"
            )
            return SW_NOT_ACTIVE
        }

        val activeDump = EmulationManager.activeDump.value
        val uidBytes = activeDump?.uid?.replace(":", "")?.replace(" ", "")?.hexToByteArray()
            ?: byteArrayOf(0x4A, 0x2B, 0x9C.toByte(), 0x1D)

        val cmdHex = commandApdu.toHex(" ")

        // 1. SELECT AID (CLA=00, INS=A4, P1=04, P2=00)
        if (commandApdu.size >= 4 && commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xA4.toByte() && commandApdu[2] == 0x04.toByte()) {
            val aidHex = if (commandApdu.size > 5) {
                val len = commandApdu[4].toInt() and 0xFF
                if (commandApdu.size >= 5 + len) {
                    commandApdu.copyOfRange(5, 5 + len).toHex(" ")
                } else commandApdu.toHex(" ")
            } else "NFC Forum / Access"

            val desc = when {
                aidHex.contains("D2 76 00 00 85 01 01", ignoreCase = true) -> "SELECT AID: NFC Forum Type 4 Tag (NDEF)"
                aidHex.contains("F0 01 02 03 04 05 06", ignoreCase = true) -> "SELECT AID: СКУД Считыватель (Generic Access)"
                aidHex.contains("A0 00 00 00 04 10 10", ignoreCase = true) -> "SELECT AID: Домофонный ключ / Пропуск"
                aidHex.contains("F0 4B 65 79 4D 73 74", ignoreCase = true) -> "SELECT AID: KeyMaster Virtual Token"
                else -> "SELECT AID ($aidHex)"
            }

            EmulationManager.logInteraction(cmdHex, SW_SUCCESS.toHex(" "), desc)
            com.example.util.HapticHelper.vibrateKeyRead()
            return SW_SUCCESS
        }

        // 2. GET UID / GET DATA (FF CA 00 00 Le or FF CA 01 00 Le) - Standard PC/SC reader command
        if (commandApdu.size >= 4 && commandApdu[0] == 0xFF.toByte() && commandApdu[1] == 0xCA.toByte()) {
            val response = uidBytes + SW_SUCCESS
            EmulationManager.logInteraction(
                cmdHex,
                response.toHex(" "),
                "GET UID: Отправлен UID ${uidBytes.toHex(":")} (${activeDump?.cardType?.displayName ?: "MIFARE"})"
            )
            com.example.util.HapticHelper.vibrateKeyRead()
            return response
        }

        // 3. SELECT FILE (CLA=00, INS=A4, P1=00, P2=0C or P2=00)
        if (commandApdu.size >= 4 && commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xA4.toByte() &&
            (commandApdu[2] == 0x00.toByte() || commandApdu[2] == 0x01.toByte() || commandApdu[2] == 0x02.toByte())) {
            val fileId = if (commandApdu.size >= 7) {
                "${String.format("%02X", commandApdu[5])}${String.format("%02X", commandApdu[6])}"
            } else "CC"
            EmulationManager.logInteraction(cmdHex, SW_SUCCESS.toHex(" "), "SELECT FILE ($fileId)")
            return SW_SUCCESS
        }

        // 4. READ BINARY (CLA=00, INS=B0)
        if (commandApdu.size >= 4 && commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xB0.toByte()) {
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val le = if (commandApdu.size > 4) (commandApdu[4].toInt() and 0xFF).let { if (it == 0) 256 else it } else 16

            // Try to extract block 0 of sector 0 from dump if available
            val sector0Block0 = activeDump?.sectors?.firstOrNull()?.blocks?.firstOrNull()
            val payload = when {
                offset < CC_FILE.size -> {
                    CC_FILE.copyOfRange(offset, (offset + le).coerceAtMost(CC_FILE.size))
                }
                sector0Block0 != null -> {
                    sector0Block0.copyOfRange(0, le.coerceAtMost(sector0Block0.size))
                }
                else -> {
                    uidBytes.copyOfRange(0, le.coerceAtMost(uidBytes.size))
                }
            }

            val response = payload + SW_SUCCESS
            EmulationManager.logInteraction(
                cmdHex,
                response.toHex(" "),
                "READ BINARY (offset: $offset, len: ${payload.size}): Отправлены данные дампа"
            )
            return response
        }

        // 5. READ SECTOR / CUSTOM COMMAND
        // E.g. CLA=FF, INS=B0 or proprietary СКУД reader ping
        val block0 = activeDump?.sectors?.firstOrNull()?.blocks?.firstOrNull() ?: uidBytes
        val fallbackResponse = block0 + SW_SUCCESS
        EmulationManager.logInteraction(
            cmdHex,
            fallbackResponse.toHex(" "),
            "Запрос данных считывателя (Ответ с блоком дампа)"
        )
        return fallbackResponse
    }
}
