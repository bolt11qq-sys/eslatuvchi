package uz.tanish.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.audio.VoiceRecorder
import uz.tanish.face.RecognitionConfig
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.PersonPhoto
import uz.tanish.ui.components.SectionTitle
import uz.tanish.ui.components.rememberPermission
import uz.tanish.ui.theme.Muted
import uz.tanish.ui.viewmodels.EditMessage
import uz.tanish.ui.viewmodels.PersonEditViewModel

/** Qarovchi: odam qo'shish / tahrirlash, yuz namunalari va ovoz (TZ, 5-bo'lim, 3–5-bandlar). */
@Composable
fun PersonEditScreen(
    personId: Long,
    onCaptureFaces: (Long) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val vm = appViewModel { PersonEditViewModel(it, context.applicationContext, personId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val savedText = stringResource(R.string.saved)
    val deletedText = stringResource(R.string.person_deleted)
    val micDenied = stringResource(R.string.voice_permission)

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickPhoto(uri)
    }
    val mic = rememberPermission(Manifest.permission.RECORD_AUDIO) { ok ->
        if (ok) vm.startRecording() else Toast.makeText(context, micDenied, Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) { vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stopAll() }
    LaunchedEffect(state.message) {
        if (state.message == EditMessage.SAVED) {
            Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            Toast.makeText(context, deletedText, Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    if (state.loading) return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (state.id > 0) R.string.person_edit else R.string.person_new),
            style = MaterialTheme.typography.headlineLarge,
        )

        // ---- Rasm ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonPhoto(state.photoPath.ifBlank { null }, size = 112.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.person_photo), style = MaterialTheme.typography.titleLarge)
                if (state.photoPath.isBlank() && state.pendingPhoto == null) {
                    Text(stringResource(R.string.person_photo_none), style = MaterialTheme.typography.bodyMedium, color = Muted)
                } else if (state.pendingPhoto != null) {
                    Text("✓", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        BigButton(
            stringResource(R.string.person_photo_pick),
            onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            outlined = true,
        )

        // ---- Matnlar ----
        val fieldStyle = TextStyle(fontSize = 22.sp)
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::setName,
            label = { Text(stringResource(R.string.person_name)) },
            singleLine = true,
            textStyle = fieldStyle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.relation,
            onValueChange = vm::setRelation,
            label = { Text(stringResource(R.string.person_relation)) },
            singleLine = true,
            textStyle = fieldStyle,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.displayText,
            onValueChange = vm::setDisplayText,
            label = { Text(stringResource(R.string.person_display_text)) },
            textStyle = fieldStyle,
            modifier = Modifier.fillMaxWidth(),
        )
        val error = when (state.message) {
            EditMessage.REQUIRED -> R.string.person_required
            EditMessage.PHOTO_ERROR -> R.string.person_photo_error
            EditMessage.VOICE_ERROR -> R.string.voice_error
            EditMessage.ERROR -> R.string.generic_error
            else -> null
        }
        if (error != null) Text(stringResource(error), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)

        BigButton(stringResource(R.string.save), vm::save, enabled = !state.busy && !state.recording)

        if (state.id <= 0) {
            Text(stringResource(R.string.person_save_first), style = MaterialTheme.typography.bodyMedium, color = Muted)
        } else {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---- Yuz namunalari ----
            SectionTitle(stringResource(R.string.person_samples, state.sampleCount))
            if (state.sampleCount < RecognitionConfig.ENROLL_MIN) {
                Text(stringResource(R.string.person_samples_warning), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            BigButton(
                stringResource(if (state.sampleCount > 0) R.string.person_samples_retake else R.string.person_samples_take),
                onClick = { vm.stopAll(); onCaptureFaces(state.id) },
                enabled = !state.recording,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---- Ovoz ----
            SectionTitle(stringResource(R.string.voice_title))
            Text(stringResource(R.string.voice_hint), style = MaterialTheme.typography.bodyMedium, color = Muted)
            when {
                state.recording -> {
                    Text(
                        stringResource(R.string.voice_recording, state.recordSeconds, VoiceRecorder.MAX_SECONDS),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    BigButton(stringResource(R.string.voice_stop), vm::stopRecording)
                }
                state.voicePath != null -> {
                    Text(stringResource(R.string.voice_exists), style = MaterialTheme.typography.bodyLarge)
                    BigButton(
                        stringResource(if (state.playing) R.string.voice_stop_play else R.string.voice_play),
                        vm::togglePlay,
                    )
                    BigButton(
                        stringResource(R.string.voice_rerecord),
                        onClick = { if (mic.granted) vm.startRecording() else mic.request() },
                        outlined = true,
                    )
                    BigButton(stringResource(R.string.voice_delete), vm::deleteVoice, outlined = true)
                }
                else -> {
                    Text(stringResource(R.string.voice_none), style = MaterialTheme.typography.bodyLarge)
                    BigButton(
                        stringResource(R.string.voice_start),
                        onClick = { if (mic.granted) vm.startRecording() else mic.request() },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            BigButton(stringResource(R.string.delete), { confirmDelete = true }, outlined = true, enabled = !state.recording)
        }

        BigButton(stringResource(R.string.back), { vm.stopAll(); onDone() }, outlined = true)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.person_delete_title, state.name)) },
            text = { Text(stringResource(R.string.person_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete()
                }) { Text(stringResource(R.string.yes_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.no)) }
            },
        )
    }
}
