package com.example.pokeexplorer.data.repository

import com.example.pokeexplorer.core.MemoryLruCache
import com.example.pokeexplorer.core.safeApiCall
import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.cache.CachePolicy
import com.example.pokeexplorer.data.local.PokemonCacheDao
import com.example.pokeexplorer.data.local.PokemonIndexDao
import com.example.pokeexplorer.data.local.toCacheEntity
import com.example.pokeexplorer.data.local.toDomain
import com.example.pokeexplorer.data.local.toIndexEntity
import com.example.pokeexplorer.data.mapper.toDetail
import com.example.pokeexplorer.data.mapper.toListItem
import com.example.pokeexplorer.data.mapper.toSpeciesInfo
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.model.PokemonPage
import com.example.pokeexplorer.data.remote.PokeApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Реализация репозитория: Retrofit + Room (offline-first) + кэш в памяти.
 *
 * Список: индекс в таблице pokemon_index, страница — срез индекса.
 * Детали: таблица pokemon_cache.
 *
 * Общая схема для обоих:
 *  1. свежие данные (моложе TTL) из памяти или Room отдаются без сети;
 *  2. устаревшие или отсутствующие загружаются из сети и сохраняются;
 *  3. если сети нет, отдаются устаревшие данные вместо ошибки
 *     (у деталей при этом заполнено offlineSince — для плашки в UI).
 *
 * TTL берётся из ttlHoursProvider при каждом запросе — так настройка
 * «Срок жизни кэша» активного профиля сразу влияет на поведение.
 *
 * DAO = null — режим без базы (старые юнит-тесты): только сеть и память.
 */
