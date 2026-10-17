package com.vardiya.notlar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.LockState
import com.vardiya.notlar.domain.Security
import com.vardiya.notlar.domain.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the app shows over everything else. */
enum class LockGate {
    /** The lock settings are still loading: show nothing, so notes never flash before the lock. */
    LOADING,
    LOCKED,
    OPEN,
}

/** The answer to a PIN typed on the lock screen. */
sealed interface UnlockResult {
    data object Opened : UnlockResult
    data class Wrong(val triesLeft: Int, val lockedOutUntil: Long) : UnlockResult
    data class Waiting(val until: Long) : UnlockResult
}

/** The app lock: the PIN, the wait after wrong tries, and locking again after time away. */
class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(application)
    private val unlocked = MutableStateFlow(false)
    private var backgroundAt: Long? = null
    private var skipNextRelock = false

    val lock: StateFlow<LockState?> = repository.lock.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val settings: StateFlow<Settings?> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gate: StateFlow<LockGate> = combine(settings, lock, unlocked) { settings, lock, unlocked ->
        when {
            settings == null || lock == null -> LockGate.LOADING
            settings.lockEnabled && lock.hasPin && !unlocked -> LockGate.LOCKED
            else -> LockGate.OPEN
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LockGate.LOADING)

    init {
        // Values from before encryption are rewritten encrypted.
        viewModelScope.launch { repository.encryptLegacyValues() }
    }

    fun onBackground(nowMillis: Long = now()) {
        backgroundAt = nowMillis
    }

    fun onForeground(nowMillis: Long = now()) {
        val timeout = settings.value?.lockTimeout ?: return
        if (!skipNextRelock && Security.shouldRelock(backgroundAt, nowMillis, timeout)) unlocked.value = false
        skipNextRelock = false
        backgroundAt = null
    }

    /**
     * The app is about to open the share sheet or a file picker itself; coming back from it is not
     * time away, so it does not lock again.
     */
    fun stayOpenThroughNextTrip() {
        skipNextRelock = true
    }

    suspend fun tryPin(pin: String, nowMillis: Long = now()): UnlockResult {
        val state = repository.lock.first()
        if (nowMillis < state.lockedOutUntil) return UnlockResult.Waiting(state.lockedOutUntil)
        val correct = Security.matches(state, pin)
        val next = Security.afterTry(state, correct, nowMillis)
        repository.editLock { next }
        if (correct) unlocked.value = true
        return if (correct) UnlockResult.Opened else UnlockResult.Wrong(Security.triesLeft(next), next.lockedOutUntil)
    }

    /** The system confirmed the owner with a fingerprint, face or the device's own lock. */
    fun openedByBiometric() {
        unlocked.value = true
        viewModelScope.launch { repository.editLock { it.copy(failedTries = 0, lockedOutUntil = 0) } }
    }

    /** Turns the lock on with [pin], or changes the PIN. */
    fun setPin(pin: String) {
        require(Security.isValidPin(pin))
        unlocked.value = true
        viewModelScope.launch {
            repository.editLock { Security.withPin(pin) }
            repository.saveSettings(repository.settings.first().copy(lockEnabled = true))
        }
    }

    fun disableLock() {
        viewModelScope.launch {
            repository.editLock { LockState() }
            repository.saveSettings(repository.settings.first().copy(lockEnabled = false, biometricUnlock = false))
        }
    }

    private fun now() = System.currentTimeMillis()
}
