package com.example.todoapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay // Import delay from kotlinx.coroutines

import com.example.todoapp.ui.theme.ToDoAppTheme

@SuppressLint("SplashScreen")
class SplashScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoAppTheme {
                SplashScreenContent()
            }
        }
    }

    @Preview
    @Composable
    private fun SplashScreenContent() {
        LaunchedEffect(key1 = true) {
            delay(1000)
            startActivity(Intent(this@SplashScreen, MainActivity::class.java))
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = R.drawable.splash), contentDescription = null)
        }
    }
}