package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Профили приложения. Профиль — локальный пользователь без серверной авторизации.
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirst(): ProfileEntity?

    /** Возвращает id вставленной строки — он нужен, чтобы сразу переключиться. */
    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}
