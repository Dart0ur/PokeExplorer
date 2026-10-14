package com.example.pokeexplorer.data.local

import com.example.pokeexplorer.data.model.AbilityEntry
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.StatEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * PokemonDetail <-> PokemonCacheEntity.
 * Списки (types, stats, abilities) хранятся в JSON-колонках через org.json,
 * встроенный в Android, поэтому отдельная библиотека не нужна.
 */
fun PokemonDetail.toCacheEntity(fetchedAt: Long): PokemonCacheEntity =
    PokemonCacheEntity(
        pokemonId = id,
        name = name,
        imageUrl = imageUrl,
        heightDecimetres = heightDecimetres,
        weightHectograms = weightHectograms,
        baseExperience = baseExperience,
        typesJson = JSONArray(types).toString(),
        statsJson = JSONArray().apply {
            stats.forEach { put(JSONObject().put("name", it.name).put("value", it.value)) }
        }.toString(),
        abilitiesJson = JSONArray().apply {
            abilities.forEach { put(JSONObject().put("name", it.name).put("isHidden", it.isHidden)) }
        }.toString(),
        description = description,
        genus = genus,
        captureRate = captureRate,
        habitat = habitat,
        isLegendary = isLegendary,
        isMythical = isMythical,
        fetchedAt = fetchedAt,
    )

fun PokemonCacheEntity.toDomain(): PokemonDetail =
    PokemonDetail(
        id = pokemonId,
        name = name,
        imageUrl = imageUrl,
        heightDecimetres = heightDecimetres,
        weightHectograms = weightHectograms,
        baseExperience = baseExperience,
        types = parseArray(typesJson) { arr, i -> arr.getString(i) },
        stats = parseArray(statsJson) { arr, i ->
            val o = arr.getJSONObject(i)
            StatEntry(name = o.getString("name"), value = o.getInt("value"))
        },
        abilities = parseArray(abilitiesJson) { arr, i ->
            val o = arr.getJSONObject(i)
            AbilityEntry(name = o.getString("name"), isHidden = o.getBoolean("isHidden"))
        },
        description = description,
        genus = genus,
        isLegendary = isLegendary,
        isMythical = isMythical,
        habitat = habitat,
        captureRate = captureRate,
    )

/** Повреждённый JSON не роняет приложение: просто получится пустой список. */
private inline fun <T> parseArray(json: String, item: (JSONArray, Int) -> T): List<T> =
    runCatching {
        val arr = JSONArray(json)
        List(arr.length()) { i -> item(arr, i) }
    }.getOrDefault(emptyList())