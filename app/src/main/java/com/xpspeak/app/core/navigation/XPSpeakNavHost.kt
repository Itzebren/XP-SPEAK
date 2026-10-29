package com.xpspeak.app.core.navigation

import com.xpspeak.app.core.ui.theme.XPFondo
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xpspeak.app.feature.account.ui.AccountScreen
import com.xpspeak.app.feature.account.ui.ConfirmarEliminacionScreen
import com.xpspeak.app.feature.auth.ui.AuthScreen
import com.xpspeak.app.feature.auth.ui.RecuperarAccesoScreen
import com.xpspeak.app.feature.auth.ui.SeleccionarNivelScreen
import com.xpspeak.app.feature.chat.ui.ChatScreen
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.GamesScreen
import com.xpspeak.app.feature.games.ui.MinijuegoScreen
import com.xpspeak.app.feature.home.ui.InicioScreen
import com.xpspeak.app.feature.notifications.ui.NotificacionesScreen
import com.xpspeak.app.feature.lessons.ui.LeccionScreen
import com.xpspeak.app.feature.lessons.ui.LessonsScreen
import com.xpspeak.app.feature.lessons.ui.RepasoScreen
import com.xpspeak.app.feature.progress.ui.ProgressScreen
import com.xpspeak.app.feature.voice.ui.PronunciacionScreen
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

// Mismo orden que la barra de las Ilustraciones 32–41, con Inicio al centro.
// Cuenta (Ajustes de perfil) se abre desde el avatar del Inicio.
private val bottomTabs = listOf(
    BottomTab(Routes.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
    BottomTab(Routes.LESSONS, "Lecciones", Icons.AutoMirrored.Filled.MenuBook),
    BottomTab(Routes.INICIO, "Inicio", Icons.Filled.Home),
    BottomTab(Routes.PROGRESS, "Progreso", Icons.Filled.WorkspacePremium),
    BottomTab(Routes.GAMES, "Juegos", Icons.Filled.SportsEsports),
)

/**
 * Grafo de navegación raíz. Por ahora arranca directo en AUTH; cuando
 * construyamos el módulo de autenticación (Fase 2) esto cambiará para
 * decidir el destino inicial según si hay una sesión guardada.
 */
@Composable
fun XPSpeakNavHost(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination

    // La barra inferior no se muestra en las pantallas de acceso.
    val hideBottomBar = currentRoute?.hierarchy?.any {
        it.route == Routes.AUTH || it.route == Routes.SELECCIONAR_NIVEL || it.route == Routes.RECUPERAR_ACCESO || it.route == Routes.PRONUNCIACION
    } == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Sin el inset de arriba: cada pantalla pone su encabezado azul debajo de la barra
        // de estado (statusBarsPadding dentro del encabezado), como en las Ilustraciones 31–41.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(containerColor = XPBlanco) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            // También resalta la pestaña en sus pantallas hijas (p. ej. games/{id})
                            selected = currentRoute?.hierarchy?.any {
                                it.route == tab.route || it.route?.startsWith("${tab.route}/") == true
                            } == true,
                            onClick = { navController.irAPestana(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = XPMarino,
                                selectedTextColor = XPMarino,
                                indicatorColor = XPAzulClaro,
                                unselectedIconColor = XPTextoSecundario,
                                unselectedTextColor = XPTextoSecundario
                            ),
                            // Una sola línea: con fuente grande en pantallas chicas se partía la palabra.
                            label = { Text(tab.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.AUTH,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .then(if (hideBottomBar) Modifier.navigationBarsPadding() else Modifier)
            ) {
                composable(Routes.AUTH) {
                    AuthScreen(
                        onAuthSuccess = {
                            navController.navigate(Routes.INICIO) {
                                popUpTo(Routes.AUTH) { inclusive = true }
                            }
                        },
                        onNecesitaNivel = {
                            navController.navigate(Routes.SELECCIONAR_NIVEL) {
                                popUpTo(Routes.AUTH) { inclusive = true }
                            }
                        },
                        onOlvideContrasena = {
                            navController.navigate(Routes.RECUPERAR_ACCESO)
                        }
                    )
                }
                composable(Routes.RECUPERAR_ACCESO) {
                    RecuperarAccesoScreen(onCompletado = {
                        navController.popBackStack()
                    })
                }
                composable(Routes.SELECCIONAR_NIVEL) {
                    SeleccionarNivelScreen(onNivelSeleccionado = {
                        navController.navigate(Routes.INICIO) {
                            popUpTo(Routes.SELECCIONAR_NIVEL) { inclusive = true }
                        }
                    })
                }
                composable(Routes.INICIO) {
                    InicioScreen(
                        onLecciones = { navController.irAPestana(Routes.LESSONS) },
                        onMinijuegos = { navController.irAPestana(Routes.GAMES) },
                        onChat = { navController.irAPestana(Routes.CHAT) },
                        onProgreso = { navController.irAPestana(Routes.PROGRESS) },
                        onAjustes = { navController.navigate(Routes.ACCOUNT) },
                        onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) }
                    )
                }
                composable(Routes.ELIMINAR_CUENTA) {
                    ConfirmarEliminacionScreen(
                        onCancelar = { navController.popBackStack() },
                        onEliminada = {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.NOTIFICACIONES) {
                    NotificacionesScreen(onVolver = { navController.popBackStack() })
                }
                composable(Routes.CHAT) {
                    ChatScreen(
                        onNavigateToPronunciacion = {
                            navController.navigate(Routes.PRONUNCIACION)
                        }
                    )
                }
                composable(Routes.LESSONS) {
                    LessonsScreen(
                        onAbrirLeccion = { id -> navController.navigate(Routes.leccion(id)) },
                        onAbrirRepaso = { navController.navigate(Routes.REPASO) }
                    )
                }
                composable(Routes.REPASO) {
                    RepasoScreen(onVolver = { navController.popBackStack() })
                }
                composable(Routes.LECCION) {
                    LeccionScreen(onVolver = { navController.popBackStack() })
                }
                composable(Routes.GAMES) {
                    GamesScreen(onAbrirJuego = { juego -> navController.navigate(Routes.minijuego(juego.id)) })
                }
                composable(Routes.MINIJUEGO) { entry ->
                    MinijuegoScreen(
                        juego = Minijuego.porId(entry.arguments?.getString("id")),
                        onVolver = { navController.popBackStack() }
                    )
                }
                composable(Routes.PROGRESS) {
                    ProgressScreen(onIrALecciones = { navController.irAPestana(Routes.LESSONS) })
                }
                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        onVolver = { navController.popBackStack() },
                        onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                        onEliminarCuenta = { navController.navigate(Routes.ELIMINAR_CUENTA) },
                        onCerrarSesion = {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.PRONUNCIACION) {
                    PronunciacionScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            // Franja detrás de la barra de estado: al hacer scroll el contenido no se encima con la
            // hora. Azul claro como los encabezados; color de fondo en las pantallas sin encabezado.
            val sinEncabezado = currentRoute?.hierarchy?.any {
                it.route == Routes.AUTH || it.route == Routes.RECUPERAR_ACCESO || it.route == Routes.ELIMINAR_CUENTA
            } == true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(if (sinEncabezado) XPFondo else XPAzulClaro)
            )
        }
    }
}

/** Cambia de pestaña conservando el estado de cada una (como la barra inferior). */
private fun NavHostController.irAPestana(ruta: String) = navigate(ruta) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
