package uz.tanish.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import uz.tanish.R
import uz.tanish.face.AnalyzerMode
import uz.tanish.face.Decision
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.FaceCamera
import uz.tanish.ui.components.MessageWithBack
import uz.tanish.ui.components.PatientScale
import uz.tanish.ui.components.PermissionMessage
import uz.tanish.ui.components.PersonPhoto
import uz.tanish.ui.components.openAppSettings
import uz.tanish.ui.components.rememberPermission
import uz.tanish.ui.viewmodels.CameraHint
import uz.tanish.ui.viewmodels.RecognitionViewModel
import uz.tanish.ui.viewmodels.ResultViewModel

/** Natija ekranidan avtomatik qaytish vaqti. */
private const val AUTO_HOME_MS = 10_000L

/** Bemor: kamera ekrani (TZ, 5-bo'lim, bemor rejimi 2-band). */
@Composable
fun RecognizeScreen(
    fontScale: Float,
    onRecognized: (Long) -> Unit,
    onNotRecognized: (sure: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val vm = appViewModel { RecognitionViewModel(it, testMode = false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val camera = rememberPermission(Manifest.permission.CAMERA)
    var lens by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    LaunchedEffect(Unit) { if (!camera.granted) camera.request() }
    LaunchedEffect(state.decision) {
        when (val d = state.decision) {
            is Decision.Recognized -> onRecognized(d.personId)
            Decision.Unknown -> onNotRecognized(true)
            Decision.Unsure -> onNotRecognized(false)
            null -> Unit
        }
    }

    PatientScale(fontScale) {
        when {
            !camera.granted && camera.denied -> PermissionMessage(
                title = stringResource(R.string.camera_permission_title),
                text = stringResource(R.string.camera_permission_text),
                buttonText = stringResource(R.string.camera_permission_button),
                onRequest = camera.request,
                onOpenSettings = { openAppSettings(context) },
                onBack = onBack,
                settingsText = stringResource(R.string.open_settings),
                backText = stringResource(R.string.back),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            )
            state.modelError -> MessageWithBack(stringResource(R.string.model_error), stringResource(R.string.back), onBack, Modifier.fillMaxSize())
            state.cameraError -> MessageWithBack(stringResource(R.string.camera_error), stringResource(R.string.back), onBack, Modifier.fillMaxSize())
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                    if (camera.granted && state.ready) {
                        FaceCamera(
                            lensFacing = lens,
                            mode = AnalyzerMode.RECOGNITION,
                            embedder = state.embedder,
                            wantPortrait = false,
                            onFrame = vm::onFrame,
                            onError = vm::onCameraError,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    val hint = when (state.hint) {
                        CameraHint.NO_FACE -> R.string.camera_point_face
                        CameraHint.CLOSER -> R.string.camera_closer
                        CameraHint.WAIT -> R.string.camera_wait
                    }
                    Text(
                        text = stringResource(hint),
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
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BigButton(
                        stringResource(R.string.switch_camera),
                        onClick = {
                            lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        outlined = true,
                    )
                    BigButton(stringResource(R.string.back), onBack)
                }
            }
        }
    }
}

/** Bemor: natija ekrani — rasm, katta yozuv, qarindosh ovozi. */
@Composable
fun ResultScreen(personId: Long, fontScale: Float, onHome: () -> Unit) {
    val vm = appViewModel { ResultViewModel(it, personId) }
    val state by vm.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.stop() }
    // Ovoz tugaganidan (yoki ovoz yo'q bo'lsa — ochilganidan) 10 soniya o'tib bosh ekranga qaytadi.
    LaunchedEffect(state.loading, state.playing) {
        if (!state.loading && !state.playing) {
            delay(AUTO_HOME_MS)
            onHome()
        }
    }

    PatientScale(fontScale) {
        val person = state.person
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.loading) return@Column
            if (person == null) {
                Spacer(Modifier.height(48.dp))
                Text(stringResource(R.string.not_known), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
            } else {
                PersonPhoto(person.photoPath, size = 300.dp)
                Text(
                    text = person.displayText,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                if (state.voiceEnabled && !person.voicePath.isNullOrBlank()) {
                    BigButton(stringResource(R.string.play_again), vm::play, enabled = !state.playing)
                }
            }
            BigButton(stringResource(R.string.go_home), onHome, outlined = true)
        }
    }
}

/** Bemor: "Notanish" yoki "Aniq bilmayman" — xotirjam ohangda. */
@Composable
fun NotRecognizedScreen(sure: Boolean, fontScale: Float, onRetry: () -> Unit, onHome: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(AUTO_HOME_MS)
        onHome()
    }
    PatientScale(fontScale) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(if (sure) R.string.not_known else R.string.not_sure),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            BigButton(stringResource(R.string.try_again), onRetry)
            BigButton(stringResource(R.string.go_home), onHome, outlined = true)
        }
    }
}
