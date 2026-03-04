package com.example.recuerdos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.recuerdos.ui.theme.RecuerdosTheme

data class Paciente(
    val nombre: String,
    val edad: String,
    val tipoDemencia: String,
    val institucion: String,
    val contactoEmergencia: String,
    val alergias: String,
    val observaciones: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecuerdosTheme {
                RecuerdosApp()
            }
        }
    }
}

sealed class DestinoPrincipal(
    val titulo: String,
    val icono: ImageVector
) {
    data object RegistroPacientes : DestinoPrincipal("Registro de pacientes", Icons.Default.Assignment)
    data object ActividadPorHacer : DestinoPrincipal("Actividad por hacer", Icons.Default.TaskAlt)
    data object PerfilCuidador : DestinoPrincipal("Perfil del cuidador", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuerdosApp() {
    val pacientes = remember { mutableStateListOf<Paciente>() }
    var tabSeleccionada by rememberSaveable { mutableStateOf(0) }
    val destinos = listOf(
        DestinoPrincipal.RegistroPacientes,
        DestinoPrincipal.ActividadPorHacer,
        DestinoPrincipal.PerfilCuidador
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Recuerdos") }
            )
        },
        bottomBar = {
            NavigationBar {
                destinos.forEachIndexed { index, destino ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = destino.icono,
                                contentDescription = destino.titulo
                            )
                        },
                        label = { Text(destino.titulo) },
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (tabSeleccionada) {
            0 -> RegistroDePacientesScreen(
                modifier = Modifier.padding(innerPadding),
                pacientes = pacientes
            )
            1 -> ActividadPorHacerScreen(modifier = Modifier.padding(innerPadding))
            2 -> PerfilCuidadorScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun RegistroDePacientesScreen(
    modifier: Modifier = Modifier,
    pacientes: MutableList<Paciente>
) {
    var seccionSeleccionada by rememberSaveable { mutableStateOf(0) }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var tipoDemencia by remember { mutableStateOf("") }
    var institucion by remember { mutableStateOf("") }
    var contactoEmergencia by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val peso = Modifier.weight(1f)
            if (seccionSeleccionada == 0) {
                Button(
                    onClick = { seccionSeleccionada = 0 },
                    modifier = peso
                ) {
                    Text("Registro de pacientes")
                }
                OutlinedButton(
                    onClick = { seccionSeleccionada = 1 },
                    modifier = peso
                ) {
                    Text("Pacientes registrados")
                }
            } else {
                OutlinedButton(
                    onClick = { seccionSeleccionada = 0 },
                    modifier = peso
                ) {
                    Text("Registro de pacientes")
                }
                Button(
                    onClick = { seccionSeleccionada = 1 },
                    modifier = peso
                ) {
                    Text("Pacientes registrados")
                }
            }
        }

        when (seccionSeleccionada) {
            0 -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Registro de pacientes con demencia",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { nombre = it },
                                label = { Text("Nombre del paciente") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = edad,
                                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) edad = it },
                                    label = { Text("Edad") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = tipoDemencia,
                                    onValueChange = { tipoDemencia = it },
                                    label = { Text("Tipo de demencia") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = institucion,
                                onValueChange = { institucion = it },
                                label = { Text("Institución donde se encuentra") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = contactoEmergencia,
                                onValueChange = { contactoEmergencia = it },
                                label = { Text("Contacto de emergencia") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = alergias,
                                onValueChange = { alergias = it },
                                label = { Text("Alergias") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = observaciones,
                                onValueChange = { observaciones = it },
                                label = { Text("Observaciones") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (nombre.isNotBlank()) {
                                        pacientes.add(
                                            Paciente(
                                                nombre = nombre,
                                                edad = edad,
                                                tipoDemencia = tipoDemencia,
                                                institucion = institucion,
                                                contactoEmergencia = contactoEmergencia,
                                                alergias = alergias,
                                                observaciones = observaciones
                                            )
                                        )
                                        nombre = ""
                                        edad = ""
                                        tipoDemencia = ""
                                        institucion = ""
                                        contactoEmergencia = ""
                                        alergias = ""
                                        observaciones = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Guardar paciente")
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pacientes registrados",
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pacientes) { paciente ->
                            PacienteCard(
                                paciente = paciente,
                                onEliminar = { pacientes.remove(paciente) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActividadPorHacerScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TaskAlt,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Actividad por hacer",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Aquí podrás gestionar las actividades pendientes.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PerfilCuidadorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Perfil del cuidador",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Aquí podrás ver y editar tu perfil de cuidador.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PacienteCard(
    paciente: Paciente,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = paciente.nombre,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (paciente.edad.isNotBlank()) {
                        Text(
                            text = "Edad: ${paciente.edad}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar paciente"
                    )
                }
            }

            if (paciente.tipoDemencia.isNotBlank()) {
                Text("Tipo de demencia: ${paciente.tipoDemencia}", style = MaterialTheme.typography.bodySmall)
            }
            if (paciente.institucion.isNotBlank()) {
                Text("Institución: ${paciente.institucion}", style = MaterialTheme.typography.bodySmall)
            }
            if (paciente.contactoEmergencia.isNotBlank()) {
                Text("Contacto de emergencia: ${paciente.contactoEmergencia}", style = MaterialTheme.typography.bodySmall)
            }
            if (paciente.alergias.isNotBlank()) {
                Text("Alergias: ${paciente.alergias}", style = MaterialTheme.typography.bodySmall)
            }
            if (paciente.observaciones.isNotBlank()) {
                Text("Observaciones: ${paciente.observaciones}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecuerdosAppPreview() {
    RecuerdosTheme {
        RecuerdosApp()
    }
}