package com.example.pokeexplorer.data.cache

import java.util.concurrent.TimeUnit

/**
 * Политика актуальности кэша. Чистая функция без Android-зависимостей,
 * поэтому тестируется обычным юнит-тестом. Используется репозиторием,
 * чтобы решить, нужно ли идти в сеть.
 */
object CachePolicy {

    /** true, если данные старше TTL. ttlHours <= 0 означает «всегда обновлять». */
    fun isStale(fetchedAt: Long, now: Long, ttlHours: Int): Boolean {
        if (ttlHours <= 0) return true
        return now - fetchedAt >= TimeUnit.HOURS.toMillis(ttlHours.toLong())
    }

    /** Возраст данных в часах, для подписи «обновлено N ч назад». */
    fun ageHours(fetchedAt: Long, now: Long): Long =
        TimeUnit.MILLISECONDS.toHours((now - fetchedAt).coerceAtLeast(0))
}