package com.example.pokeexplorer.integration

import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.model.AbilityEntry
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.model.PokemonPage
import com.example.pokeexplorer.data.model.StatEntry
import kotlinx.coroutines.delay

/**
 * Ручная подмена сетевого репозитория.
 *
 * Почему не MockK: нам нужен не «вызов был», а поведение — сколько раз дёрнули,
 * с какими аргументами, и возможность подсунуть задержку, чтобы поймать
 * промежуточное состояние Loading. Всё это настраиваемые поля, и на защите
 * такой фейк читается за десять секунд.
 */
class FakePokemonRepository : PokemonRepository {

    /** Что вернуть на следующем вызове страницы. */
    var pageResult: Result<PokemonPage> = Result.success(
        PokemonPage(
            items = listOf(
                PokemonListItem(25, "pikachu", "https://img/25.png"),
                PokemonListItem(6, "charizard", "https://img/6.png"),
            ),
            totalCount = 1302,
            hasMore = true,
        )
    )

    var indexResult: Result<List<PokemonListItem>> = Result.success(DEFAULT_INDEX)

    var detailResult: Result<PokemonDetail> = Result.success(DEFAULT_DETAIL)

    /** Задержка ответа — чтобы состояние Loading было наблюдаемым. */
    var pageDelayMs: Long = 0
    var indexDelayMs: Long = 0

    /** Счётчики вызовов: по ним проверяем «retry реально сходил в сеть ещё раз». */
    var pageCalls: Int = 0
        private set
    var indexCalls: Int = 0
        private set
    var detailCalls: Int = 0
        private set

    val requestedDetailIds = mutableListOf<Int>()

    /** С какими offset пришли за страницами — для проверки пагинации. */
    val requestedOffsets = mutableListOf<Int>()

    /** Если задано — вместо результата бросаем исключение. */
    var pageThrows: Throwable? = null

    override suspend fun getPokemonPage(offset: Int, limit: Int): Result<PokemonPage> {
        pageCalls++
        requestedOffsets += offset
        if (pageDelayMs > 0) delay(pageDelayMs)
        pageThrows?.let { throw it }
        return pageResult
    }

    override suspend fun getPokemonIndex(): Result<List<PokemonListItem>> {
        indexCalls++
        if (indexDelayMs > 0) delay(indexDelayMs)
        return indexResult
    }

    override suspend fun getPokemonDetail(id: Int): Result<PokemonDetail> {
        detailCalls++
        requestedDetailIds += id   // ← новая строка
        requestedDetailIds += id
        return detailResult
    }

    fun reset() {
        pageCalls = 0
        indexCalls = 0
        detailCalls = 0
        requestedOffsets.clear()
        requestedDetailIds.clear()
    }

    companion object {
        /**
         * Индекс специально подобран под тест debounce:
         * «cha» совпадает с тремя именами, «char» — только с двумя.
         */
        val DEFAULT_INDEX = listOf(
            PokemonListItem(4, "charmander", "https://img/4.png"),
            PokemonListItem(6, "charizard", "https://img/6.png"),
            PokemonListItem(609, "chandelure", "https://img/609.png"),
        )
        val DEFAULT_DETAIL_RESULT: Result<PokemonDetail> = Result.success(
            PokemonDetail(
                id = 25,
                name = "pikachu",
                imageUrl = "https://img/25.png",
                heightDecimetres = 4,
                weightHectograms = 60,
                baseExperience = 112,
                types = listOf("electric"),
                stats = emptyList(),
                abilities = emptyList(),
            )
        )

        val DEFAULT_DETAIL = PokemonDetail(
            id = 25,
            name = "pikachu",
            imageUrl = "https://img/25.png",
            heightDecimetres = 4,
            weightHectograms = 60,
            baseExperience = 112,
            types = listOf("electric"),
            stats = listOf(StatEntry("hp", 35)),
            abilities = listOf(AbilityEntry("static", false)),
        )
    }
}
