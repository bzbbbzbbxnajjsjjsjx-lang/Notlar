package com.vardiya.notlar.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts what Notlar stores on the device with an AES-256-GCM key that lives in the Android
 * Keystore: the key never leaves secure hardware where the phone has it, and it cannot be copied off.
 */
class KeystoreCipher {

    private val key: SecretKey by lazy { existingKey() ?: newKey() }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray())
        return PREFIX + Base64.getEncoder().encodeToString(sealed)
    }

    /** The text inside [stored]; text written before encryption existed is returned as it is. */
    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        val sealed = Base64.getDecoder().decode(stored.removePrefix(PREFIX))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        return String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES))
    }

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    private fun existingKey(): SecretKey? =
        (KeyStore.getInstance(KEYSTORE).apply { load(null) }.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun newKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
        init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "notlar-data"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "enc1:"
        const val KEY_BITS = 256
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
