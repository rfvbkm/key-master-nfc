package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardsDao {
    @Query("SELECT * FROM saved_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM saved_cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM saved_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    // Dictionary Keys
    @Query("SELECT * FROM dictionary_keys ORDER BY isCustom DESC, id ASC")
    fun getAllDictionaryKeys(): Flow<List<DictionaryKeyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKey(key: DictionaryKeyEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeys(keys: List<DictionaryKeyEntity>)

    @Delete
    suspend fun deleteKey(key: DictionaryKeyEntity)

    @Query("SELECT COUNT(*) FROM dictionary_keys")
    suspend fun getKeysCount(): Int
}
