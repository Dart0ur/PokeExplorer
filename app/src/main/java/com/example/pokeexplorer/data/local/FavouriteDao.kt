package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к таблице избранного. Версия 2: добавлена фильтрация по profileId.
 */
@Dao
interface FavouriteDao {

    @Query("SELECT * FROM favourites WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun observeAll(profileId: Long): Flow<List<FavouriteEntity>>

    @Query("SELECT id FROM favourites WHERE profileId = :profileId")
    fun observeIds(profileId: Long): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE profileId = :profileId AND id = :id)")
    suspend fun exists(profileId: Long, id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE profileId = :profileId AND id = :id")
    suspend fun deleteById(profileId: Long, id: Int)

    @Query("DELETE FROM favourites WHERE profileId = :profileId")
    suspend fun clear(profileId: Long)
}
