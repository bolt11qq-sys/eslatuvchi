package uz.tanish.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.tanish.AppContainer
import uz.tanish.TanishApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as TanishApp).container

/** ViewModel'ni ilova konteyneri bilan yaratadi (joriy ekran doirasida). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel { create(container) }
}
