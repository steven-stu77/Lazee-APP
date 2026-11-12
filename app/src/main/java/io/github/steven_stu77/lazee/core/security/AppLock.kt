package io.github.steven_stu77.lazee.core.security

import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spec §5.15: when enabled, coming back after more than 30 seconds (or a fresh start) asks for
 * fingerprint or the phone's PIN. Registered on the process lifecycle by MainActivity.
 */
@Singleton
class AppLock @Inject constructor() : DefaultLifecycleObserver {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Kept in sync with settings by MainActivity. */
    @Volatile var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) _locked.value = false
        }

    private var backgroundAt = 0L

    override fun onStop(owner: LifecycleOwner) { backgroundAt = SystemClock.elapsedRealtime() }

    override fun onStart(owner: LifecycleOwner) {
        if (enabled && backgroundAt > 0 && SystemClock.elapsedRealtime() - backgroundAt > TIMEOUT_MS) _locked.value = true
    }

    fun unlocked() { _locked.value = false }

    companion object {
        const val TIMEOUT_MS = 30_000L

        /** Fingerprint/face or the phone's own PIN. Strong+credential is only allowed together from Android 11. */
        val authenticators: Int = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

        fun canAuthenticate(activity: FragmentActivity): Boolean =
            BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

        fun prompt(activity: FragmentActivity, title: String, onSuccess: () -> Unit, onFail: () -> Unit = {}) {
            val p = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail()
            })
            p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(authenticators).build())
        }
    }
}
