package io.github.steven_stu77.lazee.core.security

import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import java.security.SecureRandom

/** Random 32-byte SQLCipher key, created once and stored wrapped by the Keystore. */
object DbPassphrase {
    suspend fun get(settings: SettingsRepository): ByteArray {
        settings.dbPassphraseEncrypted()?.let { return KeystoreCipher.decrypt(it) }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        settings.setDbPassphraseEncrypted(KeystoreCipher.encrypt(fresh))
        return fresh
    }
}
