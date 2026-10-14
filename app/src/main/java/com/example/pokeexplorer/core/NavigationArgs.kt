package com.example.pokeexplorer.core

/**
 * Разбор навигационного аргумента id.
 *
 * Зачем отдельная функция. NavType.IntType кладёт в SavedStateHandle
 * именно Int (Integer), а SavedStateHandle.get делает безусловный каст.
 * Поэтому такой код падает с ClassCastException и «теряет» id:
 *
 *     val id = savedStateHandle.get<String>(ARG_ID)?.toIntOrNull()   // ✗ крашится
 *
 * Опаснее то, что и «страховка» через два локальных val тоже падает —
 * они вычисляются оба, поэтому строковое чтение выполняется всегда:
 *
 *     val a: Int? = handle.get<Int>(KEY)
 *     val b: Int? = handle.get<String>(KEY)?.toIntOrNull()   // ✗ тоже крашится
 *
 * Каждое чтение обёрнуто в runCatching, поэтому ClassCastException
 * на любом из вариантов не роняет приложение, а просто даёт null.
 *
 * Функция принимает лямбды, а не сам SavedStateHandle, чтобы её можно было
 * проверить юнит-тестом без Android-рантайма.
 */
fun resolveIntArgument(
    readInt: () -> Int?,
    readString: () -> String?,
    fallback: Int = -1,
): Int = runCatching { readInt() }.getOrNull()
    ?: runCatching { readString()?.toIntOrNull() }.getOrNull()
    ?: fallback
