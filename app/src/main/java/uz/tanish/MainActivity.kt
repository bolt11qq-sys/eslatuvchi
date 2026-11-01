package uz.tanish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import uz.tanish.data.Settings
import uz.tanish.log.ErrorLog
import uz.tanish.ui.TanishNavHost
import uz.tanish.ui.theme.TanishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TanishApp).container
        setContent {
            TanishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Boshlang'ich ekranni tanlash uchun sozlamalar bir marta o'qiladi.
                    val initial by produceState<Settings?>(initialValue = null) {
                        value = try {
                            container.settings.current()
                        } catch (e: Exception) {
                            ErrorLog.error("Ilova", "Sozlamalarni o'qib bo'lmadi", e)
                            null
                        }
                    }
                    Box(Modifier.safeDrawingPadding()) {
                        initial?.let { TanishNavHost(it) }
                    }
                }
            }
        }
    }
}
