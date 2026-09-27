package com.example.swasthyatech.ui.calibration

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SizeF
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.swasthyatech.data.CameraIntrinsics
import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceStatus
import com.example.swasthyatech.engine.DistanceEstimationEngine
import com.example.swasthyatech.engine.FaceDistanceAnalyzer
import com.example.swasthyatech.ui.calibration.experiment.ExperimentMode
import com.example.swasthyatech.ui.calibration.experiment.GroundTruthExperimentManager
import com.example.swasthyatech.ui.calibration.experiment.TrialState
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun GroundTruthCollectionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Manager defined later
    var currentEstimate by remember { mutableStateOf<DistanceEstimate?>(null) }
    var intrinsics by remember { mutableStateOf<CameraIntrinsics?>(null) }
    var selectedDistance by remember { mutableStateOf<Int?>(null) }

    val distances = listOf(300, 350, 400, 450, 500)
    val estimationEngine = remember { DistanceEstimationEngine(400f) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    // Re-render triggers
    var forceRecompose by remember { mutableStateOf(0) }

    val logFile = remember {
        File(context.getExternalFilesDir(null), "ground_truth_protocol_log.csv").apply {
            if (!exists()) {
                writeText("timestamp,device_model,camera_id,mode,ground_truth_mm,trial_num,mean_dist_mm,median_dist_mm,within_trial_stddev,abs_error,pct_error,bias,yaw,pitch,observed_ipd_px,focal_length_px,ipd_mm_scale,ipd_mode,status,confidence,uncertainty_mm,number_of_frames,rejected_frames\n")
            }
        }
    }

    val manager = remember { 
        var m: GroundTruthExperimentManager? = null
        m = GroundTruthExperimentManager(
            onTrialCompleted = { trial ->
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(trial.timestamp))
                val trialNum = m?.accuracyTrials?.get(trial.groundTruthMm)?.size ?: 1
                val ipdMode = if (estimationEngine.getExplicitIpdMm() != null) "MEASURED" else "POPULATION"
                val line = "$ts,${android.os.Build.MODEL},${intrinsics?.cameraId},ACCURACY,${trial.groundTruthMm},$trialNum,${trial.mean},${trial.median},${trial.withinTrialStdDev},${trial.absoluteError},${trial.percentageError},${trial.bias},${trial.avgYaw},${trial.avgPitch},${trial.avgIpdPx},${trial.focalLengthPx},${trial.ipdMmScale},$ipdMode,VALID,${trial.avgConfidence},${trial.avgUncertaintyMm},${trial.numberOfFrames},${trial.rejectedFrames}\n"
                FileWriter(logFile, true).use { it.append(line) }
            }
        )
        m
    }

    LaunchedEffect(Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var found: CameraIntrinsics? = null
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val sensorSize: SizeF? = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    if (focalLengths != null && sensorSize != null && activeArray != null) {
                        found = CameraIntrinsics(
                            focalLengthMm = focalLengths[0],
                            sensorWidthMm = sensorSize.width,
                            sensorHeightMm = sensorSize.height,
                            activeArrayWidthPx = activeArray.width(),
                            activeArrayHeightPx = activeArray.height(),
                            cameraId = id
                        )
                    }
                    break
                }
            }
        } catch (e: Exception) { }
        intrinsics = found
    }

    val analyzer = remember(intrinsics) {
        FaceDistanceAnalyzer(
            estimationEngine = estimationEngine,
            cameraIntrinsics = intrinsics,
            onEstimate = { est ->
                currentEstimate = est
                manager.processFrame(est)
                forceRecompose++ 
            }
        )
    }

    val imageAnalysis = remember { ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build() }
    LaunchedEffect(analyzer) { imageAnalysis.setAnalyzer(analyzerExecutor, analyzer) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // Mode Tabs
        Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray)) {
            Button(
                onClick = { manager.setMode(ExperimentMode.ACCURACY) },
                colors = ButtonDefaults.buttonColors(containerColor = if (manager.mode == ExperimentMode.ACCURACY) Color.Blue else Color.Gray),
                modifier = Modifier.weight(1f).padding(4.dp)
            ) { Text("ACCURACY") }
            Button(
                onClick = { manager.setMode(ExperimentMode.ROBUSTNESS) },
                colors = ButtonDefaults.buttonColors(containerColor = if (manager.mode == ExperimentMode.ROBUSTNESS) Color.Blue else Color.Gray),
                modifier = Modifier.weight(1f).padding(4.dp)
            ) { Text("ROBUSTNESS") }
        }

        // Camera Preview
        Box(modifier = Modifier.weight(0.3f).fillMaxWidth()) {
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
            
            val stateText = when (manager.currentState) {
                TrialState.IDLE -> "READY"
                TrialState.STABILIZING -> "HOLD STILL (Stabilizing...)"
                TrialState.WAITING_FOR_MOVEMENT -> "MOVE AWAY TO RESET"
                TrialState.FAILED -> "FAILED: ${manager.currentFailureReason}"
                else -> ""
            }
            Text(
                text = stateText,
                color = when(manager.currentState) {
                    TrialState.STABILIZING -> Color.Yellow
                    TrialState.WAITING_FOR_MOVEMENT -> Color.Green
                    TrialState.FAILED -> Color.Red
                    else -> Color.White
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.BottomCenter).background(Color(0x88000000)).padding(8.dp)
            )
        }

        // Live Stats
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(8.dp)) {
            val est = currentEstimate
            if (est != null) {
                Text("DIST: ${est.distanceMm.toInt()}mm | STATUS: ${est.status}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Yaw: ${"%.1f".format(est.headEulerY)}° | Pitch: ${"%.1f".format(est.headEulerX)}°", color = Color.LightGray)
            } else {
                Text("Waiting for frames...", color = Color.White)
            }
        }

        // Control Panel
        var measuredIpdText by remember { mutableStateOf("") }

        Column(modifier = Modifier.weight(0.7f).fillMaxWidth().background(Color.DarkGray).padding(8.dp)) {
            Text("Ground Truth: Camera lens to outer canthus of eye", color = Color.Yellow, fontSize = 12.sp)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Subject IPD (mm): ", color = Color.White)
                TextField(
                    value = measuredIpdText,
                    onValueChange = { 
                        measuredIpdText = it
                        estimationEngine.setExplicitIpdMm(it.toFloatOrNull())
                    },
                    modifier = Modifier.width(100.dp).height(50.dp),
                    placeholder = { Text("63.0") }
                )
                Text(if (estimationEngine.getExplicitIpdMm() != null) " (MEASURED MODE)" else " (POPULATION MODE)", color = Color.Cyan, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
            
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(100.dp).padding(vertical = 8.dp)) {
                items(distances) { dist ->
                    val trialCount = manager.accuracyTrials[dist]?.size ?: 0
                    Button(
                        onClick = { 
                            selectedDistance = dist
                            manager.setTargetDistance(dist)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedDistance == dist) Color(0xFF1976D2) else Color(0xFF424242)),
                        modifier = Modifier.padding(2.dp).height(45.dp)
                    ) {
                        Text("$dist ($trialCount/10)", fontSize = 10.sp)
                    }
                }
            }
            
            if (manager.mode == ExperimentMode.ACCURACY) {
                Button(
                    onClick = { manager.startTrial() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = selectedDistance != null && manager.currentState == TrialState.IDLE,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("START STABILITY WINDOW", fontSize = 16.sp)
                }
                
                // Show latest trial stats
                val target = selectedDistance
                if (target != null && manager.accuracyTrials[target]?.isNotEmpty() == true) {
                    val latest = manager.accuracyTrials[target]!!.last()
                    Spacer(Modifier.height(8.dp))
                    Text("LATEST TRIAL ($target mm):", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Mean: ${"%.1f".format(latest.mean)} mm | Median: ${"%.1f".format(latest.median)} mm", color = Color.LightGray)
                    Text("Within-trial StdDev: ${"%.1f".format(latest.withinTrialStdDev)} mm", color = Color.LightGray)
                    Text("Abs Error: ${"%.1f".format(latest.absoluteError)} mm | Pct: ${"%.1f".format(latest.percentageError)}%", color = Color.LightGray)
                    Text("Data automatically saved to CSV.", color = Color.Green, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

            } else {
                // ROBUSTNESS
                Button(
                    onClick = { 
                        val est = currentEstimate ?: return@Button
                        val target = selectedDistance ?: return@Button
                        
                        manager.recordRobustnessTrial(est)
                        
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        val line = "$ts,${android.os.Build.MODEL},${intrinsics?.cameraId},${manager.mode},$target,0,0,0,0,0,0,0,${est.headEulerY},${est.headEulerX},${est.status}\n"
                        FileWriter(logFile, true).use { it.append(line) }
                        Toast.makeText(context, "Recorded Robustness Frame", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = selectedDistance != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("RECORD ANGLE (ROBUSTNESS)", fontSize = 16.sp)
                }
                
                Text("Robustness frames saved: ${manager.robustnessTrials.size}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

