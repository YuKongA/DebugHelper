package top.kmiit.debughelper.ui.pages

import android.hardware.Sensor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kmiit.debughelper.R
import top.kmiit.debughelper.ui.components.audio.AudioTestComponent
import top.kmiit.debughelper.ui.components.camera.CameraTestComponent
import top.kmiit.debughelper.ui.components.gnss.GNSSTestComponent
import top.kmiit.debughelper.ui.components.nfc.NfcTestComponent
import top.kmiit.debughelper.ui.components.sensor.SensorTestComponent
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.overScrollVertical
@Composable
fun TestPage(
    paddingValues: PaddingValues, 
    scrollBehavior: ScrollBehavior,
    onTestSensor: (Sensor) -> Unit
) {
    val tabs = listOf(
        stringResource(R.string.sensors),
        stringResource(R.string.audio),
        stringResource(R.string.camera),
        stringResource(R.string.gnss),
        stringResource(R.string.nfc),
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding()
            ),
    ) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            minWidth = 140.dp,
            modifier = Modifier.padding(8.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            when (it) {
                0 -> SensorTestComponent(scrollBehavior, onTestSensor)
                1 -> AudioTestComponent(scrollBehavior)
                2 -> CameraTestComponent(scrollBehavior)
                3 -> GNSSTestComponent(scrollBehavior)
                4 -> NfcTestComponent(scrollBehavior)
                else -> Text("test")
            }
        }
    }
}
