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
import com.example.mobile_app.screens.box.boxes.BoxesScreen
import com.example.mobile_app.screens.box.box_detail.BoxDetailScreen
import com.example.mobile_app.screens.box.edit_box.EditBoxScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
@OptIn(ExperimentalMaterial3Api::class) // Opt-in required because some Material3 APIs (like Scaffold) might still be experimental

fun BoxApp() {
    BoxTheme {
        // A container that fills the screen and sets the default background color
        Surface(color = MaterialTheme.colorScheme.background) {

            val snackbarHostState = remember { SnackbarHostState() }
            val appState = rememberAppState(snackbarHostState)

            // Monitoring the current route to show/hide the top app bar
            val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ""
            // Where we do not want the bar
            val noBarScreens = listOf(SPLASH_SCREEN, SIGN_IN_SCREEN, SIGN_UP_SCREEN)
            val showTopBar = currentRoute !in noBarScreens
            //Title dynamically
            val topBarTitle = when {
                currentRoute == BOXES_SCREEN -> "My Boxes"
                currentRoute == EDIT_BOX_SCREEN -> "New Box"
                currentRoute == ACCOUNT_CENTER_SCREEN -> "My Profile"
                currentRoute.startsWith("BoxDetailScreen") -> "Box Details"
                else -> "Box App"
            }

            // Changed the snackBarHostState popup style with custom layout
            Scaffold(
                topBar = {
                    if (showTopBar) {
                        BoxTopAppBar(
                            title = topBarTitle,
                            onProfileClick = { appState.navigate(ACCOUNT_CENTER_SCREEN) }
                        )
                    }
                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxTopAppBar(
    title: String,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
            }
        }
    )
}

fun NavGraphBuilder.boxGraph(appState: BoxAppState) {

    // 1. Authentication
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

    // 2. Box
    // All Boxes Screen
    composable(BOXES_SCREEN) {
        BoxesScreen(
            openScreen = { route -> appState.navigate(route) }
        )
    }

    // Add New Box Screen
    composable(EDIT_BOX_SCREEN) {
        EditBoxScreen(
            popUpScreen = { appState.popUp() }
        )
    }

    // Detail Screen (with arguments)
    composable(
        route = BOX_DETAIL_SCREEN, // "BoxDetailScreen/{boxId}"
        arguments = listOf(navArgument("boxId") { type = NavType.StringType })
    ) {
        BoxDetailScreen(
            popUpScreen = { appState.popUp() }
        )
    }
}