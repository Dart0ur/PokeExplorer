package com.example.pokeexplorer.data.remote

import com.example.pokeexplorer.data.remote.dto.PageResponseDto
import com.example.pokeexplorer.data.remote.dto.PokemonDto
import com.example.pokeexplorer.data.remote.dto.PokemonSpeciesDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit-описание PokeAPI (https://pokeapi.co/docs/v2).
 * Все методы — suspend-функции: Retrofit сам уводит их в фоновый поток,
 * а корутина, из которой вызван метод, может быть отменена.
 */
interface PokeApiService {

    /** Постраничный список покемонов. */
    @GET("pokemon")
    suspend fun getPokemonPage(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageResponseDto

    /**
     * Полный индекс имён одним запросом (~1300 записей, ~100 КБ).
     * PokeAPI не умеет искать по подстроке, поэтому локальный поиск
     * делается по этому индексу — так поиск мгновенный и без лишних запросов.
     */
    @GET("pokemon")
    suspend fun getAllPokemon(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0,
    ): PageResponseDto

    /**
     * Боевые характеристики покемона: рост, вес, типы, статы, способности.
     *
     * ВАЖНО: текстового описания в этом ответе НЕТ — PokeAPI держит его
     * в отдельном ресурсе «species» (см. [getPokemonSpecies]).
     */
    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonDto

    /**
     * Описание вида: flavor_text_entries (тексты Покедекса), genus («Seed Pokémon»),
     * легендарность, место обитания. Отдельный ресурс, потому что эти данные
     * не меняются от формы к форме, а PokeAPI разносит их по разным эндпоинтам.
     */
    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(@Path("id") id: Int): PokemonSpeciesDto
}
