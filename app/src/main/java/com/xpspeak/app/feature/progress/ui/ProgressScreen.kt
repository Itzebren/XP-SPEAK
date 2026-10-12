package com.xpspeak.app.feature.progress.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.core.ui.theme.XPBlueDark
import com.xpspeak.app.core.ui.theme.XPBlueLight
import com.xpspeak.app.core.ui.theme.XPBluePrimary
import com.xpspeak.app.core.ui.theme.XPOrangeAccent
import com.xpspeak.app.feature.progress.data.HabilidadDto
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pestaña Progreso y logros (CU-08, RF-05, RF-12, RF-14; Ilustraciones 33 y 34):
 * nivel MCER con su dominio, XP y nivel de experiencia, racha, habilidades e
 * insignias (a color las obtenidas, en gris con candado las pendientes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onIrALecciones: () -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.refrescando,
        onRefresh = viewModel::refrescar,
        modifier = Modifier.fillMaxSize()
    ) {
        when (val estado = uiState.estado) {
            EstadoProgreso.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is EstadoProgreso.Error -> ErrorProgreso(estado.mensaje, onReintentar = viewModel::refrescar)
            is EstadoProgreso.Listo -> ContenidoProgreso(
                datos = estado.datos,
                desactualizado = estado.desactualizado,
                actualizadoEn = estado.actualizadoEn,
                sinActividad = false,
                onIrALecciones = onIrALecciones,
                onInsignia = viewModel::seleccionarInsignia
            )
            is EstadoProgreso.SinActividad -> ContenidoProgreso(
                datos = estado.datos,
                desactualizado = estado.desactualizado,
                actualizadoEn = estado.actualizadoEn,
                sinActividad = true,
                onIrALecciones = onIrALecciones,
                onInsignia = viewModel::seleccionarInsignia
            )
        }
    }

    uiState.insigniaSeleccionada?.let { insignia ->
        DetalleInsignia(insignia, onCerrar = { viewModel.seleccionarInsignia(null) })
    }
}

@Composable
private fun ErrorProgreso(mensaje: String, onReintentar: () -> Unit) {
    // Con scroll para que el pull-to-refresh también funcione aquí.
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(120.dp))
        Text(mensaje, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
private fun ContenidoProgreso(
    datos: ProgresoDto,
    desactualizado: Boolean,
    actualizadoEn: Long?,
    sinActividad: Boolean,
    onIrALecciones: () -> Unit,
    onInsignia: (InsigniaDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Encabezado()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (desactualizado) AvisoDesactualizado(actualizadoEn)
            if (sinActividad) MensajeMotivacional(onIrALecciones)
            TarjetaNivel(datos)
            TarjetaXp(datos)
            TarjetaRacha(datos)
            TarjetaHabilidades(datos.habilidades)
            Insignias(datos.insignias, onInsignia)
        }
    }
}

/** Encabezado azul con la mascota (provisional, Decisión 6). */
@Composable
private fun Encabezado() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(XPBlueLight)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val mascota = recursoDrawable("mascota")
            if (mascota != 0) {
                Image(painterResource(mascota), contentDescription = "Mascota de XP-SPEAK", modifier = Modifier.size(52.dp))
            } else {
                Icon(Icons.Filled.SmartToy, contentDescription = "Mascota de XP-SPEAK", tint = XPBluePrimary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                "Tu perfil: Progreso y logros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = XPBlueDark,
                modifier = Modifier.semantics { heading() }
            )
            Text("Mira cuánto has avanzado", style = MaterialTheme.typography.bodyMedium, color = XPBlueDark)
        }
    }
}

@Composable
private fun AvisoDesactualizado(actualizadoEn: Long?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null)
            Text(
                if (actualizadoEn != null) "Sin conexión · datos de ${haceCuanto(actualizadoEn)}"
                else "Sin conexión · mostrando lo guardado en este teléfono",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** CU-08 flujo alterno A1. */
@Composable
private fun MensajeMotivacional(onIrALecciones: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = XPOrangeAccent.copy(alpha = 0.15f))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("¡Tu aventura empieza hoy! 🚀", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Completa tu primera lección para ganar XP, empezar tu racha y desbloquear tu primera insignia.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onIrALecciones) { Text("Haz tu primera lección") }
        }
    }
}

