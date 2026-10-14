package com.example.pokeexplorer.navigation

import android.net.Uri
import com.example.pokeexplorer.ui.settings.SettingsScreen
import com.example.pokeexplorer.ui.settings.SettingsViewModel

/**
 * Все маршруты приложения в одном месте.
 *
 * Detail принимает аргументы прямо в route: id — обязательный (путь),
 * name — необязательный query-параметр. Имя нужно, чтобы заголовок
 * был осмысленным ещё до ответа сети; если его не передать, всё равно
 * работает — после загрузки название подставится из ответа API.
 */
object Routes {
    const val PROFILES = "profiles"
    const val LIST = "list"
    const val FAVOURITES = "favourites"

    const val SETTINGS = "settings"

    const val HISTORY = "history"
    const val DETAIL_ARG_ID = "id"
    const val DETAIL_ARG_NAME = "name"
    const val DETAIL_BASE = "detail"
    const val DETAIL = "$DETAIL_BASE/{$DETAIL_ARG_ID}?$DETAIL_ARG_NAME={$DETAIL_ARG_NAME}"

    const val TEAMS = "teams"
    const val TEAM_ARG_ID = "teamId"
    const val TEAM = "team/{$TEAM_ARG_ID}"
    fun team(teamId: Long) = "team/$teamId"

    /**
     * Uri.encode страхует от имён со спецсимволами (например «mr-mime» или «nidoran-f»):
     * без экранирования такое значение сломало бы разбор маршрута.
     */
    fun detail(id: Int, name: String = ""): String =
        "$DETAIL_BASE/$id?$DETAIL_ARG_NAME=${Uri.encode(name)}"
}
