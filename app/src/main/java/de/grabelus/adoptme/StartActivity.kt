package de.grabelus.adoptme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.grabelus.adoptme.databinding.ActivityStartBinding
import de.grabelus.adoptme.ui.login.LoginFragment
import de.grabelus.adoptme.ui.register.RegisterFragment
import io.realm.Realm

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val loginButton = binding.loginButton
        val registerButton = binding.registerButton;

        loginButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.start_container, LoginFragment()).commit()
        }

        registerButton?.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.start_container, RegisterFragment()).commit()
        }

        Realm.init(this) // context, usually an Activity or Application
    }
}