package com.xpspeak.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xpspeak.app.feature.account.ui.AccountScreen
import com.xpspeak.app.feature.auth.ui.AuthScreen
import com.xpspeak.app.feature.auth.ui.RecuperarAccesoScreen
import com.xpspeak.app.feature.auth.ui.SeleccionarNivelScreen
import com.xpspeak.app.feature.chat.ui.ChatScreen
import com.xpspeak.app.feature.games.ui.GamesScreen
import com.xpspeak.app.feature.lessons.ui.LessonsScreen
import com.xpspeak.app.feature.progress.ui.ProgressScreen

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.CHAT, "Chat", Icons.Filled.Chat),
    BottomTab(Routes.LESSONS, "Lecciones", Icons.Filled.MenuBook),
    BottomTab(Routes.PROGRESS, "Progreso", Icons.Filled.Home),
    BottomTab(Routes.GAMES, "Juegos", Icons.Filled.EmojiEvents),
    BottomTab(Routes.ACCOUNT, "Cuenta", Icons.Filled.AccountCircle),
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

    Scaffold(
        bottomBar = {
            // La barra inferior no se muestra en la pantalla de Auth
            val hideBottomBar = currentRoute?.hierarchy?.any {
                it.route == Routes.AUTH || it.route == Routes.SELECCIONAR_NIVEL || it.route == Routes.RECUPERAR_ACCESO
            } == true
            if (!hideBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.AUTH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.AUTH) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Routes.CHAT) {
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
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.SELECCIONAR_NIVEL) { inclusive = true }
                    }
                })
            }
            composable(Routes.CHAT) { ChatScreen() }
            composable(Routes.LESSONS) { LessonsScreen() }
            composable(Routes.GAMES) { GamesScreen() }
            composable(Routes.PROGRESS) { ProgressScreen() }
            composable(Routes.ACCOUNT) { AccountScreen() }
        }
    }
}
