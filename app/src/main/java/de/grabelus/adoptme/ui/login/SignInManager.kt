package de.grabelus.adoptme.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import de.grabelus.adoptme.R
import de.grabelus.adoptme.utils.NonceCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInManager {
    companion object {
        fun googleSignIn(
            context: Context,
            scope: CoroutineScope,
            login: () -> Unit
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptions(context))
                .build()
            scope.launch {
                try {
                    val result = credentialManager.getCredential(context,request)
                    handleSignIn(result, login)

                } catch (e: GetCredentialException){
                    e.printStackTrace()
                }
            }
        }

        private fun getCredentialOptions(context: Context): CredentialOption {
            return GetSignInWithGoogleOption.Builder(getString(context, R.string.default_web_client_id))
                //.setFilterByAuthorizedAccounts(true)
                //.setAutoSelectEnabled(true)
                .setNonce(NonceCreator.createNonce())
                .build()
        }

        private suspend fun handleSignIn(result: GetCredentialResponse, login: () -> Unit) {
            when (val credential = result.credential) {
                // Passkey credential
//            is PublicKeyCredential -> {
//                // Share responseJson such as a GetCredentialResponse on your server to
//                // validate and authenticate
//                responseJson = credential.authenticationResponseJson
//            }
                // Password credential
//            is PasswordCredential -> {
//                // Send ID and password to your server to validate and authenticate.
//                val username = credential.id
//                val password = credential.password
//            }

                // GoogleIdToken credential
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            if(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential = GoogleAuthProvider.getCredential(googleTokenId,null)
                                val user = Firebase.auth.signInWithCredential(authCredential).await().user
                                user?.let {
                                    if(it.isAnonymous.not()){
                                        login.invoke()
                                    }
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
    }
}