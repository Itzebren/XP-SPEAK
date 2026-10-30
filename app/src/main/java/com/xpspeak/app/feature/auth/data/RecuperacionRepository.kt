package com.xpspeak.app.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.gson.JsonParser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** El servidor no pudo mandar el código (503 CORREO_NO_CONFIGURADO / CORREO_NO_DISPONIBLE). */
class ServicioCorreoNoDisponible(mensaje: String) : Exception(mensaje)

class RecuperacionRepository @Inject constructor(
    private val api: RecuperacionApi,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun enviarCodigo(correo: String): Result<String> =
        manejarRespuesta { api.enviarCodigo(EnviarCodigoRequest(correo)) }

    suspend fun verificarCodigo(correo: String, codigo: String, nuevaPassword: String): Result<String> =
        manejarRespuesta {
            api.verificarCodigo(VerificarCodigoRequest(correo, codigo.uppercase(), nuevaPassword))
        }

    /**
     * Respaldo cuando el servidor no puede mandar el código (QA 29-oct, H8): el correo de
     * restablecimiento del propio Firebase, que no depende de Resend. Si el correo no existe
     * se responde igual que si existiera, para no revelar qué cuentas hay (RNF-06).
     */
    suspend fun enviarEnlaceFirebase(correo: String): Result<Unit> = runCatching {
        try {
            firebaseAuth.sendPasswordResetEmail(correo.trim()).await()
        } catch (error: FirebaseAuthInvalidUserException) {
            // Cuenta inexistente: mismo resultado hacia afuera.
        }
    }

    private suspend fun manejarRespuesta(
        llamada: suspend () -> retrofit2.Response<RecuperacionResponse>
    ): Result<String> = runCatching {
        val respuesta = llamada()
        if (respuesta.isSuccessful) {
            respuesta.body()?.mensaje ?: "Listo"
        } else {
            val cuerpo = respuesta.errorBody()?.string()
            val mensaje = mensajeDe(cuerpo) ?: "Ocurrió un error. Intenta de nuevo."
            if (esServicioNoDisponible(respuesta.code(), codigoDe(cuerpo))) throw ServicioCorreoNoDisponible(mensaje)
            throw Exception(mensaje)
        }
    }

    companion object {
        private val CODIGOS_SIN_CORREO = setOf("CORREO_NO_CONFIGURADO", "CORREO_NO_DISPONIBLE")

        /** Un 503 del envío de correo (o su código) amerita el respaldo de Firebase. */
        fun esServicioNoDisponible(httpCode: Int, codigo: String?): Boolean =
            codigo in CODIGOS_SIN_CORREO || httpCode == 503

        fun mensajeDe(cuerpo: String?): String? = campo(cuerpo, "error")
        fun codigoDe(cuerpo: String?): String? = campo(cuerpo, "codigo")

        private fun campo(cuerpo: String?, nombre: String): String? = cuerpo?.let {
            runCatching { JsonParser.parseString(it).asJsonObject.get(nombre)?.takeIf { e -> e.isJsonPrimitive }?.asString }.getOrNull()
        }?.takeIf { it.isNotBlank() }
    }
}
