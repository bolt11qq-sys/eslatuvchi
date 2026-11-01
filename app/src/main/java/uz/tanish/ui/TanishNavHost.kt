package uz.tanish.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import uz.tanish.data.Settings
import uz.tanish.log.ErrorLog
import uz.tanish.ui.screens.DisclaimerScreen
import uz.tanish.ui.screens.FaceCaptureScreen
import uz.tanish.ui.screens.HomeScreen
import uz.tanish.ui.screens.JournalScreen
import uz.tanish.ui.screens.NotRecognizedScreen
import uz.tanish.ui.screens.PeopleScreen
import uz.tanish.ui.screens.PersonEditScreen
import uz.tanish.ui.screens.PinScreen
import uz.tanish.ui.screens.RecognizeScreen
import uz.tanish.ui.screens.ResultScreen
import uz.tanish.ui.screens.SettingsScreen
import uz.tanish.ui.screens.TestRecognitionScreen
import uz.tanish.ui.viewmodels.PinMode

private object Routes {
    const val DISCLAIMER = "disclaimer"
    const val PIN_SETUP = "pin_setup"
    const val HOME = "home"
    const val RECOGNIZE = "recognize"
    const val RESULT = "result/{id}"
    const val NOT_RECOGNIZED = "not_recognized/{sure}"
    const val PIN_ENTER = "pin_enter"
    const val PEOPLE = "people"
    const val PERSON = "person/{id}"
    const val CAPTURE = "capture/{id}"
    const val SETTINGS = "settings"
    const val PIN_CHANGE = "pin_change"
    const val TEST = "test"
    const val JOURNAL = "journal"

    /** Qarovchi rejimidagi ekranlar — ilova uzoq fonda qolsa, bemor rejimiga qaytariladi. */
    val caregiver = setOf(PEOPLE, PERSON, CAPTURE, SETTINGS, PIN_CHANGE, TEST, JOURNAL)

    fun result(id: Long) = "result/$id"
    fun notRecognized(sure: Boolean) = "not_recognized/$sure"
    fun person(id: Long) = "person/$id"
    fun capture(id: Long) = "capture/$id"
}

/** Qarovchi rejimidan chiqib ketgandan keyin shuncha vaqt o'tsa, qayta PIN so'raladi. */
private const val AUTO_LOCK_MS = 60_000L

@Composable
fun TanishNavHost(initial: Settings) {
    val nav = rememberNavController()
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = initial)
    val fontScale = settings.fontSize.scale

    val start = remember {
        when {
            !initial.disclaimerAccepted -> Routes.DISCLAIMER
            !initial.hasPin -> Routes.PIN_SETUP
            else -> Routes.HOME
        }
    }

    var stoppedAt by remember { mutableLongStateOf(0L) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stoppedAt = SystemClock.elapsedRealtime() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val route = nav.currentDestination?.route
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > AUTO_LOCK_MS && route in Routes.caregiver) {
            nav.goHome()
        }
        stoppedAt = 0L
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(onAccept = {
                scope.launch {
                    try {
                        container.settings.acceptDisclaimer()
                    } catch (e: Exception) {
                        ErrorLog.error("Ogohlantirish", "Saqlab bo'lmadi", e)
                    }
                    val next = if (settings.hasPin) Routes.HOME else Routes.PIN_SETUP
                    nav.navigate(next) { popUpTo(Routes.DISCLAIMER) { inclusive = true } }
                }
            })
        }
        composable(Routes.PIN_SETUP) {
            PinScreen(mode = PinMode.SETUP, onSuccess = { nav.goHome() }, onCancel = null)
        }

        // ---- Bemor rejimi ----
        composable(Routes.HOME) {
            HomeScreen(
                onWhoIsThis = { nav.navigate(Routes.RECOGNIZE) { launchSingleTop = true } },
                onLockHeld = { nav.navigate(Routes.PIN_ENTER) { launchSingleTop = true } },
            )
        }
        composable(Routes.RECOGNIZE) {
            RecognizeScreen(
                fontScale = fontScale,
                onRecognized = { id -> nav.navigate(Routes.result(id)) { popUpTo(Routes.HOME) } },
                onNotRecognized = { sure -> nav.navigate(Routes.notRecognized(sure)) { popUpTo(Routes.HOME) } },
                onBack = { nav.goHome() },
            )
        }
        composable(Routes.RESULT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ResultScreen(personId = id, fontScale = fontScale, onHome = { nav.goHome() })
        }
        composable(Routes.NOT_RECOGNIZED, arguments = listOf(navArgument("sure") { type = NavType.BoolType })) { entry ->
            val sure = entry.arguments?.getBoolean("sure") ?: false
            NotRecognizedScreen(
                sure = sure,
                fontScale = fontScale,
                onRetry = { nav.navigate(Routes.RECOGNIZE) { popUpTo(Routes.HOME) } },
                onHome = { nav.goHome() },
            )
        }

        // ---- Qarovchi rejimi ----
        composable(Routes.PIN_ENTER) {
            PinScreen(
                mode = PinMode.ENTER,
                onSuccess = { nav.navigate(Routes.PEOPLE) { popUpTo(Routes.HOME) } },
                onCancel = { nav.goHome() },
            )
        }
        composable(Routes.PEOPLE) {
            PeopleScreen(
                onAdd = { nav.navigate(Routes.person(0)) },
                onOpen = { id -> nav.navigate(Routes.person(id)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onExit = { nav.goHome() },
            )
        }
        composable(Routes.PERSON, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            PersonEditScreen(
                personId = id,
                onCaptureFaces = { pid -> nav.navigate(Routes.capture(pid)) },
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.CAPTURE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            FaceCaptureScreen(personId = id, onDone = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onChangePin = { nav.navigate(Routes.PIN_CHANGE) },
                onTest = { nav.navigate(Routes.TEST) },
                onJournal = { nav.navigate(Routes.JOURNAL) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PIN_CHANGE) {
            PinScreen(mode = PinMode.CHANGE, onSuccess = { nav.popBackStack() }, onCancel = { nav.popBackStack() })
        }
        composable(Routes.TEST) {
            TestRecognitionScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.JOURNAL) {
            JournalScreen(onBack = { nav.popBackStack() })
        }
    }
}

/** Bemor bosh ekraniga qaytish: orqadagi barcha ekranlar tozalanadi. */
private fun NavHostController.goHome() {
    navigate(Routes.HOME) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
