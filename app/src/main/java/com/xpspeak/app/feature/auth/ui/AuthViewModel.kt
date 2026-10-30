package com.xpspeak.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.xpspeak.app.feature.auth.data.AuthRepository
import com.xpspeak.app.feature.auth.data.PerfilRepository
import com.xpspeak.app.feature.auth.domain.NombreUsuario
import com.xpspeak.app.feature.auth.domain.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTRO }
enum class DestinoPostAuth { NINGUNO, SELECCIONAR_NIVEL, HOME }

data class AuthUiState(
    val modo: AuthMode = AuthMode.LOGIN,
    val correo: String = "",
    val nombre: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
    /** RN-01: la cuenta existe pero su correo aún no se confirma con el enlace. */
    val pendienteVerificacion: Boolean = false,
    /** Al abrir la app con una sesión guardada: se decide a dónde ir sin mostrar el formulario. */
    val retomandoSesion: Boolean = false,
    val destino: DestinoPostAuth = DestinoPostAuth.NINGUNO
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val perfilRepository: PerfilRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        retomarSesion()
    }

    /**
     * Firebase guarda la sesión en el teléfono: si ya hay una, no se vuelve a pedir
     * iniciar sesión. Funciona sin conexión (el perfil está en Room); si el correo
     * aún no se verifica, se va directo al aviso de verificación (RN-01).
     */
    private fun retomarSesion() {
        val usuario = authRepository.usuarioActual ?: return
        _uiState.value = _uiState.value.copy(retomandoSesion = true, correo = usuario.email.orEmpty())
        viewModelScope.launch {
            if (usuario.isEmailVerified) {
                continuar(usuario.uid)
            } else {
                _uiState.value = _uiState.value.copy(retomandoSesion = false, pendienteVerificacion = true)
            }
        }
    }

    fun cambiarModo(modo: AuthMode) {
        _uiState.value = _uiState.value.copy(modo = modo, error = null)
    }

    // Un correo nunca lleva espacios: se quitan también el Tab o salto de línea
    // que mete el teclado físico al pasar de un campo a otro.
    fun onCorreoChange(valor: String) {
        _uiState.value = _uiState.value.copy(correo = valor.filterNot { it.isWhitespace() }, error = null)
    }

    fun onNombreChange(valor: String) {
        _uiState.value = _uiState.value.copy(nombre = valor, error = null)
    }

    fun onPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(password = valor, error = null)
    }

    fun onConfirmarPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(confirmarPassword = valor, error = null)
    }

    fun enviar() {
        val estado = _uiState.value

        if (estado.correo.isBlank() || !estado.correo.contains("@")) {
            _uiState.value = estado.copy(error = "Ingresa un correo válido.")
            return
        }

        if (estado.modo == AuthMode.REGISTRO) {
            NombreUsuario.validar(estado.nombre)?.let { mensaje ->
                _uiState.value = estado.copy(error = mensaje)
                return
            }
            if (!PasswordValidator.esValida(estado.password)) {
                _uiState.value = estado.copy(error = PasswordValidator.mensajeError(estado.password))
                return
            }
            if (estado.password != estado.confirmarPassword) {
                _uiState.value = estado.copy(error = "Las contraseñas no coinciden.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)

            val resultado = if (estado.modo == AuthMode.REGISTRO) {
                authRepository.registrar(estado.correo, estado.password, estado.nombre)
            } else {
                authRepository.iniciarSesion(estado.correo, estado.password)
            }

            resultado
                .onSuccess { usuario ->
                    if (usuario.isEmailVerified) {
                        continuar(usuario.uid)
                    } else {
                        // Al registrarse, si el correo no salió se dice por qué (antes se callaba).
                        val falloEnvio = authRepository.falloEnvioVerificacion.takeIf { estado.modo == AuthMode.REGISTRO }
                        _uiState.value = _uiState.value.copy(
                            cargando = false, pendienteVerificacion = true, password = "", confirmarPassword = "",
                            error = falloEnvio?.let(::mensajeEnvio)
                        )
                    }
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = mapearError(excepcion))
                }
        }
    }

    /** "Ya verifiqué": deja pasar solo si Firebase ya marca el correo como confirmado. */
    fun comprobarVerificacion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null, aviso = null)
            authRepository.correoVerificado()
                .onSuccess { verificado ->
                    val uid = authRepository.usuarioActual?.uid
                    if (verificado && uid != null) {
                        continuar(uid)
                    } else {
                        _uiState.value = _uiState.value.copy(cargando = false, error = "Tu correo aún no está verificado. Abre el enlace que te enviamos.")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No pudimos comprobarlo. Revisa tu conexión.")
                }
        }
    }

    fun reenviarVerificacion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null, aviso = null)
            authRepository.reenviarVerificacion()
                .onSuccess { _uiState.value = _uiState.value.copy(cargando = false, aviso = "Te enviamos un nuevo enlace.") }
                .onFailure { _uiState.value = _uiState.value.copy(cargando = false, error = mensajeEnvio(it)) }
        }
    }

    /** Sale del aviso de verificación sin dejar la sesión abierta. */
    fun usarOtraCuenta() {
        authRepository.cerrarSesion()
        _uiState.value = AuthUiState(correo = _uiState.value.correo)
    }

    private suspend fun continuar(uid: String) {
        val tienePerfil = perfilRepository.existePerfil(uid)
        val destino = if (tienePerfil) DestinoPostAuth.HOME else DestinoPostAuth.SELECCIONAR_NIVEL
        _uiState.value = _uiState.value.copy(cargando = false, retomandoSesion = false, pendienteVerificacion = false, destino = destino)
    }

    private fun mapearError(excepcion: Throwable): String = when (excepcion) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        else -> "Ocurrió un error. Intenta de nuevo."
    }

    /** Por qué no salió el correo de verificación (RN-01). */
    private fun mensajeEnvio(excepcion: Throwable): String = when (excepcion) {
        is FirebaseTooManyRequestsException ->
            "Firebase pausó los envíos por demasiados intentos seguidos. Espera unos minutos y toca «Reenviar correo»."
        is FirebaseNetworkException -> "No se pudo enviar el correo: revisa tu conexión y toca «Reenviar correo»."
        else -> "No se pudo enviar el correo de verificación. Toca «Reenviar correo» en un momento."
    }
}
