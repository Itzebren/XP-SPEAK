package com.xpspeak.app.feature.account.data

import android.content.Context
import androidx.work.WorkManager
import com.xpspeak.app.core.data.local.AppDatabase
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.auth.data.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import javax.inject.Inject

data class EliminarCuentaResponse(val eliminada: Boolean, val documentos: Int)

/** POST /api/account/delete (backend/api/account/delete.js). Exige el ID token. */
interface CuentaApi {
    @POST("api/account/delete")
    suspend fun eliminar(@Header("Authorization") auth: String): Response<EliminarCuentaResponse>
}

/**
 * RN-14 / Ilustración 36: borra la cuenta en el servidor (Firestore y Firebase Auth)
 * y luego todo lo que quedó en el teléfono. Sin conexión no se borra nada: la cuenta
 * no puede quedar a medias.
 */
class CuentaRepository @Inject constructor(
    private val api: CuentaApi,
    private val sesion: SesionActual,
    private val baseDeDatos: AppDatabase,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun eliminarCuenta(): Result<Unit> = runCatching {
        val respuesta = try {
            api.eliminar(sesion.autorizacion())
        } catch (e: IOException) {
            throw Exception("Necesitas conexión a internet para eliminar tu cuenta.")
        }
        if (!respuesta.isSuccessful) throw Exception("No se pudo eliminar la cuenta. Inténtalo de nuevo.")

        // Lo pendiente de enviar ya no tiene a quién pertenecer.
        WorkManager.getInstance(context).cancelAllWork()
        withContext(Dispatchers.IO) { baseDeDatos.clearAllTables() }
        authRepository.cerrarSesion()
    }
}
