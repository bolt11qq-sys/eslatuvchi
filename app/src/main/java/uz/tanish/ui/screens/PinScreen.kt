package uz.tanish.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.ui.appViewModel
import uz.tanish.ui.theme.Muted
import uz.tanish.ui.viewmodels.PinMessage
import uz.tanish.ui.viewmodels.PinMode
import uz.tanish.ui.viewmodels.PinPhase
import uz.tanish.ui.viewmodels.PinViewModel

/** PIN o'rnatish, o'zgartirish yoki kiritish. */
@Composable
fun PinScreen(mode: PinMode, onSuccess: () -> Unit, onCancel: (() -> Unit)?) {
    val vm = appViewModel { PinViewModel(it.settings, mode) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) { if (state.success) onSuccess() }

    val title = when {
        mode == PinMode.ENTER -> stringResource(R.string.pin_enter_title)
        state.phase == PinPhase.CONFIRM -> stringResource(R.string.pin_confirm_title)
        mode == PinMode.CHANGE -> stringResource(R.string.pin_new_title)
        else -> stringResource(R.string.pin_setup_title)
    }
    val message = when {
        state.lockedSeconds > 0 -> stringResource(R.string.pin_wait, state.lockedSeconds)
        state.message == PinMessage.MISMATCH -> stringResource(R.string.pin_mismatch)
        state.message == PinMessage.TOO_SHORT -> stringResource(R.string.pin_too_short)
        state.message == PinMessage.WRONG -> stringResource(R.string.pin_wrong)
        state.message == PinMessage.ERROR -> stringResource(R.string.generic_error)
        mode == PinMode.SETUP && state.phase == PinPhase.FIRST -> stringResource(R.string.pin_setup_hint)
        else -> ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val shown = if (mode == PinMode.ENTER) maxOf(PinViewModel.MIN, state.digits.length) else PinViewModel.MAX
            repeat(shown) { i ->
                val filled = i < state.digits.length
                Box(
                    Modifier
                        .size(22.dp)
                        .then(
                            if (filled) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(2.dp, Muted, CircleShape)
                        )
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 56.dp),
        )
        Spacer(Modifier.weight(1f))

        val rows = listOf("123", "456", "789")
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d -> Key(d.toString(), Modifier.weight(1f)) { vm.digit(d) } }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Key("⌫", Modifier.weight(1f), outlined = true, description = stringResource(R.string.pin_erase)) { vm.erase() }
            Key("0", Modifier.weight(1f)) { vm.digit('0') }
            Key("✓", Modifier.weight(1f), description = stringResource(
                if (mode == PinMode.ENTER) R.string.pin_enter_button else R.string.pin_continue
            )) { vm.submit() }
        }
        Spacer(Modifier.height(16.dp))
        if (onCancel != null) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier,
    outlined: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val m = modifier.height(76.dp)
    val shape = RoundedCornerShape(20.dp)
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 30.sp)
            if (description != null) Text(description, fontSize = 12.sp)
        }
    }
    if (outlined) OutlinedButton(onClick = onClick, modifier = m, shape = shape) { content() }
    else Button(onClick = onClick, modifier = m, shape = shape) { content() }
}
