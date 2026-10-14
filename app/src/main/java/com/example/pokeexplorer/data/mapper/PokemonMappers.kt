package com.example.pokeexplorer.data.mapper

import com.example.pokeexplorer.data.model.AbilityEntry
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.model.SpeciesInfo
import com.example.pokeexplorer.data.model.StatEntry
import com.example.pokeexplorer.data.remote.dto.NamedResourceDto
import com.example.pokeexplorer.data.remote.dto.PokemonDto
import com.example.pokeexplorer.data.remote.dto.PokemonSpeciesDto

/**
 * PokeAPI отдаёт в списочных ответах ссылку вида
 * `https://pokeapi.co/api/v2/pokemon/25/` — id достаём из последнего сегмента.
 */
fun NamedResourceDto.extractId(): Int? =
    url.trimEnd('/').substringAfterLast('/').toIntOrNull()

/** Официальный арт всегда лежит по шаблону с id — отдельный запрос не нужен. */
fun artworkUrl(id: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

fun NamedResourceDto.toListItem(): PokemonListItem? {
    val id = extractId() ?: return null
    return PokemonListItem(id = id, name = name, imageUrl = artworkUrl(id))
}

/** Приоритет языков описания: английский, затем любой доступный. */
private const val PREFERRED_LANGUAGE = "en"

fun PokemonDto.toDetail(species: SpeciesInfo? = null): PokemonDetail = PokemonDetail(
    id = id,
    name = name,
    imageUrl = sprites?.other?.officialArtwork?.frontDefault
        ?: sprites?.other?.home?.frontDefault
        ?: sprites?.frontDefault
        ?: artworkUrl(id),
    heightDecimetres = height,
    weightHectograms = weight,
    baseExperience = baseExperience,
    types = types.sortedBy { it.slot }.map { it.type.name },
    stats = stats.map { StatEntry(name = it.stat.name, value = it.baseStat) },
    abilities = abilities.sortedBy { it.slot }.map { AbilityEntry(name = it.ability.name, isHidden = it.isHidden) },
    description = species?.description,
    genus = species?.genus,
    isLegendary = species?.isLegendary ?: false,
    isMythical = species?.isMythical ?: false,
    habitat = species?.habitat,
    captureRate = species?.captureRate,
)

/**
 * Текст Покедекса приходит из игр «как есть»: с переносами строк (\n),
 * разделителями страниц (\u000C) и мягкими переносами (\u00AD).
 * Без чистки в UI получилась бы каша из символов.
 */
fun String.cleanFlavorText(): String = this
    .replace('\n', ' ')
    .replace('\u000C', ' ')
    .replace("\u00AD", "")
    .replace(Regex("\\s+"), " ")
    .trim()

fun PokemonSpeciesDto.toSpeciesInfo(): SpeciesInfo {
    val entry = flavorTextEntries.firstOrNull { it.language.name == PREFERRED_LANGUAGE }
        ?: flavorTextEntries.firstOrNull()

    val genre = genera.firstOrNull { it.language.name == PREFERRED_LANGUAGE }
        ?: genera.firstOrNull()

    return SpeciesInfo(
        description = entry?.flavorText?.cleanFlavorText()?.takeIf { it.isNotBlank() },
        genus = genre?.genus?.takeIf { it.isNotBlank() },
        isLegendary = isLegendary,
        isMythical = isMythical,
        habitat = habitat?.name,
        captureRate = captureRate,
    )
}
