package com.example.pokeexplorer

import com.example.pokeexplorer.data.mapper.cleanFlavorText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Отдельный тест на чистку текста Покедекса.
 *
 * PokeAPI отдаёт описание «как в игре»: с переносами строк (\n),
 * разделителями страниц (\u000C) и мягкими переносами (\u00AD).
 * Если это не почистить, на экране деталей будет каша из символов.
 */
class FlavorTextTest {

    /** Строка ровно в том виде, в каком её отдаёт API (с реальными спецсимволами). */
    private val rawFromApi =
        "When several of\u000Athese POKéMON gather,\u000Ctheir electricity\u000A" +
        "can build and crackle. \u00AD People in fear of this POKéMON\u000Ararely show themselves."

    @Test
    fun `переносы строк заменяются на пробел`() {
        val cleaned = rawFromApi.cleanFlavorText()

        assertFalse("В тексте не должно остаться переносов строк", cleaned.contains('\n'))
        assertTrue(
            "Предложение должно читаться слитно",
            cleaned.contains("When several of these POKéMON gather, their electricity"),
        )
    }

    @Test
    fun `разделитель страницы и мягкий перенос убираются`() {
        val cleaned = rawFromApi.cleanFlavorText()

        assertFalse("Символ \u000C должен быть убран", cleaned.contains('\u000C'))
        assertFalse("Мягкий перенос \u00AD должен быть убран", cleaned.contains('\u00AD'))
    }

    @Test
    fun `множественные пробелы схлопываются в один`() {
        val messy = "Слишком    много        пробелов   подряд"
        assertEquals("Слишком много пробелов подряд", messy.cleanFlavorText())
    }

    @Test
    fun `пробелы в начале и конце обрезаются`() {
        assertEquals("Текст", "   \n  Текст \u000C  ".cleanFlavorText())
    }

    @Test
    fun `диакритика не портится`() {
        // В текстах PokeAPI полно «POKéMON» — чистка не должна ломать юникод.
        val cleaned = "POKéMON\u000Aare cute.".cleanFlavorText()

        assertEquals("POKéMON are cute.", cleaned)
    }
}
