package com.example.pokeexplorer.di

import android.content.Context
import androidx.room.Room
import com.example.pokeexplorer.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.pokeexplorer.data.local.PokemonIndexDao
/**
 * Граф базы. Версия 2: добавлены профили, команды, кэш, история, скрытые.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "pokeexplorer.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PokeDatabase =
        Room.databaseBuilder(context, PokeDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(DefaultProfileCallback)
            .build()

    @Provides
    @Singleton
    fun provideFavouriteDao(db: PokeDatabase): FavouriteDao = db.favouriteDao()

    @Provides
    @Singleton
    fun provideProfileDao(db: PokeDatabase): ProfileDao = db.profileDao()


    @Provides
    fun providePokemonIndexDao(db: PokeDatabase): PokemonIndexDao = db.pokemonIndexDao()
    @Provides
    @Singleton
    fun provideTeamDao(db: PokeDatabase): TeamDao = db.teamDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: PokeDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideHiddenPokemonDao(db: PokeDatabase): HiddenPokemonDao = db.hiddenPokemonDao()

    @Provides
    @Singleton
    fun providePokemonCacheDao(db: PokeDatabase): PokemonCacheDao = db.pokemonCacheDao()

    @Provides
    fun provideSyncDao(database: PokeDatabase): SyncDao = database.syncDao()
}
