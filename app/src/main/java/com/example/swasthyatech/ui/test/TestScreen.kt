package com.example.swasthyatech.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swasthyatech.data.Direction
import com.example.swasthyatech.data.Eye
import com.example.swasthyatech.data.EyeTestResult
import com.example.swasthyatech.data.ScreeningSession
import com.example.swasthyatech.engine.EngineState
import com.example.swasthyatech.ui.optotype.TumblingERenderer

@Composable
fun TestScreen(
    session: ScreeningSession,
    eye: Eye,
    onTestComplete: (EyeTestResult) -> Unit,
    onAbortRequest: () -> Unit,
    viewModel: TestViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(eye) {
        viewModel.startTest(session, eye)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.eye.name} EYE TEST",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.eye == Eye.RIGHT) Color(0xFF0055AA) else Color(0xFFAA0055)
                )
                Button(onClick = onAbortRequest, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
                    Text("Cancel")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (state.engineState) {
                EngineState.NOT_STARTED -> {
                    Text("Preparing test...")
                }
                EngineState.PRESENTING, EngineState.WAITING_FOR_RESPONSE -> {
                    Text(
                        text = "Please cover your ${if (state.eye == Eye.RIGHT) "LEFT" else "RIGHT"} eye.\nKeep the phone approx 40 cm away.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // OPTOTYPE DISPLAY AREA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 250.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.dimensions != null && state.currentOrientation != null) {
                            TumblingERenderer(
                                direction = state.currentOrientation!!,
                                totalPixelSize = state.dimensions!!.rendered.totalPixelSize,
                                strokePixelSize = state.dimensions!!.rendered.strokePixelSize
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // TOUCH INPUT CONTROLS
                    TouchControls(
                        inputEnabled = state.inputEnabled,
                        onDirectionSelected = { dir -> 
                            viewModel.submitTouchResponse(dir, onTestComplete)
                        },
                        onNotVisibleSelected = {
                            viewModel.submitTouchResponse(Direction.NOT_VISIBLE, onTestComplete)
                        }
                    )
                }
                EngineState.COMPLETED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Saving results...", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                EngineState.ABORTED -> {
                    Text("Test Aborted")
                }
            }
        }
    }
}

@Composable
fun TouchControls(
    inputEnabled: Boolean,
    onDirectionSelected: (Direction) -> Unit,
    onNotVisibleSelected: () -> Unit
) {
    var tappedDirection by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Direction?>(null) }
    
    // Reset tapped state when input is re-enabled
    LaunchedEffect(inputEnabled) {
        if (inputEnabled) {
            tappedDirection = null
        }
    }

    val getButtonColors = @Composable { dir: Direction ->
        if (!inputEnabled && tappedDirection == dir) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            ButtonDefaults.buttonColors()
        }
    }

    // Cross layout for directional buttons
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Button(
            onClick = { 
                tappedDirection = Direction.UP
                onDirectionSelected(Direction.UP) 
            },
            enabled = inputEnabled || tappedDirection == Direction.UP,
            colors = getButtonColors(Direction.UP),
            modifier = Modifier.size(80.dp)
        ) {
            Text("⬆️")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Button(
                onClick = { 
                    tappedDirection = Direction.LEFT
                    onDirectionSelected(Direction.LEFT) 
                },
                enabled = inputEnabled || tappedDirection == Direction.LEFT,
                colors = getButtonColors(Direction.LEFT),
                modifier = Modifier.size(80.dp)
            ) {
                Text("⬅️")
            }
            // E spacer in the middle visually mapping the E center
            Text(" E ", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.align(Alignment.CenterVertically))
            
            Button(
                onClick = { 
                    tappedDirection = Direction.RIGHT
                    onDirectionSelected(Direction.RIGHT) 
                },
                enabled = inputEnabled || tappedDirection == Direction.RIGHT,
                colors = getButtonColors(Direction.RIGHT),
                modifier = Modifier.size(80.dp)
            ) {
                Text("➡️")
            }
        }
        Button(
            onClick = { 
                tappedDirection = Direction.DOWN
                onDirectionSelected(Direction.DOWN) 
            },
            enabled = inputEnabled || tappedDirection == Direction.DOWN,
            colors = getButtonColors(Direction.DOWN),
            modifier = Modifier.size(80.dp)
        ) {
            Text("⬇️")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // NOT_VISIBLE separate semantic control
        Button(
            onClick = {
                tappedDirection = Direction.NOT_VISIBLE
                onNotVisibleSelected()
            },
            enabled = inputEnabled || tappedDirection == Direction.NOT_VISIBLE,
            colors = if (!inputEnabled && tappedDirection == Direction.NOT_VISIBLE) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Text("Can't see / Not visible", style = MaterialTheme.typography.titleMedium)
        }
    }
}
