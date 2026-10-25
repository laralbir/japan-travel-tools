package com.laralbir.japantraveltools.core.cloud

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.core.portability.SharedTravelPlanStore
import com.laralbir.japantraveltools.core.preferences.DefaultUserPreferencesRepository
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.feature.documents.SharedDocumentsAndGuidesStore
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
import com.laralbir.japantraveltools.ui.main.AppPrimaryButton
import com.laralbir.japantraveltools.ui.main.AppSecondaryButton
import com.laralbir.japantraveltools.ui.main.KotlinConfCard

@Composable
fun GoogleDriveSyncCard(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var syncStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val allPlans by SharedTravelPlanStore.allPlansState.collectAsState()
    val allGuides by SharedDocumentsAndGuidesStore.guidesState.collectAsState()
    val allDocs by SharedDocumentsAndGuidesStore.documentsState.collectAsState()
    val allCities by SharedWeatherForecastStore.citiesState.collectAsState()

    // 1. Native System Google Account Chooser
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val email = GoogleAccountSelectorHelper.extractAccountEmail(result.data)
            if (email != null) {
                prefsRepository.setGoogleAccountSync(
                    email = email,
                    lastSyncedMillis = prefs.lastCloudSyncEpochMillis
                )
                syncStatusMessage = if (isEs) {
                    "✅ Cuenta de Google seleccionada: $email"
                } else {
                    "✅ Google account selected: $email"
                }
            }
        }
    }

    // 2. Export / Save backup package directly to Google Drive (Storage Access Framework)
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val email = prefs.googleAccountEmail ?: "usuario.japon@gmail.com"
                val bundle = GoogleDriveSyncHelper.buildSyncBundle(
                    accountEmail = email,
                    deviceModel = android.os.Build.MODEL ?: "Android Device",
                    plans = allPlans,
                    guides = allGuides,
                    documents = allDocs,
                    cities = allCities,
                    prefs = prefs,
                    timestampMillis = System.currentTimeMillis()
                )
                val json = GoogleDriveSyncHelper.exportCloudBackupJson(bundle)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                GoogleDriveSyncHelper.saveToLocalCloudCache(context, bundle)
                prefsRepository.setGoogleAccountSync(
                    email = email,
                    lastSyncedMillis = bundle.timestampMillis
                )
                syncStatusMessage = if (isEs) {
                    "✅ Copia guardada en Google Drive (${allPlans.size} planes, ${allGuides.size} guías, ${allDocs.size} documentos y ${allCities.size} ciudades)"
                } else {
                    "✅ Backup saved to Google Drive (${allPlans.size} plans, ${allGuides.size} guides, ${allDocs.size} docs and ${allCities.size} cities)"
                }
            }.onFailure {
                syncStatusMessage = if (isEs) {
                    "❌ Error al guardar en Google Drive: ${it.localizedMessage}"
                } else {
                    "❌ Error saving to Google Drive: ${it.localizedMessage}"
                }
            }
        }
    }

    // 3. Import / Restore & Merge backup package from Google Drive
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val bundle = GoogleDriveSyncHelper.importCloudBackupJson(json)
                if (bundle != null) {
                    val mergedPlans = GoogleDriveSyncHelper.mergePlans(allPlans, bundle.travelPlans)
                    SharedTravelPlanStore.replaceAllPlans(mergedPlans)

                    val mergedGuides = GoogleDriveSyncHelper.mergeGuides(allGuides, bundle.travelGuides)
                    SharedDocumentsAndGuidesStore.replaceAllGuides(mergedGuides)

                    val mergedDocs = GoogleDriveSyncHelper.mergeDocuments(allDocs, bundle.travelDocuments)
                    SharedDocumentsAndGuidesStore.replaceAllDocuments(mergedDocs)

                    val mergedCities = GoogleDriveSyncHelper.mergeWeatherCities(allCities, bundle.weatherCities)
                    SharedWeatherForecastStore.replaceAllCities(mergedCities)

                    GoogleDriveSyncHelper.saveToLocalCloudCache(context, bundle)

                    val emailToSet = bundle.accountEmail.ifBlank { prefs.googleAccountEmail }
                    prefsRepository.setGoogleAccountSync(
                        email = emailToSet,
                        lastSyncedMillis = bundle.timestampMillis
                    )

                    syncStatusMessage = if (isEs) {
                        "✅ Sincronización completada desde Google Drive: ${bundle.travelPlans.size} planes, ${bundle.travelGuides.size} guías, ${bundle.travelDocuments.size} reservas y ${bundle.weatherCities.size} ciudades actualizadas."
                    } else {
                        "✅ Synced from Google Drive: ${bundle.travelPlans.size} plans, ${bundle.travelGuides.size} guides, ${bundle.travelDocuments.size} bookings and ${bundle.weatherCities.size} cities updated."
                    }
                } else {
                    syncStatusMessage = if (isEs) {
                        "⚠️ El archivo seleccionado no contiene un respaldo válido de Japan Travel Tools"
                    } else {
                        "⚠️ Selected file is not a valid Japan Travel Tools backup"
                    }
                }
            }.onFailure {
                syncStatusMessage = if (isEs) {
                    "❌ Error al leer archivo de Drive: ${it.localizedMessage}"
                } else {
                    "❌ Error reading Drive file: ${it.localizedMessage}"
                }
            }
        }
    }

    fun launchSystemAccountPicker() {
        try {
            val intent = GoogleAccountSelectorHelper.createAccountChooserIntent()
            accountPickerLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            // Fallback for emulators without Play Services
            val defaultEmail = "carlos.travel.japan@gmail.com"
            prefsRepository.setGoogleAccountSync(
                email = defaultEmail,
                lastSyncedMillis = prefs.lastCloudSyncEpochMillis
            )
            syncStatusMessage = if (isEs) {
                "ℹ️ Selector nativo no disponible en este dispositivo. Vinculada cuenta de prueba: $defaultEmail"
            } else {
                "ℹ️ Native account picker unavailable. Linked test account: $defaultEmail"
            }
        }
    }

    KotlinConfCard(modifier = modifier) {
        Text(
            text = if (isEs) "Cuenta de Google y Respaldo en Drive" else "Google Account & Drive Sync",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (isEs) {
                "Sincroniza todos tus planes de viaje, horarios, guías, reservas y ciudades entre tu móvil Pixel y tu tablet TCL NXTPAPER mediante tu cuenta de Google."
            } else {
                "Sync all travel plans, schedules, guides, bookings and cities between your Pixel phone and TCL NXTPAPER tablet using Google Drive."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        // Selected Google Account Badge / Selector Button
        if (prefs.googleAccountEmail != null && prefs.googleAccountEmail.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "G",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text(
                                text = prefs.googleAccountEmail,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEs) "Cuenta vinculada del sistema" else "System Google Account",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(onClick = { launchSystemAccountPicker() }) {
                        Text(if (isEs) "Cambiar" else "Change", fontSize = 13.sp)
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEs) "Ninguna cuenta de Google seleccionada" else "No Google account selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppPrimaryButton(
                        text = if (isEs) "👤 Seleccionar Cuenta del Dispositivo" else "👤 Select Device Google Account",
                        onClick = { launchSystemAccountPicker() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Data inventory pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📦 " + (if (isEs) "Datos a sincronizar: " else "Items to sync: ") +
                        "${allPlans.size} " + (if (isEs) "planes" else "plans") + " · " +
                        "${allGuides.size} " + (if (isEs) "guías" else "guides") + " · " +
                        "${allDocs.size} " + (if (isEs) "documentos" else "docs") + " · " +
                        "${allCities.size} " + (if (isEs) "ciudades" else "cities"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Last synced status
        Text(
            text = "🕒 " + GoogleDriveSyncHelper.formatSyncTime(prefs.lastCloudSyncEpochMillis, isEs),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        // Drive Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppPrimaryButton(
                text = if (isEs) "☁️ Guardar en Drive" else "☁️ Save to Drive",
                onClick = {
                    val email = prefs.googleAccountEmail ?: "usuario.japon"
                    val filename = GoogleDriveSyncHelper.getCloudBackupFileName(email)
                    createBackupLauncher.launch(filename)
                },
                modifier = Modifier.weight(1f)
            )

            AppSecondaryButton(
                text = if (isEs) "🔄 Sincronizar de Drive" else "🔄 Sync from Drive",
                onClick = {
                    openBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Fast Sync button for quick 1-tap local cloud backup update
        OutlinedButton(
            onClick = {
                val email = prefs.googleAccountEmail ?: "usuario.japon@gmail.com"
                val bundle = GoogleDriveSyncHelper.buildSyncBundle(
                    accountEmail = email,
                    deviceModel = android.os.Build.MODEL ?: "Android Device",
                    plans = allPlans,
                    guides = allGuides,
                    documents = allDocs,
                    cities = allCities,
                    prefs = prefs,
                    timestampMillis = System.currentTimeMillis()
                )
                GoogleDriveSyncHelper.saveToLocalCloudCache(context, bundle)
                prefsRepository.setGoogleAccountSync(
                    email = email,
                    lastSyncedMillis = bundle.timestampMillis
                )
                syncStatusMessage = if (isEs) {
                    "✅ Copia de sincronización actualizada para $email (${allPlans.size} planes, ${allGuides.size} guías, ${allDocs.size} documentos)"
                } else {
                    "✅ Sync copy updated for $email (${allPlans.size} plans, ${allGuides.size} guides, ${allDocs.size} documents)"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isEs) "⚡ Actualizar Respaldo Local de Sincronización" else "⚡ Update Local Sync Backup",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (syncStatusMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = syncStatusMessage!!,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
