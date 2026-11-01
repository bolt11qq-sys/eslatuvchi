package uz.tanish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import uz.tanish.ui.screens.HomeScreen
import uz.tanish.ui.screens.JournalScreen
import uz.tanish.ui.theme.TanishTheme

/** Ekranlar ro'yxati. Keyingi bosqichlarda kengaytiriladi. */
private enum class Screen { Home, Journal }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TanishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

                    Box(Modifier.safeDrawingPadding()) {
                        when (screen) {
                            Screen.Home -> HomeScreen(onLockHeld = { screen = Screen.Journal })
                            Screen.Journal -> JournalScreen(onBack = { screen = Screen.Home })
                        }
                    }
                }
            }
        }
    }
}
