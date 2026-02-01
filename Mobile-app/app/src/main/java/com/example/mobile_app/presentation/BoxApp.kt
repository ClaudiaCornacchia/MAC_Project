package com.example.mobile_app.presentation

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
import com.example.mobile_app.presentation.account_center.AccountCenterScreen
import com.example.mobile_app.presentation.authentication.sign_in.SignInScreen
import com.example.mobile_app.presentation.authentication.sign_up.SignUpScreen
import com.example.mobile_app.presentation.splash.SplashScreen
import com.example.mobile_app.ui.theme.BoxTheme
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.mobile_app.presentation.box.boxes.BoxesScreen
import com.example.mobile_app.presentation.box.box_detail.BoxDetailScreen
import com.example.mobile_app.presentation.box.new_box.NewBoxScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import com.example.mobile_app.presentation.scan_qr.ScanQrScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mobile_app.ACCOUNT_CENTER_SCREEN
import com.example.mobile_app.BOXES_SCREEN
import com.example.mobile_app.BOX_DETAIL_SCREEN
import com.example.mobile_app.presentation.BoxAppState
import com.example.mobile_app.NEW_BOX_SCREEN
import com.example.mobile_app.R
import com.example.mobile_app.SCAN_QR_SCREEN
import com.example.mobile_app.SIGN_IN_SCREEN
import com.example.mobile_app.SIGN_UP_SCREEN
import com.example.mobile_app.SPLASH_SCREEN
import com.example.mobile_app.presentation.SnackbarManager

@Composable
@OptIn(ExperimentalMaterial3Api::class) // Opt-in required because some Material3 APIs (like Scaffold) might still be experimental

fun BoxApp() {
    BoxTheme {
        // A container that fills the screen and sets the default background color
        Surface(color = MaterialTheme.colorScheme.background) {

            val snackbarHostState = remember { SnackbarHostState() }
            val appState = rememberAppState(snackbarHostState)
            val context = LocalContext.current

            // State to track if the environment is dark
            var isDarkEnvironment by remember { mutableStateOf(false) }
            // State to track if the flashlight is currently ON
            var isFlashlightOn by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

                val sensorEventListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                            val lux = event.values[0]
                            // Threshold: 10 lux is typical for "dim/dark room"
                            // We use a small buffer to avoid flickering
                            isDarkEnvironment = lux < 10f

                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                // Register listener
                sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_UI)
                // Cleanup when app closes
                onDispose { sensorManager.unregisterListener(sensorEventListener) }
            }

            // Function to toggle flashlight
            fun toggleFlashlight() {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList[0]
                    val newState = !isFlashlightOn
                    cameraManager.setTorchMode(cameraId, newState)
                    isFlashlightOn = newState
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            // Monitoring the current route to show/hide the top app bar
            val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ""
            // Where we do not want the bar
            val noBarScreens = listOf(SPLASH_SCREEN, SIGN_IN_SCREEN, SIGN_UP_SCREEN)
            val showTopBar = currentRoute !in noBarScreens

            // Changed the snackBarHostState popup style with custom layout
            Scaffold(
                topBar = {
                    if (showTopBar) {
                        BoxTopAppBar(
                            onProfileClick = { appState.navigate(ACCOUNT_CENTER_SCREEN) },
                            // Show icon ONLY if it is dark OR if the flash is already on (so you can turn it off)
                            showFlashlight = isDarkEnvironment || isFlashlightOn,
                            isFlashlightOn = isFlashlightOn,
                            onFlashlightClick = { toggleFlashlight() }
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


@Composable
fun BoxlyLogo(modifier: Modifier = Modifier) {

        Image(
            painter = painterResource(id = R.drawable.boxly_logo),
            contentDescription = "Boxly App Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxTopAppBar(
    showFlashlight: Boolean,       // Should the icon appear? (Controlled by Light Sensor)
    isFlashlightOn: Boolean,       // Is the torch currently active?
    onFlashlightClick: () -> Unit, // Action to toggle torch
    onProfileClick: () -> Unit
) {
    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Surface(
        color = backgroundColor,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            TopAppBar(
                title = {
                    // Boxly branding with logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // App Logo
                        BoxlyLogo(
                            modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                        )

                        // App Name
                        Text(
                            text = "Boxly",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Flashlight icon with animation
                    if (showFlashlight) {
                        FlashlightButton(
                            isFlashlightOn = isFlashlightOn,
                            onClick = onFlashlightClick
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Profile button with modern styling
                    ProfileButton(onClick = onProfileClick)

                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    }
}

@Composable
private fun FlashlightButton(
    isFlashlightOn: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isFlashlightOn) 1.1f else 1f,
        animationSpec = tween(200),
        label = "flashlightScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isFlashlightOn)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "flashlightColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFlashlightOn)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "flashlightBackgroundColor"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .size(42.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = if (isFlashlightOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
            contentDescription = if (isFlashlightOn) "Turn off flashlight" else "Turn on flashlight",
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ProfileButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "profileScale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                isPressed = true
                onClick()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
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
    composable(NEW_BOX_SCREEN) {
        NewBoxScreen(

            navigate = { route -> appState.navigateAndPopUp(route, NEW_BOX_SCREEN) }
        )
    }

    // Detail Screen (with arguments)
    composable(
        route = "${BOX_DETAIL_SCREEN}?localUri={localUri}", // Aggiungi ?localUri={localUri}
        arguments = listOf(
            navArgument("boxId") { type = NavType.StringType },
            navArgument("localUri") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        ),
        deepLinks = listOf(
            navDeepLink {
                // This pattern must match the one in the Manifest and the one generated by Node.js
                uriPattern = "boxapp://box/{boxId}"
            }
        )

    ) { backStackEntry ->
        val boxId = backStackEntry.arguments?.getString("boxId") ?: ""
        val localUri = backStackEntry.arguments?.getString("localUri")
        BoxDetailScreen(
            boxId = boxId,
            localUri = localUri,
            // Case of redirecting from the QR code to the Box Detail screen
            openScreen = { route -> appState.navigate(route) },
            popUpScreen = { appState.popUp() }
        )
    }

    // QR Scanner Screen
    composable(SCAN_QR_SCREEN) {
        ScanQrScreen(
            openScreen = { route ->
                // We use navigate, but we might want to pop the scanner so back button goes to list
                appState.navigateAndPopUp(route, SCAN_QR_SCREEN)
            },
            popUpScreen = { appState.popUp() }
        )
    }
}