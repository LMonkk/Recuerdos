package com.example.recuerdos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmasScreen(
    paciente: Paciente,
    alarmasIniciales: List<Alarma>,
    onVolver: () -> Unit,
    onGuardarAlarmas: (List<Alarma>) -> Unit
) {
    var alarmas by remember { mutableStateOf(alarmasIniciales) }
    var mostrarDialogoNuevaAlarma by remember { mutableStateOf(false) }

    // Variable para controlar si estamos en proceso de guardado
    var guardando by remember { mutableStateOf(false) }

    // Función para guardar cambios
    fun guardarCambios(nuevasAlarmas: List<Alarma>) {
        if (!guardando) {
            guardando = true
            println("💾 [AlarmasScreen] Guardando ${nuevasAlarmas.size} alarmas")

            // Asegurar que todas las alarmas tengan el pacienteId correcto
            val alarmasConPacienteId = nuevasAlarmas.map { alarma ->
                alarma.copy(pacienteId = paciente.id)
            }

            onGuardarAlarmas(alarmasConPacienteId)
            guardando = false
        }
    }

    // Efecto para guardar cuando cambian las alarmas
    LaunchedEffect(alarmas) {
        if (alarmas != alarmasIniciales) {
            guardarCambios(alarmas)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón volver
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }

            // Título
            Text(
                text = "Alarmas de ${paciente.nombre}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Botón agregar alarma
            FloatingActionButton(
                onClick = { mostrarDialogoNuevaAlarma = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar alarma")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de alarmas
        if (alarmas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay alarmas configuradas",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Toca el botón + para agregar una",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarmas, key = { it.id }) { alarma ->
                    AlarmaItem(
                        alarma = alarma,
                        onToggleActiva = {
                            println("🔄 [AlarmasScreen] Toggle alarma: ${alarma.id}")
                            val nuevasAlarmas = alarmas.map { a ->
                                if (a.id == alarma.id) a.copy(activa = !a.activa) else a
                            }
                            alarmas = nuevasAlarmas
                        },
                        onEliminar = {
                            println("🗑️ [AlarmasScreen] Eliminando alarma: ${alarma.id}")
                            val nuevasAlarmas = alarmas.filter { it.id != alarma.id }
                            alarmas = nuevasAlarmas
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón guardar cambios - AHORA SIEMPRE VISIBLE pero deshabilitado si no hay cambios
        Button(
            onClick = {
                println("💾 [AlarmasScreen] Guardado manual de ${alarmas.size} alarmas")
                guardarCambios(alarmas)
                onVolver()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando  // Deshabilitar mientras se guarda
        ) {
            if (guardando) {
                Row(horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                }
            } else {
                Text("Guardar cambios")
            }
        }
    }

    // Diálogo para nueva alarma
    if (mostrarDialogoNuevaAlarma) {
        NuevaAlarmaDialog(
            onDismiss = { mostrarDialogoNuevaAlarma = false },
            onGuardar = { nuevaAlarma ->
                println("🆕 [AlarmasScreen] Nueva alarma para paciente ${paciente.nombre}")

                // Asegurar que la nueva alarma tenga el pacienteId correcto
                val alarmaCompleta = nuevaAlarma.copy(pacienteId = paciente.id)

                val nuevasAlarmas = alarmas + alarmaCompleta
                alarmas = nuevasAlarmas
                mostrarDialogoNuevaAlarma = false
            }
        )
    }
}

@Composable
fun AlarmaItem(
    alarma: Alarma,
    onToggleActiva: () -> Unit,
    onEliminar: () -> Unit
) {
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarma.activa)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información de la alarma
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icono según tipo
                    Icon(
                        if (alarma.soloNotificacion)
                            Icons.Default.Notifications
                        else
                            Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (alarma.activa)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Título
                    Text(
                        text = alarma.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alarma.activa)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hora y tipo
                Text(
                    text = String.format("%02d:%02d • %s",
                        alarma.hora,
                        alarma.minuto,
                        if (alarma.soloNotificacion) "🔕 Solo notificación" else "🔔 Con sonido"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alarma.activa)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Acciones
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch activar/desactivar
                Switch(
                    checked = alarma.activa,
                    onCheckedChange = {
                        println("🔄 [AlarmaItem] Switch cambiado para: ${alarma.titulo}")
                        onToggleActiva()
                    },
                    modifier = Modifier.scale(0.8f)
                )

                // Botón eliminar
                IconButton(
                    onClick = {
                        println("🗑️ [AlarmaItem] Click en eliminar para: ${alarma.titulo}")
                        mostrarDialogoEliminar = true
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Diálogo confirmar eliminación
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = {
                println("❌ [AlarmaItem] Diálogo cancelado")
                mostrarDialogoEliminar = false
            },
            title = { Text("Eliminar alarma") },
            text = { Text("¿Estás seguro de eliminar esta alarma?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        println("✅ [AlarmaItem] Confirmación de eliminación para: ${alarma.titulo}")
                        onEliminar()
                        mostrarDialogoEliminar = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        println("❌ [AlarmaItem] Eliminación cancelada")
                        mostrarDialogoEliminar = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun NuevaAlarmaDialog(
    onDismiss: () -> Unit,
    onGuardar: (Alarma) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var hora by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var minuto by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    var soloNotificacion by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nueva Alarma",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Campo título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de la alarma") },
                    placeholder = { Text("Ej: Tomar medicamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Selector de hora
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarTimePicker = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hora de la alarma")
                        Text(
                            text = String.format("%02d:%02d", hora, minuto),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Opción solo notificación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Solo notificación (sin sonido)")
                    Switch(
                        checked = soloNotificacion,
                        onCheckedChange = { soloNotificacion = it }
                    )
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (titulo.isNotBlank()) {
                                val nuevaAlarma = Alarma(
                                    id = UUID.randomUUID().toString(),
                                    pacienteId = "",  // Se asignará después
                                    titulo = titulo,
                                    hora = hora,
                                    minuto = minuto,
                                    activa = true,
                                    soloNotificacion = soloNotificacion
                                    // ✅ SIN NOTAS
                                )
                                onGuardar(nuevaAlarma)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = titulo.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    // TimePicker
    if (mostrarTimePicker) {
        TimePickerDialog(
            onTimeSelected = { selectedHour, selectedMinute ->
                hora = selectedHour
                minuto = selectedMinute
                mostrarTimePicker = false
            },
            onDismiss = { mostrarTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar hora",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            onTimeSelected(timePickerState.hour, timePickerState.minute)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}