package com.example.pokeexplorer.di

import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import com.example.pokeexplorer.data.profile.DataStoreActiveProfileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Привязка интерфейса к DataStore-реализации. Теперь activeProfileId
 * читается из DataStore, и переключение профиля работает через него.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindActiveProfileProvider(
        impl: DataStoreActiveProfileProvider,
    ): ActiveProfileProvider
}