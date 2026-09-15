package com.xpspeak.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

/**
 * Módulo de red base. Los clientes Retrofit específicos para Azure OpenAI
 * (chatbot, RF-06) y Azure AI Speech (voz, RF-07/RF-08) se agregan aquí
 * mismo en la Fase 3, una vez que definamos cómo se manejarán las claves
 * de API de forma segura (NUNCA hardcodeadas).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
}
