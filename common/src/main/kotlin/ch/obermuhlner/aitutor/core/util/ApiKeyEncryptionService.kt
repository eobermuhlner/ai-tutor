package ch.obermuhlner.aitutor.core.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service for encrypting and decrypting API keys using AES-256-GCM.
 *
 * The master encryption key must be provided via the ENCRYPTION_KEY environment variable.
 * Format: Base64-encoded 32-byte (256-bit) key
 *
 * To generate a new key, run:
 * ```bash
 * openssl rand -base64 32
 * ```
 */
@Service
class ApiKeyEncryptionService(
    @Value("\${encryption.key:#{null}}")
    private val encryptionKeyBase64: String?
) {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256 // AES-256
        private const val IV_SIZE = 12 // 96 bits for GCM
        private const val TAG_SIZE = 128 // 128-bit authentication tag
    }

    private val secretKey: SecretKey by lazy {
        if (encryptionKeyBase64.isNullOrBlank()) {
            throw IllegalStateException(
                "Encryption key not configured. Set ENCRYPTION_KEY environment variable or encryption.key property."
            )
        }

        try {
            val keyBytes = Base64.getDecoder().decode(encryptionKeyBase64)
            if (keyBytes.size != 32) {
                throw IllegalArgumentException("Encryption key must be 32 bytes (256 bits)")
            }
            SecretKeySpec(keyBytes, "AES")
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid encryption key format. Expected Base64-encoded 32-byte key.", e)
        }
    }

    /**
     * Encrypts a plain text API key.
     *
     * @param plainText The API key to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext + auth tag)
     * @throws IllegalArgumentException if plainText is blank
     * @throws EncryptionException if encryption fails
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) {
            throw IllegalArgumentException("Cannot encrypt blank text")
        }

        try {
            // Generate random IV
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Encrypt
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV + ciphertext + tag
            val combined = iv + cipherText

            // Return as Base64
            return Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt API key", e)
        }
    }

    /**
     * Decrypts an encrypted API key.
     *
     * @param encryptedText Base64-encoded encrypted data
     * @return The decrypted plain text API key
     * @throws IllegalArgumentException if encryptedText is blank
     * @throws EncryptionException if decryption fails
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) {
            throw IllegalArgumentException("Cannot decrypt blank text")
        }

        try {
            // Decode from Base64
            val combined = Base64.getDecoder().decode(encryptedText)

            if (combined.size < IV_SIZE) {
                throw IllegalArgumentException("Invalid encrypted data: too short")
            }

            // Extract IV and ciphertext
            val iv = combined.copyOfRange(0, IV_SIZE)
            val cipherText = combined.copyOfRange(IV_SIZE, combined.size)

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // Decrypt
            val plainBytes = cipher.doFinal(cipherText)

            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt API key", e)
        }
    }

    /**
     * Generates a new random AES-256 encryption key and returns it as Base64.
     * This is a utility method for key generation, not used in normal operation.
     */
    fun generateNewKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        val key = keyGen.generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }
}

/**
 * Exception thrown when encryption or decryption operations fail.
 */
class EncryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)