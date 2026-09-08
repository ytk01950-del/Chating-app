package com.example.ui.components

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    suspend fun initiateGoogleSignIn(
        context: Context,
        onSuccess: (idToken: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val serverClientId = if (resId != 0) {
                    context.getString(resId)
                } else {
                    "830188890451-4ggb17rtmp6vhtedb1o9bghf88md3pb0.apps.googleusercontent.com"
                }
                if (serverClientId.isBlank()) {
                    onError("Google Web Client ID is not configured.")
                    return@withContext
                }

                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    if (idToken.isNotBlank()) {
                        onSuccess(idToken)
                    } else {
                        onError("Failed to retrieve valid Google ID token.")
                    }
                } else {
                    onError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Google Sign-In was cancelled by the user")
                // User cancelled the prompt - no need for a scary error, or clear friendly info
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In credential error: ${e.message}", e)
                onError(e.localizedMessage ?: "Google Sign-In failed.")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In general error: ${e.message}", e)
                onError(e.localizedMessage ?: "Could not complete Google Sign-In.")
            }
        }
    }
}
