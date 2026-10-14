package com.example.pokeexplorer.core

/**
 * Маленький LRU-кэш в памяти. Нужен, чтобы:
 *  - не перезапрашивать уже открытую страницу списка (поворот экрана, возврат с деталей);
 *  - не ходить в сеть второй раз за тем же покемоном на экране деталей.
 *
 * Никакой БД — по условию задания данные живут только в оперативке.
 */
class MemoryLruCache<K : Any, V : Any>(private val maxSize: Int = 64) {

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, /* accessOrder = */ true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    /**
     * Текущее количество записей.
     * Аннотация идёт с целью @get: — иначе компилятор попытается применить
     * @Synchronized к самому свойству, а у него нет backing field.
     */
    @get:Synchronized
    val size: Int
        get() = map.size
}
