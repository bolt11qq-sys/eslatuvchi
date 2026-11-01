package uz.tanish.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tanish.R
import uz.tanish.ui.holdFor
import uz.tanish.ui.theme.Muted

/**
 * Bemor bosh ekrani: ekranning katta qismini egallagan bitta "Bu kim?" tugmasi.
 * Yuqori o'ngdagi kichik qulf 3 soniya bosib turilsa — PIN so'raladi.
 */
@Composable
fun HomeScreen(onWhoIsThis: () -> Unit, onLockHeld: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        Button(
            onClick = onWhoIsThis,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 24.dp, end = 24.dp, top = 88.dp, bottom = 48.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Text(
                text = stringResource(R.string.who_is_this),
                fontSize = 56.sp,
                lineHeight = 64.sp,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(64.dp)
                .holdFor(3_000L, onProgress = { progress = it }, onTriggered = onLockHeld),
            contentAlignment = Alignment.Center,
        ) {
            if (progress > 0f) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(44.dp),
                    color = Muted,
                    strokeWidth = 3.dp,
                )
            }
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.lock_description),
                tint = Muted.copy(alpha = if (progress > 0f) 0.9f else 0.45f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
