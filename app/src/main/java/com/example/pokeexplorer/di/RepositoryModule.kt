package com.example.pokeexplorer.di

import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.favourites.FavouritesRepository
import com.example.pokeexplorer.data.favourites.RoomFavouritesRepository
import com.example.pokeexplorer.data.local.FavouriteDao
import com.example.pokeexplorer.data.local.PokemonCacheDao
import com.example.pokeexplorer.data.local.PokemonIndexDao
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import com.example.pokeexplorer.data.remote.PokeApiService
import com.example.pokeexplorer.data.repository.PokemonRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Singleton
import com.example.pokeexplorer.data.hidden.HiddenPokemonRepository
import com.example.pokeexplorer.data.hidden.RoomHiddenPokemonRepository

/**
 * Связка «интерфейс -> реализация».
 *
 * Почему @Provides, а не @Binds: @Binds требует abstract class/interface модуля
 * и конструктор с @Inject, а PokemonRepositoryImpl у нас без аннотаций —
 * переписывать рабочий код из ДЗ №3 ради этого не нужно.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePokemonRepository(
        api: PokeApiService,
        indexDao: PokemonIndexDao,
        cacheDao: PokemonCacheDao,
        preferences: UserPreferencesRepository,
    ): PokemonRepository = PokemonRepositoryImpl(
        api = api,
        indexDao = indexDao,
        cacheDao = cacheDao,
        // TTL активного профиля читается при каждом запросе —
        // настройка «Срок жизни кэша» сразу влияет на поведение.
        ttlHoursProvider = {
            preferences.getTtlHours(preferences.activeProfileId.first()).first()
        },
    )

    @Provides
    @Singleton
    fun provideFavouritesRepository(
        dao: FavouriteDao,
        activeProfile: ActiveProfileProvider,
    ): FavouritesRepository = RoomFavouritesRepository(dao, activeProfile)

    @Provides
    @Singleton
    fun provideHiddenPokemonRepository(
        impl: RoomHiddenPokemonRepository,
    ): HiddenPokemonRepository = impl

}