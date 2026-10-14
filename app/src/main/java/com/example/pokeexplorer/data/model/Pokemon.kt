package com.example.pokeexplorer.data.model

/** Строка списка: минимум данных, чтобы отрисовать элемент и открыть детали. */
data class PokemonListItem(
    val id: Int,
    val name: String,
    val imageUrl: String,
)

/** Одна страница списка + информация для пагинации. */
data class PokemonPage(
    val items: List<PokemonListItem>,
    val totalCount: Int,
    val hasMore: Boolean,
)

/**
 * Полные детали покемона. Собираются из ДВУХ запросов PokeAPI:
 * `/pokemon/{id}` (характеристики) и `/pokemon-species/{id}` (описание, вид),
 * поэтому текстовые поля объявлены nullable — если species не пришёл,
 * экран всё равно показывает характеристики.
 */
data class PokemonDetail(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val heightDecimetres: Int,
    val weightHectograms: Int,
    val baseExperience: Int?,
    val types: List<String>,
    val stats: List<StatEntry>,
    val abilities: List<AbilityEntry>,
    // --- из /pokemon-species ---
    val description: String? = null,
    val genus: String? = null,
    val isLegendary: Boolean = false,
    val isMythical: Boolean = false,
    val habitat: String? = null,
    val captureRate: Int? = null,

    val offlineSince: Long? = null,
)

data class StatEntry(
    val name: String,
    val value: Int,
)

data class AbilityEntry(
    val name: String,
    val isHidden: Boolean,
)

/** Данные из `/pokemon-species`, уже очищенные от служебных символов. */
data class SpeciesInfo(
    val description: String?,
    val genus: String?,
    val isLegendary: Boolean,
    val isMythical: Boolean,
    val habitat: String?,
    val captureRate: Int?,
)
