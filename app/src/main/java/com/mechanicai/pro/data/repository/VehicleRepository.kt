package com.mechanicai.pro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mechanicai.pro.data.model.Vehicle
import com.mechanicai.pro.data.remote.nhtsa.NhtsaApiService
import com.mechanicai.pro.data.remote.nhtsa.NhtsaVinResult
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages vehicle data in Firestore and validates vehicle info via NHTSA vPIC.
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val nhtsaApi: NhtsaApiService
) {

    private fun userId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User must be signed in")

    private fun vehiclesCollection() = firestore
        .collection("users")
        .document(userId())
        .collection("vehicles")

    /**
     * Live stream of the user's vehicles ordered by creation date.
     */
    fun observeVehicles(): Flow<List<Vehicle>> {
        return vehiclesCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects() }
    }

    /**
     * Decodes a VIN using the NHTSA vPIC API.
     */
    suspend fun decodeVin(vin: String): Result<NhtsaVinResult> = runCatching {
        require(vin.length == 17) { "VIN must be 17 characters" }
        val response = nhtsaApi.decodeVin(vin)
        val result = response.Results.firstOrNull()
            ?: throw IllegalStateException("No data returned for VIN")
        result
    }

    /**
     * Saves a vehicle to Firestore. Generates an ID if the vehicle is new.
     */
    suspend fun saveVehicle(vehicle: Vehicle): Result<Vehicle> = runCatching {
        val id = vehicle.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val now = Date()
        val toSave = vehicle.copy(
            id = id,
            userId = userId(),
            createdAt = vehicle.createdAt ?: now,
            updatedAt = now
        )
        vehiclesCollection().document(id).set(toSave).await()
        toSave
    }

    /**
     * Deletes a vehicle and its associated sessions.
     * Note: sessions are left for Phase 3 implementation of cascading delete.
     */
    suspend fun deleteVehicle(vehicleId: String): Result<Unit> = runCatching {
        vehiclesCollection().document(vehicleId).delete().await()
    }
}
