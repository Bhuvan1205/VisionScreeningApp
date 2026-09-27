package com.example.swasthyatech.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swasthyatech.data.Direction
import com.example.swasthyatech.ui.optotype.TumblingERenderer
import kotlinx.coroutines.delay

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun PracticeScreen(onPracticeComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var userTappedCorrectly by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (step == 0) {
                Text("PRACTICE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "You will see an E-shaped symbol on the screen. The opening of the 'E' can point UP, DOWN, LEFT, or RIGHT.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                // Large example optotype (e.g. 150px)
                Box(modifier = Modifier.size(150.dp).background(Color.White), contentAlignment = Alignment.Center) {
                    TumblingERenderer(direction = Direction.RIGHT, totalPixelSize = 150, strokePixelSize = 30)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("For example, this one points RIGHT.", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Try a Practice Symbol")
                }
            } else if (step == 1) {
                Text("Which way is this symbol pointing?", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(modifier = Modifier.size(150.dp).background(Color.White), contentAlignment = Alignment.Center) {
                    TumblingERenderer(direction = Direction.UP, totalPixelSize = 150, strokePixelSize = 30)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (userTappedCorrectly) {
                    Text("Correct! Good job.", color = Color(0xFF009900), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    LaunchedEffect(Unit) {
                        delay(1500)
                        onPracticeComplete()
                    }
                } else {
                    TouchControls(inputEnabled = true, onDirectionSelected = { dir ->
                        if (dir == Direction.UP) {
                            userTappedCorrectly = true
                        }
                    }, onNotVisibleSelected = {
                        // Ignore during practice
                    })
                }
            }
        }
    }
}
