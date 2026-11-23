package com.xpspeak.app.feature.games.ui.comun

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.xpspeak.app.R

/**
 * Sonido de acierto y de error de cada acción (CU-07 paso 6: feedback visual y
 * auditivo). Los WAV están en res/raw y se generan con `npm run generar:sonidos`.
 * SoundPool los deja cargados en memoria para que suenen sin retraso.
 */
class Sonidos(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        // Sigue el volumen multimedia, igual que la voz del Locutor.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val acierto = pool.load(context, R.raw.sonido_acierto, 1)
    private val error = pool.load(context, R.raw.sonido_error, 1)

    fun reproducir(veredicto: Veredicto) {
        val id = when (veredicto) {
            Veredicto.ACIERTO -> acierto
            Veredicto.ERROR -> error
        }
        pool.play(id, VOLUMEN, VOLUMEN, 1, 0, 1f)
    }

    fun liberar() = pool.release()

    private companion object {
        /** Por debajo de la voz del Locutor, que suele sonar justo después. */
        const val VOLUMEN = 0.6f
    }
}

/** Los da [PantallaPartida] a todo el juego; fuera de una partida no hay sonidos. */
val LocalSonidos = staticCompositionLocalOf<Sonidos?> { null }

@Composable
fun rememberSonidos(): Sonidos {
    val context = LocalContext.current
    val sonidos = remember(context) { Sonidos(context.applicationContext) }
    DisposableEffect(sonidos) { onDispose { sonidos.liberar() } }
    return sonidos
}
