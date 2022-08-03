package de.grabelus.adoptme.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.grabelus.adoptme.data.UserDataSource
import de.grabelus.adoptme.data.UserRepository
import de.grabelus.adoptme.data.UserService

class RegisterViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(
                userService = UserService(
                    userRepository = UserRepository(dataSource = UserDataSource()
                    )
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}