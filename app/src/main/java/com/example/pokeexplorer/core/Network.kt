package com.example.pokeexplorer.core

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Обёртка над сетевым вызовом: не даёт исключению уронить корутину,
 * но пропускает CancellationException наверх — иначе отменённый запрос
 * (например, при debounce поиска) выглядел бы как ошибка экрана.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

/** Человеческие тексты для типичных сетевых сбоев. */
object ErrorMapper {

    fun messageFor(throwable: Throwable): String = when (throwable) {
        is UnknownHostException,
        is ConnectException,
        is SocketTimeoutException -> "Нет подключения к сети. Проверьте интернет и попробуйте снова."

        is HttpException -> when (throwable.code()) {
            404 -> "Запись не найдена (404)"
            in 500..599 -> "Сервер PokeAPI временно недоступен (${throwable.code()})"
            429 -> "Слишком много запросов. Подождите немного."
            else -> "Ошибка запроса: ${throwable.code()} ${throwable.message()}"
        }

        is IOException -> "Проблема с сетью: ${throwable.localizedMessage ?: "неизвестная ошибка"}"
        else -> throwable.localizedMessage ?: "Неизвестная ошибка"
    }
}
