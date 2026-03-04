package com.example.recuerdos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recuerdos.ui.theme.RecuerdosTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RecuerdosTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(
    onFinished: () -> Unit
) {
    var mostrarNombre by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(450)
        mostrarNombre = true
        delay(900)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            // `ic_launcher` suele ser un adaptive icon (mipmap XML) y puede fallar con painterResource().
            // Usamos el foreground (drawable) que sí es compatible con Compose.
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = mostrarNombre,
            enter = fadeIn()
        ) {
            Text(
                text = "Recuerdos",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

