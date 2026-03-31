package top.kmiit.debughelper.ui.components.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import top.kmiit.debughelper.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.coroutines.resume

const val TAG = "CameraTest"

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraTestComponent(
    scrollBehavior: ScrollBehavior,
    viewModel: CameraTestViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val cameras by viewModel.cameras.collectAsState()
    val selectedCameraId by viewModel.selectedCameraId.collectAsState()
    val torchState by viewModel.torchStates.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadCameras()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.loadCameras()
        }
        // Initialize helper once; real camera instance will be attached after preview binds.
        viewModel.initializeTorchHelper()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(all = 8.dp)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        if (selectedCameraId != null) {
            Text(
                text = stringResource(R.string.preview) + " ("+ stringResource(R.string.camera_id) + ": $selectedCameraId)",
                style = MiuixTheme.textStyles.title2,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                ) {
                    val previewView = remember { PreviewView(context) }

                    AndroidView(
                        factory = {
                            previewView.apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    LaunchedEffect(selectedCameraId) {
                        val cameraProvider = context.getCameraProvider()

                        val preview = Preview.Builder().build()
                        preview.surfaceProvider = previewView.surfaceProvider

                        val cameraSelector = CameraSelector.Builder()
                            .addCameraFilter { cameraInfos ->
                                cameraInfos.filter { cameraInfo ->
                                    try {
                                        val id = Camera2CameraInfo.from(cameraInfo).cameraId
                                        id == selectedCameraId
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                            }
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            viewModel.onPreviewCameraBound(boundCamera)
                        } catch (e: Exception) {
                            Log.e(TAG, "Use case binding failed", e)
                        }
                    }
                }
                 Button(
                    modifier = Modifier.padding(16.dp),
                    onClick = { viewModel.selectCamera(null) }
                ) {
                     Text(stringResource(R.string.close_preview))
                }
            }
             Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = stringResource(R.string.available_cameras),
            style = MiuixTheme.textStyles.title2,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        cameras.forEach { camera ->
            CameraItem(
                camera = camera,
                isSelected = selectedCameraId == camera.id,
                isTorchOn = torchState[camera.id] ?: false,
                onSelect = {
                     viewModel.selectCamera(camera.id)
                },
                onTorchToggle = { enabled ->
                    viewModel.toggleTorch(camera.id, enabled)
                },
                onTorchStrengthChange = { strength ->
                    viewModel.setTorchStrength(camera.id, strength)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CameraItem(
    camera: CameraInfoItem,
    isSelected: Boolean,
    isTorchOn: Boolean,
    onSelect: () -> Unit,
    onTorchToggle: (Boolean) -> Unit,
    onTorchStrengthChange: (Int) -> Unit
) {
    var torchStrength by remember { mutableIntStateOf(1) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.camera_id) + ": ${camera.id} (${camera.facing})",
                style = MiuixTheme.textStyles.subtitle
            )
            Text(
                text = stringResource(R.string.resolution) + ": ${camera.resolution}",
                style = MiuixTheme.textStyles.body2
            )
             Text(
                text = stringResource(R.string.orientation) + ": ${camera.sensorOrientation}°",
                style = MiuixTheme.textStyles.body2
            )

            // Torch configuration
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Torch: " + if (camera.torchConfig.supportsTorch) "✓" else "✗",
                style = MiuixTheme.textStyles.body2
            )
            Text(
                text = "Flash Strength Control: " + if (camera.torchConfig.supportsFlashStrengthControl) "✓" else "✗",
                style = MiuixTheme.textStyles.body2
            )
            if (camera.torchConfig.maxFlashStrength > 0) {
                Text(
                    text = "Max Flash Strength: ${camera.torchConfig.maxFlashStrength}",
                    style = MiuixTheme.textStyles.body2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera preview button and torch test button
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSelect,
                    enabled = !isSelected,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                     Text(if (isSelected)
                            stringResource(R.string.previewing)
                        else stringResource(R.string.preview))
                }

                // Torch test button
                if (camera.torchConfig.supportsTorch) {
                    Button(
                        onClick = { onTorchToggle(!isTorchOn) },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text(if (isTorchOn) "Torch: ON ✓" else "Torch: OFF")
                    }
                }
            }

            // Flash strength control (if supported)
            if (camera.torchConfig.supportsFlashStrengthControl && camera.torchConfig.maxFlashStrength > 0 && isTorchOn) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Flash Strength: $torchStrength / ${camera.torchConfig.maxFlashStrength}",
                    style = MiuixTheme.textStyles.body2
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Slider-like control using buttons
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (torchStrength > 1) {
                                torchStrength--
                                onTorchStrengthChange(torchStrength)
                            }
                        },
                        enabled = torchStrength > 1,
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Text("-")
                    }

                    Spacer(modifier = Modifier.padding(4.dp))

                    // Strength display bar
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(32.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        ) {
                            repeat(camera.torchConfig.maxFlashStrength) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .then(
                                            if (index < torchStrength) {
                                                Modifier.background(Color.Green)
                                            } else {
                                                Modifier.border(1.dp, Color.Gray)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.padding(4.dp))

                    Button(
                        onClick = {
                            if (torchStrength < camera.torchConfig.maxFlashStrength) {
                                torchStrength++
                                onTorchStrengthChange(torchStrength)
                            }
                        },
                        enabled = torchStrength < camera.torchConfig.maxFlashStrength,
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also {
            it.addListener({
                continuation.resume(it.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }
