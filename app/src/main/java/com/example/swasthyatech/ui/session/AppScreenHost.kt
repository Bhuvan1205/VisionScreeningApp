package com.example.swasthyatech.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swasthyatech.ui.calibration.CalibrationScreen
import com.example.swasthyatech.ui.test.PracticeScreen
import com.example.swasthyatech.ui.test.TestScreen

@Composable
fun AppScreenHost(
    sessionViewModel: SessionViewModel = viewModel()
) {
    val state by sessionViewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    
    val isMidSession = state.currentScreen in listOf(
        AppScreen.INTRO,
        AppScreen.PRACTICE,
        AppScreen.TESTING,
        AppScreen.EYE_TRANSITION
    )
    
    if (isMidSession) {
        BackHandler {
            showExitDialog = true
        }
    }
    
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit screening?") },
            text = { Text("Your current screening session will be stopped.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    sessionViewModel.abortSession()
                }) {
                    Text("Exit Screening", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Test")
                }
            }
        )
    }

    when (state.currentScreen) {
        AppScreen.HOME -> HomeScreen(state, sessionViewModel)
        AppScreen.CALIBRATION -> CalibrationScreen(
            onCalibrationComplete = { 
                sessionViewModel.checkCalibration()
                sessionViewModel.navigateTo(AppScreen.HOME) 
            }
        )
        AppScreen.DISTANCE_CHECK -> com.example.swasthyatech.ui.calibration.DistanceValidationScreen(
            viewModel = sessionViewModel,
            onValidationComplete = { sessionViewModel.completeDistanceValidation() }
        )
        AppScreen.INTRO -> IntroScreen(sessionViewModel)
        AppScreen.PRACTICE -> PracticeScreen(
            onPracticeComplete = {
                sessionViewModel.navigateTo(AppScreen.TESTING)
            }
        )
        AppScreen.TESTING -> {
            if (state.activeSession != null && state.currentTestingEye != null) {
                TestScreen(
                    session = state.activeSession!!,
                    eye = state.currentTestingEye!!,

                    onTestComplete = { eyeResult ->
                        sessionViewModel.saveEyeResult(eyeResult)
                    },
                    onAbortRequest = {
                        showExitDialog = true
                    }
                )
            } else {
                Text("Invalid Session State.")
            }
        }
        AppScreen.EYE_TRANSITION -> EyeTransitionScreen(sessionViewModel)
        AppScreen.RESULT -> ResultScreen(state, sessionViewModel)
    }
}

@Composable
fun HomeScreen(state: SessionUiState, viewModel: SessionViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text("SwasthyaTech Vision", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Low-cost visual acuity screening", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(48.dp))

        if (state.isCalibrationValid) {
            Button(
                onClick = { viewModel.startNewSession() },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Start Screening", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.navigateTo(AppScreen.CALIBRATION) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Recalibrate Screen")
            }
        } else {
            Text("Screen size must be set up before testing.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.navigateTo(AppScreen.CALIBRATION) },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Set Up Screen Size", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    }
}

@Composable
fun IntroScreen(viewModel: SessionViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("VISION SCREENING", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("This test checks how clearly you can see symbols at a fixed distance.", textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text("BEFORE YOU START", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("1. Sit comfortably.")
            Text("2. Hold the phone approximately 40 cm from your face. It is important to keep this distance consistent. (As an approximate guide, 40 cm is roughly the length of an adult's forearm from elbow to wrist.)")
            Text("3. Keep both eyes open, but gently cover your left eye with your hand or an occluder.")
            Text("4. Look at the symbol in the centre of the screen.")
            Text("5. Select the direction the opening points, or tap 'Can't see'.")
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.navigateTo(AppScreen.DISTANCE_CHECK) },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Ready? Let's Practice", style = MaterialTheme.typography.titleMedium)
        }
    }
    }
}

@Composable
fun EyeTransitionScreen(viewModel: SessionViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Right eye test complete.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Rest for a moment.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Now cover your right eye and keep your left eye open.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Hold the phone approximately 40 cm away. Keep this distance consistent.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { viewModel.navigateTo(AppScreen.DISTANCE_CHECK) },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Start Left Eye Test", style = MaterialTheme.typography.titleMedium)
        }
    }
    }
}

@Composable
fun ResultScreen(state: SessionUiState, viewModel: SessionViewModel) {
    val session = state.activeSession
    val finalResult = session?.finalResult
    val isInterrupted = finalResult?.overallStatus == com.example.swasthyatech.data.TestStatus.INTERRUPTED
    
    var showDebug by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        if (isInterrupted) {
            Text("Session Interrupted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This session was aborted before completion. Results may be incomplete.", textAlign = TextAlign.Center)
        } else {
            Text("Vision Screening Result", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Screening complete. The data has been saved securely on this device.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "IMPORTANT: This is a prototype screening indication, not a medical device. This result is not a clinical diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RIGHT EYE", fontWeight = FontWeight.Bold, color = Color(0xFF0055AA))
                val rLog = finalResult?.rightEyeAcuityLogMar
                Text("Result: ${if (rLog != null) "LogMAR $rLog" else "Not completed"}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("LEFT EYE", fontWeight = FontWeight.Bold, color = Color(0xFFAA0055))
                val lLog = finalResult?.leftEyeAcuityLogMar
                Text("Result: ${if (lLog != null) "LogMAR $lLog" else "Not completed"}")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        if (showDebug && session != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(8.dp)
            ) {
                Text("--- OPERATOR DEBUG VIEW ---", fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
                Text("Pixels/mm: ${session.testConfiguration.deviceCalibration.pixelsPerMm}", fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
                Text("Target Dist: ${session.testConfiguration.viewingDistance.targetDistanceMm} mm", fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
                Text("R Trials: ${session.rightEyeTest?.trials?.size ?: 0} | L Trials: ${session.leftEyeTest?.trials?.size ?: 0}", fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
                val avgTime = session.rightEyeTest?.trials?.mapNotNull { it.responseTimeMs }?.average()
                Text("Avg R Resp Time: ${if (avgTime != null && avgTime.toInt() > 0) "${avgTime.toInt()}ms" else "N/A"}", fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showDebug = !showDebug }) {
                Text("🛠", fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), color = Color.LightGray)
            }
            Button(
                onClick = { 
                    viewModel.returnHome() 
                },
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text("Return to Home", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    }
}
