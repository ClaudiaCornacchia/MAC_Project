package com.example.mobile_app.screens.splash


import com.example.mobile_app.BOXES_SCREEN
import com.example.mobile_app.SIGN_IN_SCREEN
import com.example.mobile_app.SPLASH_SCREEN
import com.example.mobile_app.model.service.AccountService
import com.example.mobile_app.screens.BoxAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val accountService: AccountService
) : BoxAppViewModel() {

    fun onAppStart(openAndPopUp: (String, String) -> Unit) {
        if (accountService.hasUser()) {
            // User is logged go to the home page
            openAndPopUp(BOXES_SCREEN, SPLASH_SCREEN)
        } else {
            // User not logged go to login
            openAndPopUp(SIGN_IN_SCREEN, SPLASH_SCREEN)
        }
    }
}