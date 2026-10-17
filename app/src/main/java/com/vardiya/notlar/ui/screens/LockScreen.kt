package com.vardiya.notlar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.vardiya.notlar.domain.Security
import com.vardiya.notlar.ui.UnlockResult
import com.vardiya.notlar.ui.components.MorphShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SECOND_MILLIS = 1_000L
private const val SHAKE_MILLIS = 400
private val KEY_SIZE = 76.dp

/**
 * The lock over the whole app: a PIN pad, and the fingerprint/face button when that is turned on.
 * Wrong PINs shake the dots; after too many the pad waits with a countdown.
 */
@Composable
fun LockScreen(
    onPin: suspend (String) -> UnlockResult,
    modifier: Modifier = Modifier,
    biometricAvailable: Boolean = false,
    onBiometric: () -> Unit = {},
    onForgetPin: (() -> Unit)? = null,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var waitUntil by rememberSaveable { mutableLongStateOf(0L) }
    var waitStarted by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(nowMillis()) }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val shakeDistance = with(LocalDensity.current) { 12.dp.toPx() }

    val currentOnBiometric by rememberUpdatedState(onBiometric)
    val clock by rememberUpdatedState(nowMillis)
    // Ask for the fingerprint or face right away when it is turned on.
    LaunchedEffect(biometricAvailable) { if (biometricAvailable) currentOnBiometric() }
    LaunchedEffect(waitUntil) {
        while (clock() < waitUntil) {
            now = clock()
            delay(SECOND_MILLIS / 4)
        }
        now = clock()
    }
    val waiting = now < waitUntil

    fun submit() {
        val typed = pin
        scope.launch {
            when (val result = onPin(typed)) {
                UnlockResult.Opened -> haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                is UnlockResult.Wrong -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    pin = ""
                    if (result.lockedOutUntil > nowMillis()) {
                        waitStarted = nowMillis()
                        waitUntil = result.lockedOutUntil
                        message = null
                    } else {
                        message = "Yanlış PIN · ${result.triesLeft} hak kaldı"
                    }
                    shake.animateTo(
                        0f,
                        keyframes {
                            durationMillis = SHAKE_MILLIS
                            -shakeDistance at 50
                            shakeDistance at 130
                            -shakeDistance / 2 at 210
                            shakeDistance / 2 at 290
                        },
                    )
                }
                is UnlockResult.Waiting -> {
                    pin = ""
                    waitStarted = nowMillis()
                    waitUntil = result.until
                }
            }
        }
    }

    PinPad(
        title = "PIN'ini gir",
        pin = pin,
        maxLength = Security.PIN_MAX_LENGTH,
        enabled = !waiting,
        onDigit = {
            message = null
            pin += it
        },
        onBackspace = { pin = pin.dropLast(1) },
        onDone = if (pin.length >= Security.PIN_MIN_LENGTH) ::submit else null,
        shakeOffset = { shake.value },
        modifier = modifier,
        biometric = if (biometricAvailable) onBiometric else null,
        secondary = onForgetPin?.let { "PIN'imi unuttum" to it },
    ) {
        when {
            waiting -> {
                val total = (waitUntil - waitStarted).coerceAtLeast(1)
                val left = (waitUntil - now).coerceAtLeast(0)
                Text(
                    "${(left + SECOND_MILLIS - 1) / SECOND_MILLIS} saniye bekle",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                LinearWavyProgressIndicator(
                    progress = { 1f - left.toFloat() / total },
                    modifier = Modifier
                        .width(160.dp)
                        .padding(top = 8.dp),
                )
            }
            message != null -> Text(
                message.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** Choosing a PIN: typed twice, the second time to confirm it. */
@Composable
fun PinSetupScreen(onPinConfirm: (String) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var pin by rememberSaveable { mutableStateOf("") }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    PinPad(
        title = if (first == null) "Yeni PIN" else "PIN'i tekrar gir",
        pin = pin,
        maxLength = Security.PIN_MAX_LENGTH,
        enabled = true,
        onDigit = {
            mismatch = false
            pin += it
        },
        onBackspace = { pin = pin.dropLast(1) },
        onDone = if (Security.isValidPin(pin)) {
            {
                when (first) {
                    null -> {
                        first = pin
                        pin = ""
                    }
                    pin -> onPinConfirm(pin)
                    else -> {
                        first = null
                        pin = ""
                        mismatch = true
                    }
                }
            }
        } else {
            null
        },
        shakeOffset = { 0f },
        modifier = modifier,
        onCancel = onCancel,
    ) {
        Text(
            if (mismatch) "PIN'ler eşleşmedi, baştan gir" else "${Security.PIN_MIN_LENGTH}–${Security.PIN_MAX_LENGTH} rakam",
            color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun PinPad(
    title: String,
    pin: String,
    maxLength: Int,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onDone: (() -> Unit)?,
    shakeOffset: () -> Float,
    modifier: Modifier = Modifier,
    biometric: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    secondary: Pair<String, () -> Unit>? = null,
    status: @Composable () -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(24.dp))
            PinDots(
                length = pin.length,
                modifier = Modifier.graphicsLayer { translationX = shakeOffset() },
            )
            Column(
                Modifier
                    .height(56.dp)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { status() }
            Spacer(Modifier.weight(1f))
            val rows = listOf("123", "456", "789")
            rows.forEach { row ->
                KeyRow { row.forEach { digit -> DigitKey(digit, enabled && pin.length < maxLength) { onDigit(digit) } } }
            }
            KeyRow {
                when {
                    biometric != null -> IconKey(Icons.Filled.Fingerprint, "Parmak izi veya yüz ile aç", enabled, biometric)
                    else -> Spacer(Modifier.size(KEY_SIZE))
                }
                DigitKey('0', enabled && pin.length < maxLength) { onDigit('0') }
                IconKey(Icons.AutoMirrored.Filled.Backspace, "Son rakamı sil", enabled && pin.isNotEmpty(), onBackspace)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                when {
                    onCancel != null -> TextButton(onClick = onCancel) { Text("Vazgeç") }
                    secondary != null -> TextButton(onClick = secondary.second) { Text(secondary.first) }
                    else -> Spacer(Modifier)
                }
                TextButton(onClick = { onDone?.invoke() }, enabled = onDone != null && enabled) { Text("Tamam") }
            }
        }
    }
}

/** One dot per typed digit, each a circle that blooms into a cookie as it fills. */
@Composable
private fun PinDots(length: Int, modifier: Modifier = Modifier) {
    val slots = maxOf(Security.PIN_MIN_LENGTH, length)
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie6Sided) }
    Row(
        modifier.semantics { contentDescription = "$length rakam girildi" },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(slots) { index ->
            val filled = index < length
            val progress by animateFloatAsState(
                targetValue = if (filled) 1f else 0f,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "pinDot",
            )
            Box(
                Modifier
                    .size(16.dp + 6.dp * progress)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        MorphShape(morph, progress),
                    ),
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(
        Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick()
        },
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.size(KEY_SIZE),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IconKey(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes(), modifier = Modifier.size(KEY_SIZE)) {
        Icon(icon, contentDescription = description, Modifier.size(28.dp))
    }
}
