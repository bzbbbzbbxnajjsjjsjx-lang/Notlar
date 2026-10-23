package com.vardiya.notlar

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.Lifecycle
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
    private var biometricsReady by mutableStateOf(false)
    private var deviceLockReady by mutableStateOf(false)

    /**
     * Created once in onCreate, as BiometricPrompt requires, so a prompt shown before a rotation
     * still reports to the recreated activity.
     */
    private lateinit var prompt: BiometricPrompt

    /** The prompt on screen is the "PIN'imi unuttum" one, which ends with a hint to set a new PIN. */
    private var recovering = false

    /** What a fingerprint opens; null is the app itself (also after a rotation drops the request). */
    private var onBiometricSuccess: (() -> Unit)? = null

    /** Before Android 11 the screen lock alone cannot be asked through BiometricPrompt. */
    private val confirmDeviceLock = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) openedByDeviceLock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    when {
                        recovering -> openedByDeviceLock()
                        else -> onBiometricSuccess?.invoke() ?: lockViewModel.openedBySystem()
                    }
                    recovering = false
                    onBiometricSuccess = null
                    promptClosed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    recovering = false
                    onBiometricSuccess = null
                    promptClosed()
                }
            },
        )
        // After a configuration change the intent has already been handled.
        if (savedInstanceState == null) launchAction = intent.toLaunchAction()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val gate by lockViewModel.gate.collectAsStateWithLifecycle()
            val lock by lockViewModel.lock.collectAsStateWithLifecycle()
            val locking = lock?.hasPin == true || lock?.unreadable == true
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
            DisposableEffect(settings.hideContent, locking) {
                applyWindowPrivacy(hideContent = settings.hideContent, locking = locking)
                onDispose {}
            }
            NotlarTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                when (gate) {
                    LockGate.LOADING -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {}
                    LockGate.LOCKED -> LockScreen(
                        onPin = { lockViewModel.tryPin(it) },
                        biometricAvailable = settings.biometricUnlock && biometricsReady,
                        onBiometric = { askBiometric() },
                        onForgetPin = if (deviceLockReady) ({ askDeviceLock(settings.biometricUnlock) }) else null,
                    )
                    LockGate.OPEN -> NotlarApp(
                        viewModel = viewModel,
                        lockViewModel = lockViewModel,
                        biometricAvailable = biometricsReady,
                        onBiometric = ::askBiometric,
                        launchAction = launchAction,
                        onLaunchActionConsume = { launchAction = null },
                    )
                }
            }
        }
    }

    /**
     * "Ekranı gizle" blocks screenshots too; with the lock on, the recent-apps preview is always
     * blank, so notes never show while the app is locked away.
     */
    private fun applyWindowPrivacy(hideContent: Boolean, locking: Boolean) {
        val recentsOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (recentsOnly) setRecentsScreenshotEnabled(!locking)
        if (hideContent || (locking && !recentsOnly)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onStart() {
        super.onStart()
        lockViewModel.onForeground()
    }

    override fun onResume() {
        super.onResume()
        lockViewModel.onResume()
        // Enrolled fingerprints or a screen lock can change while the app is away.
        val manager = BiometricManager.from(this)
        biometricsReady = manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        deviceLockReady = getSystemService(KeyguardManager::class.java).isDeviceSecure
    }

    override fun onStop() {
        super.onStop()
        // A rotation or theme change stops the activity too, but that is not time away.
        if (!isChangingConfigurations) lockViewModel.onBackground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchAction()?.let { launchAction = it }
    }

    /**
     * Some phones show the system prompt as a screen of its own, which stops this activity. That is
     * not time away: without the allowance the lock closes again behind the prompt and asks anew.
     */
    private fun askBiometric(title: String = "Notlar'ı aç", onSuccess: (() -> Unit)? = null) {
        recovering = false
        onBiometricSuccess = onSuccess
        lockViewModel.stayOpenThroughNextTrip()
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Parmak izi veya yüz ile")
            .setNegativeButtonText("PIN kullan")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    /**
     * A forgotten PIN: the phone's own screen lock proves the owner, so the app opens without
     * erasing anything and a new PIN can be set. Fingerprint or face count only if the user allowed
     * them for Notlar; otherwise it is the pattern, PIN or password alone.
     */
    private fun askDeviceLock(allowBiometrics: Boolean) {
        val title = "PIN'imi unuttum"
        val subtitle = "Telefonunun ekran kilidiyle doğrula"
        recovering = true
        lockViewModel.stayOpenThroughNextTrip()
        when {
            allowBiometrics -> prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    .build(),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                    .build(),
            )
            else -> {
                @Suppress("DEPRECATION")
                val intent = getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent != null) confirmDeviceLock.launch(intent)
            }
        }
    }

    /** Where the prompt did not leave the screen, its allowance must not outlive it. */
    private fun promptClosed() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) lockViewModel.onResume()
    }

    private fun openedByDeviceLock() {
        lockViewModel.openedBySystem()
        Toast.makeText(applicationContext, "Yeni PIN'i Ayarlar > Gizlilik'ten belirle", Toast.LENGTH_LONG).show()
    }

    private fun Intent.toLaunchAction(): LaunchAction? = when (action) {
        ACTION_NEW_NOTE -> LaunchAction.NewNote
        ACTION_NEW_CHECKLIST -> LaunchAction.NewChecklist
        ACTION_OPEN_NOTE -> getLongExtra(EXTRA_NOTE_ID, 0L).takeIf { it != 0L }?.let(LaunchAction::Open)
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?.let { LaunchAction.Shared(it, getStringExtra(Intent.EXTRA_SUBJECT)) }
        else -> null
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.vardiya.notlar.NEW_NOTE"
        const val ACTION_NEW_CHECKLIST = "com.vardiya.notlar.NEW_CHECKLIST"
        private const val ACTION_OPEN_NOTE = "com.vardiya.notlar.OPEN_NOTE"
        private const val EXTRA_NOTE_ID = "noteId"

        /** Opens the app as the launcher icon does. */
        fun launchIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Opens the note [noteId] (after the lock, if it is on); used by reminders and widgets. */
        fun openNoteIntent(context: Context, noteId: Long): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_NOTE)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** A new note or to-do list, like the launcher shortcuts; used by the quick widget. */
        fun newNoteIntent(context: Context, checklist: Boolean): Intent = Intent(context, MainActivity::class.java)
            .setAction(if (checklist) ACTION_NEW_CHECKLIST else ACTION_NEW_NOTE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
