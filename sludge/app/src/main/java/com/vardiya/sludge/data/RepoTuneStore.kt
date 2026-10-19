package com.vardiya.sludge.data

import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.TuneCache
import com.vardiya.sludge.domain.TuneStore
import kotlinx.coroutines.flow.first

/** The tuning cache kept in the settings store. */
class RepoTuneStore(private val repo: SettingsRepository) : TuneStore {
    override suspend fun cached(key: String): AiProfile? = repo.cache.first()[key]

    override suspend fun remember(key: String, profile: AiProfile) {
        repo.updateCache { TuneCache.put(it, key, profile) }
    }
}
