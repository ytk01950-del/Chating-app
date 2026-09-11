package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AvatarColorPairs
import com.example.ui.components.GoogleAuthHelper
import com.example.ui.components.NexaGeometricLogo
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.OnlineGreen
import com.example.viewmodel.AuthUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium Dark Glassmorphic Login & Sign-Up Screen for Nexa Messenger
 */
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

    // Real-time username availability check with debouncing
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
                usernameValidationError = "Letters, numbers, dot, and underscore only"
            }
            usernameAvailable = null
            isCheckingUsername = false
            return@LaunchedEffect
        }
        usernameValidationError = null
        isCheckingUsername = true
        delay(400)
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
            .background(Color(0xFF000000))
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Centered Nexa Logo
            NexaGeometricLogo(
                size = 88.dp,
                shape = RoundedCornerShape(22.dp),
                hasBorder = true,
                borderColor = Color(0x33FFFFFF),
                testTag = "auth_nexa_logo"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name & Subtitle
            Text(
                text = "Nexa Messenger",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Real-time online communication",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 0.2.sp
                ),
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glassmorphism-style Login Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0x55000000),
                        spotColor = Color(0x33000000)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF121212),
                border = BorderStroke(1.dp, Color(0x24FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Segmented Control Pill: Log In vs Sign Up
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181818))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Log In Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (selectedTab == 0) Color.White else Color.Transparent
                                    )
                                    .clickable {
                                        selectedTab = 0
                                        onClearError()
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) Color.Black else Color(0xFF888888)
                                )
                            }

                            // Sign Up Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (selectedTab == 1) Color.White else Color.Transparent
                                    )
                                    .clickable {
                                        selectedTab = 1
                                        onClearError()
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_signup"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) Color.Black else Color(0xFF888888)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Error Notification Banner
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF381215))
                                        .border(1.dp, Color(0xFF8C1D24), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFFFB4AB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
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

                        // Sign Up specific fields: Display Name, Username, Avatar Selector
                        if (selectedTab == 1) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name", color = Color(0xFFCCCCCC)) },
                                placeholder = { Text("e.g. Alex River", color = Color(0xFF777777)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_display_name"),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF181818),
                                    unfocusedContainerColor = Color(0xFF181818),
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0x2EFFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Chat ID / Unique Username
                            OutlinedTextField(
                                value = username,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '@' }
                                    username = clean
                                },
                                label = { Text("Chat ID / Username (Unique)", color = Color(0xFFCCCCCC)) },
                                placeholder = { Text("e.g. alex_99", color = Color(0xFF777777)) },
                                prefix = {
                                    Text(
                                        "@",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color.White)
                                },
                                trailingIcon = {
                                    when {
                                        isCheckingUsername -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        usernameAvailable == true -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Available",
                                                tint = Color.White
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
                                            Text("Checking availability...", color = Color(0xFF888888), fontSize = 12.sp)
                                        }
                                        usernameValidationError != null -> {
                                            Text(usernameValidationError.orEmpty(), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                                        }
                                        usernameAvailable == true -> {
                                            Text("✓ @$normalizedUsername is available!", color = Color.White, fontSize = 12.sp)
                                        }
                                        usernameAvailable == false -> {
                                            Text("✗ @$normalizedUsername is already taken", color = Color(0xFFFFB4AB), fontSize = 12.sp)
                                        }
                                        else -> {
                                            Text("4–20 characters (letters, numbers, _, .)", color = Color(0xFF777777), fontSize = 12.sp)
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
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF181818),
                                    unfocusedContainerColor = Color(0xFF181818),
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0x2EFFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Choose Profile Avatar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCCCCCC)
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
                                                color = if (isSelected) Color.White else Color.Transparent,
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
                            label = { Text("Email Address", color = Color(0xFFCCCCCC)) },
                            placeholder = { Text("Email or Username", color = Color(0xFF777777)) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
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
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF181818),
                                unfocusedContainerColor = Color(0xFF181818),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0x2EFFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFFCCCCCC)) },
                            placeholder = { Text("Password", color = Color(0xFF777777)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF888888)
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
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF181818),
                                unfocusedContainerColor = Color(0xFF181818),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0x2EFFFFFF)
                            )
                        )

                        // Forgot Password Link
                        if (selectedTab == 0) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TextButton(
                                    onClick = {
                                        resetEmail = email
                                        showForgotPasswordDialog = true
                                    }
                                ) {
                                    Text(
                                        "Forgot Password?",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Premium Monochrome Login Action Button (White with Black text)
                        val buttonInteraction = remember { MutableInteractionSource() }
                        val isPressed by buttonInteraction.collectIsPressedAsState()
                        val animatedBtnScale by animateFloatAsState(
                            targetValue = if (isPressed && !isLoading) 0.97f else 1f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "btnScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(animatedBtnScale)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = buttonInteraction,
                                    indication = ripple(bounded = true, color = Color.Black),
                                    role = Role.Button,
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (selectedTab == 0) {
                                            onLogIn(email, password)
                                        } else {
                                            onSignUp(email, password, displayName, username, selectedAvatarId)
                                        }
                                    }
                                )
                                .testTag("submit_auth_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (selectedTab == 0) "LOGIN" else "CREATE ACCOUNT",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch Mode text links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Don't have an account? " else "Already have an account? ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = if (selectedTab == 0) "Create a New Account" else "Log In",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier
                                    .clickable {
                                        selectedTab = if (selectedTab == 0) 1 else 0
                                        onClearError()
                                    }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // OR Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(0x28FFFFFF))
                            )
                            Text(
                                text = "OR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF757582),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(0x28FFFFFF))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Continue with Google Button (Black with white border and white text)
                        val googleInteraction = remember { MutableInteractionSource() }
                        val isGooglePressed by googleInteraction.collectIsPressedAsState()
                        val googleBtnScale by animateFloatAsState(
                            targetValue = if (isGooglePressed && !isLoading) 0.98f else 1f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                            label = "googleScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(googleBtnScale)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181818))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = googleInteraction,
                                    indication = ripple(bounded = true, color = Color.White),
                                    role = Role.Button,
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
                                    }
                                )
                                .padding(horizontal = 16.dp)
                                .testTag("google_sign_in_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google 'G' Symbol representation
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Password Dialog (Dark Monochrome Style)
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = Color(0xFF181818),
            title = {
                Text(
                    "Reset Password",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Enter the email associated with your account to receive password reset instructions.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address", color = Color(0xFFCCCCCC)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0x2EFFFFFF)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetPassword(resetEmail)
                        showForgotPasswordDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Send Email", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        )
    }
}
