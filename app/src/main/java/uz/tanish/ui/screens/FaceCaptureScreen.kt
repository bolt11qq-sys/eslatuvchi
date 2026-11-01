package uz.tanish.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.face.AnalyzerMode
import uz.tanish.face.RecognitionConfig
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.FaceCamera
import uz.tanish.ui.components.MessageWithBack
import uz.tanish.ui.components.PermissionMessage
import uz.tanish.ui.components.openAppSettings
import uz.tanish.ui.components.rememberPermission
import uz.tanish.ui.theme.Muted
import uz.tanish.ui.viewmodels.CaptureStatus
import uz.tanish.ui.viewmodels.FaceCaptureViewModel
import uz.tanish.ui.viewmodels.Pose

/** Qarovchi: yuz namunalarini olish (TZ, 5-bo'lim, qarovchi rejimi 4-band). */
@Composable
fun FaceCaptureScreen(personId: Long, onDone: () -> Unit) {
    val vm = appViewModel { FaceCaptureViewModel(it, personId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val camera = rememberPermission(Manifest.permission.CAMERA)
    var lens by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val savedText = stringResource(R.string.capture_saved)

    LaunchedEffect(Unit) { if (!camera.granted) camera.request() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    when {
        !camera.granted && camera.denied -> PermissionMessage(
            title = stringResource(R.string.camera_permission_title),
            text = stringResource(R.string.camera_permission_text),
            buttonText = stringResource(R.string.camera_permission_button),
            onRequest = camera.request,
            onOpenSettings = { openAppSettings(context) },
            onBack = onDone,
            settingsText = stringResource(R.string.open_settings),
            backText = stringResource(R.string.back),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        )
        state.modelError -> MessageWithBack(stringResource(R.string.model_error), stringResource(R.string.back), onDone, Modifier.fillMaxSize())
        state.cameraError -> MessageWithBack(stringResource(R.string.camera_error), stringResource(R.string.back), onDone, Modifier.fillMaxSize())
        else -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                if (camera.granted && state.ready && !state.done) {
                    FaceCamera(
                        lensFacing = lens,
                        mode = AnalyzerMode.ENROLL,
                        embedder = state.embedder,
                        wantPortrait = true,
                        onFrame = vm::onFrame,
                        onError = vm::onCameraError,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val instruction = if (state.done) stringResource(R.string.capture_done) else stringResource(
                    when (state.pose) {
                        Pose.FRONT -> R.string.capture_front
                        Pose.LEFT -> R.string.capture_left
                        Pose.RIGHT -> R.string.capture_right
                        Pose.UP -> R.string.capture_up
                        Pose.DOWN -> R.string.capture_down
                    }
                )
                Text(
                    instruction,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(Color(0x99000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.capture_counter, state.count, state.target),
                        fontSize = 40.sp,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    val status = when (state.status) {
                        CaptureStatus.NO_FACE -> R.string.capture_no_face
                        CaptureStatus.CLOSER -> R.string.capture_closer
                        CaptureStatus.BLURRY -> R.string.capture_blurry
                        CaptureStatus.DARK -> R.string.capture_dark
                        CaptureStatus.TURNED -> R.string.capture_turned
                        CaptureStatus.FOLLOW -> null
                        CaptureStatus.GOOD -> R.string.capture_good
                    }
                    if (status != null && !state.done) {
                        Text(
                            stringResource(status),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
                if (!state.canSave) {
                    Text(
                        stringResource(R.string.capture_min_hint, RecognitionConfig.ENROLL_MIN),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }
                if (state.saveError) Text(stringResource(R.string.generic_error), style = MaterialTheme.typography.bodyLarge)
                BigButton(stringResource(R.string.save), vm::save, enabled = state.canSave)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton(
                        stringResource(R.string.switch_camera),
                        onClick = {
                            lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        outlined = true,
                        modifier = Modifier.weight(1f),
                    )
                    BigButton(stringResource(R.string.capture_restart), vm::restart, outlined = true, modifier = Modifier.weight(1f))
                }
                BigButton(stringResource(R.string.cancel), onDone, outlined = true)
            }
        }
    }
}
