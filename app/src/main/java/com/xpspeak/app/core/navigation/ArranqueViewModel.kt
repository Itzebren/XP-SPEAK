package com.xpspeak.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.xpspeak.app.feature.auth.data.PerfilRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decide la primera pantalla al abrir la app (RF-02, CU-02). Firebase guarda la sesión
 * en el teléfono, así que con una sesión vigente se entra directo, también sin conexión:
 * no se vuelven a pedir las credenciales cada vez.
 */
@HiltViewModel
class ArranqueViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val perfilRepository: PerfilRepository
) : ViewModel() {

    /** null mientras se consulta el perfil local (unos milisegundos). */
    private val _destino = MutableStateFlow<String?>(null)
    val destino: StateFlow<String?> = _destino.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = firebaseAuth.currentUser
            _destino.value = destinoInicial(
                haySesion = usuario != null,
                tienePerfil = usuario != null && perfilRepository.existePerfil(usuario.uid)
            )
        }
    }

    companion object {
        /** Sin sesión, al acceso; con sesión pero sin nivel elegido, a la nivelación (RN-02). */
        fun destinoInicial(haySesion: Boolean, tienePerfil: Boolean): String = when {
            !haySesion -> Routes.AUTH
            tienePerfil -> Routes.INICIO
            else -> Routes.SELECCIONAR_NIVEL
        }
    }
}
