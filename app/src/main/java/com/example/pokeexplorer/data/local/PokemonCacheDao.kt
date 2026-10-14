package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Кэш деталей покемонов. Запросы для offline-first и фоновой синхронизации.
 */
@Dao
interface PokemonCacheDao {

    @Query("SELECT * FROM pokemon_cache WHERE pokemonId = :pokemonId")
    suspend fun get(pokemonId: Int): PokemonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: PokemonCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PokemonCacheEntity>)

    @Query("DELETE FROM pokemon_cache WHERE pokemonId = :pokemonId")
    suspend fun delete(pokemonId: Int)

    @Query("DELETE FROM pokemon_cache WHERE fetchedAt < :thresholdMillis")
    suspend fun deleteOlderThan(thresholdMillis: Long)

    @Query("SELECT COUNT(*) FROM pokemon_cache")
    suspend fun count(): Int

    /**
     * Устаревшие записи — то, что обновляет фоновая синхронизация.
     */
    @Query("SELECT * FROM pokemon_cache WHERE fetchedAt < :thresholdMillis")
    suspend fun getStale(thresholdMillis: Long): List<PokemonCacheEntity>

    /** Кэш по списку id — для офлайн-предзагрузки состава команд. */
    @Query("SELECT * FROM pokemon_cache WHERE pokemonId IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<PokemonCacheEntity>
}
