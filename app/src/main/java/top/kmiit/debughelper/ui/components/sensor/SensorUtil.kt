package top.kmiit.debughelper.ui.components.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import top.kmiit.debughelper.R
import top.kmiit.debughelper.ui.components.Utils.ShowItem
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight

data class SensorInfo(
    val name: String,
    val vendor: String,
    val version: Int,
    val type: Int,
    val maxRange: Float,
    val resolution: Float,
    val power: Float,
    val minDelay: Int,
    val fifoReservedEventCount: Int,
    val fifoMaxEventCount: Int,
    val stringType: String,
    val id: Int,
    val reportingMode: Int,
    val isWakeUpSensor: Boolean,
    val isDynamicSensor: Boolean,
    val isAdditionalInfoSupported: Boolean
)

fun Sensor.toSensorInfo(): SensorInfo {
    return SensorInfo(
        name = this.name,
        vendor = this.vendor,
        version = this.version,
        type = this.type,
        maxRange = this.maximumRange,
        resolution = this.resolution,
        power = this.power,
        minDelay = this.minDelay,
        fifoReservedEventCount = this.fifoReservedEventCount,
        fifoMaxEventCount = this.fifoMaxEventCount,
        stringType = this.stringType ?: "",
        id = this.id,
        reportingMode = this.reportingMode,
        isWakeUpSensor = this.isWakeUpSensor,
        isDynamicSensor = this.isDynamicSensor,
        isAdditionalInfoSupported = this.isAdditionalInfoSupported
    )
}

@Composable
fun Sensor.Show(onTestClick: (Sensor) -> Unit) {
    val sensor = this
    var showDialog by remember { mutableStateOf(false) }

    BasicComponent(
        title = sensor.name,
        summary = sensor.stringType,
        modifier = Modifier.fillMaxSize(),
        onClick = {
            showDialog = true
        },
        endActions = {
            if (sensor.batchable()) {
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = stringResource(R.string.test),
                    modifier = Modifier.clickable { onTestClick(sensor) }

                )
            }
        }
    )

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                insideMargin = PaddingValues(16.dp)
            ) {
                ListDetails()
            }
        }
    }
}

@Composable
fun Sensor.ListDetails() {
    val info = toSensorInfo()
    ShowItem(stringResource(R.string.sensor_name), info.name)
    ShowItem(stringResource(R.string.sensor_vendor), info.vendor)
    ShowItem(stringResource(R.string.sensor_version), info.version)
    ShowItem(stringResource(R.string.sensor_type), info.type)
    ShowItem(stringResource(R.string.sensor_max_range), info.maxRange)
    ShowItem(stringResource(R.string.sensor_resolution), info.resolution)
    ShowItem(stringResource(R.string.sensor_power), info.power)
    ShowItem(stringResource(R.string.sensor_min_delay), info.minDelay)
    ShowItem(stringResource(R.string.sensor_fifo_reserved), info.fifoReservedEventCount)
    ShowItem(stringResource(R.string.sensor_fifo_max_event_count), info.fifoMaxEventCount)
    ShowItem(stringResource(R.string.sensor_string_type), info.stringType)
    ShowItem(stringResource(R.string.sensor_id), info.id)
    ShowItem(stringResource(R.string.sensor_reporting_mode), info.reportingMode)
    ShowItem(stringResource(R.string.sensor_is_wake_up), info.isWakeUpSensor)
    ShowItem(stringResource(R.string.sensor_is_dynamic), info.isDynamicSensor)
    ShowItem(stringResource(R.string.sensor_is_additional_info_supported), info.isAdditionalInfoSupported)
}

@Composable
fun Sensor.Batch() {
    val sensor = this
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    val values by produceState<FloatArray?>(initialValue = null, sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    value = event.values.clone()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    if (values != null) {
        HandleData(values!!)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))
            Text(stringResource(R.string.waiting_for_data))
        }
    }
}

@Composable
fun Sensor.HandleData(values: FloatArray) {
    when(this.type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LINEAR_ACCELERATION,
            -> HandleXYZ(values)
        Sensor.TYPE_ORIENTATION -> HandleOrientation(values)

        else -> HandleDataCommon(values)
    }
}

@Composable
fun HandleDataCommon(values: FloatArray) {
    values.forEachIndexed { index, value ->
        val suffix = if (index == 0) "" else index.toString()
        ShowItem(stringResource(R.string.sensor_value) + " $suffix", value)
    }
}

@Composable
fun HandleXYZ(values: FloatArray) {
    ShowItem(stringResource(R.string.x_value), values[0])
    ShowItem(stringResource(R.string.y_value), values[1])
    ShowItem(stringResource(R.string.z_value), values[2])
}

@Composable
fun HandleOrientation(values: FloatArray) {
    ShowItem(stringResource(R.string.orientation), values[0])
    ShowItem(stringResource(R.string.x_value), values[1])
    ShowItem(stringResource(R.string.y_value), values[2])
}

fun Sensor.batchable(): Boolean {
    return when(this.type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_GAME_ROTATION_VECTOR,
            -> true
        else -> false
    }
}