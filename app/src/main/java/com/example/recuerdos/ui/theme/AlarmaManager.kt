package com.example.recuerdos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmaManager {

    fun programarAlarma(
        context: Context,
        alarma: Alarma,
        paciente: Paciente
    ) {
        println("=".repeat(60))
        println("⏰ [AlarmaManager] PROGRAMANDO ALARMA:")
        println("   Título: ${alarma.titulo}")
        println("   Hora configurada: ${alarma.hora}:${alarma.minuto}")
        println("   Activa: ${alarma.activa}")
        println("   Solo notificación: ${alarma.soloNotificacion}")
        println("   Paciente: ${paciente.nombre} (ID: ${paciente.id})")
        println("   Alarma ID: ${alarma.id}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Crear intent para el receiver
        val intent = Intent(context, AlarmaReceiver::class.java).apply {
            putExtra("titulo", alarma.titulo)
            putExtra("mensaje", if (alarma.soloNotificacion) {
                "🔔 Recordatorio para ${paciente.nombre}"
            } else {
                "⏰ Alarma para ${paciente.nombre}"
            })
            putExtra("alarmaId", alarma.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarma.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancelar alarma anterior
        alarmManager.cancel(pendingIntent)
        println("   🔴 Alarma anterior cancelada")

        if (!alarma.activa) {
            println("   ⚠️ Alarma inactiva, no se programa")
            println("=".repeat(60))
            return
        }

        // Calcular tiempo usando la hora configurada
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarma.hora)
            set(Calendar.MINUTE, alarma.minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var triggerTime = calendar.timeInMillis
        val ahora = System.currentTimeMillis()

        println("   🕐 Hora actual: ${String.format("%02d:%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE),
            Calendar.getInstance().get(Calendar.SECOND))}")
        println("   🎯 Hora objetivo: ${String.format("%02d:%02d", alarma.hora, alarma.minuto)}")

        // Si la hora ya pasó hoy, programar para mañana
        if (triggerTime <= ahora) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            triggerTime = calendar.timeInMillis
            println("   📅 Hora ya pasó hoy, programando para MAÑANA a las ${String.format("%02d:%02d", alarma.hora, alarma.minuto)}")
        } else {
            println("   ✅ Hora es FUTURA, programando para HOY")
        }

        val delay = triggerTime - ahora
        println("   ⏱️ Delay: ${delay}ms (${delay/60000} minutos, ${delay/3600000} horas)")

        // Programar alarma
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                println("   📱 Usando setExactAndAllowWhileIdle (API 23+)")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                println("   📱 Usando setExact (API < 23)")
            }
            println("✅ [AlarmaManager] Alarma programada EXITOSAMENTE para ${String.format("%02d:%02d", alarma.hora, alarma.minuto)}")
        } catch (e: Exception) {
            println("❌ [AlarmaManager] Error al programar alarma: ${e.message}")
            e.printStackTrace()
        }
        println("=".repeat(60))
    }

    fun cancelarAlarma(context: Context, alarma: Alarma) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarma.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        println("❌ [AlarmaManager] Alarma cancelada: ${alarma.titulo}")
    }

    fun reprogramarTodasLasAlarmas(
        context: Context,
        alarmas: List<Alarma>,
        pacientes: List<Paciente>
    ) {
        println("=".repeat(60))
        println("🔄 [AlarmaManager] REPROGRAMANDO TODAS LAS ALARMAS")
        println("   Total alarmas: ${alarmas.size}")
        println("   Total pacientes: ${pacientes.size}")

        // Cancelar todas las alarmas
        alarmas.forEach { alarma ->
            cancelarAlarma(context, alarma)
        }

        // Programar solo las activas
        val activas = alarmas.filter { it.activa }
        println("   Alarmas activas: ${activas.size}")

        activas.forEach { alarma ->
            val paciente = pacientes.find { it.id == alarma.pacienteId }
            if (paciente != null) {
                programarAlarma(context, alarma, paciente)
            } else {
                println("⚠️ [AlarmaManager] Paciente NO ENCONTRADO para alarma: ${alarma.titulo} (PacienteID: ${alarma.pacienteId})")
            }
        }
        println("=".repeat(60))
    }
}