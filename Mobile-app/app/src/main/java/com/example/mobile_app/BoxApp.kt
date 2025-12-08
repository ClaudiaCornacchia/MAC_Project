package com.example.mobile_app


import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color


@Composable
@OptIn(ExperimentalMaterial3Api::class) // Opt-in required because some Material3 APIs (like Scaffold) might still be experimental

fun BoxApp() {
    BoxTheme {
        // A container that fills the screen and sets the default background color
        Surface(color = MaterialTheme.colorScheme.background) {

            val snackbarHostState = remember { SnackbarHostState() }
            val appState = rememberAppState(snackbarHostState)
            // Changed the snackBarHostState popup style with custom layout
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                    { data ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red),
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
//                                Icon(
//                                    painter = painterResource(id = R.drawable.ic_warning),
//                                    contentDescription = null,
//                                    tint = Color.White
//                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = data.visuals.message, // <--- Take the text from the Manager
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            ) { innerPaddingModifier ->
                NavHost(
                    navController = appState.navController,
                    startDestination = SPLASH_SCREEN, // The first screen to load (Splash)
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
    // Sign In Screen (login page)
    composable(SIGN_IN_SCREEN) {
        //
        SignInScreen(
            // Standard navigation (SigningScreen.kt use openScreen to go to the signup page through appState.navigate(SIGNUP_SCREEN))
            // Navigate and not navigateAndPopup because from the signup I can with the arrow go back to sign in
            openScreen = { route -> appState.navigate(route) },
            // Example: go to the Home screen and REMOVE the Login screen (used by SigningScreen.kt)
            openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) }
        )
    }

    // Signup Screen
    composable(SIGN_UP_SCREEN) {
        // When registration is successful, we behave exactly like the login:
        // Navigate to Home and remove the Sign Up screen from history.
        SignUpScreen(openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) })
    }

    // The start destination. It checks if the user is already logged in.
    composable(SPLASH_SCREEN) {
        SplashScreen(openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) })
    }

    // Account Center (logout and account management)
    composable(ACCOUNT_CENTER_SCREEN) {
        AccountCenterScreen(restartApp = { route -> appState.clearAndNavigate(route) })
    }
}