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
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import de.grabelus.adoptme.R
import de.grabelus.adoptme.data.Result
import de.grabelus.adoptme.data.UserRepository
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.utils.NonceCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SignInManager {
    companion object {
        private lateinit var authStateListener: AuthStateListener

        fun startGoogleSignIn(
            userRepository: UserRepository,
            context: Context,
            scope: CoroutineScope,
            login: (Result<LoggedInUser>) -> Unit
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptionsForSignIn(context))
                .build()
            scope.launch {
                try {
                    val result = credentialManager.getCredential(context,request)
                    handleSignIn(userRepository, result, login)

                } catch (e: GetCredentialException){
                    e.printStackTrace()
                }
            }
        }

        fun newAuthStateListener(): AuthStateListener {
            authStateListener = AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    Log.d(SignInManager::class.java.name, "onAuthStateChanged:signed_in:" + user.uid)
                } else {
                    Log.d(SignInManager::class.java.name, "onAuthStateChanged:signed_out")
                }
            }
            return authStateListener
        }

        fun authStateListener(): AuthStateListener {
           return authStateListener;
        }

        private fun getCredentialOptionsForSignIn(context: Context): CredentialOption {
            return GetSignInWithGoogleOption.Builder(getString(context, R.string.default_web_client_id))
                .setNonce(NonceCreator.createNonce())
                .build()
        }

        private suspend fun handleSignIn(userRepository: UserRepository, result: GetCredentialResponse, login: (Result<LoggedInUser>) -> Unit) {
            when (val credential = result.credential) {
                // GoogleIdToken credential
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            if(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                                val user = getUserByGooleAuth(result)
                                user?.let {
                                    if(it.isAnonymous.not()){
                                            login.invoke(userRepository.login(user))
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

        private suspend fun getUserByGooleAuth(result: GetCredentialResponse): FirebaseUser? {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val googleTokenId = googleIdTokenCredential.idToken
            val authCredential = GoogleAuthProvider.getCredential(googleTokenId,null)
            return Firebase.auth.signInWithCredential(authCredential).await().user
        }
    }
}