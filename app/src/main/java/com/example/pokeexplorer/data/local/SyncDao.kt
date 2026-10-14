package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Query

/**
 * Запросы для фоновой синхронизации. Только чтение существующих таблиц,
 * поэтому версию базы менять не нужно.
 */
@Dao
interface SyncDao {

    /**
     * Покемоны, которые важны пользователю (избранное, команды, история
     * всех профилей). Сначала самые свежие. Их детали воркер заранее
     * кладёт в кэш, чтобы они открывались без сети.
     */
    @Query(
        """
        SELECT pokemonId FROM (
            SELECT id AS pokemonId, addedAt AS ts FROM favourites
            UNION ALL
            SELECT pokemonId, addedAt AS ts FROM team_members
            UNION ALL
            SELECT pokemonId, viewedAt AS ts FROM history
        )
        GROUP BY pokemonId
        ORDER BY MAX(ts) DESC
        LIMIT :limit
        """
    )
    suspend fun importantPokemonIds(limit: Int): List<Int>
}