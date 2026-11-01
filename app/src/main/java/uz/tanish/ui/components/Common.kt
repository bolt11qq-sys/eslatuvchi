package uz.tanish.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.tanish.data.FileStore
import uz.tanish.log.ErrorLog
import uz.tanish.ui.theme.SoftSurface

/** Katta tugma: kamida 64dp (TZ, 7-bo'lim). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    val m = modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = m) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = m,
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Bemor ekranlarida shriftni sozlamaga qarab kattalashtiradi. */
@Composable
fun PatientScale(scale: Float, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * scale)) {
        content()
    }
}

/** Fayldan rasmni fon oqimida kichraytirib o'qiydi. */
@Composable
fun rememberPhoto(path: String?, maxSide: Int = 800): Bitmap? {
    val bmp by produceState<Bitmap?>(initialValue = null, path, maxSide) {
        value = if (path.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            try {
                FileStore.loadBitmap(path, maxSide)
            } catch (e: Exception) {
                ErrorLog.error("Rasm", "Rasmni o'qib bo'lmadi", e)
                null
            }
        }
    }
    return bmp
}

@Composable
fun PersonPhoto(path: String?, size: Dp, modifier: Modifier = Modifier) {
    val bmp = rememberPhoto(path, maxSide = 800)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(SoftSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Ruxsat berilmaganda xotirjam xabar va tugmalar. */
@Composable
fun PermissionMessage(
    title: String,
    text: String,
    buttonText: String,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    settingsText: String,
    backText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        BigButton(buttonText, onRequest)
        BigButton(settingsText, onOpenSettings, outlined = true)
        BigButton(backText, onBack, outlined = true)
    }
}

@Composable
fun MessageWithBack(text: String, backText: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        BigButton(backText, onBack)
    }
}

@Composable
fun SectionTitle(text: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        ErrorLog.error("Ruxsat", "Sozlamalarni ochib bo'lmadi", e)
    }
}
