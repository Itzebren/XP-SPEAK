package com.xpspeak.app.feature.notifications.ui

import androidx.lifecycle.ViewModel
import com.xpspeak.app.feature.notifications.data.PreferenciasNotificaciones
import com.xpspeak.app.feature.notifications.data.ProgramadorRecordatorios
import com.xpspeak.app.feature.notifications.domain.AjustesNotificacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Gestionar notificaciones (Ilustración 41): cada cambio se guarda y reprograma el aviso. */
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val preferencias: PreferenciasNotificaciones,
    private val programador: ProgramadorRecordatorios
) : ViewModel() {

    val ajustes: StateFlow<AjustesNotificacion> = preferencias.ajustes

    private val _permiso = MutableStateFlow(programador.permisoConcedido())
    val permiso: StateFlow<Boolean> = _permiso.asStateFlow()

    fun cambiar(nuevos: AjustesNotificacion) {
        preferencias.guardar(nuevos)
        programador.programar(nuevos)
    }

    /** Al volver de la ventana de permiso (o de los ajustes del sistema). */
    fun revisarPermiso() {
        _permiso.value = programador.permisoConcedido()
        if (_permiso.value) programador.programar(ajustes.value)
    }
}
