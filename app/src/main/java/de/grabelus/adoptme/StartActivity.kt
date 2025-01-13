package de.grabelus.adoptme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.grabelus.adoptme.data.UserDataSource
import de.grabelus.adoptme.data.UserRepository
import de.grabelus.adoptme.databinding.ActivityStartBinding
import de.grabelus.adoptme.ui.PasswordResetFragment
import de.grabelus.adoptme.ui.login.SignInManager
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm

class StartActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var passwordResetText: TextView
    private lateinit var googleButton: CircleImageView

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getComponentsFromBinding()
        configComponents()

        Realm.init(this) // context, usually an Activity or Application
        val dataSource = UserDataSource()
        userRepository = UserRepository(dataSource)

        if (userRepository.isLoggedIn) {
            navigateToMainScreen()
        }
    }

    private fun getComponentsFromBinding() {
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText
        loginButton = binding.loginButton
        googleButton = binding.googleLoginButton
        passwordResetText = binding.passwordResetText
    }

    private fun configComponents() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // TODO Fix
                val count = supportFragmentManager.backStackEntryCount
                if (count == 0) {
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        })

        loginButton.setOnClickListener {
            val email: String = emailEditText.text.toString()
            val password: String = passwordEditText.text.toString()
            login(email, password)
        }

        passwordResetText.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.start_container, PasswordResetFragment()).commit()
        }

        googleButton.setOnClickListener {
            SignInManager.googleSignIn(this, lifecycleScope, login = { navigateToMainScreen() })
        }
    }

    private fun login(username: String, password: String) {
        val result = userRepository.login(username, password)
        if (result is de.grabelus.adoptme.data.Result.Success) {
            navigateToMainScreen()
        } else {
            if (result is de.grabelus.adoptme.data.Result.Error) {
                Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun onClick(view: View) {}
}