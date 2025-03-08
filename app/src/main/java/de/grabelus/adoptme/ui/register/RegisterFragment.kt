package de.grabelus.adoptme.ui.register

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.grabelus.adoptme.R
import de.grabelus.adoptme.databinding.FragmentRegisterBinding
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.Date


class RegisterFragment : Fragment() {

    private lateinit var email: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var repeatedPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var birthDateEditText: EditText

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerViewModel = ViewModelProvider(this, RegisterViewModelFactory())[RegisterViewModel::class.java]

        email = binding.emailEditText
        usernameEditText = binding.nameEditText
        passwordEditText = binding.passwordEditText
        repeatedPasswordEditText = binding.repeatedPasswordEditText
        registerButton = binding.registerButton
        loadingProgressBar = binding.registerLoading
        birthDateEditText = binding.birthDateEditText!!

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
        birthDateEditText!!.setOnClickListener {
            showBirthDatePicker()
        }
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
            // TODO : initiate successful registered experience
            val appContext = context?.applicationContext ?: return
            Toast.makeText(appContext, "The registration was successfull", Toast.LENGTH_LONG).show()
            navigateToLogin()
        } else {
            showRegisterFailed(R.string.something_went_wrong)
        }
    }

    private fun showBirthDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context?.applicationContext ?: return,
            { _: DatePicker?, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate =
                    selectedDay.toString() + "/" + (selectedMonth + 1) + "/" + selectedYear
                birthDateEditText?.setText(selectedDate)
            },
            1900, 0, 1
        )
        datePickerDialog.datePicker.maxDate = Date().time
        datePickerDialog.datePicker.minDate = LocalDate.of(1900, Month.JANUARY, 1)
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()
        datePickerDialog.show()
    }

    private fun showRegisterFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    private fun backToStart() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    private fun navigateToLogin() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}