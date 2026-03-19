/**
 * Main application activity.
 *
 * Responsibilities:
 * - Owns the navigation graph (Splash -> permission screens -> Home).
 * - Drives the overlay service start/stop based on required permissions.
 * - Wires the cached calendar event list from [com.okmoto.calamari.calendar.CalendarRepository]
 *   into [com.okmoto.calamari.home.HomeScreen] for the events bottom sheet.
 */
package com.okmoto.calamari

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.okmoto.calamari.calendar.CalendarRepository
import com.okmoto.calamari.core.REQUIRED_PERMISSIONS_FOR_HOME
import com.okmoto.calamari.core.isGranted
import com.okmoto.calamari.home.HomeScreen
import com.okmoto.calamari.navigation.Routes
import com.okmoto.calamari.navigation.routeForPermission
import com.okmoto.calamari.overlay.MainBubbleService
import com.okmoto.calamari.overlay.ListeningStateStore
import com.okmoto.calamari.overlay.MainActivityForegroundStore
import com.okmoto.calamari.permissions.PermissionsGateViewModel
import com.okmoto.calamari.permissions.screens.CalendarPermissionScreen
import com.okmoto.calamari.permissions.screens.CalendarSetupScreen
import com.okmoto.calamari.permissions.screens.MicrophonePermissionScreen
import com.okmoto.calamari.permissions.screens.NotificationsPermissionScreen
import com.okmoto.calamari.permissions.screens.OverlayPermissionScreen
import com.okmoto.calamari.permissions.screens.SplashScreen
import com.okmoto.calamari.ui.theme.CalamariTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gateViewModel: PermissionsGateViewModel by viewModels()
    private var calendarSetupAcknowledged: Boolean = false
    @Inject
    lateinit var listeningStateStore: ListeningStateStore
    @Inject
    lateinit var mainActivityForegroundStore: MainActivityForegroundStore
    @Inject
    lateinit var calendarRepository: CalendarRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalamariTheme {
                val navController = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current
                val listeningState by listeningStateStore.state.collectAsState()
                val createdEvents by calendarRepository.createdEvents.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Splash,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                    ) {
                        composable(Routes.Splash) {
                            SplashScreen(
                                onReady = {
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.PermissionNotifications) {
                            NotificationsPermissionScreen(
                                onPermissionSatisfied = {
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.PermissionMicrophone) {
                            MicrophonePermissionScreen(
                                onPermissionSatisfied = {
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.PermissionCalendar) {
                            CalendarPermissionScreen(
                                onPermissionSatisfied = {
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.CalendarSetup) {
                            CalendarSetupScreen(
                                onSkip = {
                                    calendarSetupAcknowledged = true
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.PermissionOverlay) {
                            OverlayPermissionScreen(
                                onPermissionSatisfied = {
                                    navigateToFirstMissingOrHome(navController)
                                },
                            )
                        }
                        composable(Routes.Home) {
                            HomeScreen(
                                listeningState = listeningState,
                                createdEvents = createdEvents,
                            )
                        }
                    }

                    // Any time we resume (or the user revokes permissions in settings),
                    // force navigation back to the first missing permission.
                    DisposableEffect(lifecycleOwner) {
                        val job = lifecycleOwner.lifecycleScope.launch {
                            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                navigateToFirstMissingOrHome(navController)
                            }
                        }
                        onDispose { job.cancel() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivityForegroundStore.setMainActivityResumed(true)
        val allGranted = REQUIRED_PERMISSIONS_FOR_HOME.all { it.isGranted(this) }
        if (allGranted) {
            val intent = Intent(this, MainBubbleService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } else {
            stopService(Intent(this, MainBubbleService::class.java))
        }
    }

    override fun onPause() {
        mainActivityForegroundStore.setMainActivityResumed(false)
        super.onPause()
    }

    private fun navigateToFirstMissingOrHome(navController: androidx.navigation.NavHostController) {
        val missing = gateViewModel.firstMissingPermission()
        val targetRoute = when {
            shouldShowCalendarSetup() -> Routes.CalendarSetup
            missing != null -> missing.routeForPermission()
            else -> Routes.Home
        }

        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute == targetRoute) return

        navController.navigate(targetRoute) {
            launchSingleTop = true
            // Keep the back stack clean: the user should never "back" into Home
            // when a required permission is missing.
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
        }
    }

    private fun shouldShowCalendarSetup(): Boolean {
        return !calendarSetupAcknowledged && !CalendarRepository.hasWritableCalendar(this)
    }
}
