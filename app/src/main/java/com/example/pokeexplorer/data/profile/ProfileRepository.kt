package com.example.pokeexplorer.data.profile

import com.example.pokeexplorer.data.local.ProfileDao
import com.example.pokeexplorer.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий профилей. Обёртка над ProfileDao, в будущем здесь может
 * появиться маппинг в доменные модели.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
) {
    val profiles: Flow<List<ProfileEntity>> = dao.observeAll()

    suspend fun getById(id: Long): ProfileEntity? = dao.getById(id)

    suspend fun create(name: String): Long = dao.insert(
        ProfileEntity(name = name, createdAt = System.currentTimeMillis())
    )

    suspend fun delete(profile: ProfileEntity) = dao.delete(profile)

    suspend fun count(): Int = dao.count()
}