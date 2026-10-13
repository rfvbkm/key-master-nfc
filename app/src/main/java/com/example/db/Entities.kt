package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tagType: String,
    val uidHex: String,
    val category: String = "Общие",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val encryptedPayloadBase64: String,
    val ivBase64: String
)

@Entity(tableName = "dictionary_keys")
data class DictionaryKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyHex: String,
    val description: String = "",
    val isCustom: Boolean = true
)
