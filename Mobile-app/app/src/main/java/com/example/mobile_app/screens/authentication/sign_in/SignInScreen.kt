package com.example.mobile_app.screens.authentication.sign_in

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobile_app.screens.authentication.AuthenticationButton
import com.example.mobile_app.screens.authentication.launchCredManBottomSheet
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.AccountCircle // O Icons.Filled.Person
import com.example.mobile_app.R
import com.example.mobile_app.SIGN_UP_SCREEN


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SignInScreen(
    openScreen: (String) -> Unit,
    openAndPopUp: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val email = viewModel.email.collectAsState()
    val password = viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        launchCredManBottomSheet(context) { result ->
            viewModel.onSignInWithGoogle(result, openAndPopUp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Auth Icon",
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .size(100.dp), // Diamo una dimensione visibile
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp))

        OutlinedTextField(
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            value = email.value,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = { Text(stringResource(R.string.email)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email") }
        )

        OutlinedTextField(
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            value = password.value,
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = { Text(stringResource(R.string.password)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp))

        Button(
            onClick = { viewModel.onSignInClick(openAndPopUp) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 16.sp,
                modifier = modifier.padding(0.dp, 6.dp)
            )
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        Text(
            text = stringResource(R.string.or),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        AuthenticationButton(buttonText = R.string.sign_in_with_google) { credential ->
            viewModel.onSignInWithGoogle(credential, openAndPopUp)
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        TextButton(onClick = { openScreen(SIGN_UP_SCREEN) }) {
            Text(
                text = stringResource(R.string.sign_up_description),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

