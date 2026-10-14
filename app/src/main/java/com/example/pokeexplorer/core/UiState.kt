package com.example.pokeexplorer.core

/**
 * Единое состояние экрана. Любой экран приложения описывается одной из четырёх
 * ситуаций, поэтому UI никогда не приходится гадать, что рисовать.
 *
 * `out T` нужен, чтобы [Empty] и [Error] (без данных) подходили любому экрану:
 * UiState<Nothing> является подтипом UiState<PokemonDetail> и т.д.
 */
sealed interface UiState<out T> {

    /** Идёт загрузка — показываем индикатор. */
    data object Loading : UiState<Nothing>

    /** Запрос упал — показываем сообщение и кнопку Retry. */
    data class Error(val message: String) : UiState<Nothing>

    /** Запрос прошёл, но данных нет — например, поиск ничего не нашёл. */
    data object Empty : UiState<Nothing>

    /** Есть что показать. */
    data class Success<T>(val data: T) : UiState<T>
}

/** Удобный доступ к данным без приведения типов на стороне UI. */
val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success<T>)?.data

/** Сообщение об ошибке, если экран сейчас в состоянии ошибки. */
val <T> UiState<T>.errorMessageOrNull: String?
    get() = (this as? UiState.Error)?.message
