package com.example.pokeexplorer.data.sync

import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.local.SyncDao
import javax.inject.Inject

/** Итог одного прогона синхронизации. */
data class SyncReport(
    val indexRefreshed: Boolean,
    val prefetched: Int,
    val failed: Int,
) {
    /** Не получилось вообще ничего — скорее всего, нет сети. Воркер повторит позже. */
    val isTotalFailure: Boolean
        get() = !indexRefreshed && prefetched == 0 && failed > 0
}

/**
 * Логика фоновой синхронизации, отдельно от WorkManager,
 * чтобы её можно было проверить обычным юнит-тестом.
 *
 * 1. Обновляет индекс списка (репозиторий сам сохранит его в Room с учётом TTL).
 * 2. Заранее загружает детали важных пользователю покемонов,
 *    чтобы они открывались без сети.
 */
class OfflineSyncEngine @Inject constructor(
    private val repository: PokemonRepository,
    private val syncDao: SyncDao,
) {

    suspend fun run(prefetchLimit: Int = DEFAULT_PREFETCH_LIMIT): SyncReport {
        val indexRefreshed = repository.getPokemonIndex().isSuccess

        var prefetched = 0
        var failed = 0
        for (id in syncDao.importantPokemonIds(prefetchLimit)) {
            val detail = repository.getPokemonDetail(id).getOrNull()
            // offlineSince != null — данные из старого кэша, сеть не ответила.
            if (detail != null && detail.offlineSince == null) prefetched++ else failed++
        }

        return SyncReport(indexRefreshed, prefetched, failed)
    }

    companion object {
        const val DEFAULT_PREFETCH_LIMIT = 50
    }
}