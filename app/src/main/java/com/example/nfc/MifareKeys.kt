package com.example.nfc

import com.example.model.hexToByteArray

object MifareKeys {
    val DEFAULT_KEYS_HEX = listOf(
        "FFFFFFFFFFFF",
        "A0A1A2A3A4A5",
        "D3F7D3F7D3F7",
        "000000000000",
        "B0B1B2B3B4B5",
        "A0B0C0D0E0F0",
        "4D3A99C351DD",
        "1A982C7E459A",
        "533CB6C723F6",
        "8FD0A4F256E9",
        "A1B2C3D4E5F6",
        "123456789ABC",
        "E1E2E3E4E5E6",
        "ABCDEF123456",
        "714C5C886E97",
        "587EE2A163B0",
        "A0478CEEBADF",
        "533CB6C723F8"
    )

    val DEFAULT_KEYS_BYTES: List<ByteArray> by lazy {
        DEFAULT_KEYS_HEX.map { it.hexToByteArray() }
    }

    data class AccessCondition(
        val block0Permission: String,
        val block1Permission: String,
        val block2Permission: String,
        val trailerPermission: String,
        val isDefaultOrInsecure: Boolean
    )

    /**
     * Decodes 3 access bytes (bytes 6, 7, 8 of sector trailer).
     */
    fun decodeAccessBits(accessBytes: ByteArray): AccessCondition {
        if (accessBytes.size < 3) {
            return AccessCondition(
                "Неизвестно", "Неизвестно", "Неизвестно", "Неизвестно", false
            )
        }
        val b6 = accessBytes[0].toInt() and 0xFF
        val b7 = accessBytes[1].toInt() and 0xFF
        val b8 = accessBytes[2].toInt() and 0xFF

        // C1, C2, C3 for blocks 0, 1, 2, 3
        val c1_0 = (b7 shr 4) and 1
        val c1_1 = (b7 shr 5) and 1
        val c1_2 = (b7 shr 6) and 1
        val c1_3 = (b7 shr 7) and 1

        val c2_0 = (b8 shr 0) and 1
        val c2_1 = (b8 shr 1) and 1
        val c2_2 = (b8 shr 2) and 1
        val c2_3 = (b8 shr 3) and 1

        val c3_0 = (b8 shr 4) and 1
        val c3_1 = (b8 shr 5) and 1
        val c3_2 = (b8 shr 6) and 1
        val c3_3 = (b8 shr 7) and 1

        val isDefault = (b6 == 0xFF && b7 == 0x07 && b8 == 0x80)

        fun describeDataBlock(c1: Int, c2: Int, c3: Int): String {
            return when {
                c1 == 0 && c2 == 0 && c3 == 0 -> "Чтение: A|B, Запись: A|B (Транспортный режим)"
                c1 == 0 && c2 == 1 && c3 == 0 -> "Чтение: A|B, Запись: Никогда (Только для чтения)"
                c1 == 1 && c2 == 0 && c3 == 0 -> "Чтение: A|B, Запись: B"
                c1 == 1 && c2 == 1 && c3 == 0 -> "Чтение: A|B, Запись: B (Значение)"
                c1 == 0 && c2 == 0 && c3 == 1 -> "Чтение: A|B, Запись: Никогда"
                c1 == 0 && c2 == 1 && c3 == 1 -> "Чтение: B, Запись: B"
                c1 == 1 && c2 == 0 && c3 == 1 -> "Чтение: B, Запись: Никогда"
                else -> "Доступ закрыт"
            }
        }

        fun describeTrailer(c1: Int, c2: Int, c3: Int): String {
            return when {
                c1 == 0 && c2 == 0 && c3 == 0 -> "KeyA/B запись: KeyA. Свободный доступ (Заводской FF 07 80)"
                c1 == 0 && c2 == 1 && c3 == 0 -> "KeyA запись: Никогда; KeyB запись: Никогда"
                c1 == 1 && c2 == 0 && c3 == 0 -> "KeyA запись: KeyB; KeyB запись: KeyB"
                c1 == 1 && c2 == 1 && c3 == 0 -> "KeyA запись: Никогда; KeyB чтение: KeyB"
                c1 == 0 && c2 == 0 && c3 == 1 -> "KeyA запись: KeyA; KeyB запись: KeyA"
                c1 == 0 && c2 == 1 && c3 == 1 -> "KeyA запись: KeyB; KeyB запись: KeyB"
                c1 == 1 && c2 == 0 && c3 == 1 -> "KeyA запись: Никогда; KeyB запись: KeyB"
                else -> "Трейлер защищен от перезаписи"
            }
        }

        return AccessCondition(
            block0Permission = describeDataBlock(c1_0, c2_0, c3_0),
            block1Permission = describeDataBlock(c1_1, c2_1, c3_1),
            block2Permission = describeDataBlock(c1_2, c2_2, c3_2),
            trailerPermission = describeTrailer(c1_3, c2_3, c3_3),
            isDefaultOrInsecure = isDefault
        )
    }
}
