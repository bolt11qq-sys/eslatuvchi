package uz.tanish.ui.components

import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import uz.tanish.face.AnalyzerMode
import uz.tanish.face.FaceEmbedder
import uz.tanish.face.FaceFrame
import uz.tanish.face.FaceFrameAnalyzer
import uz.tanish.face.FaceQuality
import uz.tanish.face.RecognitionConfig
import uz.tanish.log.ErrorLog
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * To'liq ekranli kamera + topilgan yuz atrofida ramka.
 * Kamera faqat shu komponent ekranda turganda ishlaydi; ekrandan chiqilganda darhol to'xtatiladi.
 * [onFrame] asosiy oqimda chaqiriladi.
 */
@Composable
fun FaceCamera(
    lensFacing: Int,
    mode: AnalyzerMode,
    embedder: FaceEmbedder?,
    wantPortrait: Boolean,
    onFrame: (FaceFrame) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnError by rememberUpdatedState(onError)
    var lastFrame by remember { mutableStateOf<FaceFrame?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lensFacing, embedder, lifecycleOwner) {
        val mainHandler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        var active = true
        val analyzer = FaceFrameAnalyzer(
            embedder = embedder,
            mode = mode,
            mirror = lensFacing == CameraSelector.LENS_FACING_FRONT,
            wantPortrait = wantPortrait,
        ) { frame ->
            mainHandler.post {
                if (active) {
                    lastFrame = frame
                    currentOnFrame(frame)
                }
            }
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!active) return@addListener
            try {
                val provider = future.get()
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                if (!provider.hasCamera(selector)) {
                    ErrorLog.error("Kamera", "Kerakli kamera topilmadi (lensFacing=$lensFacing)")
                    currentOnError()
                    return@addListener
                }
                val aspect = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
                val preview = Preview.Builder().setResolutionSelector(aspect).build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analysisSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(RecognitionConfig.ANALYSIS_WIDTH, RecognitionConfig.ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(analysisSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(executor, analyzer)

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                ErrorLog.error("Kamera", "Kamerani ishga tushirib bo'lmadi", e)
                currentOnError()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            active = false
            try {
                if (future.isDone) future.get().unbindAll()
            } catch (e: Exception) {
                ErrorLog.error("Kamera", "Kamerani to'xtatishda xato", e)
            }
            executor.execute { analyzer.close() }
            executor.shutdown()
            lastFrame = null
        }
    }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        FaceBoxOverlay(lastFrame, Modifier.fillMaxSize())
    }
}

@Composable
private fun FaceBoxOverlay(frame: FaceFrame?, modifier: Modifier) {
    Canvas(modifier) {
        val f = frame ?: return@Canvas
        val box = f.box ?: return@Canvas
        // PreviewView FILL_CENTER bilan bir xil o'lchov.
        val scale = max(size.width / f.imageWidth, size.height / f.imageHeight)
        val dx = (size.width - f.imageWidth * scale) / 2f
        val dy = (size.height - f.imageHeight * scale) / 2f
        val left = box.left * scale + dx
        val top = box.top * scale + dy
        val w = box.width() * scale
        val h = box.height() * scale
        val color = if (f.quality == FaceQuality.OK) Color(0xFFFFFFFF) else Color(0xB3FFFFFF)
        val r = CornerRadius(16.dp.toPx())
        drawRoundRect(
            color = Color(0x66000000),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = r,
            style = Stroke(width = 8.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = r,
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}
