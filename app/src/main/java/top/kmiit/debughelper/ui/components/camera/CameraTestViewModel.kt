package top.kmiit.debughelper.ui.components.camera

import android.app.Application
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.camera.core.Camera
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TorchConfig(
    val supportsTorch: Boolean,
    val supportsFlashStrengthControl: Boolean,
    val maxFlashStrength: Int = 0
)

data class CameraInfoItem(
    val id: String,
    val facing: String,
    val resolution: String,
    val sensorOrientation: Int,
    val torchConfig: TorchConfig
)

open class CameraTestViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _cameras = MutableStateFlow<List<CameraInfoItem>>(emptyList())
    val cameras = _cameras.asStateFlow()

    private val _selectedCameraId = MutableStateFlow<String?>(null)
    val selectedCameraId = _selectedCameraId.asStateFlow()

    private var torchHelper: CameraTorchHelper? = null
    private val _torchStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val torchStates = _torchStates.asStateFlow()

    init {
        loadCameras()
    }

    fun loadCameras() {
        try {
            val cameraIds = cameraManager.cameraIdList
            val cameraList = cameraIds.map { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                
                // Get sensor resolution
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes = map?.getOutputSizes(ImageFormat.JPEG)
                val maxResolution = sizes?.maxByOrNull { it.width * it.height }
                val resolutionStr = if (maxResolution != null) {
                    "${maxResolution.width}x${maxResolution.height} (${(maxResolution.width * maxResolution.height) / 1000000}MP)"
                } else {
                    "Unknown"
                }
                
                val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

                // Get torch configuration
                val torchConfig = getTorchConfig(id, characteristics)

                CameraInfoItem(id, facing, resolutionStr, orientation, torchConfig)
            }
            _cameras.value = cameraList
        } catch (e: Exception) {
            e.printStackTrace()
            _cameras.value = emptyList()
        }
    }

    private fun getTorchConfig(cameraId: String, characteristics: CameraCharacteristics): TorchConfig {
        return try {
            // Check if camera has flash unit
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // Check if camera supports torch mode (continuous LED)
            val supportsTorch = hasFlash && characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT

            // Check if camera supports flash strength control (API 33+)
            var supportsFlashStrengthControl = false
            var maxFlashStrength = 0

            if (supportsTorch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @Suppress("NewApi")
                val maxLevel = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                if (maxLevel > 1) {
                    supportsFlashStrengthControl = true
                    maxFlashStrength = maxLevel
                }
            }

            TorchConfig(
                supportsTorch = supportsTorch,
                supportsFlashStrengthControl = supportsFlashStrengthControl,
                maxFlashStrength = maxFlashStrength
            )
        } catch (e: Exception) {
            e.printStackTrace()
            TorchConfig(supportsTorch = false, supportsFlashStrengthControl = false)
        }
    }

    fun selectCamera(id: String?) {
        _selectedCameraId.value = id
    }

    /**
     * Initialize torch helper for torch control
     */
    fun initializeTorchHelper() {
        if (torchHelper != null) return

        torchHelper = CameraTorchHelper(getApplication())
        viewModelScope.launch {
            try {
                torchHelper?.initialize()
            } catch (e: Exception) {
                Log.e("CameraTestViewModel", "Failed to initialize torch helper", e)
            }
        }
    }

    fun onPreviewCameraBound(camera: Camera?) {
        torchHelper?.attachPreviewCamera(camera)
    }

    /**
     * Toggle torch for specified camera
     */
    fun toggleTorch(cameraId: String, enabled: Boolean) {
        val helper = torchHelper ?: run {
            Log.e("CameraTestViewModel", "Torch helper not initialized")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraTestViewModel", "Toggling torch for camera $cameraId, enabled=$enabled")
                val success = if (enabled) {
                    helper.enableTorch(cameraId)
                } else {
                    helper.disableTorch()
                }

                if (success) {
                    // Update state only if operation succeeded
                    _torchStates.value = _torchStates.value.toMutableMap().apply {
                        this[cameraId] = enabled
                    }
                    Log.d("CameraTestViewModel", "Torch toggled successfully")
                } else {
                    Log.e("CameraTestViewModel", "Failed to toggle torch")
                }
            } catch (e: Exception) {
                Log.e("CameraTestViewModel", "Failed to toggle torch", e)
            }
        }
    }

    /**
     * Set torch strength for specified camera (API 33+)
     */
    fun setTorchStrength(cameraId: String, strength: Int) {
        val helper = torchHelper ?: return

        viewModelScope.launch {
            try {
                val success = helper.setTorchStrength(cameraId, strength)
                if (success) {
                    _torchStates.value = _torchStates.value.toMutableMap().apply {
                        this[cameraId] = true
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraTestViewModel", "Failed to set torch strength", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        torchHelper?.release()
        torchHelper = null
    }
}
