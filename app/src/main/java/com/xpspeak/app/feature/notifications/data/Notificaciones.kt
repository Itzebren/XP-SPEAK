package com.xpspeak.app.feature.notifications.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.xpspeak.app.MainActivity
import com.xpspeak.app.R
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.notifications.domain.AjustesNotificacion
import com.xpspeak.app.feature.notifications.domain.Frecuencia
import com.xpspeak.app.feature.notifications.domain.Recordatorio
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Ajustes de la Ilustración 41, guardados en el teléfono. */
@Singleton
class PreferenciasNotificaciones @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("notificaciones", Context.MODE_PRIVATE)

    private val _ajustes = MutableStateFlow(leer())
    val ajustes: StateFlow<AjustesNotificacion> = _ajustes.asStateFlow()

    fun guardar(ajustes: AjustesNotificacion) {
        prefs.edit()
            .putBoolean(ACTIVADAS, ajustes.activadas)
            .putBoolean(DIARIO, ajustes.recordatorioDiario)
            .putBoolean(RACHA, ajustes.alertasRacha)
            .putString(FRECUENCIA, ajustes.frecuencia.name)
            .putString(HORA, ajustes.hora.toString())
            .apply()
        _ajustes.value = ajustes
    }

    private fun leer(): AjustesNotificacion {
        val porDefecto = AjustesNotificacion()
        return AjustesNotificacion(
            activadas = prefs.getBoolean(ACTIVADAS, porDefecto.activadas),
            recordatorioDiario = prefs.getBoolean(DIARIO, porDefecto.recordatorioDiario),
            alertasRacha = prefs.getBoolean(RACHA, porDefecto.alertasRacha),
            frecuencia = prefs.getString(FRECUENCIA, null)
                ?.let { runCatching { Frecuencia.valueOf(it) }.getOrNull() } ?: porDefecto.frecuencia,
            hora = prefs.getString(HORA, null)
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: porDefecto.hora
        )
    }

    private companion object {
        const val ACTIVADAS = "activadas"
        const val DIARIO = "recordatorio_diario"
        const val RACHA = "alertas_racha"
        const val FRECUENCIA = "frecuencia"
        const val HORA = "hora"
    }
}

/** Programa (o cancela) el recordatorio diario a la hora elegida. */
class ProgramadorRecordatorios @Inject constructor(@ApplicationContext private val context: Context) {

    fun programar(ajustes: AjustesNotificacion) {
        val workManager = WorkManager.getInstance(context)
        if (!ajustes.programadas) {
            workManager.cancelUniqueWork(TRABAJO)
            return
        }
        val demora = Recordatorio.demoraHasta(ajustes.hora, LocalDateTime.now())
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(demora.toMinutes(), TimeUnit.MINUTES)
            .build()
        // UPDATE no reinicia la demora; al cambiar la hora hay que volver a encolar.
        workManager.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, solicitud)
    }

    /** El usuario puede negar o quitar el permiso desde los ajustes del sistema. */
    fun permisoConcedido(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private companion object {
        const val TRABAJO = "recordatorio_diario"
    }
}

/** Corre una vez al día: decide el mensaje con [Recordatorio.mensaje] y lo muestra. */
class RecordatorioWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun preferencias(): PreferenciasNotificaciones
        fun usuarioDao(): UsuarioDao
        fun firebaseAuth(): FirebaseAuth
    }

    override suspend fun doWork(): Result {
        val dependencias = EntryPointAccessors.fromApplication(applicationContext, Dependencias::class.java)
        val uid = dependencias.firebaseAuth().currentUser?.uid ?: return Result.success()
        val usuario = dependencias.usuarioDao().buscarPorUid(uid)
        val mensaje = Recordatorio.mensaje(
            dependencias.preferencias().ajustes.value,
            LocalDate.now(),
            racha = usuario?.racha ?: 0,
            ultimoDia = usuario?.ultimoDiaActivo
        ) ?: return Result.success()
        mostrar(applicationContext, mensaje)
        return Result.success()
    }

    companion object {
        private const val CANAL = "recordatorios"
        private const val ID = 1001

        fun mostrar(context: Context, mensaje: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CANAL, "Recordatorios de práctica", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val abrir = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notificacion = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("XP-SPEAK")
                .setContentText(mensaje)
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(ID, notificacion)
        }
    }
}
