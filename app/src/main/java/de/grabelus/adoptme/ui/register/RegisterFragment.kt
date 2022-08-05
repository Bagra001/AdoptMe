package de.grabelus.adoptme.ui.register

import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import de.grabelus.adoptme.databinding.FragmentRegisterBinding

import de.grabelus.adoptme.R
import de.grabelus.adoptme.ui.login.LoginActivity

class RegisterFragment : Fragment() {

    private lateinit var registerViewModel: RegisterViewModel
    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerViewModel = ViewModelProvider(this, RegisterViewModelFactory())
            .get(RegisterViewModel::class.java)

        val email = binding.email
        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val repeatedPasswordEditText = binding.repeatedPassword
        val registerButton = binding.register
        val loadingProgressBar = binding.registerLoading

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

    private fun navigateToLogin(success: Boolean) {
        if(success) {
            // TODO : initiate successful logged in experience
            val appContext = context?.applicationContext ?: return
            Toast.makeText(appContext, "The registration was successfull", Toast.LENGTH_LONG).show()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        } else {
            showRegisterFailed(R.string.something_went_wrong)
        }
    }

    private fun showRegisterFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}