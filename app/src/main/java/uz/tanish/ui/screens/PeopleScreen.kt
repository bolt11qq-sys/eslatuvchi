package uz.tanish.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.data.db.PersonWithCount
import uz.tanish.face.RecognitionConfig
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.PersonPhoto
import uz.tanish.ui.theme.Muted
import uz.tanish.ui.theme.SoftSurface
import uz.tanish.ui.viewmodels.PeopleViewModel

/** Qarovchi: odamlar ro'yxati (TZ, 5-bo'lim, qarovchi rejimi 2-band). */
@Composable
fun PeopleScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
) {
    val vm = appViewModel { PeopleViewModel(it) }
    val people by vm.people.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.people_title), style = MaterialTheme.typography.headlineLarge)
        BigButton(stringResource(R.string.people_add), onAdd)

        val list = people
        if (list != null && list.isEmpty()) {
            Text(stringResource(R.string.people_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list.orEmpty(), key = { it.person.id }) { item -> PersonRow(item) { onOpen(item.person.id) } }
            }
        }

        BigButton(stringResource(R.string.settings), onSettings, outlined = true)
        BigButton(stringResource(R.string.patient_mode), onExit, outlined = true)
    }
}

@Composable
private fun PersonRow(item: PersonWithCount, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftSurface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PersonPhoto(item.person.photoPath, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.person.name, style = MaterialTheme.typography.titleLarge)
                Text(item.person.relation, style = MaterialTheme.typography.bodyLarge)
                val low = item.sampleCount < RecognitionConfig.ENROLL_MIN
                Text(
                    stringResource(if (low) R.string.people_samples_low else R.string.people_samples, item.sampleCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            }
        }
    }
}
