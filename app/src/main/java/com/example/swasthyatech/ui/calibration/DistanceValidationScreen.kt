package com.example.swasthyatech.ui.calibration

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.swasthyatech.data.CameraIntrinsics
import com.example.swasthyatech.data.DistanceStatus
import com.example.swasthyatech.data.ValidationStatus
import com.example.swasthyatech.engine.FaceDistanceAnalyzer
import com.example.swasthyatech.ui.session.SessionViewModel
import java.util.concurrent.Executors

@Composable
fun DistanceValidationScreen(
    viewModel: SessionViewModel,
    onValidationComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val validationEngine = viewModel.distanceValidationEngine
    val targetDistanceCm = (validationEngine.validationState.targetDistanceMm / 10).toInt()
    
    var intrinsics by remember { mutableStateOf<CameraIntrinsics?>(null) }
    var refreshTicker by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        validationEngine.resetValidation()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var found: CameraIntrinsics? = null
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val physicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    if (focalLengths != null && focalLengths.isNotEmpty() && activeArray != null && physicalSize != null) {
                        found = CameraIntrinsics(
                            focalLengthMm = focalLengths[0],
                            sensorWidthMm = physicalSize.width,
                            sensorHeightMm = physicalSize.height,
                            activeArrayWidthPx = activeArray.width(),
                            activeArrayHeightPx = activeArray.height(),
                            cameraId = id
                        )
                    }
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        intrinsics = found
        validationEngine.startValidation()
        
        while(true) {
            kotlinx.coroutines.delay(100)
            refreshTicker++
        }
    }
    
    // Observer trigger
    val _tick = refreshTicker
    val currentEstimate = validationEngine.currentEstimate
    val isReady = validationEngine.validationState.status == ValidationStatus.VALIDATED

    LaunchedEffect(isReady) {
        if (isReady) {
            kotlinx.coroutines.delay(1000)
            onValidationComplete()
        }
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember(intrinsics) {
        FaceDistanceAnalyzer(validationEngine.getEstimator(), intrinsics) { estimate ->
            validationEngine.updateFromCameraEstimate(estimate)
        }
    }

    val imageAnalysis = remember { ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build() }
    LaunchedEffect(analyzer) { imageAnalysis.setAnalyzer(analyzerExecutor, analyzer) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Position Yourself", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hold the device approximately $targetDistanceCm cm from your face. Ensure your face is clearly visible.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Circular camera preview
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = if (isReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cp = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        cp.unbindAll()
                        cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Subtle alignment overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status Area
        val statusText = when {
            isReady -> "Distance Ready"
            currentEstimate == null -> "Detecting face..."
            currentEstimate.status == DistanceStatus.TOO_CLOSE -> "Too Close — Move back"
            currentEstimate.status == DistanceStatus.TOO_FAR -> "Too Far — Move closer"
            currentEstimate.status == DistanceStatus.INVALID_HEAD_POSE -> "Keep your head straight"
            currentEstimate.status == DistanceStatus.INVALID_FACE_GEOMETRY -> "Face not fully visible"
            currentEstimate.status == DistanceStatus.VALID -> "Hold still..."
            else -> "Adjusting..."
        }
        
        val statusColor = when {
            isReady -> Color(0xFF4CAF50)
            currentEstimate?.status == DistanceStatus.VALID -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isReady) {
                Text("âœ”", color = statusColor, style = MaterialTheme.typography.titleLarge)
            } else if (currentEstimate?.status != DistanceStatus.VALID && currentEstimate != null) {
                Text("âš ", color = statusColor, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onValidationComplete,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = isReady
        ) {
            Text("Proceed", style = MaterialTheme.typography.titleLarge)
        }
    }
    }
}
