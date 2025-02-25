package de.grabelus.adoptme.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.JsonObject
import de.grabelus.adoptme.R
import de.grabelus.adoptme.data.Result
import de.grabelus.adoptme.data.UserService
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.execptions.CredentialsNotFoundException
import de.grabelus.adoptme.utils.NonceCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64
import java.util.Collections


class SignInManager {
    companion object {

        fun processIfUserLoggedIn() {
            // TODO implement
        }

        fun startCredLogin(userService: UserService, email: String, password: String,
                           login: (Result<LoggedInUser>) -> Unit) {
            login.invoke(userService.login(email, password))
        }

        fun startSignIn(
            userService: UserService,
            context: Context,
            scope: CoroutineScope,
            login: (Result<LoggedInUser>) -> Unit
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptionsForGoogleSignIn(context))
                .addCredentialOption(GetPasswordOption())
                .addCredentialOption(GetPublicKeyCredentialOption(
                    requestJson = createPassKeyRequestJson()
                ))
                .build()
            scope.launch {
                try {
                    val result = credentialManager.getCredential(context,request)
                    handleSignIn(userService, result, login)

                } catch (e: GetCredentialException){
                    e.printStackTrace()
                } catch (e: NoCredentialException) {
                    login.invoke(Result.Error(CredentialsNotFoundException("No Credentials found")))
                }
            }
        }

        private fun getCredentialOptionsForGoogleSignIn(context: Context): CredentialOption {
            return GetSignInWithGoogleOption.Builder(getString(context, R.string.default_web_client_id))
                .setNonce(NonceCreator.createNonce())
                .build()
        }

        private fun handleSignIn(userService: UserService, result: GetCredentialResponse, login: (Result<LoggedInUser>) -> Unit) {
            when (val credential = result.credential) {
                is PasswordCredential -> {
                    val savedPassword = credential.password
                    val savedEMail = credential.id
                    // TODO validiere und hole user
                }
                is PublicKeyCredential -> {
                    val responseJson = credential.authenticationResponseJson
                    // TODO validiere und hole user
                }
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            if(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val googleTokenId = googleIdTokenCredential.idToken
                                val payload = validateGoogleIdToken(googleTokenId)
                                if(payload != null){
                                    login.invoke(userService.loginWithUserId(payload.email, payload.subject))
                                } else {
                                    Log.e(SignInManager::class.java.name, "Payload was null")
                                }
                            }
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e(SignInManager::class.java.name, "Received an invalid google id token response", e)
                        }
                    } else {
                        Log.e(SignInManager::class.java.name, "Unexpected type of credential")
                    }
                }
                else -> {
                    Log.e(SignInManager::class.java.name, "Unexpected type of credential")
                }
            }
        }

        private fun validateGoogleIdToken(idToken: String): GoogleIdToken.Payload? {
            var payLoad: GoogleIdToken.Payload? = null
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(Collections.singletonList(R.string.default_web_client_id.toString()))
                .build()
            try {
                val googleIdToken = verifier.verify(idToken)
                if(googleIdToken != null) {
                    payLoad = googleIdToken.payload
                } else {
                    Log.e(SignInManager::class.java.name, "Invalid ID Token")
                }
            } catch (e: Exception) {
                println("Error verifying ID token: ${e.message}")
                return payLoad
            }
            return payLoad;
        }

        private fun createPassKeyRequestJson(): String {
            val random = SecureRandom()
            val challenge = ByteArray(32)
            random.nextBytes(challenge)

            val challengeBase64: String =
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge)

            return JsonObject().apply {
                addProperty("challenge", challengeBase64)
                addProperty("rpId", "de.grabelus.adoptme")
                addProperty("timeout", 180000)
                addProperty("userVerification", "required")
            }.toString()
        }
    }
}