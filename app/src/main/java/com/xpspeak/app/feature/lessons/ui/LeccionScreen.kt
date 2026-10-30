package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * CU-06: teoría de la lección + evaluación corta (Ilustración 39). La evaluación
 * aparece al pulsar "Empezar evaluación", después de la teoría. La calificación
 * la hace el servidor (autoritativa); sin conexión se califica en el teléfono y el
 * intento se envía después (§6.2).
 */
@Composable
fun LeccionScreen(
    onVolver: () -> Unit,
    viewModel: LeccionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leccion = uiState.leccion
    val resultado = uiState.resultado
    val locutor = rememberLocutor()
    var evaluando by rememberSaveable { mutableStateOf(false) }
    val irAEvaluacion = remember { BringIntoViewRequester() }

    LaunchedEffect(evaluando) { if (evaluando) irAEvaluacion.bringIntoView() }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        EncabezadoXP(
            titulo = leccion?.let { "Lección: ${it.moduloTematico}" } ?: "Lección",
            onVolver = onVolver,
            mascotaAlInicio = true
        )

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (uiState.cargando) CircularProgressIndicator()
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (leccion == null) return@Column

            val portada = recursoDrawable("leccion_${leccion.id.replace('-', '_')}")
                .takeIf { it != 0 } ?: R.drawable.inicio_lecciones
            val (evaluaciones, teoria) = leccion.secciones.partition { it.tipo == TipoSeccion.EVALUACION }

            teoria.forEach { seccion ->
                SeccionTeoria(
                    seccion,
                    nivel = leccion.nivelMcer,
                    portada = portada,
                    canDo = leccion.canDo,
                    onEscuchar = locutor::decir
                )
            }

            if (!evaluando) {
                BotonPrincipal(texto = "Empezar evaluación", onClick = { evaluando = true })
                return@Column
            }

            evaluaciones.forEach { seccion ->
                Text(
                    "Evaluación",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.bringIntoViewRequester(irAEvaluacion)
                )
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
