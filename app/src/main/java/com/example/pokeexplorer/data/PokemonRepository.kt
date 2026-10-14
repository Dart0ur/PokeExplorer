package com.example.pokeexplorer.data

import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.model.PokemonPage

/**
 * Слой данных: Repository — единственная дверь к сети для ViewModel.
 * Retrofit, кэш и обработка ошибок спрятаны за этим интерфейсом,
 * поэтому ViewModel не знает ни про HTTP, ни про DTO.
 */
interface PokemonRepository {

    /**
     * Страница списка покемонов.
     * @param offset сдвиг, @param limit размер страницы.
     */
    suspend fun getPokemonPage(offset: Int, limit: Int): Result<PokemonPage>

    /**
     * Полный индекс имён (id + имя) одним запросом.
     * Кэшируется в памяти: нужен для быстрого локального поиска по подстроке.
     */
    suspend fun getPokemonIndex(): Result<List<PokemonListItem>>

    /** Детали покемона по id. Повторный вызов отдаётся из кэша. */
    suspend fun getPokemonDetail(id: Int): Result<PokemonDetail>
}
