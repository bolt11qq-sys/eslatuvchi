package uz.tanish.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class PermissionState(
    val granted: Boolean,
    /** Kamida bir marta so'ralib, rad etilgan. */
    val denied: Boolean,
    val request: () -> Unit,
)

/** Ruxsat holati. Telefon sozlamalaridan qaytilganda qayta tekshiriladi. */
@Composable
fun rememberPermission(permission: String, onResult: (Boolean) -> Unit = {}): PermissionState {
    val context = LocalContext.current
    fun check() = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(check()) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
        onResult(ok)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = check()
        if (granted) denied = false
    }
    return PermissionState(granted, denied) { launcher.launch(permission) }
}
