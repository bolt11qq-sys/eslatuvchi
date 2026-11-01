package uz.tanish.ui

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

private class Ended(@Suppress("unused") val change: PointerInputChange?)

/**
 * Barmoq [millis] davomida qo'yib turilganda [onTriggered] chaqiriladi.
 * [onProgress] 0..1 oralig'ida bosib turish jarayonini bildiradi (barmoq ko'tarilsa 0).
 * Tizimning odatiy uzoq bosishi (~0.5 s) bemor tasodifan bosib qo'yishi uchun juda qisqa.
 */
fun Modifier.holdFor(
    millis: Long,
    onProgress: (Float) -> Unit = {},
    onTriggered: () -> Unit,
): Modifier = pointerInput(millis) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val start = SystemClock.uptimeMillis()
        onProgress(0f)
        while (true) {
            // Natija null — vaqt tugadi (barmoq hali bosilgan); Ended — barmoq ko'tarildi yoki chiqib ketdi.
            val ended = withTimeoutOrNull(40L) { Ended(waitForUpOrCancellation()) }
            if (ended != null) {
                onProgress(0f)
                break
            }
            val elapsed = SystemClock.uptimeMillis() - start
            if (elapsed >= millis) {
                onProgress(0f)
                onTriggered()
                waitForUpOrCancellation()
                break
            }
            onProgress(elapsed.toFloat() / millis)
        }
    }
}
