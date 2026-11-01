package uz.tanish.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Barmoq [millis] davomida qo'yib turilganda [onTriggered] chaqiriladi.
 * Tizimning odatiy uzoq bosishi (~0.5 s) bemor tasodifan bosib qo'yishi uchun juda qisqa.
 */
fun Modifier.holdFor(millis: Long, onTriggered: () -> Unit): Modifier = pointerInput(millis) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val released = withTimeoutOrNull(millis) { waitForUpOrCancellation() }
        if (released == null) {
            onTriggered()
            // Barmoq ko'tarilguncha qolgan hodisalarni yutib yuboramiz.
            waitForUpOrCancellation()
        }
    }
}
