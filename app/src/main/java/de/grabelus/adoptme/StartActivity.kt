package de.grabelus.adoptme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.grabelus.adoptme.data.Result
import de.grabelus.adoptme.data.UserDataSource
import de.grabelus.adoptme.data.UserRepository
import de.grabelus.adoptme.data.UserService
import de.grabelus.adoptme.data.entity.Status
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.databinding.ActivityStartBinding
import de.grabelus.adoptme.execptions.CredentialsNotFoundException
import de.grabelus.adoptme.execptions.MissingInputDataException
import de.grabelus.adoptme.ui.PasswordResetFragment
import de.grabelus.adoptme.ui.login.SignInManager
import de.grabelus.adoptme.ui.register.RegisterFragment
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm



class StartActivity : AppCompatActivity() {

    private lateinit var userService: UserService

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var passwordResetText: TextView
    private lateinit var googleButton: CircleImageView
    private lateinit var facebookButton: CircleImageView

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getComponentsFromBinding()
        configComponents()

        Realm.init(this) // context, usually an Activity or Application
        val dataSource = UserDataSource()
        userService = UserService(UserRepository(dataSource))

        if (userService.isLoggedIn) {
            navigateToMainScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        // TODO check user already logged in
    }

    private fun getComponentsFromBinding() {
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText
        loginButton = binding.loginButton
        googleButton = binding.googleLoginButton
        facebookButton = binding.facebookLoginButton
        passwordResetText = binding.passwordResetText

    }

    private fun configComponents() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val count = supportFragmentManager.backStackEntryCount
                if (count > 0) {
                    supportFragmentManager.popBackStack()
                }
            }
        })

        emailEditText.setOnFocusChangeListener {
          view, focus ->
            run {
                // TODO test regex
                if (!focus && emailEditText.text.isBlank()) {
                    emailEditText.error = R.string.empty_email.toString()
                } else {
                    emailEditText.error = null
                }
            }
        }

        passwordEditText.setOnFocusChangeListener {
                view, focus ->
            run {
                // TODO test regex
                if (!focus && passwordEditText.text.isBlank()) {
                    passwordEditText.error = R.string.empty_password.toString()
                } else if (!focus && passwordEditText.text.length <= 8) {
                    passwordEditText.error = R.string.invalid_password_length.toString()
                } else {
                    passwordEditText.error = null
                }
            }
        }

        loginButton.setOnClickListener {
            val email: String = emailEditText.text.toString()
            val password: String = passwordEditText.text.toString()
            if(email.isNotBlank() && password.isNotBlank()) {
                SignInManager.startCredLogin(userService, email, password, login = { loginResult -> login(loginResult) })
            } else {
                Toast.makeText(this, R.string.empty_login_fields, Toast.LENGTH_SHORT).show()
            }
        }

        passwordResetText.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.start_container, PasswordResetFragment()).commit()
        }

        googleButton.setOnClickListener {
            SignInManager.startSignIn(userService, this, lifecycleScope, login = { loginResult -> login(loginResult) })
        }
    }

    private fun login(result: Result<LoggedInUser>) {
        if (result is Result.Success) {
            when(result.data.status) {
                Status.IN_VERIFICATION, Status.IN_REGISTRATION -> showActivationNeeded(R.string.activation_needed_title, R.string.profile_not_activated)
                Status.ACTIVE -> navigateToMainScreen()
                Status.INACTIVATED, Status.BLOCKED -> showActivationNeeded(R.string.reactivation_needed_title, R.string.profile_reactivation_needed)
                Status.DELETED, Status.DELETION_REQUESTED, Status.IN_DELETION -> Toast.makeText(this, R.string.profile_deleted_message, Toast.LENGTH_SHORT).show()
            }
        } else {
            if (result is Result.Error) {
                when(result.exception) {
                    is CredentialsNotFoundException -> navigateToRegister()
                    is MissingInputDataException -> showErrorLogin(result.exception.message!!)
                    else -> showErrorLogin(R.string.login_failed.toString())
                }
            }
        }
    }

    private fun showActivationNeeded(title: Int, message: Int) {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.send_activation_link) { dialog, id -> TODO() }
            .setNegativeButton(R.string.cancel) { dialog, id -> dialog.cancel() }
            .show()
    }

    private fun showErrorLogin(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToRegister() {
        supportFragmentManager.beginTransaction()
            .add(R.id.start_container, RegisterFragment()).commit()
    }
}