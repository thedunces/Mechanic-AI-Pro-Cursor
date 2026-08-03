package com.mechanicai.pro.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * A single diagnostic session: inputs and the AI-generated result.
 */
data class DiagnosticSession(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val vehicleName: String = "",
    val inputs: DiagnosticInputs = DiagnosticInputs(),
    val result: DiagnosisResult? = null,
    val createdAt: Date? = null
)

/**
 * User-provided diagnostic inputs.
 */
data class DiagnosticInputs(
    val obdCodes: List<String> = emptyList(),
    val liveData: List<LiveDataParameter> = emptyList(),
    val symptoms: String = "",
    val notes: String = ""
)

/**
 * A live data parameter reading (e.g., coolant temp, RPM).
 */
data class LiveDataParameter(
    val name: String,
    val value: String,
    val unit: String
)

/**
 * Structured AI diagnosis output.
 */
data class DiagnosisResult(
    val explanation: String = "",
    val likelyCauses: List<String> = emptyList(),
    val recommendedFixes: List<String> = emptyList(),
    val severity: Severity = Severity.UNKNOWN,
    val partsNeeded: List<String> = emptyList(),
    val safetyNotes: List<String> = emptyList(),
    val whenToSeeMechanic: String = ""
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN
}
