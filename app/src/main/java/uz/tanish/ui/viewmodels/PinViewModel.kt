package uz.tanish.ui.viewmodels

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.tanish.data.SettingsRepository
import uz.tanish.log.ErrorLog

enum class PinMode { SETUP, CHANGE, ENTER }
enum class PinPhase { FIRST, CONFIRM }
enum class PinMessage { NONE, MISMATCH, TOO_SHORT, WRONG, ERROR }

data class PinUiState(
    val digits: String = "",
    val phase: PinPhase = PinPhase.FIRST,
    val message: PinMessage = PinMessage.NONE,
    val busy: Boolean = false,
    val lockedSeconds: Int = 0,
    val success: Boolean = false,
)

class PinViewModel(private val settings: SettingsRepository, val mode: PinMode) : ViewModel() {
    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state

    private var firstPin = ""
    private var wrongCount = 0
    private var lockedUntil = 0L

    fun digit(d: Char) = _state.update {
        if (it.busy || it.lockedSeconds > 0 || it.digits.length >= MAX) it
        else it.copy(digits = it.digits + d, message = PinMessage.NONE)
    }

    fun erase() = _state.update { if (it.busy) it else it.copy(digits = it.digits.dropLast(1)) }

    fun submit() {
        val s = _state.value
        if (s.busy || s.lockedSeconds > 0) return
        if (s.digits.length < MIN) {
            _state.update { it.copy(message = PinMessage.TOO_SHORT) }
            return
        }
        when (mode) {
            PinMode.ENTER -> check(s.digits)
            PinMode.SETUP, PinMode.CHANGE -> if (s.phase == PinPhase.FIRST) {
                firstPin = s.digits
                _state.update { it.copy(digits = "", phase = PinPhase.CONFIRM) }
            } else if (s.digits != firstPin) {
                firstPin = ""
                _state.update { it.copy(digits = "", phase = PinPhase.FIRST, message = PinMessage.MISMATCH) }
            } else {
                save(s.digits)
            }
        }
    }

    private fun check(pin: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val ok = try {
                withContext(Dispatchers.Default) { settings.checkPin(pin) }
            } catch (e: Exception) {
                ErrorLog.error("PIN", "PIN'ni tekshirishda xato", e)
                false
            }
            if (ok) {
                wrongCount = 0
                _state.update { it.copy(busy = false, success = true) }
            } else {
                wrongCount++
                _state.update { it.copy(busy = false, digits = "", message = PinMessage.WRONG) }
                if (wrongCount >= 5) lockFor(30)
            }
        }
    }

    private fun lockFor(seconds: Int) {
        lockedUntil = SystemClock.elapsedRealtime() + seconds * 1000L
        viewModelScope.launch {
            while (true) {
                val left = ((lockedUntil - SystemClock.elapsedRealtime()) / 1000L).toInt() + 1
                if (SystemClock.elapsedRealtime() >= lockedUntil) break
                _state.update { it.copy(lockedSeconds = left) }
                delay(500)
            }
            wrongCount = 0
            _state.update { it.copy(lockedSeconds = 0, message = PinMessage.NONE) }
        }
    }

    private fun save(pin: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { settings.setPin(pin) }
                _state.update { it.copy(busy = false, success = true) }
            } catch (e: Exception) {
                ErrorLog.error("PIN", "PIN'ni saqlashda xato", e)
                firstPin = ""
                _state.update { PinUiState(message = PinMessage.ERROR) }
            }
        }
    }

    companion object {
        const val MIN = 4
        const val MAX = 6
    }
}
