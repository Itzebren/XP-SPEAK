package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.recursoDrawable
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor
import com.xpspeak.app.feature.lessons.data.TipoSeccion
import com.xpspeak.app.feature.lessons.ui.components.EjercicioEvaluacion
import com.xpspeak.app.feature.lessons.ui.components.ResultadoIntento
import com.xpspeak.app.feature.lessons.ui.components.SeccionTeoria

/**
 * CU-06: teoría de la lección + evaluación corta (Ilustración 39). La calificación
 * la hace el servidor (autoritativa); sin conexión se califica en el teléfono y el
 * intento se envía después (§6.2).
 */
@Composable
fun LeccionScreen(
    onVolver: () -> Unit,
    onAbrirRepaso: () -> Unit,
    viewModel: LeccionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leccion = uiState.leccion
    val resultado = uiState.resultado
    val locutor = rememberLocutor()
    val scroll = rememberScrollState()

    // RN-12: al salir (o si el sistema cierra la app) se guarda dónde iba; al retomar se vuelve ahí.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.guardarPosicion(scroll.value) }
    LaunchedEffect(uiState.posicionPorRestaurar) {
        val posicion = uiState.posicionPorRestaurar ?: return@LaunchedEffect
        // El contenido ya está dibujado; scrollTo se acota solo si la pantalla es más corta.
        scroll.scrollTo(posicion)
        viewModel.posicionRestaurada()
    }

    if (uiState.porRetomar != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¿Retomar la lección?") },
            text = { Text("La dejaste sin terminar. Puedes seguir donde te quedaste, con tus respuestas, o empezar de nuevo.") },
            confirmButton = {
                Button(colors = coloresBotonPrincipal(), shape = FormaBoton, onClick = viewModel::retomar) { Text("Retomar") }
            },
            dismissButton = { TextButton(onClick = viewModel::empezarDeNuevo) { Text("Empezar de nuevo") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        EncabezadoXP(titulo = leccion?.let { "Lección: ${it.moduloTematico}" } ?: "Lección", onVolver = onVolver)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (uiState.cargando) CircularProgressIndicator()
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (uiState.requiereRepaso) {
                AvisoRepaso(onAbrirRepaso = onAbrirRepaso, onVolver = onVolver)
                return@Column
            }
            if (leccion == null) return@Column

            val portada = recursoDrawable("leccion_${leccion.id.replace('-', '_')}")
                .takeIf { it != 0 } ?: R.drawable.inicio_lecciones
            leccion.canDo.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario) }

            leccion.secciones.forEach { seccion ->
                if (seccion.tipo != TipoSeccion.EVALUACION) {
                    SeccionTeoria(seccion, nivel = leccion.nivelMcer, portada = portada, onEscuchar = locutor::decir)
                    return@forEach
                }
                Text("Evaluación", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Necesitas ${((seccion.umbralAprobacion ?: 0.7) * 100).toInt()} % para aprobar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = XPTextoSecundario
                )
                seccion.items.orEmpty().forEach { item ->
                    val itemId = item.id ?: return@forEach
                    EjercicioEvaluacion(
                        item = item,
                        respuesta = uiState.respuestas[itemId],
                        feedback = resultado?.feedback?.find { it.id == itemId },
                        onResponder = { viewModel.responder(itemId, it) }
                    )
                }
            }

            if (resultado != null) {
                ResultadoIntento(
                    resultado,
                    pendiente = uiState.pendiente,
                    onReintentar = viewModel::reintentar,
                    onVolver = onVolver
                )
            } else {
                BotonPrincipal(
                    texto = "Enviar evaluación",
                    onClick = viewModel::enviar,
                    enabled = uiState.respuestas.isNotEmpty(),
                    cargando = uiState.enviando
                )
            }
        }
    }
}

/** RN-07: antes de un tema nuevo se repasan los conceptos con más errores. */
@Composable
private fun AvisoRepaso(onAbrirRepaso: () -> Unit, onVolver: () -> Unit) {
    Text("Primero, tu repaso", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Tienes conceptos que se te han dificultado y ya toca repasarlos. Haz el repaso de hoy " +
            "y después podrás empezar este tema nuevo.",
        style = MaterialTheme.typography.bodyLarge
    )
    BotonPrincipal(texto = "Ir al repaso", onClick = onAbrirRepaso)
    TextButton(onClick = onVolver) { Text("Volver al catálogo") }
}
