package com.example.pokeexplorer

import com.example.pokeexplorer.data.cache.CachePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_000_000_000_000L

    @Test
    fun `свежие данные не устарели`() {
        assertFalse(CachePolicy.isStale(fetchedAt = now - 1 * hour, now = now, ttlHours = 24))
    }

    @Test
    fun `данные старше TTL устарели`() {
        assertTrue(CachePolicy.isStale(fetchedAt = now - 25 * hour, now = now, ttlHours = 24))
    }

    @Test
    fun `ровно на границе TTL данные считаются устаревшими`() {
        assertTrue(CachePolicy.isStale(fetchedAt = now - 24 * hour, now = now, ttlHours = 24))
    }

    @Test
    fun `TTL 0 означает всегда обновлять`() {
        assertTrue(CachePolicy.isStale(fetchedAt = now, now = now, ttlHours = 0))
    }

    @Test
    fun `возраст считается в часах и не бывает отрицательным`() {
        assertEquals(5, CachePolicy.ageHours(fetchedAt = now - 5 * hour, now = now))
        assertEquals(0, CachePolicy.ageHours(fetchedAt = now + hour, now = now))
    }
}