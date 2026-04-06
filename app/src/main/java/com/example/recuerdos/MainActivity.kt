package com.example.recuerdos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import androidx.work.*
import com.example.recuerdos.AlarmaManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.app.AlarmManager
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission

// DataStore extension
val Context.dataStore by preferencesDataStore(name = "user_preferences")

// UserPreferences class
class UserPreferences(private val context: Context) {
    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_TYPE_KEY = stringPreferencesKey("user_type")
        val IS_REGISTERED_KEY = booleanPreferencesKey("is_registered")
        val PACIENTES_KEY = stringPreferencesKey("pacientes")
        val ALARMAS_KEY = stringPreferencesKey("alarmas")
    }

    suspend fun saveUserInfo(name: String, userType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[USER_TYPE_KEY] = userType
            preferences[IS_REGISTERED_KEY] = true
        }
    }

    suspend fun savePacientes(pacientes: List<Paciente>) {
        val json = if (pacientes.isEmpty()) {
            ""
        } else {
            pacientes.joinToString("||") { p ->
                "${p.id}|${p.nombre}|${p.edad}|${p.tipoDemencia}|${p.institucion}|${p.contactoEmergencia}|${p.alergias}|${p.observaciones}"
            }
        }
        context.dataStore.edit { preferences ->
            preferences[PACIENTES_KEY] = json
        }
    }

    suspend fun saveAlarmas(alarmas: List<Alarma>) {
        val json = if (alarmas.isEmpty()) {
            ""
        } else {
            alarmas.joinToString("||") { a ->
                "${a.id}|${a.pacienteId}|${a.titulo}|${a.hora}|${a.minuto}|${a.activa}|${a.soloNotificacion}"
            }
        }

        println("💾 Guardando ${alarmas.size} alarmas")
        println("📝 JSON: $json")

        context.dataStore.edit { preferences ->
            preferences[ALARMAS_KEY] = json
        }
    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME_KEY] ?: "" }

    val userType: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_TYPE_KEY] ?: "personal" }

    val isRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_REGISTERED_KEY] ?: false }

    val pacientes: Flow<List<Paciente>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[PACIENTES_KEY] ?: ""
            println("📖 [pacientes] Cargando pacientes - JSON: $json")

            if (json.isBlank()) {
                println("📖 [pacientes] No hay pacientes guardados")
                emptyList()
            } else {
                val lista = json.split("||").mapNotNull { pacienteStr ->
                    if (pacienteStr.isBlank()) return@mapNotNull null

                    val parts = pacienteStr.split("|", limit = 8)
                    val id = parts.getOrElse(0) { UUID.randomUUID().toString() }
                    val nombre = parts.getOrElse(1) { "" }
                    if (nombre.isBlank()) return@mapNotNull null

                    Paciente(
                        id = id,
                        nombre = nombre,
                        edad = parts.getOrElse(2) { "" },
                        tipoDemencia = parts.getOrElse(3) { "" },
                        institucion = parts.getOrElse(4) { "" },
                        contactoEmergencia = parts.getOrElse(5) { "" },
                        alergias = parts.getOrElse(6) { "" },
                        observaciones = parts.getOrElse(7) { "" }
                    )
                }
                println("📖 [pacientes] Cargados ${lista.size} pacientes")
                lista
            }
        }

    val alarmas: Flow<List<Alarma>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[ALARMAS_KEY] ?: ""
            println("📖 [alarmas] LEYENDO ALARMAS - JSON: $json")

            if (json.isBlank()) {
                println("📖 [alarmas] No hay alarmas guardadas")
                emptyList()
            } else {
                val lista = json.split("||").mapNotNull { alarmaStr ->
                    if (alarmaStr.isBlank()) return@mapNotNull null

                    val parts = alarmaStr.split("|", limit = 7)
                    if (parts.size >= 7) {
                        try {
                            val alarma = Alarma(
                                id = parts[0],
                                pacienteId = parts[1],
                                titulo = parts[2],
                                hora = parts[3].toIntOrNull() ?: 0,
                                minuto = parts[4].toIntOrNull() ?: 0,
                                activa = parts[5].toBoolean(),
                                soloNotificacion = parts[6].toBoolean()
                            )
                            println("   ✅ Alarma: ${alarma.titulo} → Paciente ID: '${alarma.pacienteId}'")
                            alarma
                        } catch (e: Exception) {
                            println("   ❌ Error parseando alarma: ${e.message}")
                            null
                        }
                    } else {
                        println("   ❌ Formato incorrecto: $alarmaStr (solo ${parts.size} campos, se esperaban 7)")
                        null
                    }
                }
                println("📖 [alarmas] ALARMAS CARGADAS: ${lista.size} alarmas")
                lista
            }
        }
}

