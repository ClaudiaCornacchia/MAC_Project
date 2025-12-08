package com.example.mobile_app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Stable
class BoxAppState(
    val snackbarHostState: SnackbarHostState,
    val navController: NavHostController,
    private val snackbarManager: SnackbarManager,
    coroutineScope: CoroutineScope
) {
    init {
        // Launch a coroutine to listen for messages from the SnackbarManager
        coroutineScope.launch {
            // Ignoring null values (idle state), and trigger when a new message arrives
            snackbarManager.snackbarMessages.filterNotNull().collect { message ->
                // Display the message on the UI (snackbarHostState imported we just changed the appearance in BoxApp)
                snackbarHostState.showSnackbar(message)
                // Reset the state to null to prevent the message from appearing again (e.g., on screen rotation)
                snackbarManager.clearSnackbarState()
            }
        }
    }

    fun popUp() {
        // Go back to the previous screen (removes the top screen from the stack)
        navController.popBackStack()
    }

    fun navigate(route: String) {
        // If the user is already on this screen, do not create a new copy on top.
        // Just use the existing one.
        navController.navigate(route) { launchSingleTop = true }
    }

    fun navigateAndPopUp(route: String, popUp: String) {
        navController.navigate(route) {
            launchSingleTop = true
            // Remove the previous screen (popUp) from the back stack.
            // 'inclusive = true' means we remove the 'popUp' screen itself too.
            // Useful for Login -> Home (so "Back" doesn't return to Login).
            popUpTo(popUp) { inclusive = true }
        }
    }

    fun clearAndNavigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            // Pop everything up to the ID '0' (the root/start of the graph).
            // 'inclusive = true' clears the entire history stack.
            // Useful for Logout (so "Back" doesn't return to the user profile).
            popUpTo(0) { inclusive = true }
        }
    }
}