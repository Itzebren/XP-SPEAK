package com.xpspeak.app.feature.notifications.domain

import com.xpspeak.app.core.actividad.Racha
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Días en que se manda el recordatorio (Ilustración 41, "Frecuencia"). */
enum class Frecuencia(val etiqueta: String) {
    DIARIO("Diario"),
    ENTRE_SEMANA("Entre semana"),
    FIN_DE_SEMANA("Fines de semana");

    fun incluye(dia: DayOfWeek): Boolean = when (this) {
        DIARIO -> true
        ENTRE_SEMANA -> dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY
        FIN_DE_SEMANA -> dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY
    }
}

data class AjustesNotificacion(
    /** Interruptor general ("Estado"). */
    val activadas: Boolean = true,
    val recordatorioDiario: Boolean = true,
    val alertasRacha: Boolean = true,
    val frecuencia: Frecuencia = Frecuencia.DIARIO,
    val hora: LocalTime = LocalTime.of(18, 0)
) {
    /** Hay algo que programar: el general y al menos uno de los dos avisos. */
    val programadas: Boolean get() = activadas && (recordatorioDiario || alertasRacha)
}

/** Reglas del recordatorio (RF de notificaciones, RN-08 para la racha). */
object Recordatorio {

    /** Horas que ofrece la pantalla. */
    val HORAS: List<LocalTime> = (7..22).map { LocalTime.of(it, 0) }

    /**
     * Texto de la notificación de hoy, o null si no toca mandarla: avisos apagados,
     * día fuera de la frecuencia o el usuario ya practicó hoy.
     */
    fun mensaje(ajustes: AjustesNotificacion, hoy: LocalDate, racha: Int, ultimoDia: String?): String? {
        if (!ajustes.activadas || !ajustes.frecuencia.incluye(hoy.dayOfWeek)) return null
        if (ultimoDia == hoy.toString()) return null
        val vigente = Racha.vigente(racha, ultimoDia, hoy)
        return when {
            ajustes.alertasRacha && vigente > 0 ->
                "¡No pierdas tu racha de ${Racha.texto(vigente)}! Practica unos minutos hoy."
            ajustes.recordatorioDiario -> "Es hora de practicar tu inglés con XP-SPEAK."
            else -> null
        }
    }

    /** Tiempo hasta la próxima vez que sean las [hora] (hoy si aún no pasa, si no mañana). */
    fun demoraHasta(hora: LocalTime, ahora: LocalDateTime): Duration {
        var siguiente = ahora.toLocalDate().atTime(hora)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        return Duration.between(ahora, siguiente)
    }
}