data class Paciente(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val edad: String,
    val tipoDemencia: String,
    val institucion: String,
    val contactoEmergencia: String,
    val alergias: String,
    val observaciones: String
)

data class Alarma(
    val id: String = UUID.randomUUID().toString(),
    val pacienteId: String = "",
    val titulo: String,
    val hora: Int,
    val minuto: Int,
    val activa: Boolean = true,
    val soloNotificacion: Boolean = false
)

enum class UserType(val displayName: String, val value: String) {
    PERSONAL("Uso personal", "personal"),
    CAREGIVER("Como cuidador", "cuidador")
}

sealed class DestinoPrincipal(val titulo: String, val icono: ImageVector) {
    data object Pacientes : DestinoPrincipal("Pacientes", Icons.Default.Person)
    data object Recordatorios : DestinoPrincipal("Recordatorios", Icons.Default.Favorite)
    data object Perfil : DestinoPrincipal("Perfil", Icons.Default.AccountCircle)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Permiso de notificaciones concedido")
        } else {
            println("❌ Permiso de notificaciones denegado")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Pedir permiso para alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        lifecycleScope.launch {
            val userPrefs = UserPreferences(this@MainActivity)
            NotificationUtils.createNotificationChannel(this@MainActivity)

            val pacientes = userPrefs.pacientes.first()
            val alarmas = userPrefs.alarmas.first()

            AlarmaManager.reprogramarTodasLasAlarmas(
                this@MainActivity,
                alarmas,
                pacientes
            )

            println("🔍 [MainActivity] Alarmas reprogramadas: ${alarmas.size}")
        }

        enableEdgeToEdge()
        setContent {
            RecuerdosTheme {
                RecuerdosApp()
            }
        }
    }
}

@Composable
fun RecuerdosApp() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var isRegistered by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isRegistered = userPreferences.isRegistered.first()
        userName = userPreferences.userName.first()
        userType = userPreferences.userType.first()
        isLoading = false

        println("📱 [RecuerdosApp] Usuario: $userName, Tipo: $userType, Registrado: $isRegistered")
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (!isRegistered) {
            RegistrationScreen(
                onRegisterComplete = { name, type ->
                    coroutineScope.launch {
                        userPreferences.saveUserInfo(name, type)
                        isRegistered = true
                        userName = name
                        userType = type
                        println("✅ [RecuerdosApp] Registro completado: $name, $type")
                    }
                }
            )
        } else {
            MainAppScreen(userName = userName, userType = userType)
        }
    }
}

@Composable
fun RegistrationScreen(onRegisterComplete: (String, String) -> Unit) {
    var userName by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf(UserType.PERSONAL) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "¡Bienvenido a Recuerdos!", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Por favor, completa tu información para comenzar", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it; isError = false },
            label = { Text("Tu nombre completo") },
            placeholder = { Text("Ej: María González") },
            isError = isError && userName.isBlank(),
            supportingText = { if (isError && userName.isBlank()) Text("Por favor ingresa tu nombre") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "¿Cómo vas a usar la aplicación?", fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                UserType.entries.forEachIndexed { index, type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = (selectedUserType == type), onClick = { selectedUserType = type })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (selectedUserType == type), onClick = { selectedUserType = type })
                        Text(text = type.displayName, modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 16.sp)
                    }
                    if (index < UserType.entries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { if (userName.isNotBlank()) onRegisterComplete(userName, selectedUserType.value) else isError = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = userName.isNotBlank()
        ) {
            Text(text = "Comenzar", fontSize = 18.sp)
        }
    }
}

