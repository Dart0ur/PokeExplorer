package com.example.pokeexplorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * База приложения. Версия 3: + таблица индекса для офлайн-списка.
 */
@Database(
    entities = [
        FavouriteEntity::class,
        ProfileEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        PokemonCacheEntity::class,
        PokemonIndexEntity::class,
        HistoryEntity::class,
        HiddenPokemonEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PokeDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun profileDao(): ProfileDao
    abstract fun teamDao(): TeamDao
    abstract fun historyDao(): HistoryDao
    abstract fun hiddenPokemonDao(): HiddenPokemonDao
    abstract fun pokemonCacheDao(): PokemonCacheDao
    abstract fun pokemonIndexDao(): PokemonIndexDao

    abstract fun syncDao(): SyncDao
}

/** Миграция 1→2: профили + кэш + команды + история + скрытые. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """
        )
        db.execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, 'Тренер', ${System.currentTimeMillis()})")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favourites_new (
                profileId INTEGER NOT NULL,
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(profileId, id),
                FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
            )
        """
        )
        db.execSQL(
            """
            INSERT INTO favourites_new (profileId, id, name, imageUrl, addedAt)
            SELECT 1, id, name, imageUrl, addedAt FROM favourites
        """
        )
        db.execSQL("DROP TABLE favourites")
        db.execSQL("ALTER TABLE favourites_new RENAME TO favourites")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_profileId ON favourites(profileId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS teams (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                profileId INTEGER NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_teams_profileId ON teams(profileId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS team_members (
                teamId INTEGER NOT NULL,
                pokemonId INTEGER NOT NULL,
                pokemonName TEXT NOT NULL,
                pokemonImageUrl TEXT NOT NULL,
                position INTEGER NOT NULL,
                note TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(teamId, pokemonId),
                FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_team_members_teamId ON team_members(teamId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_team_members_pokemonId ON team_members(pokemonId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pokemon_cache (
                pokemonId INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                heightDecimetres INTEGER NOT NULL,
                weightHectograms INTEGER NOT NULL,
                baseExperience INTEGER,
                typesJson TEXT NOT NULL,
                statsJson TEXT NOT NULL,
                abilitiesJson TEXT NOT NULL,
                description TEXT,
                genus TEXT,
                captureRate INTEGER,
                habitat TEXT,
                isLegendary INTEGER NOT NULL,
                isMythical INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS history (
                profileId INTEGER NOT NULL,
                pokemonId INTEGER NOT NULL,
                pokemonName TEXT NOT NULL,
                pokemonImageUrl TEXT NOT NULL,
                viewedAt INTEGER NOT NULL,
                PRIMARY KEY(profileId, pokemonId),
                FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_history_profileId ON history(profileId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_history_viewedAt ON history(viewedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hidden_pokemon (
                profileId INTEGER NOT NULL,
                pokemonId INTEGER NOT NULL,
                hiddenAt INTEGER NOT NULL,
                PRIMARY KEY(profileId, pokemonId),
                FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_hidden_pokemon_profileId ON hidden_pokemon(profileId)")
    }
}

/** Миграция 2→3: таблица индекса для офлайн-списка. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pokemon_index (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                fetchedAt INTEGER NOT NULL
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pokemon_index_name ON pokemon_index(name)")
    }
}

val DefaultProfileCallback = object : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            "INSERT OR IGNORE INTO profiles (id, name, createdAt) VALUES (1, 'Тренер', ${System.currentTimeMillis()})"
        )
    }
}
