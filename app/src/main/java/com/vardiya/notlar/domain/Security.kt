package com.vardiya.notlar.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How long the app may stay in the background before it asks for the PIN again. */
@Serializable
enum class LockTimeout(val millis: Long) {
    IMMEDIATE(0),
    ONE_MINUTE(60_000),
    FIVE_MINUTES(300_000),
}

/**
 * The app lock as stored: a salted PBKDF2 hash of the PIN (never the PIN itself) and the run of
 * wrong tries that slows guessing down.
 */
@Serializable
data class LockState(
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val failedTries: Int = 0,
    val lockedOutUntil: Long = 0,
) {
    val hasPin: Boolean get() = pinHash != null && pinSalt != null
}

object Security {

    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 8

    /** Wrong tries allowed before each wait. */
    const val FREE_TRIES = 5
    private const val FIRST_WAIT_MILLIS = 30_000L
    private const val MAX_DOUBLINGS = 6

    private const val SEALED_FORMAT = 1
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()
    private val base64 = Base64.getEncoder()
    private val unbase64 = Base64.getDecoder()
    private val json = Json { ignoreUnknownKeys = true }

    fun isValidPin(pin: String): Boolean = pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all { it in '0'..'9' }

    /** A lock state that accepts [pin]: a fresh salt and its hash, with the tries reset. */
    fun withPin(pin: String): LockState {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return LockState(pinHash = base64.encodeToString(derive(pin, salt)), pinSalt = base64.encodeToString(salt))
    }

    /** Whether [pin] matches; compared in constant time. */
    fun matches(state: LockState, pin: String): Boolean {
        val hash = state.pinHash
        val salt = state.pinSalt
        return hash != null && salt != null && MessageDigest.isEqual(derive(pin, unbase64.decode(salt)), unbase64.decode(hash))
    }

    /**
     * The state after a try at [nowMillis]. A right PIN clears the count; every [FREE_TRIES]th
     * wrong one starts a wait of 30 s that doubles each time, up to about half an hour.
     */
    fun afterTry(state: LockState, correct: Boolean, nowMillis: Long): LockState {
        if (correct) return state.copy(failedTries = 0, lockedOutUntil = 0)
        val failed = state.failedTries + 1
        val wait = if (failed % FREE_TRIES == 0) {
            FIRST_WAIT_MILLIS shl (failed / FREE_TRIES - 1).coerceAtMost(MAX_DOUBLINGS)
        } else {
            0L
        }
        return state.copy(failedTries = failed, lockedOutUntil = if (wait > 0) nowMillis + wait else state.lockedOutUntil)
    }

    /** Tries left before the next wait. */
    fun triesLeft(state: LockState): Int = FREE_TRIES - state.failedTries % FREE_TRIES

    /** Whether the app should ask for the PIN again after being away since [backgroundAt]. */
    fun shouldRelock(backgroundAt: Long?, nowMillis: Long, timeout: LockTimeout): Boolean =
        backgroundAt != null && nowMillis - backgroundAt >= timeout.millis

    /** A backup sealed with [password]: PBKDF2-derived AES-256-GCM key, salt and IV kept beside the data. */
    fun sealBackup(plain: String, password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(password, salt), "AES"), GCMParameterSpec(TAG_BITS, iv))
        val data = cipher.doFinal(plain.toByteArray())
        val sealed = SealedBackup(
            notlarSealed = SEALED_FORMAT,
            salt = base64.encodeToString(salt),
            iv = base64.encodeToString(iv),
            data = base64.encodeToString(data),
        )
        return json.encodeToString(SealedBackup.serializer(), sealed)
    }

    /** Whether [raw] is a backup made by [sealBackup]. */
    fun isSealed(raw: String): Boolean = raw.trimStart().startsWith("{") && raw.contains("\"notlarSealed\"")

    /** The backup inside [raw], or null for a wrong password or a damaged file. */
    fun openBackup(raw: String, password: String): String? = runCatching {
        val sealed = json.decodeFromString(SealedBackup.serializer(), raw)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(derive(password, unbase64.decode(sealed.salt)), "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, unbase64.decode(sealed.iv)))
        String(cipher.doFinal(unbase64.decode(sealed.data)))
    }.getOrNull()

    private fun derive(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    @Serializable
    private data class SealedBackup(
        // No default: the encoder leaves defaults out, and this field is how a sealed file is recognised.
        val notlarSealed: Int,
        val salt: String,
        val iv: String,
        val data: String,
    )
}
