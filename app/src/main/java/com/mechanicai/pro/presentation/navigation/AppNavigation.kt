package com.mechanicai.pro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mechanicai.pro.presentation.dashboard.DashboardScreen
import com.mechanicai.pro.presentation.diagnosis.ManualDiagnosisScreen
import com.mechanicai.pro.presentation.history.HistoryScreen
import com.mechanicai.pro.presentation.legal.LegalLinks
import com.mechanicai.pro.presentation.obd.BluetoothScanScreen
import com.mechanicai.pro.presentation.settings.SettingsScreen
import com.mechanicai.pro.presentation.splash.SplashScreen
import com.mechanicai.pro.presentation.subscription.SubscriptionScreen
import com.mechanicai.pro.presentation.vehicle.AddVehicleScreen
import com.mechanicai.pro.presentation.vehicle.VehicleListScreen

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isAuthenticated by mainViewModel.isAuthenticated.collectAsState()
    var hadSession by remember { mutableStateOf(isAuthenticated) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            hadSession = true
        } else if (hadSession) {
            navController.navigate(NavRoutes.Splash) {
                popUpTo(0) { inclusive = true }
            }
            hadSession = false
        }
    }

    val startDestination = if (isAuthenticated) NavRoutes.Dashboard else NavRoutes.Splash

    NavHost(navController = navController, startDestination = startDestination) {
        composable<NavRoutes.Splash> {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate(NavRoutes.Dashboard) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<NavRoutes.Dashboard> {
            DashboardScreen(
                onNavigateToVehicles = { navController.navigate(NavRoutes.Vehicles) },
                onNavigateToManualDiagnosis = { navController.navigate(NavRoutes.ManualDiagnosis) },
                onNavigateToBluetoothScan = { navController.navigate(NavRoutes.BluetoothScan) },
                onNavigateToHistory = { navController.navigate(NavRoutes.History) },
                onNavigateToAddVehicle = { navController.navigate(NavRoutes.AddVehicle()) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings) },
                onNavigateToSubscription = { navController.navigate(NavRoutes.Subscription) },
                onSignOut = { mainViewModel.signOut() }
            )
        }
        composable<NavRoutes.Vehicles> {
            VehicleListScreen(
                onAddVehicle = { navController.navigate(NavRoutes.AddVehicle()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.AddVehicle> {
            AddVehicleScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.ManualDiagnosis> {
            ManualDiagnosisScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.BluetoothScan> {
            BluetoothScanScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.DiagnosisResult> {
            ManualDiagnosisScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.History> {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoutes.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacyPolicy = { LegalLinks.openPrivacyPolicy(context) },
                onOpenTermsOfService = { LegalLinks.openTermsOfService(context) },
                onManagePlan = { navController.navigate(NavRoutes.Subscription) },
                onAccountDeleted = { mainViewModel.signOut() }
            )
        }
        composable<NavRoutes.Subscription> {
            SubscriptionScreen(onBack = { navController.popBackStack() })
        }
    }
}
