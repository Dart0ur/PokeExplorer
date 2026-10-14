package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к индексу покемонов (таблица pokemon_index) для офлайн-списка.
 */
@Dao
interface PokemonIndexDao {

    @Query("SELECT * FROM pokemon_index ORDER BY id ASC")
    fun observeAll(): Flow<List<PokemonIndexEntity>>

    @Query("SELECT * FROM pokemon_index ORDER BY id ASC")
    suspend fun getAll(): List<PokemonIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PokemonIndexEntity>)

    @Query("DELETE FROM pokemon_index")
    suspend fun clear()

    @Query("SELECT fetchedAt FROM pokemon_index ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun getLastFetchTime(): Long?

    /** Полная замена индекса одной транзакцией: в базе либо новый индекс целиком, либо старый. */
    @Transaction
    suspend fun replaceAll(items: List<PokemonIndexEntity>) {
        clear()
        insertAll(items)
    }
}