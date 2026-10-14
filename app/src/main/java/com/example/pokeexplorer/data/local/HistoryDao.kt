package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * История просмотров — источник экрана «Недавние».
 */
@Dao
interface HistoryDao {

    @Query("""
        SELECT * FROM history
        WHERE profileId = :profileId
        ORDER BY viewedAt DESC
        LIMIT 20
    """)
    fun observeByProfile(profileId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT pokemonId FROM history WHERE profileId = :profileId")
    fun observeIds(profileId: Long): Flow<List<Int>>

    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY viewedAt DESC LIMIT 20")
    suspend fun getRecent(profileId: Long): List<HistoryEntity>

    /**
     * REPLACE по составному ключу (profileId, pokemonId): повторный просмотр
     * обновляет время, а не добавляет вторую строку.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE profileId = :profileId AND pokemonId = :pokemonId")
    suspend fun remove(profileId: Long, pokemonId: Int)

    @Query("DELETE FROM history WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)

    /**
     * Оставляет только 20 последних записей профиля.
     * Вызывается после каждой вставки, чтобы история не росла бесконечно.
     */
    @Query("""
        DELETE FROM history
        WHERE profileId = :profileId
        AND pokemonId NOT IN (
            SELECT pokemonId FROM history
            WHERE profileId = :profileId
            ORDER BY viewedAt DESC
            LIMIT 20
        )
    """)
    suspend fun trimToLimit(profileId: Long)
}
