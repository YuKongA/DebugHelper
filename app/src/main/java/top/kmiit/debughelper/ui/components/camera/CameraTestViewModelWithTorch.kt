package top.kmiit.debughelper.ui.components.camera

import android.app.Application
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Extended ViewModel that includes torch control capabilities.
 * Use this if you need to control torch in addition to detecting camera capabilities.
 */
class CameraTestViewModelWithTorch(
    application: Application,
    private val lifecycleOwner: LifecycleOwner? = null
) : CameraTestViewModel(application) {

    private var torchHelper: CameraTorchHelper? = null

    private val _torchState = MutableStateFlow<CameraTorchHelper.TorchState>(CameraTorchHelper.TorchState.UNKNOWN)
    val torchState = _torchState.asStateFlow()

    private val _isTorchSupported = MutableStateFlow(false)
    val isTorchSupported = _isTorchSupported.asStateFlow()

    /**
     * Initialize torch helper
     */
    fun initializeTorchHelperWithLifecycle() {
        if (lifecycleOwner == null) {
            Log.e("CameraTestViewModelWithTorch", "LifecycleOwner not provided")
            return
        }

        if (torchHelper != null) return

        torchHelper = CameraTorchHelper(getApplication())
        viewModelScope.launch {
            try {
                torchHelper?.initialize()
                // Observe torch state changes
                torchHelper?.torchState?.collect { state ->
                    _torchState.value = state
                }
            } catch (e: Exception) {
                Log.e("CameraTestViewModelWithTorch", "Failed to initialize torch helper", e)
                _torchState.value = CameraTorchHelper.TorchState.ERROR(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Enable torch for specified camera
     * @param cameraId The camera ID to enable torch for
     */
    fun enableTorchForCamera(cameraId: String) {
        val helper = torchHelper ?: run {
            Log.e("CameraTestViewModelWithTorch", "Torch helper not initialized")
            return
        }

        // Check if camera supports torch
        val camera = cameras.value.find { it.id == cameraId }
        if (camera == null || !helper.isTorchSupported(camera)) {
            _isTorchSupported.value = false
            return
        }

        _isTorchSupported.value = true

        viewModelScope.launch {
            val success = helper.enableTorch(cameraId)
            if (!success) {
                Log.e("CameraTestViewModelWithTorch", "Failed to enable torch for camera $cameraId")
            }
        }
    }

    /**
     * Disable torch
     */
    fun disableTorch() {
        torchHelper?.disableTorch()
    }

    /**
     * Toggle torch
     * @param cameraId The camera ID to toggle torch for
     */
    fun toggleTorch(cameraId: String) {
        val helper = torchHelper ?: return

        viewModelScope.launch {
            helper.toggleTorch(cameraId)
        }
    }

    /**
     * Get torch helper (for advanced use cases)
     */
    fun getTorchHelper(): CameraTorchHelper? = torchHelper

    override fun onCleared() {
        super.onCleared()
        torchHelper?.release()
        torchHelper = null
    }
}

