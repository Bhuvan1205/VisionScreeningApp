package com.example.swasthyatech.ui.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun CalibrationScreen(
    onCalibrationComplete: () -> Unit,
    viewModel: CalibrationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    var showDiagnostic by remember { mutableStateOf(false) }
    
    // We bypass density for the preview box so it's literal pixels
    val widthDp = (state.currentPixels / displayMetrics.density).dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usableWidthPx = constraints.maxWidth
        val usableHeightPx = constraints.maxHeight

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Screen Size Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showDiagnostic = !showDiagnostic }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The app needs to know the physical size of your phone's screen so that the eye-test symbols can be displayed at the correct physical size.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(Note: This is a prototype screening calibration, not a clinically precise measurement.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text("STEP 1: Take a standard bank/ATM card.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("STEP 2: Place it against the phone near the reference rectangle below.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("STEP 3: Adjust the slider until the rectangle perfectly matches your card.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("STEP 4: Make sure the card is straight and not tilted.", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Calibration Box mimicking a standard ID-1 card outline uniformly
                val targetPhysicalWidthMm = 85.60f
                val targetPhysicalHeightMm = 53.98f
                val renderedWidthPx = state.currentPixels
                val renderedHeightPx = renderedWidthPx * (targetPhysicalHeightMm / targetPhysicalWidthMm)
                
                val widthDp = (renderedWidthPx / displayMetrics.density).dp
                val heightDp = (renderedHeightPx / displayMetrics.density).dp

                Box(
                    modifier = Modifier
                        .width(widthDp)
                        .height(heightDp) 
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .border(2.dp, Color.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Match Card Here", 
                        color = Color.Blue, 
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Slider(
                    value = state.currentPixels,
                    onValueChange = { viewModel.updatePixels(it) },
                    valueRange = 100f..2500f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveCalibration(
                            screenDensity = displayMetrics.density,
                            screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
                        )
                        onCalibrationComplete()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Text("Confirm Calibration", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showDiagnostic) {
            val wm = context.getSystemService(android.view.WindowManager::class.java)
            val windowMetrics = wm.currentWindowMetrics
            val windowBounds = windowMetrics.bounds
            val insets = windowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
            
            val targetPhysicalWidthMm = 85.60f
            val targetPhysicalHeightMm = 53.98f
            val renderedWidthPx = state.currentPixels
            val renderedHeightPx = renderedWidthPx * (targetPhysicalHeightMm / targetPhysicalWidthMm)
            val pixelsPerMm = renderedWidthPx / targetPhysicalWidthMm

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text("--- CALIBRATION DIAGNOSTIC ---", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Screen Resolution: ${windowBounds.width()} x ${windowBounds.height()} px", color = Color.Green)
                        Text("Density: ${displayMetrics.density}", color = Color.Green)
                        Text("Density DPI: ${displayMetrics.densityDpi}", color = Color.Green)
                        Text("Usable Content Bounds: $usableWidthPx x $usableHeightPx px", color = Color.Green)
                        Text("System Bar Insets (L,T,R,B): ${insets.left}, ${insets.top}, ${insets.right}, ${insets.bottom} px", color = Color.Green)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Current pixelsPerMm (Calculated): $pixelsPerMm", color = Color.Yellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("CALIBRATION OBJECT:", color = Color.Cyan)
                        Text("- Reference Physical Width: $targetPhysicalWidthMm mm", color = Color.Cyan)
                        Text("- Reference Physical Height: $targetPhysicalHeightMm mm", color = Color.Cyan)
                        Text("- Rendered Width: $renderedWidthPx px", color = Color.Cyan)
                        Text("- Rendered Height: $renderedHeightPx px", color = Color.Cyan)
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("ACTION REQUIRED: Measure the physical width and height of the blue rectangle with a caliper.", color = Color.Red)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { showDiagnostic = false }) { Text("Close Diagnostic") }
                    }
                }
            }
        }
    }
}
