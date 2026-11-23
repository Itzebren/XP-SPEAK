package com.xpspeak.app.feature.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.Minijuego
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** CU-07 paso 2: el catálogo muestra el nivel del usuario y qué partidas quedaron a medias. */
@HiltViewModel
class GamesViewModel @Inject constructor(
    repository: MinijuegosRepository
) : ViewModel() {

    private val _nivel = MutableStateFlow<String?>(null)
    val nivel: StateFlow<String?> = _nivel.asStateFlow()

    val enCurso: StateFlow<Set<Minijuego>> = repository.juegosEnCurso()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _nivel.value = runCatching { repository.nivelDelUsuario() }.getOrNull()
            // Envía en segundo plano los resultados que hayan quedado pendientes.
            runCatching { repository.sincronizarPendientes() }
        }
    }
}
