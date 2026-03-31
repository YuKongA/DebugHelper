package top.kmiit.debughelper.ui.components.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class to manage camera torch (flashlight) functionality.
 * Provides methods to enable/disable torch and check torch support.
 */
class CameraTorchHelper(
    context: Context
) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _torchState = MutableStateFlow<TorchState>(TorchState.UNKNOWN)
    val torchState = _torchState.asStateFlow()

    private var previewCamera: Camera? = null
    private var activeTorchCameraId: String? = null

    sealed class TorchState {
        object UNKNOWN : TorchState()
        object UNSUPPORTED : TorchState()
        object OFF : TorchState()
        object ON : TorchState()
        data class ERROR(val message: String) : TorchState()
    }

    /**
     * Initialize camera provider
     */
    suspend fun initialize() {
        _torchState.value = TorchState.OFF
    }

    /**
     * Attach preview camera instance so torch control does not create a competing CameraX session.
     */
    fun attachPreviewCamera(camera: Camera?) {
        previewCamera = camera
    }

    /**
     * Enable torch for specified camera ID
     * @param cameraId The camera ID to enable torch for
     * @return true if torch was enabled, false otherwise
     */
    suspend fun enableTorch(cameraId: String): Boolean {
        return try {
            val success = if (previewCamera != null) {
                previewCamera?.cameraControl?.enableTorch(true)
                true
            } else {
                cameraManager.setTorchMode(cameraId, true)
                true
            }

            activeTorchCameraId = cameraId
            _torchState.value = TorchState.ON
            Log.d("CameraTorchHelper", "Torch enabled for camera $cameraId")
            success
        } catch (e: Exception) {
            Log.e("CameraTorchHelper", "Failed to enable torch", e)
            _torchState.value = TorchState.ERROR("Failed to enable torch: ${e.message}")
            false
        }
    }

    /**
     * Disable torch
     * @return true if torch was disabled, false otherwise
     */
    fun disableTorch(): Boolean {
        return try {
            val cameraId = activeTorchCameraId
            val success = if (previewCamera != null) {
                previewCamera?.cameraControl?.enableTorch(false)
                true
            } else if (!cameraId.isNullOrEmpty()) {
                cameraManager.setTorchMode(cameraId, false)
                true
            } else {
                true
            }

            _torchState.value = TorchState.OFF
            activeTorchCameraId = null
            Log.d("CameraTorchHelper", "Torch disabled")
            success
        } catch (e: Exception) {
            Log.e("CameraTorchHelper", "Failed to disable torch", e)
            _torchState.value = TorchState.ERROR("Failed to disable torch: ${e.message}")
            false
        }
    }

    /**
     * Set torch strength level on API 33+.
     */
    fun setTorchStrength(cameraId: String, strength: Int): Boolean {
        return try {
            val level = strength.coerceAtLeast(1)
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, level)
            activeTorchCameraId = cameraId
            _torchState.value = TorchState.ON
            true
        } catch (e: Exception) {
            Log.e("CameraTorchHelper", "Failed to set torch strength", e)
            false
        }
    }

    /**
     * Toggle torch on/off
     * @param cameraId The camera ID to toggle torch for
     * @return true if operation succeeded, false otherwise
     */
    suspend fun toggleTorch(cameraId: String): Boolean {
        return when (torchState.value) {
            is TorchState.ON -> disableTorch()
            is TorchState.OFF -> enableTorch(cameraId)
            else -> false
        }
    }

    /**
     * Check if torch is enabled
     * @return true if torch is on, false otherwise
     */
    fun isTorchEnabled(): Boolean {
        return torchState.value is TorchState.ON
    }

    /**
     * Check if torch is supported for given camera info
     * @param cameraInfo The camera info item to check
     * @return true if torch is supported, false otherwise
     */
    fun isTorchSupported(cameraInfo: CameraInfoItem): Boolean {
        return cameraInfo.torchConfig.supportsTorch
    }

    /**
     * Check if flash strength control is available
     * @param cameraInfo The camera info item to check
     * @return true if flash strength control is available, false otherwise
     */
    fun hasFlashStrengthControl(cameraInfo: CameraInfoItem): Boolean {
        return cameraInfo.torchConfig.supportsFlashStrengthControl
    }

    /**
     * Get max flash strength for camera
     * @param cameraInfo The camera info item
     * @return max flash strength or 0 if not supported
     */
    fun getMaxFlashStrength(cameraInfo: CameraInfoItem): Int {
        return cameraInfo.torchConfig.maxFlashStrength
    }

    /**
     * Release resources
     */
    fun release() {
        try {
            disableTorch()
            previewCamera = null
            _torchState.value = TorchState.UNKNOWN
        } catch (e: Exception) {
            _torchState.value = TorchState.ERROR("Failed to release resources: ${e.message}")
        }
    }
}

