package com.xpspeak.app.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.core.ui.componentes.Avatar
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.notifications.data.PreferenciasNotificaciones
import com.xpspeak.app.feature.notifications.data.ProgramadorRecordatorios
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class InicioUiState(
    val racha: Int = 0,
    /** Lunes a domingo de esta semana: true si el día cuenta para la racha. */
    val semana: List<Boolean> = List(7) { false },
    val avatar: Avatar = Avatar.COHETE
)

/** Pantalla de Inicio (Ilustración 32): la racha sale del perfil local, así funciona sin red. */
@HiltViewModel
class InicioViewModel @Inject constructor(
    sesion: SesionActual,
    usuarioDao: UsuarioDao,
    preferencias: PreferenciasNotificaciones,
    programador: ProgramadorRecordatorios
) : ViewModel() {

    init {
        // Primer paso después de entrar: deja programado el recordatorio con los ajustes guardados.
        programador.programar(preferencias.ajustes.value)
    }

    val uiState: StateFlow<InicioUiState> =
        (sesion.uid?.let(usuarioDao::observarUsuario) ?: emptyFlow())
            .map { usuario ->
                val hoy = LocalDate.now()
                InicioUiState(
                    racha = Racha.vigente(usuario?.racha ?: 0, usuario?.ultimoDiaActivo, hoy),
                    semana = Racha.semana(usuario?.racha ?: 0, usuario?.ultimoDiaActivo, hoy),
                    avatar = Avatar.porId(usuario?.avatar)
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())
}
