package top.kmiit.debughelper.ui

import android.hardware.Sensor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.kmiit.debughelper.ui.components.NavigationBar
import top.kmiit.debughelper.ui.components.NavigationController
import top.kmiit.debughelper.ui.components.AppNavKey
import top.kmiit.debughelper.ui.components.TopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun App(
) {
    MiuixTheme(
        controller = ThemeController(ColorSchemeMode.System),
    ) {
        val scrollBehavior = MiuixScrollBehavior()
        val backStack = remember { mutableStateListOf<AppNavKey>(AppNavKey.Info) }
        var selectedSensor by remember { mutableStateOf<Sensor?>(null) }
        val currentRoute = backStack.lastOrNull() ?: AppNavKey.Info

        Scaffold(
            topBar = {
                if (currentRoute != AppNavKey.SensorTest) {
                    TopAppBar(scrollBehavior)
                }
            },
            bottomBar = {
                NavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        selectedSensor = null
                        if (backStack.lastOrNull() != targetRoute) {
                            backStack.clear()
                            backStack.add(targetRoute)
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavigationController(
                backStack = backStack,
                selectedSensor = selectedSensor,
                paddingValues = paddingValues,
                scrollBehavior = scrollBehavior,
                onSelectedSensorChange = { sensor ->
                    selectedSensor = sensor
                }
            )
        }
    }
}
