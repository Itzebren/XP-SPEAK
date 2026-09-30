package com.xpspeak.app.core.di

import com.xpspeak.app.BuildConfig
import com.xpspeak.app.feature.auth.data.RecuperacionApi
import com.xpspeak.app.feature.chat.data.ChatApi
import com.xpspeak.app.feature.lessons.data.LeccionesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)
}
