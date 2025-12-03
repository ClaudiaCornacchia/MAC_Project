package com.example.mobile_app


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobile_app.screens.account_center.AccountCenterScreen
import com.example.mobile_app.screens.authentication.sign_in.SignInScreen
import com.example.mobile_app.screens.authentication.sign_up.SignUpScreen
import com.example.mobile_app.screens.splash.SplashScreen
import com.example.mobile_app.ui.theme.BoxTheme
import kotlinx.coroutines.CoroutineScope

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BoxApp() {
    BoxTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val snackbarHostState = remember { SnackbarHostState() }
            val appState = rememberAppState(snackbarHostState)

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPaddingModifier ->
                NavHost(
                    navController = appState.navController,
                    startDestination = SPLASH_SCREEN,
                    modifier = Modifier.padding(innerPaddingModifier)
                ) {
                    boxGraph(appState)
                }
            }
        }
    }
}

@Composable
fun rememberAppState(
    snackbarHostState: SnackbarHostState,
    navController: NavHostController = rememberNavController(),
    snackbarManager: SnackbarManager = SnackbarManager,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): BoxAppState {
    return remember(snackbarHostState, navController, snackbarManager, coroutineScope) {
        BoxAppState(snackbarHostState, navController, snackbarManager, coroutineScope)
    }
}

fun NavGraphBuilder.boxGraph(appState: BoxAppState) {
    // Schermata di Login
    composable(SIGN_IN_SCREEN) {
        SignInScreen(
            openScreen = { route -> appState.navigate(route) },
            openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) }
        )
    }

    // Schermata di Registrazione
    composable(SIGN_UP_SCREEN) {
        SignUpScreen(openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) })
    }

    // Splash Screen (per decidere se andare al login o alla home/account)
    composable(SPLASH_SCREEN) {
        SplashScreen(openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) })
    }

    // Account Center (Per effettuare il Logout e gestire il profilo)
    composable(ACCOUNT_CENTER_SCREEN) {
        AccountCenterScreen(restartApp = { route -> appState.clearAndNavigate(route) })
    }
}