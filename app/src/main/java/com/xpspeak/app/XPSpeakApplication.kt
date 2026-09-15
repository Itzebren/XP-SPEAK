package com.xpspeak.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application raíz de XP-SPEAK.
 *
 * @HiltAndroidApp genera el contenedor de dependencias de Hilt a nivel de
 * toda la app. Es el punto de entrada obligatorio para que @AndroidEntryPoint
 * funcione en Activities/ViewModels más adelante.
 */
@HiltAndroidApp
class XPSpeakApplication : Application()
