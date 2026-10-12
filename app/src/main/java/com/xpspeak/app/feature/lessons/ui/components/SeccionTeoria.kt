package com.xpspeak.app.feature.lessons.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.lessons.data.SeccionDto
import com.xpspeak.app.feature.lessons.data.TipoSeccion

/** Una sección de teoría (introducción, vocabulario, gramática o diálogo) — CU-06 paso 3. */
@Composable
fun SeccionTeoria(seccion: SeccionDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(seccion.titulo ?: tituloPorDefecto(seccion.tipo), style = MaterialTheme.typography.titleMedium)
            seccion.cuerpo?.let { Text(it) }
            seccion.explicacion?.let { Text(it) }
            seccion.ejemplos?.forEach { Text("• $it", fontWeight = FontWeight.Medium) }
            seccion.items?.forEach { item ->
                Text("${item.en} — ${item.es}" + (item.nota?.let { "  ($it)" } ?: ""))
            }
            seccion.lineas?.forEach { linea ->
                Text("${linea.hablante}: ${linea.en}", fontWeight = FontWeight.Medium)
                Text(linea.es, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun tituloPorDefecto(tipo: TipoSeccion?) = when (tipo) {
    TipoSeccion.VOCABULARIO -> "Vocabulario"
    TipoSeccion.GRAMATICA -> "Gramática"
    TipoSeccion.DIALOGO -> "Diálogo"
    else -> ""
}