@Composable
private fun TarjetaNivel(datos: ProgresoDto) {
    val dominio = datos.dominio
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    datos.nivel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(XPBluePrimary)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Text(
                    "Nivel actual: ${datos.nivel} (${nombreNivel(datos.nivel)})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (dominio.total > 0) {
                LinearProgressIndicator(
                    progress = { dominio.porcentaje / 100f },
                    color = XPOrangeAccent,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Text(
                    "Dominio general: ${dominio.porcentaje} % (${dominio.aprobadas} de ${dominio.total} lecciones)",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Dominio general: se calcula al conectarte", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TarjetaXp(datos: ProgresoDto) {
    val xp = datos.xp
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🪙", style = MaterialTheme.typography.displaySmall, modifier = Modifier.clearAndSetSemantics {})
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${xp.total} XP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = XPOrangeAccent)
                Text("Puntos XP totales", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = { if (xp.xpParaSiguiente > 0) xp.xpEnNivel.toFloat() / xp.xpParaSiguiente else 0f },
                    color = XPOrangeAccent,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Text(
                    "Nivel ${xp.nivel} · ${xp.xpEnNivel}/${xp.xpParaSiguiente} para el siguiente",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TarjetaRacha(datos: ProgresoDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", style = MaterialTheme.typography.displaySmall, modifier = Modifier.clearAndSetSemantics {})
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Racha.texto(datos.racha.actual), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Racha de días consecutivos", style = MaterialTheme.typography.bodyMedium)
                Text("Tu mejor racha: ${Racha.texto(datos.racha.maxima)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** RF-12: habilidades en barras (no radar: son 4 ejes y se leen mejor así, Decisión 4). */
@Composable
private fun TarjetaHabilidades(habilidades: List<HabilidadDto>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Titulo("Habilidades")
            habilidades.forEach { habilidad ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(habilidad.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            habilidad.porcentaje?.let { "$it %" } ?: "Sin datos",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (habilidad.porcentaje ?: 0) / 100f },
                        color = XPBluePrimary,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    if (habilidad.porcentaje == null && habilidad.sugerencia != null) {
                        Text("Sin datos — ${habilidad.sugerencia}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Cuadrícula de 3 columnas con filas normales (no un lazy grid dentro del scroll). */
@Composable
private fun Insignias(insignias: List<InsigniaDto>, onInsignia: (InsigniaDto) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Titulo("Insignias obtenidas (${insignias.count { it.obtenida }} de ${insignias.size})")
            insignias.chunked(COLUMNAS_INSIGNIAS).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { insignia ->
                        CeldaInsignia(insignia, onClick = { onInsignia(insignia) }, modifier = Modifier.weight(1f))
                    }
                    repeat(COLUMNAS_INSIGNIAS - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CeldaInsignia(insignia: InsigniaDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progreso = insignia.progreso
    val descripcion = buildString {
        append(insignia.nombre)
        if (insignia.obtenida) {
            append(", obtenida")
        } else {
            append(", bloqueada")
            progreso?.let { append(", ${it.actual} de ${it.meta}") }
        }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Ver detalle", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconoInsignia(insignia, obtenida = insignia.obtenida)
        Text(
            insignia.nombre,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            fontWeight = if (insignia.obtenida) FontWeight.SemiBold else FontWeight.Normal,
            color = if (insignia.obtenida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!insignia.obtenida && progreso != null) {
            Text("${progreso.actual}/${progreso.meta}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetalleInsignia(insignia: InsigniaDto, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        icon = { IconoInsignia(insignia, obtenida = insignia.obtenida, tamano = 72.dp) },
        title = { Text(insignia.nombre, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(insignia.descripcion, style = MaterialTheme.typography.bodyLarge)
                insignia.criterio?.let { Text("Cómo obtenerla: $it", style = MaterialTheme.typography.bodyMedium) }
                val progreso = insignia.progreso
                when {
                    insignia.obtenida -> Text(
                        insignia.fecha?.let { "Obtenida el ${FORMATO_FECHA.format(Date(it))}" } ?: "¡Obtenida!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    progreso != null -> {
                        Text("Progreso: ${progreso.actual} de ${progreso.meta}", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { if (progreso.meta > 0) progreso.actual.toFloat() / progreso.meta else 0f },
                            color = XPOrangeAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Text("Conéctate para ver cuánto te falta.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
}

private const val COLUMNAS_INSIGNIAS = 3
private val FORMATO_FECHA = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"))

/** Nombres del MCER en español. */
private fun nombreNivel(nivel: String) = when (nivel) {
    "A1" -> "Principiante"
    "A2" -> "Elemental"
    else -> nivel
}

/** "hace 5 min", "hace 3 h", "hace 2 días". */
internal fun haceCuanto(desde: Long, ahora: Long = System.currentTimeMillis()): String {
    val minutos = TimeUnit.MILLISECONDS.toMinutes((ahora - desde).coerceAtLeast(0))
    return when {
        minutos < 1 -> "hace un momento"
        minutos < 60 -> "hace $minutos min"
        minutos < 60 * 24 -> "hace ${minutos / 60} h"
        minutos < 60 * 48 -> "ayer"
        else -> "hace ${minutos / (60 * 24)} días"
    }
}