@Composable
fun MainAppScreen(userName: String, userType: String) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var pacientes by remember { mutableStateOf<List<Paciente>>(emptyList()) }
    var alarmas by remember { mutableStateOf<List<Alarma>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        println("🔄 [MainAppScreen] Iniciando carga de datos...")

        val pacientesCargados = try {
            userPreferences.pacientes.first()
        } catch (e: Exception) {
            println("❌ [MainAppScreen] Error cargando pacientes: ${e.message}")
            emptyList()
        }

        val alarmasCargadas = try {
            userPreferences.alarmas.first()
        } catch (e: Exception) {
            println("❌ [MainAppScreen] Error cargando alarmas: ${e.message}")
            emptyList()
        }

        pacientes = pacientesCargados
        alarmas = alarmasCargadas
        isLoading = false

        println("✅ [MainAppScreen] Carga inicial - Pacientes: ${pacientesCargados.size}, Alarmas: ${alarmasCargadas.size}")
        println("📋 [MainAppScreen] IDs de pacientes: ${pacientesCargados.map { it.id }}")
        println("📋 [MainAppScreen] IDs de alarmas cargadas: ${alarmasCargadas.map { it.id }}")

        alarmasCargadas.groupBy { it.pacienteId }.forEach { (pacienteId, alarmasDelPaciente) ->
            println("   👤 Paciente ID '$pacienteId' tiene ${alarmasDelPaciente.size} alarmas")
        }
    }

    LaunchedEffect(Unit) {
        userPreferences.alarmas.collect { nuevasAlarmas ->
            if (!isLoading) {
                println("🔄 [MainAppScreen] Detectado cambio en alarmas: ${nuevasAlarmas.size} alarmas")
                println("   IDs: ${nuevasAlarmas.map { it.id }}")
                alarmas = nuevasAlarmas
            }
        }
    }

    var tabSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val destinos = listOf(
        DestinoPrincipal.Pacientes,
        DestinoPrincipal.Recordatorios,
        DestinoPrincipal.Perfil
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                destinos.forEachIndexed { index, destino ->
                    NavigationBarItem(
                        icon = { Icon(destino.icono, contentDescription = destino.titulo) },
                        label = { Text(destino.titulo) },
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (tabSeleccionada) {
                    0 -> PacientesScreen(
                        pacientes = pacientes.toMutableStateList(),
                        alarmas = alarmas,
                        userName = userName,
                        userType = userType,
                        coroutineScope = coroutineScope,
                        context = context,
                        onPacientesChange = { nuevaListaPacientes ->
                            println("📝 [MainAppScreen] Cambiando pacientes: ${pacientes.size} → ${nuevaListaPacientes.size}")
                            pacientes = nuevaListaPacientes
                            coroutineScope.launch {
                                userPreferences.savePacientes(nuevaListaPacientes)
                            }
                        },
                        onAlarmasChange = { nuevasAlarmas ->
                            println("📝 [MainAppScreen] Cambiando alarmas: ${alarmas.size} → ${nuevasAlarmas.size}")
                            println("   Nuevas alarmas IDs: ${nuevasAlarmas.map { it.id }}")

                            alarmas = nuevasAlarmas.toList()

                            coroutineScope.launch {
                                userPreferences.saveAlarmas(nuevasAlarmas)
                                println("💾 [MainAppScreen] Guardadas ${nuevasAlarmas.size} alarmas en DataStore")
                            }
                        }
                    )
                    1 -> RecordatoriosScreen(userName = userName)
                    2 -> PerfilScreen(userName = userName, userType = userType)
                }
            }
        }
    }
}

