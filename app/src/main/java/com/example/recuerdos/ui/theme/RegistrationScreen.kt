package com.example.recuerdos.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegistrationScreen(
    onRegisterComplete: (String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = "¡Bienvenido!",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Por favor, ingresa tu nombre para continuar",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de texto para el nombre
        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                isError = false
            },
            label = { Text("Tu nombre") },
            placeholder = { Text("Ej: Juan Pérez") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Por favor ingresa tu nombre")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de registrar
        Button(
            onClick = {
                if (userName.isNotBlank()) {
                    onRegisterComplete(userName)
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = userName.isNotBlank()
        ) {
            Text("Comenzar")
        }
    }
}