package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {

    @Test
    fun pinIsStoredOnlyAsASaltedHash() {
        val a = Security.withPin("2468")
        val b = Security.withPin("2468")
        assertTrue(Security.matches(a, "2468"))
        assertFalse(Security.matches(a, "2469"))
        assertFalse(a.pinHash!!.contains("2468"))
        // A fresh salt each time: the same PIN never stores the same hash.
        assertNotEquals(a.pinHash, b.pinHash)
        assertFalse(Security.matches(LockState(), "2468"))
    }

    @Test
    fun pinRules() {
        assertTrue(Security.isValidPin("0000"))
        assertTrue(Security.isValidPin("12345678"))
        assertFalse(Security.isValidPin("123"))
        assertFalse(Security.isValidPin("123456789"))
        assertFalse(Security.isValidPin("12a4"))
    }

    @Test
    fun wrongTriesStartDoublingWaits() {
        var state = Security.withPin("1111")
        repeat(4) { state = Security.afterTry(state, correct = false, nowMillis = 0) }
        assertEquals(0L, state.lockedOutUntil)
        assertEquals(1, Security.triesLeft(state))
        state = Security.afterTry(state, correct = false, nowMillis = 1_000)
        assertEquals(31_000L, state.lockedOutUntil)
        repeat(5) { state = Security.afterTry(state, correct = false, nowMillis = 100_000) }
        assertEquals(160_000L, state.lockedOutUntil)
        state = Security.afterTry(state, correct = true, nowMillis = 200_000)
        assertEquals(0, state.failedTries)
        assertEquals(0L, state.lockedOutUntil)
    }

    @Test
    fun relockAfterTheChosenTime() {
        assertFalse(Security.shouldRelock(null, 10_000, LockTimeout.IMMEDIATE))
        assertTrue(Security.shouldRelock(10_000, 10_000, LockTimeout.IMMEDIATE))
        assertFalse(Security.shouldRelock(0, 59_999, LockTimeout.ONE_MINUTE))
        assertTrue(Security.shouldRelock(0, 60_000, LockTimeout.ONE_MINUTE))
    }

    @Test
    fun sealedBackupOpensOnlyWithItsPassword() {
        val plain = """[{"id":1,"title":"Kira"}]"""
        val sealed = Security.sealBackup(plain, "gizli parola")
        assertTrue(Security.isSealed(sealed))
        assertFalse(Security.isSealed(plain))
        assertFalse(sealed.contains("Kira"))
        assertEquals(plain, Security.openBackup(sealed, "gizli parola"))
        assertNull(Security.openBackup(sealed, "yanlış"))
        assertNull(Security.openBackup("{bozuk", "gizli parola"))
    }
}
