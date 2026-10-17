package com.vardiya.notlar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.LockState
import com.vardiya.notlar.domain.Security
import com.vardiya.notlar.domain.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the app shows over everything else. */
enum class LockGate {
    /** The lock is still loading: show nothing, so notes never flash before the lock. */
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

/**
 * The app lock: the PIN, the wait after wrong tries, and locking again after time away.
 *
 * The lock is on exactly while a PIN is stored, or while the stored lock cannot be read: it fails
 * closed. Waits use the wall clock, so moving the phone's clock forward shortens them; the PIN hash
 * itself (PBKDF2, 120 000 rounds) is what makes guessing slow.
 */
class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository.get(application)
    private val unlocked = MutableStateFlow(false)
    private val tries = Mutex()
    private var backgroundAt: Long? = null
    private var skipRelockUntil = 0L

    val lock: StateFlow<LockState?> = repository.lock.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val settings: StateFlow<Settings?> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gate: StateFlow<LockGate> = combine(lock, unlocked) { lock, unlocked ->
        when {
            lock == null -> LockGate.LOADING
            (lock.hasPin || lock.unreadable) && !unlocked -> LockGate.LOCKED
            else -> LockGate.OPEN
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LockGate.LOADING)

    init {
        // Values from before encryption are rewritten encrypted.
        viewModelScope.launch { repository.encryptLegacyValues() }
    }

    /** The app left the screen for real (not a rotation). */
    fun onBackground(nowMillis: Long = now()) {
        backgroundAt = nowMillis
    }

    fun onForeground(nowMillis: Long = now()) {
        val timeout = settings.value?.lockTimeout ?: return
        val away = backgroundAt
        // An own trip counts only if it left within the announced minute and came back within ten.
        val ownTrip = away != null && away <= skipRelockUntil && nowMillis - away < OWN_TRIP_MAX_MILLIS
        if (!ownTrip && Security.shouldRelock(away, nowMillis, timeout)) unlocked.value = false
        backgroundAt = null
    }

    /** Back on screen: whatever own trip was announced is over, used or not. */
    fun onResume() {
        skipRelockUntil = 0L
    }

    /**
     * The app is about to open the share sheet or a file picker itself. Leaving for it within the
     * next minute is not time away; the allowance ends as soon as the app is back on screen.
     */
    fun stayOpenThroughNextTrip(nowMillis: Long = now()) {
        skipRelockUntil = nowMillis + OWN_TRIP_WINDOW_MILLIS
    }

    /** Checks [pin]; tries are handled one at a time, off the main thread, and counted atomically. */
    suspend fun tryPin(pin: String, nowMillis: Long = now()): UnlockResult = tries.withLock {
        val state = repository.lock.first()
        if (nowMillis < state.lockedOutUntil) return@withLock UnlockResult.Waiting(state.lockedOutUntil)
        val correct = withContext(Dispatchers.Default) { Security.matches(state, pin) }
        repository.editLock { Security.afterTry(it, correct, nowMillis) }
        if (correct) {
            unlocked.value = true
            UnlockResult.Opened
        } else {
            val next = Security.afterTry(state, correct = false, nowMillis = nowMillis)
            UnlockResult.Wrong(Security.triesLeft(next), next.lockedOutUntil)
        }
    }

    /** The system confirmed the owner with a fingerprint, face or the device's own lock. */
    fun openedBySystem() {
        unlocked.value = true
        viewModelScope.launch { repository.editLock { it.copy(failedTries = 0, lockedOutUntil = 0) } }
    }

    /** Turns the lock on with [pin], or changes the PIN; returns once both are stored. */
    suspend fun setPin(pin: String) {
        require(Security.isValidPin(pin))
        val state = withContext(Dispatchers.Default) { Security.withPin(pin) }
        repository.editLock { state }
        repository.updateSettings { it.copy(lockEnabled = true) }
        unlocked.value = true
    }

    /** Turns the lock off; the caller has just checked the current PIN. */
    suspend fun disableLock() {
        repository.editLock { LockState() }
        repository.updateSettings { it.copy(lockEnabled = false, biometricUnlock = false) }
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val OWN_TRIP_WINDOW_MILLIS = 60_000L
        const val OWN_TRIP_MAX_MILLIS = 600_000L
    }
}
