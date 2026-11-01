package com.jarvis.fileserver.security

import android.content.Context
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    private val secureRandom = SecureRandom()
    private const val PBKDF2_ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256

    /**
     * Generates a cryptographically secure, URL-safe random token.
     */
    fun generateSecureToken(byteLength: Int = 18): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder(byteLength)
        for (b in bytes) {
            val idx = (b.toInt() and 0xFF) % chars.length
            sb.append(chars[idx])
        }
        return sb.toString()
    }

    /**
     * Generates a random salt for password hashing.
     */
    fun generateSalt(length: Int = 16): String {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256.
     */
    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = hexStringToByteArray(saltHex)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time password verification against timing attacks.
     */
    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        if (password.isEmpty()) return false
        return try {
            val computedHash = hashPassword(password, saltHex)
            MessageDigestEquals(computedHash, expectedHashHex)
        } catch (e: Exception) {
            false
        }
    }

    private fun MessageDigestEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        if (aBytes.size != bBytes.size) return false
        var result = 0
        for (i in aBytes.indices) {
            result = result or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return result == 0
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Obtains or creates a persistent device identity and admin auth secret.
     */
    fun getOrCreateHostDeviceSecret(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences("wormhole_security_keystore", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("host_device_id", null)
        var adminSecret = prefs.getString("host_admin_secret", null)

        if (deviceId == null || adminSecret == null) {
            deviceId = "wormhole-host-" + UUID.randomUUID().toString().take(8)
            adminSecret = generateSecureToken(32)
            prefs.edit()
                .putString("host_device_id", deviceId)
                .putString("host_admin_secret", adminSecret)
                .apply()
        }
        return Pair(deviceId, adminSecret)
    }
}
