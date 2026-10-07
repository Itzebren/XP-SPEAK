package com.xpspeak.app.core.di

import android.content.Context
import androidx.room.Room
import com.xpspeak.app.core.data.local.AppDatabase
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.games.data.MinijuegosDao
import com.xpspeak.app.feature.lessons.data.LeccionesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xpspeak.db"
        )
        .addMigrations(AppDatabase.MIGRACION_2_3, AppDatabase.MIGRACION_3_4)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideUsuarioDao(db: AppDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideLeccionesDao(db: AppDatabase): LeccionesDao = db.leccionesDao()

    @Provides
    fun provideMinijuegosDao(db: AppDatabase): MinijuegosDao = db.minijuegosDao()
}
