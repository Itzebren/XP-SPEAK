package com.xpspeak.app.core.di

import com.google.firebase.auth.FirebaseAuth
import com.xpspeak.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        // Solo en debug con -PfirebaseAuthEmulador=true: cuentas de prueba en el emulador de Auth
        // (el correo de verificación se lee en http://localhost:9099/emulator/v1/projects/<id>/oobCodes).
        if (BuildConfig.FIREBASE_AUTH_EMULADOR) useEmulator("localhost", 9099)
    }
}
