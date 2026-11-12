package io.github.steven_stu77.lazee.core.backup

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException : Exception("Wrong backup password")
class NotABackupException : Exception("Not a Lazee backup file")

/**
 * .lgb file = "LAZEE" + version byte + 16-byte salt + 12-byte IV + AES-256-GCM(zip).
 * The key comes from the user's password via PBKDF2-HMAC-SHA256 (spec §5.13).
 */
object BackupCrypto {
    private val MAGIC = "LAZEE".toByteArray()
    const val VERSION: Byte = 1
    const val ITERATIONS = 210_000
    private const val SALT = 16
    private const val IV = 12

    private fun key(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plain: ByteArray, password: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT).also(random::nextBytes)
        val iv = ByteArray(IV).also(random::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        val header = MAGIC + byteArrayOf(VERSION)
        c.updateAAD(header) // the header is authenticated too
        return ByteArrayOutputStream().apply { write(header); write(salt); write(iv); write(c.doFinal(plain)) }.toByteArray()
    }

    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        val headerLen = MAGIC.size + 1
        if (file.size < headerLen + SALT + IV + 16 || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw NotABackupException()
        if (file[MAGIC.size] != VERSION) throw NotABackupException()
        val salt = file.copyOfRange(headerLen, headerLen + SALT)
        val iv = file.copyOfRange(headerLen + SALT, headerLen + SALT + IV)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        c.updateAAD(file.copyOfRange(0, headerLen))
        return try {
            c.doFinal(file, headerLen + SALT + IV, file.size - headerLen - SALT - IV)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }
}
