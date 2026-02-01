package com.example.mobile_app.presentation.account_center


import com.example.mobile_app.SPLASH_SCREEN
import com.example.mobile_app.domain.model.User
import com.example.mobile_app.data.repository.AccountRepository
import com.example.mobile_app.presentation.BoxAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccountCenterViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : BoxAppViewModel() {
    // Backing property to avoid state updates from other classes
    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user.asStateFlow()

    init {
        launchCatching {
            _user.value = accountRepository.getUserProfile()
        }
    }

    fun onUpdateDisplayNameClick(newDisplayName: String) {
        launchCatching {
            accountRepository.updateDisplayName(newDisplayName)
            _user.value = accountRepository.getUserProfile()
        }
    }


    fun onSignOutClick(restartApp: (String) -> Unit) {
        launchCatching {
            accountRepository.signOut()
            restartApp(SPLASH_SCREEN)
        }
    }

    fun onDeleteAccountClick(restartApp: (String) -> Unit) {
        launchCatching {
            accountRepository.deleteAccount()
            restartApp(SPLASH_SCREEN)
        }
    }
}