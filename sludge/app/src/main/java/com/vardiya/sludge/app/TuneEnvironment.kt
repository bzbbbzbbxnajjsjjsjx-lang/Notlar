package com.vardiya.sludge.app

import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.Tuning
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import kotlinx.coroutines.flow.first

/** Everything the [TuneCoordinator] reads and writes, so its rules can be tested without Android. */
interface TuneEnvironment {
    /** The stored settings now (read from the store, never a default that is still loading). */
    suspend fun settings(): EqSettings

    suspend fun apply(tuning: Tuning, source: TuneSource)

    suspend fun record(entry: ListenEntry)
}

class RepoTuneEnvironment(private val repo: SettingsRepository) : TuneEnvironment {
    override suspend fun settings(): EqSettings = repo.settings.first()

    override suspend fun apply(tuning: Tuning, source: TuneSource) {
        repo.updateSettings { it.applying(tuning, source) }
    }

    override suspend fun record(entry: ListenEntry) = repo.addListen(entry)
}
