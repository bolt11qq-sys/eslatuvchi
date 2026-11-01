package uz.tanish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.data.FontSize
import uz.tanish.data.ThresholdLevel
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.SectionTitle
import uz.tanish.ui.theme.Muted
import uz.tanish.ui.viewmodels.SettingsViewModel

/** Qarovchi: sozlamalar (TZ, 5-bo'lim, qarovchi rejimi 6-band). */
@Composable
fun SettingsScreen(
    onChangePin: () -> Unit,
    onTest: () -> Unit,
    onJournal: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
        if (s == null) return@Column

        SectionTitle(stringResource(R.string.settings_threshold))
        Choice(stringResource(R.string.threshold_strict), stringResource(R.string.threshold_strict_desc), s.threshold == ThresholdLevel.STRICT) {
            vm.setThreshold(ThresholdLevel.STRICT)
        }
        Choice(stringResource(R.string.threshold_medium), stringResource(R.string.threshold_medium_desc), s.threshold == ThresholdLevel.MEDIUM) {
            vm.setThreshold(ThresholdLevel.MEDIUM)
        }
        Choice(stringResource(R.string.threshold_soft), stringResource(R.string.threshold_soft_desc), s.threshold == ThresholdLevel.SOFT) {
            vm.setThreshold(ThresholdLevel.SOFT)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle(stringResource(R.string.settings_font))
        Choice(stringResource(R.string.font_normal), null, s.fontSize == FontSize.NORMAL) { vm.setFontSize(FontSize.NORMAL) }
        Choice(stringResource(R.string.font_large), null, s.fontSize == FontSize.LARGE) { vm.setFontSize(FontSize.LARGE) }
        Choice(stringResource(R.string.font_xlarge), null, s.fontSize == FontSize.XLARGE) { vm.setFontSize(FontSize.XLARGE) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(value = s.voiceEnabled, role = Role.Switch, onValueChange = vm::setVoiceEnabled),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_voice), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.settings_voice_desc), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            Switch(checked = s.voiceEnabled, onCheckedChange = null)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        BigButton(stringResource(R.string.settings_test), onTest)
        BigButton(stringResource(R.string.settings_change_pin), onChangePin, outlined = true)
        BigButton(stringResource(R.string.settings_journal), onJournal, outlined = true)
        BigButton(stringResource(R.string.back), onBack, outlined = true)
    }
}

@Composable
private fun Choice(title: String, description: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
