// AlarmaReceiver.kt
package com.example.recuerdos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("=".repeat(60))
        println("🔔🔔🔔 [AlarmaReceiver] ¡ALARMA DISPARADA! 🔔🔔🔔")
        println("   Hora actual: ${System.currentTimeMillis()}")

        val titulo = intent.getStringExtra("titulo") ?: "Recordatorio"
        val mensaje = intent.getStringExtra("mensaje") ?: "Alarma"
        val alarmaId = intent.getIntExtra("alarmaId", 0)

        println("   Título: $titulo")
        println("   Mensaje: $mensaje")
        println("   ID: $alarmaId")

        try {
            // Crear canal de notificaciones
            NotificationUtils.createNotificationChannel(context)

            // Construir notificación
            val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            // Mostrar notificación
            NotificationManagerCompat.from(context).notify(alarmaId, notification)

            println("✅ [AlarmaReceiver] Notificación enviada correctamente")
        } catch (e: Exception) {
            println("❌ [AlarmaReceiver] Error: ${e.message}")
            e.printStackTrace()
        }
        println("=".repeat(60))
    }
}