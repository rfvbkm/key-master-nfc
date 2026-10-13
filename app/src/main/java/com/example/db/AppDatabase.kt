package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nfc.MifareKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CardEntity::class, DictionaryKeyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardsDao(): CardsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keymaster_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default dictionary keys on first run
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).cardsDao()
                                val defaultEntities = MifareKeys.DEFAULT_KEYS_HEX.mapIndexed { idx, keyHex ->
                                    val desc = when (keyHex) {
                                        "FFFFFFFFFFFF" -> "Заводской ключ по умолчанию (Transport Key)"
                                        "A0A1A2A3A4A5" -> "Ключ NFC Forum NDEF Key A"
                                        "D3F7D3F7D3F7" -> "Ключ NFC Forum NDEF Key B"
                                        "000000000000" -> "Пустой ключ (Zero Key)"
                                        "B0B1B2B3B4B5" -> "Стандартный тестовый ключ NXP"
                                        else -> "Словарный ключ #$idx"
                                    }
                                    DictionaryKeyEntity(
                                        keyHex = keyHex,
                                        description = desc,
                                        isCustom = false
                                    )
                                }
                                dao.insertKeys(defaultEntities)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
