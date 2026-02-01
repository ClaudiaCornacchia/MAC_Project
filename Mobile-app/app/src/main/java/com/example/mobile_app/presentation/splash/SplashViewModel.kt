package com.example.mobile_app.presentation.splash


import com.example.mobile_app.BOXES_SCREEN
import com.example.mobile_app.SIGN_IN_SCREEN
import com.example.mobile_app.SPLASH_SCREEN
import com.example.mobile_app.data.repository.AccountRepository
import com.example.mobile_app.presentation.BoxAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : BoxAppViewModel() {

    fun onAppStart(openAndPopUp: (String, String) -> Unit) {
        if (accountRepository.hasUser()) {
            // User is logged go to the home page
            openAndPopUp(BOXES_SCREEN, SPLASH_SCREEN)
        } else {
            // User not logged go to login
            openAndPopUp(SIGN_IN_SCREEN, SPLASH_SCREEN)
        }
    }
}