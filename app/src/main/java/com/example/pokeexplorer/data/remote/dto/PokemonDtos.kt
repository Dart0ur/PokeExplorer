package com.example.pokeexplorer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO строго повторяют JSON PokeAPI (snake_case), поэтому мапперы переводят их
 * в доменные модели, а UI никогда не видит сырых полей с подчёркиваниями.
 */

/** Ответ `GET /pokemon?limit=&offset=` и `GET /pokemon?limit=100000` (индекс имён). */
data class PageResponseDto(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("next") val next: String? = null,
    @SerializedName("previous") val previous: String? = null,
    @SerializedName("results") val results: List<NamedResourceDto> = emptyList(),
)

/** Общая для PokeAPI пара «имя + ссылка на ресурс». */
data class NamedResourceDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("url") val url: String = "",
)

/** Ответ `GET /pokemon/{id}` (используемые поля). */
data class PokemonDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("height") val height: Int = 0,
    @SerializedName("weight") val weight: Int = 0,
    @SerializedName("base_experience") val baseExperience: Int? = null,
    @SerializedName("sprites") val sprites: SpritesDto? = null,
    @SerializedName("types") val types: List<TypeSlotDto> = emptyList(),
    @SerializedName("stats") val stats: List<StatSlotDto> = emptyList(),
    @SerializedName("abilities") val abilities: List<AbilitySlotDto> = emptyList(),
)

data class SpritesDto(
    @SerializedName("front_default") val frontDefault: String? = null,
    @SerializedName("other") val other: OtherSpritesDto? = null,
)

data class OtherSpritesDto(
    @SerializedName("official-artwork") val officialArtwork: ArtworkDto? = null,
    @SerializedName("home") val home: ArtworkDto? = null,
)

data class ArtworkDto(
    @SerializedName("front_default") val frontDefault: String? = null,
)

data class TypeSlotDto(
    @SerializedName("slot") val slot: Int = 0,
    @SerializedName("type") val type: NamedResourceDto = NamedResourceDto(),
)

data class StatSlotDto(
    @SerializedName("base_stat") val baseStat: Int = 0,
    @SerializedName("effort") val effort: Int = 0,
    @SerializedName("stat") val stat: NamedResourceDto = NamedResourceDto(),
)

data class AbilitySlotDto(
    @SerializedName("ability") val ability: NamedResourceDto = NamedResourceDto(),
    @SerializedName("is_hidden") val isHidden: Boolean = false,
    @SerializedName("slot") val slot: Int = 0,
)

// ---------------------------------------------------------------- species

/**
 * Ответ `GET /pokemon-species/{id}` — здесь лежит текстовое описание,
 * которого нет в `/pokemon/{id}`.
 */
data class PokemonSpeciesDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorTextDto> = emptyList(),
    @SerializedName("genera") val genera: List<GenusDto> = emptyList(),
    @SerializedName("is_legendary") val isLegendary: Boolean = false,
    @SerializedName("is_mythical") val isMythical: Boolean = false,
    @SerializedName("capture_rate") val captureRate: Int? = null,
    @SerializedName("habitat") val habitat: NamedResourceDto? = null,
)

data class FlavorTextDto(
    /** Текст из игры. Содержит переносы строк и служебные символы — чистим в маппере. */
    @SerializedName("flavor_text") val flavorText: String = "",
    @SerializedName("language") val language: NamedResourceDto = NamedResourceDto(),
    @SerializedName("version") val version: NamedResourceDto? = null,
)

data class GenusDto(
    /** Например «Seed Pokémon». */
    @SerializedName("genus") val genus: String = "",
    @SerializedName("language") val language: NamedResourceDto = NamedResourceDto(),
)
