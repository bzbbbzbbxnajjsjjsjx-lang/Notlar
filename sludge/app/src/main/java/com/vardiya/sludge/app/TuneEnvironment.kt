package com.vardiya.sludge.app

import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import kotlinx.coroutines.flow.first

/** Everything the [TuneCoordinator] reads and writes, so its rules can be tested without Android. */
interface TuneEnvironment {
    /** The stored settings now (read from the store, never a default that is still loading). */
    suspend fun settings(): EqSettings

    suspend fun aiConfig(): AiConfig

    suspend fun apply(profile: AiProfile, source: TuneSource)

    suspend fun record(entry: ListenEntry)
}

class RepoTuneEnvironment(private val repo: SettingsRepository) : TuneEnvironment {
    override suspend fun settings(): EqSettings = repo.settings.first()

    override suspend fun aiConfig(): AiConfig = repo.ai.first()

    override suspend fun apply(profile: AiProfile, source: TuneSource) {
        repo.updateSettings { it.applying(profile, source) }
    }

    override suspend fun record(entry: ListenEntry) = repo.addListen(entry)
}
