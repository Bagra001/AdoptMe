package de.grabelus.adoptme.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import de.grabelus.adoptme.MainActivity
import de.grabelus.adoptme.R
import de.grabelus.adoptme.databinding.FragmentLoginBinding
import de.grabelus.adoptme.ui.register.RegisterFragment

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest
    private var showOneTapUI = true
    private val REQ_ONE_TAP = 1

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    backToStart()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        configGoogleLoginAndCheckLogedIn()

        val email = binding.email
        val password = binding.password
        val login = binding.loginButton
        val loading = binding.loginLoading
        val registerButton = binding.registerButton
        val googleButton = binding.googleLoginButton
        val facebookButton = binding.facebookLoginButton

        configureGoogleButton(googleButton)
        configureFaceBookButton(facebookButton)

        loginViewModel.loginFormState.observe(viewLifecycleOwner,
            Observer { loginState ->
                if (loginState == null) {
                    return@Observer
                }
                // disable login button unless both username / password is valid
                login.isEnabled = loginState.isDataValid
                login.alpha = if (login.isEnabled) 1f else 0.25f

                if (loginState.usernameError != null) {
                    email.error = getString(loginState.usernameError)
                }
                if (loginState.passwordError != null) {
                    password.error = getString(loginState.passwordError)
                }
            })

        loginViewModel.loginResult.observe(viewLifecycleOwner,
            Observer { loginResult ->
                loginResult ?: return@Observer
                loading.visibility = View.GONE
                if (loginResult.error != null) showLoginFailed(loginResult.error)
                if (loginResult.success != null) updateUiWithUser(loginResult.email, loginResult.userId)
                activity?.setResult(Activity.RESULT_OK)
                //Complete and destroy login activity once successful
//                activity?.finish()
            })

        email.afterTextChanged {
            loginViewModel.loginDataChanged(
                email.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    email.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            email.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(email.text.toString(), password.text.toString())
            }

            registerButton.setOnClickListener {
                activity?.supportFragmentManager?.beginTransaction()?.add(R.id.loginContainer, RegisterFragment())?.commit()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val password = credential.password
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d("googleSignIn", "Got ID token.")
                        }
                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d("googleSignIn", "Got password.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d("googleSignIn", "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d("googleSignIn", "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d("googleSignIn", "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }
                        else -> {
                            Log.d("googleSignIn", "Couldn't get credential from result." +
                                    " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }
    }

    private fun configGoogleLoginAndCheckLogedIn() {
        oneTapClient = Identity.getSignInClient(this.requireActivity());

        val account = GoogleSignIn.getLastSignedInAccount(this.requireActivity())
        if (account != null) {
            loginViewModel.loginResultUpdate(account.email, account.idToken!!.toInt())
            Toast.makeText(activity?.applicationContext, "Already logged in: " + account.email, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity?.applicationContext, "Not logged in", Toast.LENGTH_SHORT).show()
            initSignInRequest()
            initSignUpRequest()
        }
    }

    private fun initSignUpRequest() {
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()
    }

    private fun initSignInRequest() {
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build();
    }

    private fun updateUiWithUser(email: String?, userId: Int?) {
        val i = Intent(this.context, MainActivity::class.java)
        i.putExtra("email", email)
        i.putExtra("id", userId)
        startActivity(i)
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(activity?.applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureFaceBookButton(facebookButton: Button) {
        facebookButton.setBackgroundColor(getColor(this.requireContext(), com.facebook.login.R.color.com_facebook_blue))

        facebookButton.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                facebookButton.setBackgroundColor(getColor(this.requireContext(), com.facebook.login.R.color.com_facebook_blue))
            } else if (event?.action == MotionEvent.ACTION_DOWN) {
                facebookButton.setBackgroundColor(getColor(this.requireContext(), com.facebook.login.R.color.com_facebook_button_background_color_pressed))
            }
            false
        }
    }

    private fun configureGoogleButton(googleLoginButton: com.shobhitpuri.custombuttons.GoogleSignInButton) {
        googleLoginButton.alpha

        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this.requireActivity()) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("googleSignIn", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this.requireActivity()) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d("googleSignIn", e.localizedMessage)
                signUpWithGoogle()
            }
    }

    private fun signUpWithGoogle() {
        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(this.requireActivity()) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("googleSignUp", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this.requireActivity()) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                Log.d("googleSignUp", e.localizedMessage)
            }
    }

    private fun backToStart() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}