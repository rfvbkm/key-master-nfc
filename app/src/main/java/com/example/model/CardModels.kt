package com.example.model

enum class CardType(val displayName: String, val memorySize: String) {
    MIFARE_CLASSIC_1K("MIFARE Classic 1K", "1024 bytes (16 sectors)"),
    MIFARE_CLASSIC_4K("MIFARE Classic 4K", "4096 bytes (40 sectors)"),
    MIFARE_ULTRALIGHT("MIFARE Ultralight", "64 bytes (16 pages)"),
    NTAG213("NTAG213", "180 bytes (45 pages)"),
    NTAG215("NTAG215", "540 bytes (135 pages)"),
    NTAG216("NTAG216", "924 bytes (231 pages)"),
    ISO_DEP("ISO 14443-4 (ISO-DEP / DESFire)", "Variable"),
    NDEF_TAG("Standard NDEF Tag", "Variable"),
    UNKNOWN("Неизвестный тип чипа", "Unknown")
}

data class SectorData(
    val sectorIndex: Int,
    val blocks: List<ByteArray>,
    val keyA: String? = null,
    val keyB: String? = null,
    val accessBits: String? = null,
    val isReadSuccess: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SectorData
        if (sectorIndex != other.sectorIndex) return false
        if (isReadSuccess != other.isReadSuccess) return false
        if (keyA != other.keyA) return false
        if (keyB != other.keyB) return false
        if (accessBits != other.accessBits) return false
        if (blocks.size != other.blocks.size) return false
        for (i in blocks.indices) {
            if (!blocks[i].contentEquals(other.blocks[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = sectorIndex
        result = 31 * result + isReadSuccess.hashCode()
        result = 31 * result + (keyA?.hashCode() ?: 0)
        result = 31 * result + (keyB?.hashCode() ?: 0)
        return result
    }
}

data class PageData(
    val pageNumber: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PageData
        return pageNumber == other.pageNumber && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return 31 * pageNumber + data.contentHashCode()
    }
}

data class CardDump(
    val uid: String,
    val atqa: String = "",
    val sak: String = "",
    val cardType: CardType = CardType.UNKNOWN,
    val sectors: List<SectorData> = emptyList(),
    val pages: List<PageData> = emptyList(),
    val ndefPayload: String? = null,
    val rawTechs: List<String> = emptyList(),
    val readTimestamp: Long = System.currentTimeMillis()
)

// Extension functions for HEX conversions
fun ByteArray.toHex(separator: String = ""): String =
    joinToString(separator) { "%02X".format(it) }

fun String.hexToByteArray(): ByteArray {
    val clean = this.replace(" ", "").replace(":", "").uppercase()
    if (clean.length % 2 != 0) return ByteArray(0)
    val result = ByteArray(clean.length / 2)
    for (i in result.indices) {
        val index = i * 2
        result[i] = clean.substring(index, index + 2).toInt(16).toByte()
    }
    return result
}
