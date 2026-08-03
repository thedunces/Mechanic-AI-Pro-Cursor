package com.mechanicai.pro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import com.mechanicai.pro.data.model.DiagnosticSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private fun userId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User must be signed in")

    fun observeSessions(): Flow<List<DiagnosticSession>> {
        return firestore
            .collection("users")
            .document(userId())
            .collection("sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects() }
    }
}
