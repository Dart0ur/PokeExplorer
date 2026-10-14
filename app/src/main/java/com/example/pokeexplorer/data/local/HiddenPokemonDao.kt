package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Скрытые покемоны. Список читает их как набор id и вычитает из выдачи API.
 */
@Dao
interface HiddenPokemonDao {

    @Query("SELECT pokemonId FROM hidden_pokemon WHERE profileId = :profileId")
    fun observeIds(profileId: Long): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_pokemon WHERE profileId = :profileId AND pokemonId = :pokemonId)")
    suspend fun isHidden(profileId: Long, pokemonId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HiddenPokemonEntity)

    @Query("DELETE FROM hidden_pokemon WHERE profileId = :profileId AND pokemonId = :pokemonId")
    suspend fun remove(profileId: Long, pokemonId: Int)

    @Query("DELETE FROM hidden_pokemon WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)
}
