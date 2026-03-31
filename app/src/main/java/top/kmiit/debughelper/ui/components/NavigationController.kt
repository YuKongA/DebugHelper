package top.kmiit.debughelper.ui.components

import android.hardware.Sensor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import top.kmiit.debughelper.ui.pages.AboutPage
import top.kmiit.debughelper.ui.pages.InfoPage
import top.kmiit.debughelper.ui.pages.SensorTestPage
import top.kmiit.debughelper.ui.pages.TestPage
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/**
 * 导航控制器 - 处理页面显示和导航逻辑
 */
@Composable
fun NavigationController(
    backStack: MutableList<AppNavKey>,
    selectedSensor: Sensor?,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onSelectedSensorChange: (Sensor?) -> Unit
) {
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                if (backStack.last() == AppNavKey.SensorTest) {
                    onSelectedSensorChange(null)
                }
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {
            entry<AppNavKey.Info> {
                InfoPage(paddingValues, scrollBehavior)
            }
            entry<AppNavKey.Test> {
                TestPage(
                    paddingValues,
                    scrollBehavior,
                    onTestSensor = { sensor ->
                        onSelectedSensorChange(sensor)
                        if (backStack.lastOrNull() != AppNavKey.SensorTest) {
                            backStack.add(AppNavKey.SensorTest)
                        }
                    }
                )
            }
            entry<AppNavKey.About> {
                AboutPage(paddingValues, scrollBehavior)
            }
            entry<AppNavKey.SensorTest> {
                if (selectedSensor != null) {
                    SensorTestPage(
                        sensor = selectedSensor,
                        onBack = {
                            onSelectedSensorChange(null)
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
            }
        }
    )
}


