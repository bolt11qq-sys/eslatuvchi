package uz.tanish.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.tanish.R
import uz.tanish.face.AnalyzerMode
import uz.tanish.face.Decision
import uz.tanish.face.FrameVerdict
import uz.tanish.face.RecognitionConfig
import uz.tanish.ui.appViewModel
import uz.tanish.ui.components.BigButton
import uz.tanish.ui.components.FaceCamera
import uz.tanish.ui.components.MessageWithBack
import uz.tanish.ui.components.PermissionMessage
import uz.tanish.ui.components.openAppSettings
import uz.tanish.ui.components.rememberPermission
import uz.tanish.ui.viewmodels.RecognitionViewModel
import kotlin.math.roundToInt

/** Qarovchi: "Tanishni sinash" — eng yaqin odam va o'xshashlik foizi (TZ, 4-bosqich). */
@Composable
fun TestRecognitionScreen(onBack: () -> Unit) {
    val vm = appViewModel { RecognitionViewModel(it, testMode = true) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val camera = rememberPermission(Manifest.permission.CAMERA)
    var lens by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    LaunchedEffect(Unit) { if (!camera.granted) camera.request() }

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
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.test_title), style = MaterialTheme.typography.titleLarge)
                if (state.emptyDatabase) {
                    Text(stringResource(R.string.test_empty_db), style = MaterialTheme.typography.bodyLarge)
                }
                val sc = state.scores
                val nobody = stringResource(R.string.test_nobody)
                Text(
                    stringResource(R.string.test_best, state.bestName ?: nobody, pct(sc?.best)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.test_second, state.secondName ?: nobody, pct(sc?.second)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val verdict = when (state.lastVerdict) {
                    is FrameVerdict.Match -> stringResource(R.string.test_verdict_match)
                    FrameVerdict.Unknown -> stringResource(R.string.test_verdict_unknown)
                    FrameVerdict.Unsure -> stringResource(R.string.test_verdict_unsure)
                    null -> ""
                }
                if (verdict.isNotEmpty()) Text(verdict, style = MaterialTheme.typography.bodyLarge)
                val decision = when (val d = state.lastDecision) {
                    is Decision.Recognized -> stringResource(R.string.test_decision_match, vm.nameOf(d.personId) ?: d.personId.toString())
                    Decision.Unknown -> stringResource(R.string.test_decision_unknown)
                    Decision.Unsure -> stringResource(R.string.test_decision_unsure)
                    null -> ""
                }
                if (decision.isNotEmpty()) Text(decision, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(
                        R.string.test_thresholds,
                        pct(RecognitionConfig.LOWER_THRESHOLD),
                        pct(RecognitionConfig.upperThreshold(state.threshold)),
                        pct(RecognitionConfig.MARGIN),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
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

private fun pct(v: Float?): Int = ((v ?: 0f) * 100).roundToInt()
