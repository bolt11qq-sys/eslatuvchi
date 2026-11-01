package uz.tanish.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.tanish.R
import uz.tanish.ui.holdFor
import uz.tanish.ui.theme.Muted

/**
 * 0-bosqich: bo'sh bosh ekran.
 * Yuqori o'ngdagi qulfni 3 soniya bosib turish hozircha to'g'ridan-to'g'ri Jurnalga olib boradi.
 * 1-bosqichda bu yerga PIN so'rash va qarovchi rejimi qo'shiladi.
 */
@Composable
fun HomeScreen(onLockHeld: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.home_placeholder),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(64.dp)
                .holdFor(3_000L, onLockHeld),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.lock_description),
                tint = Muted.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
