package com.example.pokeexplorer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Палитра «покебол»: красно-белая светлая тема и приглушённая тёмная. */
private val PokeRed = Color(0xFFE53935)
private val PokeRedDark = Color(0xFFB71C1C)
private val PokeRedContainer = Color(0xFFFFDAD6)
private val PokeBlue = Color(0xFF1E88E5)
private val PokeBlueContainer = Color(0xFFD6E3FF)

private val LightColors = lightColorScheme(
    primary = PokeRed,
    onPrimary = Color.White,
    primaryContainer = PokeRedContainer,
    onPrimaryContainer = Color(0xFF410002),
    secondary = PokeBlue,
    onSecondary = Color.White,
    secondaryContainer = PokeBlueContainer,
    onSecondaryContainer = Color(0xFF001B3D),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF0F1F5),
    onSurfaceVariant = Color(0xFF43474E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color(0xFF3A0000),
    primaryContainer = Color(0xFF8E0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF81C784),
    background = Color(0xFF101216),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF181B21),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF262A31),
    onSurfaceVariant = Color(0xFFC3C7CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * Цвета типов покемонов. Используются в чипах на экране деталей.
 * Неизвестный тип получает нейтральный серый, поэтому новые типы из API
 * приложение не сломают.
 */
fun typeColor(typeName: String): Color = when (typeName.lowercase()) {
    "normal" -> Color(0xFF9FA19F)
    "fire" -> Color(0xFFE62829)
    "water" -> Color(0xFF2980EF)
    "electric" -> Color(0xFFFAC000)
    "grass" -> Color(0xFF3FA129)
    "ice" -> Color(0xFF3DCEF3)
    "fighting" -> Color(0xFFA52A2A)
    "poison" -> Color(0xFF9141CB)
    "ground" -> Color(0xFF915121)
    "flying" -> Color(0xFF81B9EF)
    "psychic" -> Color(0xFFEF4179)
    "bug" -> Color(0xFF6FA02B)
    "rock" -> Color(0xFFAFA981)
    "ghost" -> Color(0xFF704170)
    "dragon" -> Color(0xFF5060E1)
    "dark" -> Color(0xFF4A4A4A)
    "steel" -> Color(0xFF8A8A9E)
    "fairy" -> Color(0xFFEF70EF)
    else -> Color(0xFF7A7A7A)
}

/** Русские подписи типов — интерфейс задания на русском, мелочь, но приятно. */
fun typeLabel(typeName: String): String = when (typeName.lowercase()) {
    "normal" -> "Обычный"
    "fire" -> "Огонь"
    "water" -> "Вода"
    "electric" -> "Электро"
    "grass" -> "Трава"
    "ice" -> "Лёд"
    "fighting" -> "Боевой"
    "poison" -> "Яд"
    "ground" -> "Земля"
    "flying" -> "Летящий"
    "psychic" -> "Психо"
    "bug" -> "Жук"
    "rock" -> "Камень"
    "ghost" -> "Призрак"
    "dragon" -> "Дракон"
    "dark" -> "Тёмный"
    "steel" -> "Сталь"
    "fairy" -> "Фея"
    else -> typeName.replaceFirstChar { it.uppercase() }
}

/** Название характеристики из API («special-attack») в читаемый вид. */
fun statLabel(statName: String): String = when (statName.lowercase()) {
    "hp" -> "Здоровье"
    "attack" -> "Атака"
    "defense" -> "Защита"
    "special-attack" -> "Спец. атака"
    "special-defense" -> "Спец. защита"
    "speed" -> "Скорость"
    else -> statName.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

/** «overgrow» -> «Overgrow». */
fun prettyName(raw: String): String =
    raw.replace('-', ' ').trim().replaceFirstChar { it.uppercase() }

@Composable
fun PokeExplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
