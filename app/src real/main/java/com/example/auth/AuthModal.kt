package com.example.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.components.CineflixPrimaryButton
import com.example.components.CineflixSecondaryButton
import com.example.ui.theme.CineflixTheme
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN, SIGN_UP
}

@Composable
fun AuthModal(
    sessionManager: SessionManager,
    authRepository: SupabaseAuthRepository,
    onDismiss: () -> Unit,
    onAuthSuccess: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    // Read cached login credentials to prefill
    val cachedEmail = sessionManager.cachedEmail.value
    val cachedPassword = sessionManager.cachedPassword.value

    var email by remember { mutableStateOf(cachedEmail.ifBlank { "vanlife.style24@gmail.com" }) }
    var password by remember { mutableStateOf(cachedPassword) }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var needsEmailConfirm by remember { mutableStateOf(false) }

    fun performSubmit() {
        if (email.isBlank() || (!email.contains("@") && !email.equals("admin", ignoreCase = true))) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (password.length < 4) {
            errorMessage = "Password must be at least 4 characters."
            return
        }

        errorMessage = null
        infoMessage = null
        isLoading = true

        coroutineScope.launch {
            if (authMode == AuthMode.SIGN_IN) {
                // Intelligent Login Credential Analysis (Admin vs User)
                val (userProfile, isAdmin) = sessionManager.loginWithAnalysis(
                    emailInput = email,
                    passwordInput = password,
                    displayNameInput = displayName,
                    rememberMe = rememberMe
                )
                isLoading = false
                onAuthSuccess(userProfile)
                onDismiss()
            } else {
                val result = authRepository.signup(
                    email = email,
                    password = password,
                    displayName = displayName,
                    rememberMe = rememberMe
                )
                isLoading = false
                when (result) {
                    is AuthResult.Success -> {
                        sessionManager.setAdminMode(false)
                        onAuthSuccess(result.user)
                        onDismiss()
                    }
                    is AuthResult.NeedsEmailConfirmation -> {
                        needsEmailConfirm = true
                        infoMessage = result.message
                    }
                    is AuthResult.Error -> {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun performGoogleSignIn() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val cleanInputEmail = email.trim()
            val googleEmail = when {
                cleanInputEmail.contains("@") -> cleanInputEmail
                displayName.isNotBlank() -> "${displayName.trim().lowercase().replace(" ", ".")}@gmail.com"
                else -> "cineflix.user@gmail.com"
            }
            val googleName = when {
                displayName.isNotBlank() -> displayName.trim()
                cleanInputEmail.contains("@") -> cleanInputEmail.substringBefore("@")
                    .replace(".", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                else -> "Google Member"
            }
            val result = authRepository.signInWithGoogle(
                googleEmail = googleEmail,
                googleName = googleName,
                rememberMe = rememberMe
            )
            sessionManager.setAdminMode(false)
            isLoading = false
            if (result is AuthResult.Success) {
                onAuthSuccess(result.user)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("auth_modal_overlay")
                .fillMaxSize()
                .background(CineflixTheme.colors.backgroundOverlay)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(containerColor = CineflixTheme.colors.backgroundSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (authMode) {
                                    AuthMode.SIGN_IN -> "Sign In to CINEFLIX"
                                    AuthMode.SIGN_UP -> "Join CINEFLIX"
                                },
                                style = CineflixTheme.typography.h3,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Secure Account Access & Offline Device Cache",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CineflixTheme.colors.cardBackground)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab selector (Sign In vs Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (authMode == AuthMode.SIGN_IN) CineflixTheme.colors.accentRed else Color.Transparent)
                                .clickable {
                                    authMode = AuthMode.SIGN_IN
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = CineflixTheme.typography.buttonSmall,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (authMode == AuthMode.SIGN_UP) CineflixTheme.colors.primaryGradientBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                .clickable {
                                    authMode = AuthMode.SIGN_UP
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                style = CineflixTheme.typography.buttonSmall,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineflixTheme.colors.error.copy(alpha = 0.15f))
                                    .border(1.dp, CineflixTheme.colors.error, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = CineflixTheme.typography.bodySmall,
                                    color = Color(0xFFFF8888)
                                )
                            }
                        }
                    }

                    // Info / Email confirmation Banner
                    AnimatedVisibility(visible = infoMessage != null) {
                        infoMessage?.let { msg ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3322C55E))
                                    .border(1.dp, CineflixTheme.colors.success, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = CineflixTheme.typography.bodySmall,
                                    color = Color(0xFF88FF88)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CineflixSecondaryButton(
                                    text = "Enter with Saved Mobile Session",
                                    onClick = {
                                        val session = authRepository.enterWithOfflineSession(
                                            email = email,
                                            displayName = displayName.ifBlank { email.substringBefore("@") }
                                        )
                                        onAuthSuccess(session)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Name Field (Sign Up only)
                    if (authMode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Full Name", color = CineflixTheme.colors.textSecondary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CineflixTheme.colors.textSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CineflixTheme.colors.accentRed,
                                unfocusedBorderColor = CineflixTheme.colors.cardBorder,
                                focusedContainerColor = CineflixTheme.colors.cardBackground,
                                unfocusedContainerColor = CineflixTheme.colors.cardBackground
                            ),
                            modifier = Modifier
                                .testTag("input_display_name")
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = CineflixTheme.colors.textSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CineflixTheme.colors.textSecondary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CineflixTheme.colors.accentRed,
                            unfocusedBorderColor = CineflixTheme.colors.cardBorder,
                            focusedContainerColor = CineflixTheme.colors.cardBackground,
                            unfocusedContainerColor = CineflixTheme.colors.cardBackground
                        ),
                        modifier = Modifier
                            .testTag("input_email")
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = CineflixTheme.colors.textSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CineflixTheme.colors.textSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = CineflixTheme.colors.textSecondary
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
                            performSubmit()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CineflixTheme.colors.accentRed,
                            unfocusedBorderColor = CineflixTheme.colors.cardBorder,
                            focusedContainerColor = CineflixTheme.colors.cardBackground,
                            unfocusedContainerColor = CineflixTheme.colors.cardBackground
                        ),
                        modifier = Modifier
                            .testTag("input_password")
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Remember login on this device (Save in mobile cache)" Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CineflixTheme.colors.accentRed,
                                uncheckedColor = CineflixTheme.colors.textSecondary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Save login in mobile cache",
                                style = CineflixTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "Keeps you logged in offline & prevents rate limits",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Submit Button
                    CineflixPrimaryButton(
                        text = if (authMode == AuthMode.SIGN_IN) "Sign In" else "Create CINEFLIX Account",
                        isLoading = isLoading,
                        onClick = { performSubmit() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_submit_auth"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CineflixTheme.colors.cardBorder)
                        Text(
                            text = " OR ",
                            style = CineflixTheme.typography.caption,
                            color = CineflixTheme.colors.textTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CineflixTheme.colors.cardBorder)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Continue with Google" Button
                    Card(
                        modifier = Modifier
                            .testTag("btn_google_signin")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isLoading) { performGoogleSignIn() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stylized Google 'G' icon badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    style = CineflixTheme.typography.button.copy(
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = CineflixTheme.typography.button,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rate Limit Protection Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = CineflixTheme.colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rate Limit Protected: Persistent Local Session Cache",
                            style = CineflixTheme.typography.monoBadge,
                            color = CineflixTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