@Composable
fun PacientesScreen(
    pacientes: MutableList<Paciente>,
    alarmas: List<Alarma>,
    userName: String,
    userType: String,
    coroutineScope: CoroutineScope,
    context: Context,
    onPacientesChange: (List<Paciente>) -> Unit,
    onAlarmasChange: (List<Alarma>) -> Unit
) {
    var mostrarFormulario by remember { mutableStateOf(true) }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var tipoDemencia by remember { mutableStateOf("") }
    var institucion by remember { mutableStateOf("") }
    var contactoEmergencia by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    println("🏥 [PacientesScreen] Renderizando - Pacientes: ${pacientes.size}, Alarmas totales: ${alarmas.size}")
    println("   IDs pacientes: ${pacientes.map { it.id }}")
    println("   IDs pacientes en alarmas: ${alarmas.map { it.pacienteId }.distinct()}")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "¡Hola, $userName!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (userType == "cuidador") "Cuidador • Gestión de pacientes" else "Uso personal • Tus pacientes",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val peso = Modifier.weight(1f)
            Button(
                onClick = { mostrarFormulario = true },
                modifier = peso,
                colors = if (mostrarFormulario) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) { Text("Registrar") }

            Button(
                onClick = { mostrarFormulario = false },
                modifier = peso,
                colors = if (!mostrarFormulario) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) { Text("Lista") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mostrarFormulario) {
            FormularioPacienteScreen(
                nombre = nombre, onNombreChange = { nombre = it },
                edad = edad, onEdadChange = { edad = it },
                tipoDemencia = tipoDemencia, onTipoDemenciaChange = { tipoDemencia = it },
                institucion = institucion, onInstitucionChange = { institucion = it },
                contactoEmergencia = contactoEmergencia, onContactoEmergenciaChange = { contactoEmergencia = it },
                alergias = alergias, onAlergiasChange = { alergias = it },
                observaciones = observaciones, onObservacionesChange = { observaciones = it },
                onGuardar = {
                    if (nombre.isNotBlank()) {
                        val nuevoPaciente = Paciente(
                            id = UUID.randomUUID().toString(),
                            nombre = nombre,
                            edad = edad,
                            tipoDemencia = tipoDemencia,
                            institucion = institucion,
                            contactoEmergencia = contactoEmergencia,
                            alergias = alergias,
                            observaciones = observaciones
                        )
                        println("➕ [PacientesScreen] Nuevo paciente creado: ${nuevoPaciente.nombre} (ID: ${nuevoPaciente.id})")
                        val nuevaLista = pacientes.toList() + nuevoPaciente
                        onPacientesChange(nuevaLista)
                        nombre = ""
                        edad = ""
                        tipoDemencia = ""
                        institucion = ""
                        contactoEmergencia = ""
                        alergias = ""
                        observaciones = ""
                    }
                }
            )
        } else {
            ListaPacientesScreen(
                pacientes = pacientes,
                alarmas = alarmas,
                onEliminar = { pacienteAEliminar ->
                    println("🗑️ [PacientesScreen] Eliminando paciente: ${pacienteAEliminar.nombre} (ID: ${pacienteAEliminar.id})")

                    val nuevaListaPacientes = pacientes.toList().filter { it.id != pacienteAEliminar.id }
                    onPacientesChange(nuevaListaPacientes)

                    val nuevasAlarmas = alarmas.filter { it.pacienteId != pacienteAEliminar.id }
                    println("   Alarmas eliminadas: ${alarmas.size - nuevasAlarmas.size}")
                    onAlarmasChange(nuevasAlarmas)

                    coroutineScope.launch(Dispatchers.IO) {
                        val userPrefs = UserPreferences(context)
                        val pacientesActualizados = userPrefs.pacientes.first()
                        val alarmasActualizadas = userPrefs.alarmas.first()

                        println("🔄 [PacientesScreen] Reprogramando alarmas después de eliminar paciente")
                        println("   Pacientes restantes: ${pacientesActualizados.size}")
                        println("   Alarmas restantes: ${alarmasActualizadas.size}")

                        AlarmaManager.reprogramarTodasLasAlarmas(
                            context,
                            alarmasActualizadas,
                            pacientesActualizados
                        )
                    }
                },
                onGuardarAlarmas = { pacienteId, nuevasAlarmas ->
                    println("🎯 [PacientesScreen] Guardando alarmas para paciente ID: $pacienteId")
                    println("   Nuevas alarmas: ${nuevasAlarmas.size}")

                    val alarmasDeOtrosPacientes = alarmas.filter { it.pacienteId != pacienteId }

                    println("   Alarmas de otros pacientes: ${alarmasDeOtrosPacientes.size}")
                    println("   IDs de otros pacientes: ${alarmasDeOtrosPacientes.map { it.pacienteId }.distinct()}")

                    val todasLasAlarmas = alarmasDeOtrosPacientes + nuevasAlarmas

                    println("🎯 Total alarmas: ${alarmas.size} → ${todasLasAlarmas.size}")
                    println("   - De otros pacientes: ${alarmasDeOtrosPacientes.size}")
                    println("   - Nuevas de este paciente: ${nuevasAlarmas.size}")

                    nuevasAlarmas.forEachIndexed { index, alarma ->
                        println("      Nueva alarma $index: ${alarma.titulo} (ID: ${alarma.id}) → PacienteID: '${alarma.pacienteId}'")
                    }

                    onAlarmasChange(todasLasAlarmas)
                }
            )
        }
    }
}

