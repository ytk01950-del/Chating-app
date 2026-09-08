package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SupabaseConfigManager
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SupabaseStorageConfigDialog(
    onDismiss: () -> Unit,
    onConfigSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val configStatus = remember { SupabaseConfigManager.getConfigStatus(context) }
    val initialUrl = remember { SupabaseConfigManager.getProjectUrl(context) }
    val initialKey = remember { SupabaseConfigManager.getAnonKey(context) }

    var projectUrl by remember { mutableStateOf(initialUrl) }
    var anonKey by remember { mutableStateOf(initialKey) }
    var showKey by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("supabase_config_dialog"),
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Supabase Media Storage",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zero-Cost Media Storage (50 MB / file)",
                        color = OnlineGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status banner with Diagnostics
                Surface(
                    color = if (configStatus.isConfigured) OnlineGreen.copy(alpha = 0.12f) else AccentBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (configStatus.isConfigured) OnlineGreen.copy(alpha = 0.3f) else AccentBlue.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (configStatus.isConfigured) Icons.Default.CloudDone else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (configStatus.isConfigured) OnlineGreen else AccentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (configStatus.isConfigured) "Storage is Configured" else "Storage Setup Required",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (configStatus.isConfigured)
                                    "Ready for sending photos, videos, and 24h stories."
                                else
                                    "Configure SUPABASE_URL and SUPABASE_ANON_KEY (or SUPABASE_PUBLISHABLE_KEY) in AI Studio Secrets.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Non-sensitive Diagnostic Panel
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Runtime Diagnostics:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SUPABASE_URL detected:", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = configStatus.urlDetected,
                                color = if (configStatus.hasUrl) OnlineGreen else Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SUPABASE_PUBLIC_KEY detected:", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = configStatus.publicKeyDetected,
                                color = if (configStatus.hasKey) OnlineGreen else Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Key Type:", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = configStatus.keyTypeDisplay,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Project URL Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Supabase Project URL",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = projectUrl,
                        onValueChange = {
                            projectUrl = it
                            testResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_project_url_input"),
                        placeholder = {
                            Text(
                                text = "https://abcdefghijkl.supabase.co",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }

                // Anon / Publishable Key Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Anon / Publishable Public Key",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = anonKey,
                        onValueChange = {
                            anonKey = it
                            testResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_anon_key_input"),
                        placeholder = {
                            Text(
                                text = "sb_publishable_... or eyJhbGci...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Hide key" else "Show key",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }

                // Test Connection Feedback
                AnimatedVisibility(visible = testResult != null) {
                    Surface(
                        color = if (testIsSuccess == true) OnlineGreen.copy(alpha = 0.15f) else Color(0xFFEF5350).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (testIsSuccess == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (testIsSuccess == true) OnlineGreen else Color(0xFFEF5350),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testResult.orEmpty(),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Storage Buckets Note
                Surface(
                    color = DarkSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Supabase Storage Buckets:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• chat-media (for photos, videos, files)\n• profile-photos (for avatars)\n• stories (for 24h stories)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Test Connection Action
                AppSecondaryButton(
                    text = if (isTesting) "Testing Connection..." else "Test Connection",
                    onClick = {
                        if (projectUrl.isBlank() || anonKey.isBlank()) {
                            testResult = "Please enter both Project URL and Public Key"
                            testIsSuccess = false
                            return@AppSecondaryButton
                        }
                        isTesting = true
                        testResult = null
                        coroutineScope.launch {
                            val res = SupabaseConfigManager.testConnection(projectUrl, anonKey)
                            isTesting = false
                            res.fold(
                                onSuccess = { msg ->
                                    testResult = msg
                                    testIsSuccess = true
                                },
                                onFailure = { err ->
                                    testResult = err.message ?: "Connection test failed"
                                    testIsSuccess = false
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 42.dp,
                    isLoading = isTesting,
                    enabled = !isTesting,
                    testTag = "test_supabase_connection_button"
                )

                // Revert to Secrets option
                AppSecondaryButton(
                    text = "Reset to AI Studio Secrets",
                    onClick = {
                        SupabaseConfigManager.clearConfig(context)
                        projectUrl = SupabaseConfigManager.getProjectUrl(context)
                        anonKey = SupabaseConfigManager.getAnonKey(context)
                        testResult = "Reset to environment secrets."
                        testIsSuccess = true
                        Toast.makeText(context, "Storage reset to environment secrets", Toast.LENGTH_SHORT).show()
                        onConfigSaved()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 36.dp,
                    icon = Icons.Default.RestartAlt,
                    testTag = "revert_supabase_secrets_button"
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Save & Apply",
                onClick = {
                    if (projectUrl.isNotBlank() && anonKey.isNotBlank()) {
                        SupabaseConfigManager.saveConfig(context, projectUrl, anonKey)
                        Toast.makeText(context, "Storage configuration saved!", Toast.LENGTH_SHORT).show()
                        onConfigSaved()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please enter both URL and Key", Toast.LENGTH_SHORT).show()
                    }
                },
                height = 42.dp,
                testTag = "save_supabase_config_button"
            )
        },
        dismissButton = {
            AppSecondaryButton(
                text = "Close",
                onClick = onDismiss,
                height = 42.dp,
                testTag = "cancel_supabase_config_button"
            )
        }
    )
}
