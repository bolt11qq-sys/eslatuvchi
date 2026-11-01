package uz.tanish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.tanish.R
import uz.tanish.ui.components.BigButton

/** Birinchi ochilishdagi ogohlantirish (TZ, 4-bo'lim). */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(stringResource(R.string.disclaimer_text), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        BigButton(stringResource(R.string.disclaimer_ok), onAccept)
    }
}