class PokemonRepositoryImpl(
    private val api: PokeApiService,
    private val indexDao: PokemonIndexDao? = null,
    private val cacheDao: PokemonCacheDao? = null,
    private val ttlHours: Int = DEFAULT_TTL_HOURS,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ttlHoursProvider: suspend () -> Int = { ttlHours },
) : PokemonRepository {

    /** Значение в памяти вместе со временем загрузки — чтобы и память подчинялась TTL. */
    private data class Timed<T>(val value: T, val fetchedAt: Long)

    private val pageCache = MemoryLruCache<Int, PokemonPage>(maxSize = 32)
    private val detailCache = MemoryLruCache<Int, Timed<PokemonDetail>>(maxSize = 64)
    private val pageLocks = Mutex()
    private val detailLocks = Mutex()
    private val indexLocks = Mutex()

    /** Только свежие данные. Устаревшие офлайн-данные в память не кладём,
     *  чтобы «Повторить» при появлении сети действительно сходил в сеть. */
    @Volatile
    private var indexCache: Timed<List<PokemonListItem>>? = null

    // ------------------------------------------------------------ Список

    override suspend fun getPokemonPage(offset: Int, limit: Int): Result<PokemonPage> {
        if (indexDao != null) {
            return getPokemonIndex().map { index ->
                val items = index.drop(offset).take(limit)
                PokemonPage(
                    items = items,
                    totalCount = index.size,
                    hasMore = offset + items.size < index.size,
                )
            }
        }
        return getPageFromNetwork(offset, limit)
    }

    override suspend fun getPokemonIndex(): Result<List<PokemonListItem>> {
        val ttl = ttlHoursProvider()
        freshIndex(ttl)?.let { return Result.success(it) }

        return indexLocks.withLock {
            freshIndex(ttl)?.let { return@withLock Result.success(it) }

            val now = clock()
            val dao = indexDao
                ?: return@withLock loadIndexFromNetwork().onSuccess { indexCache = Timed(it, now) }

            val cached = dao.getAll()
            val lastFetch = cached.maxOfOrNull { it.fetchedAt }

            if (lastFetch != null && !CachePolicy.isStale(lastFetch, now, ttl)) {
                val items = cached.map { it.toDomain() }
                indexCache = Timed(items, lastFetch)
                return@withLock Result.success(items)
            }

            loadIndexFromNetwork().fold(
                onSuccess = { items ->
                    dao.replaceAll(items.map { it.toIndexEntity(fetchedAt = now) })
                    indexCache = Timed(items, now)
                    Result.success(items)
                },
                onFailure = { error ->
                    if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
                    else Result.failure(error)
                },
            )
        }
    }

    private fun freshIndex(ttl: Int): List<PokemonListItem>? =
        indexCache?.takeIf { !CachePolicy.isStale(it.fetchedAt, clock(), ttl) }?.value

    private suspend fun loadIndexFromNetwork(): Result<List<PokemonListItem>> = safeApiCall {
        withContext(ioDispatcher) {
            api.getAllPokemon(limit = 100_000).results.mapNotNull { it.toListItem() }
        }
    }

    private suspend fun getPageFromNetwork(offset: Int, limit: Int): Result<PokemonPage> {
        pageCache.get(offset)?.let { return Result.success(it) }

        return safeApiCall {
            withContext(ioDispatcher) {
                pageLocks.withLock {
                    pageCache.get(offset) ?: run {
                        val response = api.getPokemonPage(limit = limit, offset = offset)
                        val items = response.results.mapNotNull { it.toListItem() }
                        val page = PokemonPage(
                            items = items,
                            totalCount = response.count,
                            hasMore = response.next != null && items.isNotEmpty(),
                        )
                        pageCache.put(offset, page)
                        page
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ Детали

    override suspend fun getPokemonDetail(id: Int): Result<PokemonDetail> {
        val ttl = ttlHoursProvider()
        freshDetail(id, ttl)?.let { return Result.success(it) }

        return detailLocks.withLock {
            freshDetail(id, ttl)?.let { return@withLock Result.success(it) }

            val now = clock()
            val cached = cacheDao?.get(id)

            // 1. Свежие детали в Room — сеть не нужна.
            if (cached != null && !CachePolicy.isStale(cached.fetchedAt, now, ttl)) {
                val detail = cached.toDomain()
                detailCache.put(id, Timed(detail, cached.fetchedAt))
                return@withLock Result.success(detail)
            }

            // 2. Нет или устарели — в сеть, результат сохраняем.
            loadDetailFromNetwork(id).fold(
                onSuccess = { detail ->
                    cacheDao?.insert(detail.toCacheEntity(fetchedAt = now))
                    detailCache.put(id, Timed(detail, now))
                    Result.success(detail)
                },
                // 3. Сети нет — устаревшие данные с отметкой для UI.
                onFailure = { error ->
                    if (cached != null) {
                        Result.success(cached.toDomain().copy(offlineSince = cached.fetchedAt))
                    } else {
                        Result.failure(error)
                    }
                },
            )
        }
    }

    private fun freshDetail(id: Int, ttl: Int): PokemonDetail? =
        detailCache.get(id)?.takeIf { !CachePolicy.isStale(it.fetchedAt, clock(), ttl) }?.value

    /**
     * /pokemon/{id} и /pokemon-species/{id} параллельно.
     * Описание необязательно: ошибка species не мешает показать характеристики.
     */
    private suspend fun loadDetailFromNetwork(id: Int): Result<PokemonDetail> = safeApiCall {
        withContext(ioDispatcher) {
            val (dto, species) = coroutineScope {
                val detailDeferred = async { api.getPokemonDetail(id) }
                val speciesDeferred = async {
                    runCatching { api.getPokemonSpecies(id).toSpeciesInfo() }.getOrNull()
                }
                detailDeferred.await() to speciesDeferred.await()
            }
            dto.toDetail(species)
        }
    }

    /** Сбросить кэш в памяти. Room не трогаем — офлайн-данные должны остаться. */
    fun clearCache() {
        pageCache.clear()
        detailCache.clear()
        indexCache = null
    }

    companion object {
        const val DEFAULT_TTL_HOURS = 24
    }
}