package com.mechanicai.pro.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class of navigation destinations.
 */
@Serializable
sealed class NavRoutes {

    @Serializable
    data object Splash : NavRoutes()

    @Serializable
    data object Dashboard : NavRoutes()

    @Serializable
    data object Vehicles : NavRoutes()

    @Serializable
    data class AddVehicle(val vehicleId: String? = null) : NavRoutes()

    @Serializable
    data object ManualDiagnosis : NavRoutes()

    @Serializable
    data object BluetoothScan : NavRoutes()

    @Serializable
    data object DiagnosisResult : NavRoutes()

    @Serializable
    data object History : NavRoutes()

    @Serializable
    data object Settings : NavRoutes()

    @Serializable
    data object Subscription : NavRoutes()
}
