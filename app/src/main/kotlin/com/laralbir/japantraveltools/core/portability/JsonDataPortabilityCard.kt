package com.laralbir.japantraveltools.core.portability

import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.core.cloud.GoogleDriveSyncHelper
import com.laralbir.japantraveltools.core.preferences.DefaultUserPreferencesRepository
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.feature.documents.DocumentsAndGuidesJsonCodec
import com.laralbir.japantraveltools.feature.documents.SharedDocumentsAndGuidesStore
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
import com.laralbir.japantraveltools.ui.main.AppConfirmationDialog
import com.laralbir.japantraveltools.ui.main.AppPrimaryButton
import com.laralbir.japantraveltools.ui.main.AppSecondaryButton
import com.laralbir.japantraveltools.ui.main.ConfirmationDialogState
import com.laralbir.japantraveltools.ui.main.KotlinConfCard

@Composable
fun JsonDataPortabilityCard(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    var detectedContentForImport by remember { mutableStateOf<DetectedJsonContent?>(null) }

    val currentPlan by SharedTravelPlanStore.planState.collectAsState()
    val allPlans by SharedTravelPlanStore.allPlansState.collectAsState()
    val allGuides by SharedDocumentsAndGuidesStore.guidesState.collectAsState()
    val allDocs by SharedDocumentsAndGuidesStore.documentsState.collectAsState()
    val allCities by SharedWeatherForecastStore.citiesState.collectAsState()

    // 1. Export Plan Launcher
    val exportPlanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = SharedTravelPlanStore.exportCurrentJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                statusMessage = if (isEs) {
                    "✅ Plan «${currentPlan.tripTitle}» exportado correctamente."
                } else {
                    "✅ Plan «${currentPlan.tripTitle}» successfully exported."
                }
            }.onFailure {
                statusMessage = if (isEs) "❌ Error al exportar plan: ${it.localizedMessage}" else "❌ Export error: ${it.localizedMessage}"
            }
        }
    }

    // 2. Export Guides Launcher
    val exportGuidesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = DocumentsAndGuidesJsonCodec.exportGuidesJson(allGuides)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                statusMessage = if (isEs) {
                    "✅ ${allGuides.size} guías de viaje exportadas correctamente."
                } else {
                    "✅ ${allGuides.size} travel guides successfully exported."
                }
            }.onFailure {
                statusMessage = if (isEs) "❌ Error al exportar guías: ${it.localizedMessage}" else "❌ Export error: ${it.localizedMessage}"
            }
        }
    }

    // 3. Export Documents / Tickets Launcher
    val exportDocsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = DocumentsAndGuidesJsonCodec.exportDocumentsJson(allDocs)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                statusMessage = if (isEs) {
                    "✅ ${allDocs.size} billetes y reservas exportados correctamente."
                } else {
                    "✅ ${allDocs.size} tickets and reservations successfully exported."
                }
            }.onFailure {
                statusMessage = if (isEs) "❌ Error al exportar documentos: ${it.localizedMessage}" else "❌ Export error: ${it.localizedMessage}"
            }
        }
    }

    // 4. Unified Smart File Picker (Local Storage / Google Drive via SAF)
    val openAnyJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val detected = JsonContentDetector.inspect(json)
                if (detected.type == JsonContentType.UNKNOWN) {
                    statusMessage = if (isEs) {
                        "⚠️ El archivo seleccionado no contiene un formato JSON válido para la app."
                    } else {
                        "⚠️ Selected file does not contain a recognized JSON format for this app."
                    }
                } else {
                    detectedContentForImport = detected
                }
            }.onFailure {
                statusMessage = if (isEs) "❌ Error al leer el archivo: ${it.localizedMessage}" else "❌ Error reading file: ${it.localizedMessage}"
            }
        }
    }

    // Modal Confirmation Dialog for Standard Actions
    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    // Smart Import Dialog for Detected JSON Content
    detectedContentForImport?.let { detected ->
        AlertDialog(
            onDismissRequest = { detectedContentForImport = null },
            title = {
                Text(
                    text = if (isEs) "📥 Importar ${detected.type.labelEs}" else "📥 Import ${detected.type.labelEn}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEs) detected.summaryEs else detected.summaryEn,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isEs) {
                            "Puedes reemplazar los datos actuales o fusionarlos con los existentes."
                        } else {
                            "You can replace existing data or merge with current records."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (detected.type != JsonContentType.FULL_BACKUP) {
                        TextButton(
                            onClick = {
                                executeImport(
                                    detected = detected,
                                    merge = true,
                                    isEs = isEs,
                                    onStatus = { statusMessage = it },
                                    context = context,
                                    prefs = prefs,
                                    prefsRepository = prefsRepository
                                )
                                detectedContentForImport = null
                            }
                        ) {
                            Text(if (isEs) "➕ Fusionar" else "➕ Merge")
                        }
                    }
                    TextButton(
                        onClick = {
                            executeImport(
                                detected = detected,
                                merge = false,
                                isEs = isEs,
                                onStatus = { statusMessage = it },
                                context = context,
                                prefs = prefs,
                                prefsRepository = prefsRepository
                            )
                            detectedContentForImport = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isEs) "🔄 Reemplazar" else "🔄 Replace")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { detectedContentForImport = null }) {
                    Text(if (isEs) "Cancelar" else "Cancel")
                }
            }
        )
    }

    KotlinConfCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "📦 Importación y Exportación JSON" else "📦 JSON Data Portability",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isEs) "Local / Drive" else "Local / Drive",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isEs) {
                "Lee y escribe archivos JSON directamente desde el almacenamiento local o Google Drive usando el selector de archivos del sistema."
            } else {
                "Read and write JSON files directly from local storage or Google Drive using Android's system file picker."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        statusMessage?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 1. SMART UNIFIED IMPORTER
        AppPrimaryButton(
            text = if (isEs) "📂 Importar archivo JSON (Local o Drive)" else "📂 Import JSON File (Local or Drive)",
            onClick = {
                openAnyJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // SECTION: TRAVEL PLANS
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isEs) "🗺️ Plan de Viaje e Itinerario" else "🗺️ Travel Plan & Itinerary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentPlan.tripTitle} · ${currentPlan.itineraryStops.size} ${if (isEs) "paradas" else "stops"} · ${currentPlan.transportSegments.size} ${if (isEs) "transportes" else "segments"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val suggested = currentPlan.tripTitle
                                .lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                                .ifBlank { "japan-travel-plan" } + ".json"
                            exportPlanLauncher.launch(suggested)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📤 Exportar" else "📤 Export", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            openAnyJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📥 Importar" else "📥 Import", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // SECTION: TRAVEL GUIDES
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isEs) "📖 Guías de Viaje" else "📖 Travel Guides",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allGuides.size} ${if (isEs) "guías guardadas" else "guides saved"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportGuidesLauncher.launch("japan-travel-guides.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📤 Exportar" else "📤 Export", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            openAnyJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📥 Importar" else "📥 Import", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // SECTION: TICKETS & RESERVATIONS (DOCUMENTS)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isEs) "🎫 Billetes y Reservas de Viaje" else "🎫 Tickets & Reservations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allDocs.size} ${if (isEs) "billetes y reservas guardados" else "documents saved"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportDocsLauncher.launch("japan-travel-documents.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📤 Exportar" else "📤 Export", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            openAnyJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "📥 Importar" else "📥 Import", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun executeImport(
    detected: DetectedJsonContent,
    merge: Boolean,
    isEs: Boolean,
    onStatus: (String) -> Unit,
    context: android.content.Context,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    runCatching {
        when (detected.type) {
            JsonContentType.TRAVEL_PLAN -> {
                SharedTravelPlanStore.importJson(detected.rawJson, mergeWithExisting = merge)
                    .onSuccess { plan ->
                        onStatus(
                            if (isEs) {
                                "✅ Plan «${plan.tripTitle}» importado (${plan.itineraryStops.size} paradas, ${plan.transportSegments.size} transportes)."
                            } else {
                                "✅ Plan «${plan.tripTitle}» imported (${plan.itineraryStops.size} stops, ${plan.transportSegments.size} segments)."
                            }
                        )
                    }
                    .onFailure {
                        onStatus(if (isEs) "❌ Error al importar plan: ${it.localizedMessage}" else "❌ Error importing plan: ${it.localizedMessage}")
                    }
            }
            JsonContentType.TRAVEL_GUIDES -> {
                val guides = DocumentsAndGuidesJsonCodec.importGuidesJson(detected.rawJson)
                SharedDocumentsAndGuidesStore.importGuides(guides, merge = merge)
                onStatus(
                    if (isEs) {
                        "✅ ${guides.size} guías de viaje importadas con éxito."
                    } else {
                        "✅ ${guides.size} travel guides successfully imported."
                    }
                )
            }
            JsonContentType.TRAVEL_DOCUMENTS -> {
                val docs = DocumentsAndGuidesJsonCodec.importDocumentsJson(detected.rawJson)
                SharedDocumentsAndGuidesStore.importDocuments(docs, merge = merge)
                onStatus(
                    if (isEs) {
                        "✅ ${docs.size} billetes y reservas importados con éxito."
                    } else {
                        "✅ ${docs.size} tickets and reservations successfully imported."
                    }
                )
            }
            JsonContentType.FULL_BACKUP -> {
                val bundle = GoogleDriveSyncHelper.importCloudBackupJson(detected.rawJson)
                if (bundle != null) {
                    val plans = if (merge) GoogleDriveSyncHelper.mergePlans(SharedTravelPlanStore.allPlansState.value, bundle.travelPlans) else bundle.travelPlans
                    SharedTravelPlanStore.replaceAllPlans(plans)

                    val guides = if (merge) GoogleDriveSyncHelper.mergeGuides(SharedDocumentsAndGuidesStore.guidesState.value, bundle.travelGuides) else bundle.travelGuides
                    SharedDocumentsAndGuidesStore.replaceAllGuides(guides)

                    val docs = if (merge) GoogleDriveSyncHelper.mergeDocuments(SharedDocumentsAndGuidesStore.documentsState.value, bundle.travelDocuments) else bundle.travelDocuments
                    SharedDocumentsAndGuidesStore.replaceAllDocuments(docs)

                    val cities = if (merge) GoogleDriveSyncHelper.mergeWeatherCities(SharedWeatherForecastStore.citiesState.value, bundle.weatherCities) else bundle.weatherCities
                    SharedWeatherForecastStore.replaceAllCities(cities)

                    GoogleDriveSyncHelper.saveToLocalCloudCache(context, bundle)
                    onStatus(
                        if (isEs) {
                            "✅ Copia de seguridad completa restaurada con éxito (${plans.size} planes, ${guides.size} guías, ${docs.size} billetes)."
                        } else {
                            "✅ Full backup package successfully restored (${plans.size} plans, ${guides.size} guides, ${docs.size} tickets)."
                        }
                    )
                } else {
                    onStatus(if (isEs) "❌ Error al parsear la copia de seguridad." else "❌ Backup package parse error.")
                }
            }
            JsonContentType.UNKNOWN -> {
                onStatus(if (isEs) "⚠️ Archivo no válido." else "⚠️ Invalid file.")
            }
        }
    }.onFailure {
        onStatus(if (isEs) "❌ Error durante la importación: ${it.localizedMessage}" else "❌ Import error: ${it.localizedMessage}")
    }
}
