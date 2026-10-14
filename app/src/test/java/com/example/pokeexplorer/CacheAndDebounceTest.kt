package com.example.pokeexplorer

import com.example.pokeexplorer.core.MemoryLruCache
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.list.PokemonListViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты кэша и констант debounce — те части бонусов, которые
 * проверяются без Android-рантайма и корутин.
 */
class CacheAndDebounceTest {

    @Test
    fun `кэш страниц отдаёт сохранённое значение`() {
        val cache = MemoryLruCache<Int, String>(maxSize = 2)
        cache.put(0, "page-0")

        assertEquals("page-0", cache.get(0))
        assertNull(cache.get(30))
    }

    @Test
    fun `кэш вытесняет самые старые записи при переполнении`() {
        val cache = MemoryLruCache<Int, PokemonListItem>(maxSize = 2)
        cache.put(1, PokemonListItem(1, "bulbasaur", ""))
        cache.put(2, PokemonListItem(2, "ivysaur", ""))
        cache.put(3, PokemonListItem(3, "venusaur", "")) // должно вытеснить запись с ключом 1

        assertNull("Самая старая запись должна быть вытеснена", cache.get(1))
        assertEquals(2, cache.get(2)?.id)
        assertEquals(3, cache.get(3)?.id)
        assertEquals(2, cache.size)
    }

    @Test
    fun `кэш деталей хранит значения по id`() {
        val cache = MemoryLruCache<Int, PokemonDetail>(maxSize = 8)
        val detail = PokemonDetail(
            id = 25, name = "pikachu", imageUrl = "", heightDecimetres = 4,
            weightHectograms = 60, baseExperience = 112, types = listOf("electric"),
            stats = emptyList(), abilities = emptyList(),
            description = "It has small electric sacs on both its cheeks.",
        )
        cache.put(25, detail)

        assertEquals("pikachu", cache.get(25)?.name)
        assertEquals(
            "It has small electric sacs on both its cheeks.",
            cache.get(25)?.description,
        )
    }

    @Test
    fun `debounce поиска укладывается в рекомендованные 300-500 мс`() {
        // Требование бонуса: Job + delay(300–500 мс) и отмена предыдущего Job.
        assertTrue(
            "Debounce должен быть в диапазоне 300..500 мс",
            PokemonListViewModel.DEBOUNCE_MS in 300..500,
        )
    }

    @Test
    fun `размер страницы положительный и помещается в один экран`() {
        assertTrue(
            "Размер страницы должен быть разумным",
            PokemonListViewModel.PAGE_SIZE in 1..100,
        )
    }
}
