package com.laralbir.japantraveltools.feature.ai

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.core.preferences.DefaultUserPreferencesRepository
import com.laralbir.japantraveltools.core.preferences.UserPreferences

@Composable
fun GeminiApiKeySetupCard(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    onKeySaved: ((String) -> Unit)? = null,
    onKeyDeleted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentSavedKey = prefs.geminiApiKey
    val hasKey = !currentSavedKey.isNullOrBlank()

    var apiKeyInput by rememberSaveable(currentSavedKey) {
        mutableStateOf(currentSavedKey ?: "")
    }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    var showGuideSteps by rememberSaveable { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val guideSteps = remember(isEs) {
        GeminiApiKeyHelper.getSetupGuideSteps(isEs)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasKey) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("✨", fontSize = 20.sp)
                    Text(
                        text = if (isEs) "Google Gemini API Key" else "Google Gemini API Key",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasKey) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (hasKey) {
                            if (isEs) "Cloud Activo" else "Cloud Active"
                        } else {
                            if (isEs) "100% Offline" else "100% Offline"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasKey) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Explanatory Subtitle
            Text(
                text = if (hasKey) {
                    if (isEs) {
                        "Clave configurada: ${GeminiApiKeyHelper.maskApiKey(currentSavedKey)}. El asistente usará Gemini 2.0 Flash en la nube y cambiará al motor experto offline si no hay conexión."
                    } else {
                        "Configured key: ${GeminiApiKeyHelper.maskApiKey(currentSavedKey)}. Assistant uses Gemini 2.0 Flash online and falls back to offline engine without connection."
                    }
                } else {
                    if (isEs) {
                        "Google ofrece acceso 100% gratuito a Gemini 2.0 Flash para usuarios particulares. Puedes obtener tu clave en 1 minuto usando tu cuenta de Google."
                    } else {
                        "Google provides 100% free access to Gemini 2.0 Flash for personal accounts. You can get your key in 1 minute using your Google account."
                    }
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Direct 1-Click Action to open Google AI Studio
            Button(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GeminiApiKeyHelper.AI_STUDIO_API_KEY_URL)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🌐", fontSize = 16.sp)
                    Text(
                        text = if (isEs) "Obtener Clave Gratis en Google AI Studio" else "Get Free Key on Google AI Studio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expandable Step-by-Step Guide
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGuideSteps = !showGuideSteps }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💡", fontSize = 14.sp)
                        Text(
                            text = if (isEs) "¿Cómo obtenerla en 4 pasos sencillos?" else "How to get it in 4 easy steps?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (showGuideSteps) "▲ Ocultar" else "▼ Ver pasos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = showGuideSteps) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    guideSteps.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = step.stepNumber.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = step.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Field with Show/Hide password toggle
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    statusMessage = null
                },
                label = { Text(if (isEs) "Pega tu Gemini API Key" else "Paste your Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Text(if (isKeyVisible) "🙈" else "👁️", fontSize = 16.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Validation indicator
            if (apiKeyInput.isNotBlank()) {
                val isValid = GeminiApiKeyHelper.isValidApiKeyFormat(apiKeyInput)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (isValid) "✅" else "⚠️", fontSize = 13.sp)
                    Text(
                        text = if (isValid) {
                            if (isEs) "Formato de clave de Google válido (AIzaSy...)" else "Valid Google API key format (AIzaSy...)"
                        } else {
                            if (isEs) "Las claves de Google suelen empezar por «AIzaSy»" else "Google API keys typically begin with «AIzaSy»"
                        },
                        fontSize = 12.sp,
                        color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status notification message
            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action Buttons: Paste / Save / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paste from clipboard button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                        if (!clipText.isNullOrBlank()) {
                            apiKeyInput = clipText
                            statusMessage = if (isEs) "📋 Clave pegada del portapapeles" else "📋 Key pasted from clipboard"
                        } else {
                            statusMessage = if (isEs) "⚠️ El portapapeles está vacío" else "⚠️ Clipboard is empty"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEs) "📋 Pegar" else "📋 Paste", fontSize = 13.sp)
                }

                // Save button
                Button(
                    onClick = {
                        val trimmed = apiKeyInput.trim()
                        if (trimmed.isNotBlank()) {
                            prefsRepository.setGeminiApiKey(trimmed)
                            statusMessage = if (isEs) "💾 Clave guardada correctamente" else "💾 Key saved successfully"
                            onKeySaved?.invoke(trimmed)
                        }
                    },
                    enabled = apiKeyInput.trim().isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEs) "💾 Guardar" else "💾 Save", fontSize = 13.sp)
                }

                // Delete button if key exists
                if (hasKey) {
                    OutlinedButton(
                        onClick = {
                            apiKeyInput = ""
                            prefsRepository.setGeminiApiKey(null)
                            statusMessage = if (isEs) "🗑️ Clave eliminada (Modo 100% offline activo)" else "🗑️ Key removed (100% offline active)"
                            onKeyDeleted?.invoke()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(if (isEs) "🗑️ Borrar" else "🗑️ Clear", fontSize = 13.sp)
                    }
                }
            }

            // Offline Peace of Mind Notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛡️", fontSize = 16.sp)
                    Text(
                        text = if (isEs) {
                            "Totalmente opcional: si no configuras clave, el Motor Experto integrado responderá siempre 100% offline sin necesidad de internet."
                        } else {
                            "Completely optional: without a key, the built-in Expert Engine will always answer 100% offline with no internet needed."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
