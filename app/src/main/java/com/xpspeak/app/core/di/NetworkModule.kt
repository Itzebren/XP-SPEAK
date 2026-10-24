package com.xpspeak.app.core.di

import com.xpspeak.app.BuildConfig
import com.xpspeak.app.feature.account.data.CuentaApi
import com.xpspeak.app.feature.auth.data.RecuperacionApi
import com.xpspeak.app.feature.chat.data.ChatApi
import com.xpspeak.app.feature.games.data.MinijuegosApi
import com.xpspeak.app.feature.lessons.data.LeccionesApi
import com.xpspeak.app.feature.progress.data.ProgresoApi
import com.xpspeak.app.feature.voice.data.SpeechApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val BASE_URL_RECUPERACION = "https://xp-speak-auth-backend.vercel.app/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Solo en debug, y sin el token: el header Authorization lleva la sesión del usuario.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL_RECUPERACION)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideRecuperacionApi(retrofit: Retrofit): RecuperacionApi =
        retrofit.create(RecuperacionApi::class.java)

    @Provides
    @Singleton
    fun provideSpeechApi(retrofit: Retrofit): SpeechApi =
        retrofit.create(SpeechApi::class.java)

    /**
     * Lecciones usa su propia base URL: en debug apunta al backend local
     * (BuildConfig.LECCIONES_BASE_URL) para probar sin desplegar.
     */
    @Provides
    @Singleton
    @Named("lecciones")
    fun provideLeccionesRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.LECCIONES_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLeccionesApi(@Named("lecciones") retrofit: Retrofit): LeccionesApi =
        retrofit.create(LeccionesApi::class.java)

    /** Minijuegos vive en el mismo backend que Lecciones. */
    @Provides
    @Singleton
    fun provideMinijuegosApi(@Named("lecciones") retrofit: Retrofit): MinijuegosApi =
        retrofit.create(MinijuegosApi::class.java)

    /** Progreso también vive en el backend de Lecciones. */
    @Provides
    @Singleton
    fun provideProgresoApi(@Named("lecciones") retrofit: Retrofit): ProgresoApi =
        retrofit.create(ProgresoApi::class.java)

    /** Eliminar la cuenta (RN-14) vive en el mismo backend que Progreso. */
    @Provides
    @Singleton
    fun provideCuentaApi(@Named("lecciones") retrofit: Retrofit): CuentaApi =
        retrofit.create(CuentaApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit, client: OkHttpClient): ChatApi {
        // RNF-02: el log en nivel BODY lee la respuesta completa antes de entregarla y
        // anularía el streaming; el chat solo registra cabeceras.
        val clienteChat = client.newBuilder().apply {
            interceptors().removeAll { it is HttpLoggingInterceptor }
            addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            })
        }
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return retrofit.newBuilder().client(clienteChat).build().create(ChatApi::class.java)
    }
}
