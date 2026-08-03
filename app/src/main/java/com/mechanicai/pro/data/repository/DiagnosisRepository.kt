package com.mechanicai.pro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.mechanicai.pro.data.model.DiagnosisResult
import com.mechanicai.pro.data.model.DiagnosticInputs
import com.mechanicai.pro.data.model.DiagnosticSession
import com.mechanicai.pro.data.model.Vehicle
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the Cloud Function AI diagnosis endpoint and persists sessions to Firestore.
 */
@Singleton
class DiagnosisRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private fun userId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User must be signed in")

    /**
     * Calls the backend diagnose function and returns the structured result.
     */
    suspend fun diagnose(
        vehicle: Vehicle,
        inputs: DiagnosticInputs
    ): Result<DiagnosisResult> = runCatching {
        val data = hashMapOf(
            "vehicle" to mapOf(
                "make" to vehicle.make,
                "model" to vehicle.model,
                "year" to vehicle.year,
                "vin" to vehicle.vin,
                "engine" to vehicle.engine,
                "trim" to vehicle.trim
            ),
            "inputs" to mapOf(
                "obdCodes" to inputs.obdCodes,
                "liveData" to inputs.liveData.map { mapOf("name" to it.name, "value" to it.value, "unit" to it.unit) },
                "symptoms" to inputs.symptoms,
                "notes" to inputs.notes
            )
        )
        val result: HttpsCallableResult = functions
            .getHttpsCallable("diagnose")
            .call(data)
            .await()
        @Suppress("UNCHECKED_CAST")
        val map = result.data as? Map<String, Any?>
            ?: throw IllegalStateException("Invalid response from diagnosis function")
        val diagnosis = map["diagnosis"] as? Map<String, Any?>
            ?: throw IllegalStateException("Diagnosis response is missing")
        diagnosis.toDiagnosisResult()
    }

    /**
     * Saves a diagnostic session to Firestore.
     */
    suspend fun saveSession(session: DiagnosticSession): Result<Unit> = runCatching {
        val id = session.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val toSave = session.copy(
            id = id,
            userId = userId(),
            createdAt = session.createdAt ?: Date()
        )
        firestore
            .collection("users")
            .document(userId())
            .collection("sessions")
            .document(id)
            .set(toSave)
            .await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toDiagnosisResult(): DiagnosisResult {
        return DiagnosisResult(
            explanation = this["explanation"] as? String ?: "",
            likelyCauses = (this["likelyCauses"] as? List<String>) ?: emptyList(),
            recommendedFixes = (this["recommendedFixes"] as? List<String>) ?: emptyList(),
            severity = parseSeverity(this["severity"] as? String),
            partsNeeded = (this["partsNeeded"] as? List<String>) ?: emptyList(),
            safetyNotes = (this["safetyNotes"] as? List<String>) ?: emptyList(),
            whenToSeeMechanic = this["whenToSeeMechanic"] as? String ?: ""
        )
    }

    private fun parseSeverity(value: String?): com.mechanicai.pro.data.model.Severity {
        return try {
            com.mechanicai.pro.data.model.Severity.valueOf(value ?: "UNKNOWN")
        } catch (_: IllegalArgumentException) {
            com.mechanicai.pro.data.model.Severity.UNKNOWN
        }
    }
}
