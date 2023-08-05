package de.grabelus.adoptme.ui.register

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import de.grabelus.adoptme.R
import de.grabelus.adoptme.databinding.FragmentRegisterBinding
import de.grabelus.adoptme.ui.login.LoginFragment


class RegisterFragment : Fragment() {

    companion object {
        private const val REQ_ONE_TAP = 2
    }
    private var showOneTapUI = true

    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest

    private lateinit var registerViewModel: RegisterViewModel
    private var _binding: FragmentRegisterBinding? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO prüfe anmelden und register
        oneTapClient = Identity.getSignInClient(this.requireContext())
        signUpRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        _binding?.facebookLoginButton?.let { configureFaceBookButton(it) }
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerViewModel = ViewModelProvider(this, RegisterViewModelFactory())[RegisterViewModel::class.java]

        val email = binding.email
        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val repeatedPasswordEditText = binding.repeatedPassword
        val registerButton = binding.register
        val loginButton = binding.loginButton
        val loadingProgressBar = binding.registerLoading
        val googleButton = binding.googleLoginButton

        googleButton.setOnClickListener {
            oneTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(this.requireActivity()) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0, null)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this.requireActivity()) { e ->
                    // No Google Accounts found. Just continue presenting the signed-out UI.
                    Log.d(TAG, e.localizedMessage)
                    Toast.makeText(this.requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        }

        registerViewModel.registerFormState.observe(viewLifecycleOwner,
            Observer { registerFormState ->
                if (registerFormState == null) {
                    return@Observer
                }
                registerButton.isEnabled = registerFormState.isDataValid
                registerButton.alpha = if(registerButton.isEnabled) 1f else 0.25f
                registerFormState.emailError?.let {
                    email.error = getString(it)
                }
                registerFormState.usernameError?.let {
                    usernameEditText.error = getString(it)
                }
                registerFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
                }
                registerFormState.repeatedPasswordError?.let {
                    repeatedPasswordEditText.error = getString(it)
                } ?: run {
                    repeatedPasswordEditText.error = null
                }
            })

        registerViewModel.registerResult.observe(viewLifecycleOwner,
            Observer { registerResult ->
                registerResult ?: return@Observer
                loadingProgressBar.visibility = View.GONE
                registerResult.error?.let {
                    showRegisterFailed(it)
                }
                registerResult.success?.let {
                    navigateToLogin(it)
                }
            })

        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                registerViewModel.registerDataChanged(
                    email.text.toString(),
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    repeatedPasswordEditText.text.toString()
                )
            }
        }
        email.addTextChangedListener(afterTextChangedListener)
        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        repeatedPasswordEditText.addTextChangedListener(afterTextChangedListener)
        repeatedPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registerViewModel.register(
                    email.text.toString(),
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    repeatedPasswordEditText.text.toString()
                )
            }
            false
        }

        registerButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            registerViewModel.register(
                email.text.toString(),
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                repeatedPasswordEditText.text.toString()
            )
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
                            //TODO parse jwt idToken id: zbf
                            registerViewModel.register(credential.id!!, credential.displayName!!, idToken, idToken)
                            Log.d(TAG, "Got ID token.")

                        }
                        password != null -> {
                            registerViewModel.register(credential.id!!, credential.displayName!!, password, password)
                            Log.d(TAG, "Got ID token.")

                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }
                        else -> {
                            Log.d(TAG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLogin(success: Boolean) {
        if(success) {
            // TODO : initiate successful registered experience
            val appContext = context?.applicationContext ?: return
            Toast.makeText(appContext, "The registration was successfull", Toast.LENGTH_LONG).show()
            navigateToLogin()
        } else {
            showRegisterFailed(R.string.something_went_wrong)
        }
    }

    private fun showRegisterFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    private fun backToStart() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    private fun navigateToLogin() {
        parentFragmentManager.beginTransaction().replace(R.id.registerContainer, LoginFragment()).commit()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureFaceBookButton(facebookButton: Button) {
        context?.let { getColor(it, com.facebook.login.R.color.com_facebook_blue) }
            ?.let { facebookButton.setBackgroundColor(it) }

        facebookButton.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                context?.let { getColor(it, com.facebook.login.R.color.com_facebook_blue) }
                    ?.let { facebookButton.setBackgroundColor(it) }
            } else if (event?.action == MotionEvent.ACTION_DOWN) {
                context?.let { getColor(it, com.facebook.login.R.color.com_facebook_button_background_color_pressed) }
                    ?.let { facebookButton.setBackgroundColor(it) }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}