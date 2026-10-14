package com.example.pokeexplorer

import com.example.pokeexplorer.core.resolveIntArgument
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Регрессионные тесты на баг «#-1 / Некорректный id покемона».
 *
 * Настоящий SavedStateHandle при NavType.IntType хранит Integer и при чтении
 * как String бросает ClassCastException. Здесь это поведение воспроизводится
 * лямбдой, поэтому тест ловит баг без Android-рантайма.
 */
class NavigationArgsTest {

    /** Имитация реального SavedStateHandle: значение 25 хранится как Int. */
    private val storedValue: Any = 25

    @Suppress("UNCHECKED_CAST")
    private fun readInt() = storedValue as? Int

    @Suppress("UNCHECKED_CAST")
    private fun readString() = storedValue as String?  // бросит ClassCastException, как в жизни

    @Test
    fun `id читается из Int и не падает на резервном строковом чтении`() {
        val id = resolveIntArgument(readInt = ::readInt, readString = ::readString)

        assertEquals("id не должен теряться из-за ClassCastException", 25, id)
    }

    @Test
    fun `падение строкового чтения не роняет разбор`() {
        // Если бы runCatching не было, здесь был бы ClassCastException.
        val id = resolveIntArgument(
            readInt = { null },
            readString = ::readString,
        )

        assertEquals(-1, id)
    }

    @Test
    fun `строковый аргумент работает как резервный вариант`() {
        val id = resolveIntArgument(
            readInt = { null },
            readString = { "42" },
        )

        assertEquals(42, id)
    }

    @Test
    fun `отсутствующий аргумент даёт fallback`() {
        val id = resolveIntArgument(readInt = { null }, readString = { null })

        assertEquals(-1, id)
    }

    @Test
    fun `нечисловая строка не превращается в мусорный id`() {
        val id = resolveIntArgument(readInt = { null }, readString = { "pikachu" })

        assertEquals(-1, id)
    }
}
