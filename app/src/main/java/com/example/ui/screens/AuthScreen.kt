package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.repository.FirebaseChatRepository
import com.example.ui.components.AvatarColorPairs
import com.example.ui.components.UserAvatar
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSecondaryButton
import com.example.ui.components.AppButtonShape
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AuthUiState

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.components.GoogleAuthHelper
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.example.ui.theme.OnlineGreen

@Composable
fun AuthScreen(
    authUiState: AuthUiState,
    errorMessage: String?,
    onSignUp: (String, String, String, String, Int) -> Unit,
    onLogIn: (String, String) -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onResetPassword: (String) -> Unit,
    onClearError: () -> Unit,
    onCheckUsernameAvailable: suspend (String) -> Boolean = { true }
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Log In, 1 = Sign Up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameValidationError by remember { mutableStateOf<String?>(null) }
    var selectedAvatarId by remember { mutableIntStateOf(0) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var googleAuthError by remember { mutableStateOf<String?>(null) }

    // Real-time username availability debounce
    val normalizedUsername = username.trim().lowercase().removePrefix("@")
    LaunchedEffect(normalizedUsername) {
        if (normalizedUsername.isBlank()) {
            usernameAvailable = null
            usernameValidationError = null
            isCheckingUsername = false
            return@LaunchedEffect
        }
        val regex = "^[a-zA-Z0-9_.]{4,20}$".toRegex()
        if (!regex.matches(normalizedUsername)) {
            if (normalizedUsername.length < 4) {
                usernameValidationError = "Must be at least 4 characters"
            } else if (normalizedUsername.length > 20) {
                usernameValidationError = "Maximum 20 characters"
            } else {
                usernameValidationError = "Only letters, numbers, dot, and underscore allowed"
            }
            usernameAvailable = null
            isCheckingUsername = false
            return@LaunchedEffect
        }
        usernameValidationError = null
        isCheckingUsername = true
        delay(400) // debounce 400ms
        val available = onCheckUsernameAvailable(normalizedUsername)
        usernameAvailable = available
        isCheckingUsername = false
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isLoading = authUiState is AuthUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AccentBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "WP CHAT Logo",
                    tint = AccentBlueDark,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "WP CHAT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Real-time online communication powered by Firebase",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkBg,
                        contentColor = AccentBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AccentBlue
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; onClearError() },
                            text = {
                                Text(
                                    "Log In",
                                    color = if (selectedTab == 0) AccentBlue else TextSecondary,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; onClearError() },
                            text = {
                                Text(
                                    "Sign Up",
                                    color = if (selectedTab == 1) AccentBlue else TextSecondary,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_signup")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message Banner
                    val displayError = errorMessage ?: googleAuthError
                    AnimatedVisibility(
                        visible = displayError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (displayError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3B1214))
                                    .border(1.dp, Color(0xFF8C1D24), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFFB4AB)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFDAD6),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Sign Up Specific: Display Name, Username / Chat ID & Avatar Picker
                    if (selectedTab == 1) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            placeholder = { Text("e.g. Alex River", color = TextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_display_name"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkBorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Unique Username / Chat ID Field
                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                // Filter strictly to letters, numbers, dot, underscore, and optional leading @
                                val clean = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '@' }
                                username = clean
                            },
                            label = { Text("Chat ID / Username (Unique)") },
                            placeholder = { Text("e.g. alex_99", color = TextSecondary) },
                            prefix = {
                                Text(
                                    "@",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = AccentBlue)
                            },
                            trailingIcon = {
                                when {
                                    isCheckingUsername -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = AccentBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    usernameAvailable == true -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Available",
                                            tint = OnlineGreen
                                        )
                                    }
                                    usernameAvailable == false || usernameValidationError != null -> {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Unavailable",
                                            tint = Color(0xFFFFB4AB)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                when {
                                    isCheckingUsername -> {
                                        Text("Checking availability...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    usernameValidationError != null -> {
                                        Text(usernameValidationError.orEmpty(), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                                    }
                                    usernameAvailable == true -> {
                                        Text("✓ @$normalizedUsername is available!", color = OnlineGreen, fontSize = 12.sp)
                                    }
                                    usernameAvailable == false -> {
                                        Text("✗ @$normalizedUsername is already taken", color = Color(0xFFFFB4AB), fontSize = 12.sp)
                                    }
                                    else -> {
                                        Text("4–20 characters (letters, numbers, _, .)", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_username"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = if (usernameAvailable == true) OnlineGreen else AccentBlue,
                                unfocusedBorderColor = DarkBorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Choose Avatar Style",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(AvatarColorPairs) { idx, _ ->
                                val isSelected = selectedAvatarId == idx
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedAvatarId = idx }
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) AccentBlue else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        name = displayName.ifBlank { username.ifBlank { "U" } },
                                        avatarId = idx,
                                        size = 38.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("name@example.com", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = AccentBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("At least 6 characters", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (selectedTab == 0) onLogIn(email, password)
                            else onSignUp(email, password, displayName, username, selectedAvatarId)
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorderSubtle
                        )
                    )

                    if (selectedTab == 0) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(onClick = {
                                resetEmail = email
                                showForgotPasswordDialog = true
                            }) {
                                Text(
                                    "Forgot Password?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentBlue
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button
                    AppPrimaryButton(
                        text = if (selectedTab == 0) "Log In to WP CHAT" else "Create WP CHAT Account",
                        onClick = {
                            focusManager.clearFocus()
                            if (selectedTab == 0) {
                                onLogIn(email, password)
                            } else {
                                onSignUp(email, password, displayName, username, selectedAvatarId)
                            }
                        },
                        isLoading = isLoading,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "submit_auth_button"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // "OR" Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(DarkBorderSubtle)
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(DarkBorderSubtle)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Continue with Google Button
                    AppSecondaryButton(
                        text = "Continue with Google",
                        onClick = {
                            focusManager.clearFocus()
                            googleAuthError = null
                            onClearError()
                            coroutineScope.launch {
                                GoogleAuthHelper.initiateGoogleSignIn(
                                    context = context,
                                    onSuccess = { idToken ->
                                        onGoogleSignIn(idToken)
                                    },
                                    onError = { error ->
                                        googleAuthError = error
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        containerColor = DarkBg,
                        borderColor = DarkBorder,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "google_sign_in_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Reset Password",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Enter the email associated with your Firebase account to receive password reset instructions.",
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorderSubtle
                        )
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = "Send Email",
                    onClick = {
                        onResetPassword(resetEmail)
                        showForgotPasswordDialog = false
                    },
                    height = 42.dp,
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}


