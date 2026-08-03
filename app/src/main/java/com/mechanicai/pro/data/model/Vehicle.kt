package com.mechanicai.pro.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a vehicle owned by a user.
 */
data class Vehicle(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val vin: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val engine: String = "",
    val trim: String = "",
    val nickname: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) {
    val displayName: String
        get() = nickname.takeIf { it.isNotBlank() }
            ?: "${year} ${make} ${model}".trim()
}
