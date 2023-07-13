package de.grabelus.adoptme.ui.login

import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.grabelus.adoptme.MainActivity
import de.grabelus.adoptme.R
import de.grabelus.adoptme.databinding.ActivityLoginBinding
import de.grabelus.adoptme.ui.register.RegisterFragment
import de.grabelus.adoptme.ui.util.FragmentChangeListener


class LoginActivity : AppCompatActivity(), FragmentChangeListener {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onBackPressed() {
        // do nothing
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = binding.email
        val password = binding.password
        val login = binding.loginButton
        val loading = binding.loginLoading
        val registerButton = binding.registerButton
        val googleButton = binding.googleLoginButton
        val facebookButton = binding.facebookLoginButton

        configureGoogleButton(googleButton)
        configureFaceBookButton(facebookButton)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())[LoginViewModel::class.java]

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

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

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) showLoginFailed(loginResult.error)
            if (loginResult.success != null) updateUiWithUser()
            setResult(Activity.RESULT_OK)
            //Complete and destroy login activity once successful
            finish()
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
                supportFragmentManager.beginTransaction()
                    .add(R.id.loginContainer, RegisterFragment()).commit()
            }
        }
    }

    private fun updateUiWithUser() {
        val i = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(i)
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureFaceBookButton(facebookButton: Button) {
        facebookButton.setBackgroundColor(getColor(com.facebook.login.R.color.com_facebook_blue))

        facebookButton.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                facebookButton.setBackgroundColor(getColor(com.facebook.login.R.color.com_facebook_blue))
            } else if (event?.action == MotionEvent.ACTION_DOWN) {
                facebookButton.setBackgroundColor(getColor(com.facebook.login.R.color.com_facebook_button_background_color_pressed))
            }
            false
        }
    }

    private fun configureGoogleButton(googleLoginButton: com.shobhitpuri.custombuttons.GoogleSignInButton) {
        // to implement
    }

    override fun replaceFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(0, fragment, fragment.toString())
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.commit()
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