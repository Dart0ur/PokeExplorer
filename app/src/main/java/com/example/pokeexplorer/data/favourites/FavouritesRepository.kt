package com.example.pokeexplorer.data.favourites

import com.example.pokeexplorer.data.model.PokemonListItem
import kotlinx.coroutines.flow.Flow

/**
 * Слой данных для избранного. ViewModel знает только этот интерфейс и понятия
 * не имеет, что под ним Room: завтра можно подменить на сеть или файл.
 */
interface FavouritesRepository {

    /** Список избранного в порядке добавления (новые сверху). */
    val favourites: Flow<List<PokemonListItem>>

    /** Только id — для быстрой проверки «в избранном или нет» в списке. */
    val ids: Flow<Set<Int>>

    suspend fun isFavourite(id: Int): Boolean

    /** Добавляет, если покемона нет в избранном; иначе удаляет. */
    suspend fun toggle(item: PokemonListItem)

    suspend fun remove(id: Int)

    suspend fun clear()
}
