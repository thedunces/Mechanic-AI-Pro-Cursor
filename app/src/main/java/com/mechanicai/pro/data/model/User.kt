package com.mechanicai.pro.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a user profile in Firestore.
 */
data class User(
    @DocumentId
    val id: String = "",
    val isAnonymous: Boolean = true,
    val displayName: String? = null,
    val email: String? = null,
    val createdAt: Date? = null,
    val lastLoginAt: Date? = null
)
