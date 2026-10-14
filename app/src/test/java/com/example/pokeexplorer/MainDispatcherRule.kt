package com.example.pokeexplorer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Подменяет Dispatchers.Main на тестовый.
 *
 * Зачем: viewModelScope внутри всегда работает на Dispatchers.Main.immediate.
 * В юнит-тесте Main нет (это Android-диспетчер), поэтому без подмены первый же
 * launch упадёт с «Module with the Main dispatcher had failed to initialize».
 *
 * StandardTestDispatcher даёт контроль над временем: корутины не выполняются
 * сами по себе, а только когда тест позовёт advanceUntilIdle / advanceTimeBy.
 * Именно это делает тесты детерминированными — никаких Thread.sleep
 * и «подождём немножко, вдруг успеет».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