@Composable
fun FormularioPacienteScreen(
    nombre: String, onNombreChange: (String) -> Unit,
    edad: String, onEdadChange: (String) -> Unit,
    tipoDemencia: String, onTipoDemenciaChange: (String) -> Unit,
    institucion: String, onInstitucionChange: (String) -> Unit,
    contactoEmergencia: String, onContactoEmergenciaChange: (String) -> Unit,
    alergias: String, onAlergiasChange: (String) -> Unit,
    observaciones: String, onObservacionesChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    onValueChange = onNombreChange,
                    label = { Text("Nombre del paciente *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nombre.isBlank(),
                    supportingText = { if (nombre.isBlank()) Text("El nombre es obligatorio") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = edad,
                        onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) onEdadChange(it) },
                        label = { Text("Edad") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = tipoDemencia,
                        onValueChange = onTipoDemenciaChange,
                        label = { Text("Tipo de demencia") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = institucion,
                    onValueChange = onInstitucionChange,
                    label = { Text("Institución donde se encuentra") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactoEmergencia,
                    onValueChange = onContactoEmergenciaChange,
                    label = { Text("Contacto de emergencia") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = alergias,
                    onValueChange = onAlergiasChange,
                    label = { Text("Alergias") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = observaciones,
                    onValueChange = onObservacionesChange,
                    label = { Text("Observaciones") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onGuardar,
                    modifier = Modifier.align(Alignment.End),
                    enabled = nombre.isNotBlank()
                ) {
                    Text("Guardar paciente")
                }
            }
        }
    }
}

@Composable
fun ListaPacientesScreen(
    pacientes: MutableList<Paciente>,
    alarmas: List<Alarma>,
    onEliminar: (Paciente) -> Unit,
    onGuardarAlarmas: (String, List<Alarma>) -> Unit
) {
    // Usamos un Set para mantener múltiples pacientes expandidos
    var pacientesExpandidos by remember { mutableStateOf<Set<String>>(emptySet()) }

    println("📋 [ListaPacientesScreen] Renderizando - Pacientes: ${pacientes.size}, Alarmas totales: ${alarmas.size}")

    if (pacientes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pacientes registrados", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pacientes, key = { it.id }) { paciente ->
                val alarmasDelPaciente = alarmas.filter { it.pacienteId == paciente.id }
                val estaExpandido = pacientesExpandidos.contains(paciente.id)

                PacienteCardConAlarmas(
                    paciente = paciente,
                    alarmas = alarmasDelPaciente,
                    estaExpandido = estaExpandido,
                    onExpandirToggle = {
                        if (estaExpandido) {
                            pacientesExpandidos = pacientesExpandidos.minus(paciente.id)
                        } else {
                            pacientesExpandidos = pacientesExpandidos.plus(paciente.id)
                        }
                    },
                    onEliminar = { onEliminar(paciente) },
                    onGuardarAlarmas = { nuevasAlarmas ->
                        onGuardarAlarmas(paciente.id, nuevasAlarmas)
                    }
                )
            }
        }
    }
}

@Composable
fun PacienteCardConAlarmas(
    paciente: Paciente,
    alarmas: List<Alarma>,
    estaExpandido: Boolean,
    onExpandirToggle: () -> Unit,
    onEliminar: () -> Unit,
    onGuardarAlarmas: (List<Alarma>) -> Unit
) {
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val alarmasActivas = remember(alarmas) {
        alarmas.filter { it.activa }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header del paciente (siempre visible)
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = paciente.nombre, style = MaterialTheme.typography.titleMedium)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (alarmasActivas.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "${alarmasActivas.size} ⏰",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Botón de alarma - ahora expande/contrae
                        IconButton(onClick = onExpandirToggle) {
                            Icon(
                                if (estaExpandido) Icons.Default.Close else Icons.Default.Alarm,
                                contentDescription = if (estaExpandido) "Cerrar alarmas" else "Configurar alarmas",
                                tint = if (alarmasActivas.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar paciente",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (paciente.edad.isNotBlank()) Text("Edad: ${paciente.edad}", style = MaterialTheme.typography.bodyMedium)
                if (paciente.tipoDemencia.isNotBlank()) Text("Demencia: ${paciente.tipoDemencia}", style = MaterialTheme.typography.bodySmall)
                if (paciente.institucion.isNotBlank()) Text("Institución: ${paciente.institucion}", style = MaterialTheme.typography.bodySmall)
                if (paciente.contactoEmergencia.isNotBlank()) Text("Contacto: ${paciente.contactoEmergencia}", style = MaterialTheme.typography.bodySmall)
                if (paciente.alergias.isNotBlank()) Text("Alergias: ${paciente.alergias}", style = MaterialTheme.typography.bodySmall)
                if (paciente.observaciones.isNotBlank()) Text("Obs: ${paciente.observaciones}", style = MaterialTheme.typography.bodySmall)
            }

            // Sección de alarmas (expandible, aparece debajo del paciente)
            if (estaExpandido) {
                HorizontalDivider()
                AlarmasIntegrado(
                    paciente = paciente,
                    alarmasIniciales = alarmas,
                    onGuardarAlarmas = { nuevasAlarmas ->
                        onGuardarAlarmas(nuevasAlarmas)
                    }
                )
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar a ${paciente.nombre}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        println("✅ Eliminando paciente: ${paciente.nombre}")
                        onEliminar()
                        mostrarDialogoEliminar = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AlarmasIntegrado(
    paciente: Paciente,
    alarmasIniciales: List<Alarma>,
    onGuardarAlarmas: (List<Alarma>) -> Unit
) {
    var alarmas by remember { mutableStateOf(alarmasIniciales) }
    var mostrarDialogoAlarma by remember { mutableStateOf(false) }
    var alarmaEditando by remember { mutableStateOf<Alarma?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏰ Alarmas para ${paciente.nombre}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { mostrarDialogoAlarma = true },
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva alarma", fontSize = 12.sp)
            }
        }

        // Lista de alarmas
        if (alarmas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay alarmas configuradas para este paciente",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            alarmas.forEach { alarma ->
                AlarmaItemIntegrado(
                    alarma = alarma,
                    onToggleActivar = {
                        val alarmaActualizada = alarma.copy(activa = !alarma.activa)
                        alarmas = alarmas.map { if (it.id == alarma.id) alarmaActualizada else it }

                        // Reprogramar después del cambio
                        coroutineScope.launch {
                            val userPrefs = UserPreferences(context)
                            val pacientesActuales = userPrefs.pacientes.first()
                            val todasLasAlarmas = alarmas.map {
                                if (it.id == alarma.id) alarmaActualizada else it
                            }
                            AlarmaManager.reprogramarTodasLasAlarmas(context, todasLasAlarmas, pacientesActuales)
                        }
                    },
                    onEditar = { alarmaEditando = alarma },
                    onEliminar = {
                        alarmas = alarmas.filter { it.id != alarma.id }
                    }
                )
            }
        }

        // Botón guardar
        Button(
            onClick = {
                println("💾 Guardando ${alarmas.size} alarmas para paciente ${paciente.nombre}")
                onGuardarAlarmas(alarmas)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Guardar alarmas")
        }
    }

    // Diálogo para crear/editar alarma
    if (mostrarDialogoAlarma || alarmaEditando != null) {
        CrearAlarmaDialogoIntegrado(
            paciente = paciente,
            alarmaExistente = alarmaEditando,
            onDismiss = {
                mostrarDialogoAlarma = false
                alarmaEditando = null
            },
            onConfirmar = { titulo, hora, minuto, soloNotificacion ->
                if (alarmaEditando != null) {
                    // Editar alarma existente
                    val alarmaActualizada = alarmaEditando!!.copy(
                        titulo = titulo,
                        hora = hora,
                        minuto = minuto,
                        soloNotificacion = soloNotificacion,
                        activa = true
                    )
                    alarmas = alarmas.map {
                        if (it.id == alarmaEditando!!.id) alarmaActualizada else it
                    }
                } else {
                    // Crear nueva alarma
                    val nuevaAlarma = Alarma(
                        id = UUID.randomUUID().toString(),
                        pacienteId = paciente.id,
                        titulo = titulo,
                        hora = hora,
                        minuto = minuto,
                        activa = true,
                        soloNotificacion = soloNotificacion
                    )
                    alarmas = alarmas + nuevaAlarma
                }
                mostrarDialogoAlarma = false
                alarmaEditando = null
            }
        )
    }
}

@Composable
fun CrearAlarmaDialogoIntegrado(
    paciente: Paciente,
    alarmaExistente: Alarma? = null,
    onDismiss: () -> Unit,
    onConfirmar: (titulo: String, hora: Int, minuto: Int, soloNotificacion: Boolean) -> Unit
) {
    var titulo by remember { mutableStateOf(alarmaExistente?.titulo ?: "Medicamento") }
    var hora by remember { mutableStateOf(alarmaExistente?.hora ?: 9) }
    var minuto by remember { mutableStateOf(alarmaExistente?.minuto ?: 0) }
    var soloNotificacion by remember { mutableStateOf(alarmaExistente?.soloNotificacion ?: false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarmaExistente != null) "Editar alarma" else "Nueva alarma") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de la alarma") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { mostrarTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${String.format("%02d", hora)}:${String.format("%02d", minuto)}")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = soloNotificacion,
                        onCheckedChange = { soloNotificacion = it }
                    )
                    Text("Alarma silenciosa (solo notificación)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (titulo.isNotBlank()) {
                        onConfirmar(titulo, hora, minuto, soloNotificacion)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // TimePicker
    if (mostrarTimePicker) {
        val timePickerState = remember { android.app.TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            hora = hour
            minuto = minute
            mostrarTimePicker = false
        } }

        DisposableEffect(Unit) {
            val dialog = android.app.TimePickerDialog(
                context,
                timePickerState,
                hora,
                minuto,
                true
            )
            dialog.setOnDismissListener { mostrarTimePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

@Composable
fun AlarmaItemIntegrado(
    alarma: Alarma,
    onToggleActivar: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de alarma
                Icon(
                    if (alarma.soloNotificacion) Icons.Default.Notifications else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (alarma.activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = alarma.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alarma.activa) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%02d", alarma.hora)}:${String.format("%02d", alarma.minuto)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (alarma.activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Switch para activar/desactivar
                Switch(
                    checked = alarma.activa,
                    onCheckedChange = { onToggleActivar() },
                    modifier = Modifier.scale(0.8f)
                )

                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RecordatoriosScreen(userName: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "¡Hola, $userName!", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Tus recordatorios", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recordatorios", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "Tus recordatorios aparecerán aquí", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun PerfilScreen(userName: String, userType: String) {
    var expandedContactos by remember { mutableStateOf(false) }
    var expandedInformacion by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Card de perfil del usuario
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 40.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (userType == "personal") "Usuario personal" else "Cuidador",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Información de la cuenta
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    text = "Información de la cuenta",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider()
                InfoRow("Nombre", userName)
                HorizontalDivider()
                InfoRow(
                    "Tipo de usuario",
                    if (userType == "personal") "Uso personal" else "Cuidador"
                )
                HorizontalDivider()
                InfoRow("Estado", "Registrado", MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menú desplegable: Contactos de apoyo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header clickeable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedContactos = !expandedContactos }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "📞 Contactos de apoyo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        if (expandedContactos) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandedContactos) "Contraer" else "Expandir"
                    )
                }

                // Contenido expandible
                if (expandedContactos) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Contactos de apoyo
                        ContactoItem(
                            nombre = "Emergencias",
                            numero = "911",
                            icono = Icons.Default.Phone
                        )
                        ContactoItem(
                            nombre = "Alerta Amber México",
                            numero = "800 00 854 00",
                            icono = Icons.Default.Warning
                        )
                        ContactoItem(
                            nombre = "Instituto de la Memoria",
                            numero = "477 187 0671",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Fiscalía del Estado de Guanajuato",
                            numero = "473 735 2100",
                            icono = Icons.Default.Balance
                        )
                        ContactoItem(
                            nombre = "Alzheimer México IAP",
                            numero = "55 5280 4202 / 55 2515 2218 / 55 5280 3349",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Federación Mexicana de Alzheimer",
                            numero = "01 800 00 33362",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Clínica de la Memoria, Ciudad de México",
                            numero = "55 3923-2052",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Fundación Alzheimer",
                            numero = "55 5575 83 20",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Centro mexicano Alzheimer",
                            numero = "564 814 31 51",
                            icono = Icons.Default.HealthAndSafety
                        )
                        ContactoItem(
                            nombre = "Alzheimer's Association",
                            numero = "800 272 39 00",
                            icono = Icons.Default.HealthAndSafety
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menú desplegable: Más información (Tipos de demencia)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header clickeable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedInformacion = !expandedInformacion }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ℹ️ Más información",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        if (expandedInformacion) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandedInformacion) "Contraer" else "Expandir"
                    )
                }

                // Contenido expandible
                if (expandedInformacion) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Información de los tipos de demencia
                        InfoDemenciaItem(
                            titulo = "Enfermedad de Creutzfeldt-Jakob",
                            descripcion = "Causa un tipo de demencia que empeora con una rapidez inusual. Las causas más comunes de demencia, como el Alzheimer, la demencia con cuerpos de Lewy y la demencia frontotemporal, suelen progresar con mayor lentitud."
                        )
                        InfoDemenciaItem(
                            titulo = "Demencia por cuerpos de Lewy",
                            descripcion = "Tipo de demencia progresiva que conlleva un deterioro del pensamiento, el razonamiento y la autonomía."
                        )
                        InfoDemenciaItem(
                            titulo = "Enfermedad de Alzheimer",
                            descripcion = "Enfermedad que afecta las células del cerebro (neuronas), provocando que se degeneren y mueran. Quienes la padecen presentan un deterioro progresivo en la capacidad para procesar el pensamiento (Memoria, orientación, lenguaje, aprendizaje, cálculo, etc.)."
                        )
                        InfoDemenciaItem(
                            titulo = "Síndrome de Down",
                            descripcion = "Las personas con este padecimiento tienen un riesgo mucho mayor de desarrollar demencia a medida que envejecen."
                        )
                        InfoDemenciaItem(
                            titulo = "Demencia frontotemporal",
                            descripcion = "Se refiere a un grupo de trastornos causados por la pérdida progresiva de células nerviosas principalmente en los lóbulos frontales del cerebro (las áreas detrás de la frente) y/o sus lóbulos temporales (las regiones detrás de las orejas)."
                        )
                        InfoDemenciaItem(
                            titulo = "Enfermedad de Huntington",
                            descripcion = "Trastorno cerebral progresivo causado por un gen defectuoso. Esta enfermedad provoca cambios en la zona central del cerebro que afectan el movimiento, el estado de ánimo y la capacidad de razonamiento."
                        )
                        InfoDemenciaItem(
                            titulo = "Demencia mixta",
                            descripcion = "Enfermedad en la que se producen simultáneamente cambios cerebrales de más de una causa de demencia."
                        )
                        InfoDemenciaItem(
                            titulo = "Hidrocefalia normotensiva",
                            descripcion = "Trastorno cerebral en el que se acumula un exceso de líquido cefalorraquídeo (LCR) en los ventrículos del cerebro, lo que provoca problemas de pensamiento y razonamiento, dificultad para caminar y pérdida del control de la vejiga."
                        )
                        InfoDemenciaItem(
                            titulo = "Atrofia cortical posterior",
                            descripcion = "Degeneración gradual y progresiva de la capa externa del cerebro (la corteza) en la parte del cerebro ubicada en la parte posterior de la cabeza."
                        )
                        InfoDemenciaItem(
                            titulo = "Demencia por enfermedad de Parkinson",
                            descripcion = "Deterioro en las habilidades de pensamiento y razonamiento que se desarrolla en algunas personas que viven con Parkinson al menos un año después del diagnóstico."
                        )
                        InfoDemenciaItem(
                            titulo = "Demencia vascular",
                            descripcion = "Disminución de la capacidad de pensamiento causada por afecciones que bloquean o reducen el flujo sanguíneo a diversas regiones del cerebro, privándolas de oxígeno y nutrientes."
                        )
                        InfoDemenciaItem(
                            titulo = "Síndrome de Korsakoff",
                            descripcion = "Trastorno crónico de la memoria causado por una deficiencia grave de tiamina (vitamina B-1). Suele estar causado por el abuso de alcohol, pero otras afecciones también pueden causarlo."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactoItem(
    nombre: String,
    numero: String,
    icono: ImageVector
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Al hacer clic, abrir el marcador con el número
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$numero".replace(" ", "").replace("/", ""))
                }
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = numero,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.Call,
                contentDescription = "Llamar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InfoDemenciaItem(
    titulo: String,
    descripcion: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descripcion,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RecuerdosTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        typography = Typography(),
        content = content
    )
}

@Composable
fun lightColorScheme() = lightColorScheme(
    primary = Color(0xFF0066B4),
    primaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFF535F70),
    secondaryContainer = Color(0xFFD7E3F7),
    surface = Color(0xFFF9F9F9),
    surfaceVariant = Color(0xFFE0E2E5),
    background = Color(0xFFF9F9F9),
    error = Color(0xFFBA1A1A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF44474E),
    onBackground = Color(0xFF1A1C1E),
    onError = Color.White
)

@Composable
fun darkColorScheme() = darkColorScheme(
    primary = Color(0xFFA2C8FF),
    primaryContainer = Color(0xFF003256),
    secondary = Color(0xFFBCC7DB),
    secondaryContainer = Color(0xFF3B485A),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF42474E),
    background = Color(0xFF1A1C1E),
    error = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF003256),
    onSecondary = Color(0xFF25323F),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC5C8D0),
    onBackground = Color(0xFFE2E2E6),
    onError = Color(0xFF690005)
)

@Preview(showBackground = true)
@Composable
fun RecuerdosAppPreview() {
    RecuerdosTheme { RecuerdosApp() }
}