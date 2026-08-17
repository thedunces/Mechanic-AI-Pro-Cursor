package com.mechanicai.pro.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCredentialClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun requestIdToken(activity: Activity): String {
        val clientIdRes = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        require(clientIdRes != 0) {
            "Google Sign-In is not configured. Add a Web client ID in Firebase Authentication."
        }
        val serverClientId = context.getString(clientIdRes)
        require(serverClientId.isNotBlank()) {
            "Google Sign-In is not configured. Add a Web client ID in Firebase Authentication."
        }

        val googleIdRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        val result = try {
            credentialManager.getCredential(activity, googleIdRequest)
        } catch (_: NoCredentialException) {
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(serverClientId).build()
                )
                .build()
            credentialManager.getCredential(activity, fallbackRequest)
        }

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Google Sign-In did not return an ID token.")
    }
}
