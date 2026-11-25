package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2-HMAC-SHA256 credential hashing so user passwords and recovery PINs
 * are never stored in plaintext.
 */
object PasswordHasher {
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedSecret(val hashHex: String, val saltHex: String)

    fun hash(secret: String): HashedSecret {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(secret.toCharArray(), salt)
        return HashedSecret(hash.toHex(), salt.toHex())
    }

    fun verify(secret: String, expectedHashHex: String, saltHex: String): Boolean {
        if (expectedHashHex.isBlank() || saltHex.isBlank()) return false
        val salt = saltHex.hexToBytes() ?: return false
        val expected = expectedHashHex.hexToBytes() ?: return false
        val actual = pbkdf2(secret.toCharArray(), salt)
        return MessageDigest.isEqual(actual, expected)
    }

    fun evaluatePasswordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 6) score++
        if (password.length >= 10) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() } || password.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(1, 4)
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { idx ->
                substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
