package de.grabelus.adoptme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.grabelus.adoptme.data.Result
import de.grabelus.adoptme.data.UserDataSource
import de.grabelus.adoptme.data.UserRepository
import de.grabelus.adoptme.data.UserService
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.databinding.ActivityStartBinding
import de.grabelus.adoptme.ui.PasswordResetFragment
import de.grabelus.adoptme.ui.login.SignInManager
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
    private lateinit var passkeyButton: CircleImageView

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
//        mAuth.addAuthStateListener(SignInManager.newAuthStateListener())
    }

    public override fun onStop() {
        super.onStop()
//        mAuth.removeAuthStateListener(SignInManager.authStateListener())
    }

    private fun getComponentsFromBinding() {
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText
        loginButton = binding.loginButton
        googleButton = binding.googleLoginButton
        facebookButton = binding.facebookLoginButton
        passkeyButton = binding.passkeyButton
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

        loginButton.setOnClickListener {
            val email: String = emailEditText.text.toString()
            val password: String = passwordEditText.text.toString()
            SignInManager.startCredLogin(userService, email, password, login = { loginResult -> login(loginResult) })
        }

        passkeyButton.setOnClickListener {
            SignInManager.startPassKeySignIn(userService,this, lifecycleScope, login = { loginResult -> login(loginResult) } )
        }

        passwordResetText.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.start_container, PasswordResetFragment()).commit()
        }

        googleButton.setOnClickListener {
            SignInManager.startGoogleSignIn(userService, this, lifecycleScope, login = { loginResult -> login(loginResult) })
        }
    }

    private fun login(result: Result<LoggedInUser>) {
        if (result is Result.Success) {
            navigateToMainScreen()
        } else {
            if (result is Result.Error) {
                showErrorLogin()
            }
        }
    }

    private fun showErrorLogin() {
        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}