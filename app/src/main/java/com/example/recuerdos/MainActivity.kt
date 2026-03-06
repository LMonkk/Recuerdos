package com.example.recuerdos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

// DataStore extension
val Context.dataStore by preferencesDataStore(name = "user_preferences")

// UserPreferences class
class UserPreferences(private val context: Context) {
    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_TYPE_KEY = stringPreferencesKey("user_type")
        val IS_REGISTERED_KEY = booleanPreferencesKey("is_registered")
    }

    suspend fun saveUserInfo(name: String, userType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[USER_TYPE_KEY] = userType
            preferences[IS_REGISTERED_KEY] = true
        }
    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME_KEY] ?: "" }

    val userType: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_TYPE_KEY] ?: "personal" }

    val isRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_REGISTERED_KEY] ?: false }
}

data class Paciente(
    val nombre: String,
    val edad: String,
    val tipoDemencia: String,
    val institucion: String,
    val contactoEmergencia: String,
    val alergias: String,
    val observaciones: String
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
    val pacientes = remember { mutableStateListOf<Paciente>() }
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
            when (tabSeleccionada) {
                0 -> PacientesScreen(pacientes = pacientes, userName = userName, userType = userType)
                1 -> RecordatoriosScreen(userName = userName)
                2 -> PerfilScreen(userName = userName, userType = userType)
            }
        }
    }
}

@Composable
fun PacientesScreen(
    pacientes: MutableList<Paciente>,
    userName: String,
    userType: String
) {
    var mostrarFormulario by remember { mutableStateOf(true) }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var tipoDemencia by remember { mutableStateOf("") }
    var institucion by remember { mutableStateOf("") }
    var contactoEmergencia by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

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
                        pacientes.add(Paciente(nombre, edad, tipoDemencia, institucion, contactoEmergencia, alergias, observaciones))
                        nombre = ""; edad = ""; tipoDemencia = ""; institucion = ""; contactoEmergencia = ""; alergias = ""; observaciones = ""
                    }
                }
            )
        } else {
            ListaPacientesScreen(pacientes = pacientes)
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
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = onNombreChange, label = { Text("Nombre del paciente") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = edad, onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) onEdadChange(it) }, label = { Text("Edad") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = tipoDemencia, onValueChange = onTipoDemenciaChange, label = { Text("Tipo de demencia") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = institucion, onValueChange = onInstitucionChange, label = { Text("Institución donde se encuentra") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactoEmergencia, onValueChange = onContactoEmergenciaChange, label = { Text("Contacto de emergencia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = alergias, onValueChange = onAlergiasChange, label = { Text("Alergias") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = observaciones, onValueChange = onObservacionesChange, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onGuardar, modifier = Modifier.align(Alignment.End)) { Text("Guardar paciente") }
            }
        }
    }
}

@Composable
fun ListaPacientesScreen(pacientes: List<Paciente>) {
    if (pacientes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pacientes registrados", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pacientes) { paciente ->
                PacienteCard(paciente = paciente)
            }
        }
    }
}

@Composable
fun PacienteCard(paciente: Paciente) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = paciente.nombre, style = MaterialTheme.typography.titleMedium)
            if (paciente.edad.isNotBlank()) Text("Edad: ${paciente.edad}", style = MaterialTheme.typography.bodyMedium)
            if (paciente.tipoDemencia.isNotBlank()) Text("Demencia: ${paciente.tipoDemencia}", style = MaterialTheme.typography.bodySmall)
            if (paciente.institucion.isNotBlank()) Text("Institución: ${paciente.institucion}", style = MaterialTheme.typography.bodySmall)
            if (paciente.contactoEmergencia.isNotBlank()) Text("Contacto: ${paciente.contactoEmergencia}", style = MaterialTheme.typography.bodySmall)
            if (paciente.alergias.isNotBlank()) Text("Alergias: ${paciente.alergias}", style = MaterialTheme.typography.bodySmall)
            if (paciente.observaciones.isNotBlank()) Text("Obs: ${paciente.observaciones}", style = MaterialTheme.typography.bodySmall)
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = userName.take(1).uppercase(), fontSize = 40.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = userName, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = if (userType == "personal") "Usuario personal" else "Cuidador", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Información de la cuenta", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                HorizontalDivider()
                InfoRow("Nombre", userName)
                HorizontalDivider()
                InfoRow("Tipo de usuario", if (userType == "personal") "Uso personal" else "Cuidador")
                HorizontalDivider()
                InfoRow("Estado", "Registrado", MaterialTheme.colorScheme.primary)
            }
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

// Tema de la aplicación
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
