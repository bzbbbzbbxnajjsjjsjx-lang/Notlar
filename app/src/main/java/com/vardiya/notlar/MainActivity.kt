package com.vardiya.notlar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardiya.notlar.domain.ThemeMode
import com.vardiya.notlar.ui.LaunchAction
import com.vardiya.notlar.ui.LockGate
import com.vardiya.notlar.ui.LockViewModel
import com.vardiya.notlar.ui.NotesViewModel
import com.vardiya.notlar.ui.NotlarApp
import com.vardiya.notlar.ui.screens.LockScreen
import com.vardiya.notlar.ui.theme.NotlarTheme

/** A FragmentActivity because BiometricPrompt shows itself as a fragment. */
class MainActivity : FragmentActivity() {

    private val viewModel: NotesViewModel by viewModels()
    private val lockViewModel: LockViewModel by viewModels()

    /** A shortcut or a share from another app that the UI has not handled yet. */
    private var launchAction by mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After a configuration change the intent has already been handled.
        if (savedInstanceState == null) launchAction = intent.toLaunchAction()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val gate by lockViewModel.gate.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // System bar icons follow the app's own light or dark choice, not only the device's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            // "Ekranı gizle": blank in the recent apps and no screenshots.
            DisposableEffect(settings.hideContent) {
                if (settings.hideContent) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose {}
            }
            NotlarTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                when (gate) {
                    LockGate.LOADING -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {}
                    LockGate.LOCKED -> LockScreen(
                        onPin = { lockViewModel.tryPin(it) },
                        biometricAvailable = settings.biometricUnlock && canUseBiometrics(),
                        onBiometric = ::askBiometric,
                        onForgetPin = if (canUseDeviceLock()) ::askDeviceLock else null,
                    )
                    LockGate.OPEN -> NotlarApp(
                        viewModel = viewModel,
                        lockViewModel = lockViewModel,
                        biometricAvailable = canUseBiometrics(),
                        launchAction = launchAction,
                        onLaunchActionConsume = { launchAction = null },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lockViewModel.onForeground()
    }

    override fun onStop() {
        super.onStop()
        lockViewModel.onBackground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchAction()?.let { launchAction = it }
    }

    private fun canUseBiometrics(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    private fun canUseDeviceLock(): Boolean =
        BiometricManager.from(this).canAuthenticate(DEVICE_LOCK) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * A forgotten PIN: the phone's own screen lock (pattern, PIN, password or biometrics) proves the
     * owner, so the app opens without erasing anything and a new PIN can be set in the settings.
     */
    private fun askDeviceLock() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockViewModel.openedByBiometric()
                    Toast.makeText(this@MainActivity, "Yeni PIN'i Ayarlar > Gizlilik'ten belirle", Toast.LENGTH_LONG).show()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PIN'imi unuttum")
            .setSubtitle("Telefonunun ekran kilidiyle doğrula")
            .setAllowedAuthenticators(DEVICE_LOCK)
            .build()
        prompt.authenticate(info)
    }

    private fun askBiometric() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockViewModel.openedByBiometric()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Notlar'ı aç")
            .setSubtitle("Parmak izi veya yüz ile")
            .setNegativeButtonText("PIN kullan")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    private fun Intent.toLaunchAction(): LaunchAction? = when (action) {
        ACTION_NEW_NOTE -> LaunchAction.NewNote
        ACTION_NEW_CHECKLIST -> LaunchAction.NewChecklist
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?.let { LaunchAction.Shared(it, getStringExtra(Intent.EXTRA_SUBJECT)) }
        else -> null
    }

    private companion object {
        const val ACTION_NEW_NOTE = "com.vardiya.notlar.NEW_NOTE"
        const val ACTION_NEW_CHECKLIST = "com.vardiya.notlar.NEW_CHECKLIST"

        // Weak biometrics or the screen lock: the combination every Android version accepts.
        const val DEVICE_LOCK = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
