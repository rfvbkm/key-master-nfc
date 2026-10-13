package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoVaultManager {
    private const val KEY_ALIAS = "KeyMasterMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    init {
        getOrCreateMasterKey()
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
        val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return keyEntry.secretKey
    }

    data class EncryptedResult(val encryptedBase64: String, val ivBase64: String)

    fun encrypt(plainText: String): EncryptedResult {
        return encryptBytes(plainText.toByteArray(Charsets.UTF_8))
    }

    fun encryptBytes(plainBytes: ByteArray): EncryptedResult {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)
        return EncryptedResult(
            encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(encryptedBase64: String, ivBase64: String): String {
        val bytes = decryptBytes(encryptedBase64, ivBase64)
        return String(bytes, Charsets.UTF_8)
    }

    fun decryptBytes(encryptedBase64: String, ivBase64: String): ByteArray {
        val secretKey = getOrCreateMasterKey()
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encrypted)
    }
}
