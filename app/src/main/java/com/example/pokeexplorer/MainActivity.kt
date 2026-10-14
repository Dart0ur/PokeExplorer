package com.example.pokeexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pokeexplorer.navigation.PokeExplorerNavHost
import com.example.pokeexplorer.ui.theme.PokeExplorerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity. @AndroidEntryPoint обязателен: без него hiltViewModel()
 * внутри NavHost бросит исключение о том, что компонент-владелец не реализует
 * GeneratedComponentManager.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokeExplorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PokeExplorerNavHost()
                }
            }
        }
    }
}
