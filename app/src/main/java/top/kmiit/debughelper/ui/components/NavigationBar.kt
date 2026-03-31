package top.kmiit.debughelper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import top.kmiit.debughelper.R
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Info

sealed interface AppNavKey : NavKey {
    data object Info : AppNavKey
    data object Test : AppNavKey
    data object About : AppNavKey
    data object SensorTest : AppNavKey
}

private data class NavigationItemConfig(
    val key: AppNavKey,
    val labelResId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val NAVIGATION_ITEMS = listOf(
    NavigationItemConfig(AppNavKey.Info, R.string.info, MiuixIcons.Album),
    NavigationItemConfig(AppNavKey.Test, R.string.test, MiuixIcons.Basic.Check),
    NavigationItemConfig(AppNavKey.About, R.string.about, MiuixIcons.Info)
)

/**
 * App导航栏组件
 * 仅在不是SensorTest页面时显示
 */
@Composable
fun NavigationBar(
    currentRoute: AppNavKey,
    onNavigate: (AppNavKey) -> Unit
) {
    // 仅在不是SensorTest时显示导航栏
    if (currentRoute == AppNavKey.SensorTest) {
        return
    }

    val navItems = remember {
        NAVIGATION_ITEMS
    }

    MiuixNavigationBar {
        navItems.forEach { config ->
            NavigationBarItem(
                selected = config.key == currentRoute,
                onClick = { onNavigate(config.key) },
                icon = config.icon,
                label = stringResource(config.labelResId)
            )
        }
    }
}