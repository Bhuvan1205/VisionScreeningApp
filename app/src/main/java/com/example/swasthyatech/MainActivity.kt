package com.example.swasthyatech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.swasthyatech.theme.SwasthyaTechTheme


import androidx.compose.foundation.layout.safeDrawingPadding

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SwasthyaTechTheme { 
        Surface(
          modifier = Modifier.fillMaxSize().safeDrawingPadding(), 
          color = MaterialTheme.colorScheme.background
        ) { 
          com.example.swasthyatech.ui.session.AppScreenHost() 
        } 
      }
    }
  }
}




