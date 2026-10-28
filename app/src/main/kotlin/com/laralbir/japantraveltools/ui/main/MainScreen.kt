package com.laralbir.japantraveltools.ui.main

import android.app.Activity
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.laralbir.japantraveltools.feature.ai.CameraOcrLine
import com.laralbir.japantraveltools.feature.ai.CameraOcrResult
import com.laralbir.japantraveltools.feature.ai.OcrTextZone
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.material3.Slider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import com.laralbir.japantraveltools.feature.ai.CropBounds
import com.laralbir.japantraveltools.feature.ai.ImageCropPreset
import com.laralbir.japantraveltools.feature.ai.TranslatorTabMode
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.laralbir.japantraveltools.core.cloud.GoogleDriveSyncCard
import com.laralbir.japantraveltools.core.datetime.TravelDateTimeFormatter
import com.laralbir.japantraveltools.core.portability.JsonDataPortabilityCard
import com.laralbir.japantraveltools.core.i18n.AppLanguagePreference
import com.laralbir.japantraveltools.core.i18n.EffectiveLanguage
import com.laralbir.japantraveltools.core.i18n.LanguageResolver
import com.laralbir.japantraveltools.core.network.ActiveConnectionKind
import com.laralbir.japantraveltools.core.network.ConnectivityStatusHelper
import com.laralbir.japantraveltools.core.portability.AiPlanBuilderEngine
import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import com.laralbir.japantraveltools.core.portability.SharedTravelPlanStore
import com.laralbir.japantraveltools.core.preferences.AppThemeMode
import com.laralbir.japantraveltools.core.preferences.DefaultUserPreferencesRepository
import com.laralbir.japantraveltools.core.preferences.SharedPrefsKeyValueStore
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.data.DefaultDataRepository
import com.laralbir.japantraveltools.feature.ai.AssistantSource
import com.laralbir.japantraveltools.feature.ai.GeminiAssistantEngine
import com.laralbir.japantraveltools.feature.ai.OfflineTranslatorEngine
import com.laralbir.japantraveltools.feature.ai.OnlineTranslatorClient
import com.laralbir.japantraveltools.feature.ai.SimultaneousTranslationItem
import com.laralbir.japantraveltools.feature.ai.GeminiApiKeySetupCard
import com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper
import com.laralbir.japantraveltools.feature.ai.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.laralbir.japantraveltools.feature.clock.CalculateClockComparisonUseCase
import com.laralbir.japantraveltools.feature.clock.OfflineTimeZoneResolver
import com.laralbir.japantraveltools.feature.currency.ConvertCurrencyUseCase
import com.laralbir.japantraveltools.feature.currency.FrankfurterJsonParser
import com.laralbir.japantraveltools.feature.currency.SeedExchangeRates
import com.laralbir.japantraveltools.feature.currency.SharedCurrencyRateStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laralbir.japantraveltools.feature.documents.SharedDocumentsAndGuidesStore
import com.laralbir.japantraveltools.feature.taxfree.SharedShoppingReceiptsStore
import com.laralbir.japantraveltools.feature.documents.VaultSecurityPolicy
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.DocumentsFilterUseCase
import com.laralbir.japantraveltools.feature.documents.FullScreenZoomableImageViewerDialog
import com.laralbir.japantraveltools.feature.documents.CustomIdDocumentItem
import com.laralbir.japantraveltools.feature.documents.GuideSortOrder
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.settings.OpenSourceLibrariesCatalog
import com.laralbir.japantraveltools.feature.transport.SharedTransportAlertStore
import com.laralbir.japantraveltools.feature.transport.TransportAlert
import com.laralbir.japantraveltools.feature.transport.TransportAlertEvaluator
import com.laralbir.japantraveltools.feature.transport.TransportAlertNotificationHelper
import com.laralbir.japantraveltools.feature.transport.TransportAlertSeverity
import com.laralbir.japantraveltools.feature.transport.TransportAlertType
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.weather.JapanWeatherCity
import com.laralbir.japantraveltools.feature.weather.OpenMeteoWeatherUseCase
import com.laralbir.japantraveltools.feature.weather.PlanningSuitability
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
import com.laralbir.japantraveltools.feature.weather.WeatherCityCatalog
import com.laralbir.japantraveltools.feature.weather.WeatherCityItem
import com.laralbir.japantraveltools.feature.weather.CityGeocodingHelper
import com.laralbir.japantraveltools.feature.weather.GeocodedCity
import com.laralbir.japantraveltools.ui.theme.JapanTravelToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class DrawerSection(val titleEs: String, val titleEn: String) {
    QUICK_TOOLS("HERRAMIENTAS RÁPIDAS", "QUICK TOOLS"),
    PLANNING("PLANIFICACIÓN Y RUTAS", "PLANNING & ROUTES"),
    VOICE_AI("VOZ E INTELIGENCIA ARTIFICIAL", "VOICE & AI"),
    DOCS_GUIDES("DOCUMENTOS Y GUÍAS", "DOCUMENTS & GUIDES"),
    SYSTEM("SISTEMA", "SYSTEM")
}

enum class JapanDrawerDestination(
    val section: DrawerSection,
    val titleEs: String,
    val titleEn: String,
    val iconSymbol: String
) {
    CURRENCY(DrawerSection.QUICK_TOOLS, "Conversor de Divisas", "Currency Converter", "💴"),
    CLOCKS(DrawerSection.QUICK_TOOLS, "Relojes y Husos", "Clocks & Timezones", "🕒"),
    WEATHER(DrawerSection.QUICK_TOOLS, "Tiempo (Open-Meteo)", "Weather (Open-Meteo)", "🌦️"),
    TAX_FREE_RECEIPTS(DrawerSection.QUICK_TOOLS, "Tickets y Tax-Free", "Receipts & Tax-Free", "🧾"),

    PLANNER(DrawerSection.PLANNING, "Planificador de Viaje", "Trip Planner", "🗓️"),
    TRANSPORT(DrawerSection.PLANNING, "Transporte y Trenes", "Trains & Flights", "🚄"),

    VOICE_TRANSLATOR(DrawerSection.VOICE_AI, "Traductor", "Translator", "🗣️"),
    AI_ASSISTANT(DrawerSection.VOICE_AI, "Asistente IA de Viaje", "AI Travel Assistant", "✨"),

    PASSPORT_CUSTOMS(DrawerSection.DOCS_GUIDES, "Pasaporte y Aduanas", "Passport & Customs", "🛂"),
    BOOKINGS_VAULT(DrawerSection.DOCS_GUIDES, "Reservas y Billetes", "Bookings & Tickets", "📄"),
    TRAVEL_GUIDES(DrawerSection.DOCS_GUIDES, "Guías de Japón", "Japan Guides", "📖"),

    SETTINGS(DrawerSection.SYSTEM, "Ajustes y Sincronización", "Settings & Sync", "⚙️"),
    ABOUT(DrawerSection.SYSTEM, "Acerca de", "About App", "ℹ️")
}

// ============================================================================
// STANDARDIZED DESIGN SYSTEM COMPONENTS (FIXED HEIGHT, SHAPE, COLORS, NO WRAP)
// ============================================================================

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppButtonRow(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppPrimaryButton(
            text = primaryText,
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f)
        )
        AppSecondaryButton(
            text = secondaryText,
            onClick = onSecondaryClick,
            enabled = secondaryEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    horizontalPadding: Dp = 10.dp
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KotlinConfCard(
    modifier: Modifier = Modifier,
    accentBorderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun InfoTagPill(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ============================================================================
// MAIN SCREEN ENTRY POINT WITH SIDE NAVIGATION DRAWER
// ============================================================================

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) }
) {
    val context = LocalContext.current
    val prefsRepository = remember {
        DefaultUserPreferencesRepository(SharedPrefsKeyValueStore(context))
    }
    val prefs by prefsRepository.preferencesFlow.collectAsState(initial = prefsRepository.current)
    val isDark = when (prefs.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    JapanTravelToolsTheme(darkTheme = isDark, dynamicColor = false) {
        JapanTravelToolsAppContent(
            prefs = prefs,
            prefsRepository = prefsRepository,
            modifier = modifier
        )
    }
}

/**
 * Legacy overload kept for existing AndroidTest compatibility.
 */
@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        data.forEach { Greeting(it) }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier.padding(8.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JapanTravelToolsAppContent(
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SharedTravelPlanStore.init(context)
        SharedDocumentsAndGuidesStore.init(context)
        SharedWeatherForecastStore.init(context)
        SharedShoppingReceiptsStore.init(context)
    }
    val configuration = LocalConfiguration.current
    val isTabletExpanded = configuration.screenWidthDp >= 720

    val effectiveLang = LanguageResolver.resolveEffectiveLanguage(prefs.language)
    val isEs = effectiveLang == EffectiveLanguage.SPANISH

    val initialDestination = remember(prefs.lastSelectedDestination) {
        runCatching { JapanDrawerDestination.valueOf(prefs.lastSelectedDestination) }
            .getOrDefault(JapanDrawerDestination.CURRENCY)
    }
    var selectedDestination by rememberSaveable { mutableStateOf(initialDestination) }

    fun navigateTo(dest: JapanDrawerDestination) {
        selectedDestination = dest
        prefsRepository.setLastSelectedDestination(dest.name)
    }

    var showJsonPortabilitySheet by rememberSaveable { mutableStateOf(false) }
    var showConnectivitySheet by rememberSaveable { mutableStateOf(false) }

    var currentInstant by remember { mutableStateOf(Instant.now()) }
    var connectivitySnapshot by remember {
        mutableStateOf(ConnectivityStatusHelper.inspectCurrentConnection(context, prefs.forceOfflineMode))
    }
    LaunchedEffect(prefs.forceOfflineMode) {
        while (true) {
            currentInstant = Instant.now()
            connectivitySnapshot = ConnectivityStatusHelper.inspectCurrentConnection(context, prefs.forceOfflineMode)
            delay(15_000L)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (showJsonPortabilitySheet) {
        JsonPortabilityModalSheet(
            isEs = isEs,
            onDismiss = { showJsonPortabilitySheet = false }
        )
    }

    if (showConnectivitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectivitySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QuickConnectivitySwitcherContent(
                isEs = isEs,
                kind = connectivitySnapshot.kind,
                forceOffline = prefs.forceOfflineMode,
                onToggleForceOffline = { prefsRepository.setForceOfflineMode(it) },
                onOpenInternetPanel = {
                    ConnectivityStatusHelper.openAndroidInternetPanel(context)
                    showConnectivitySheet = false
                },
                onOpenSimSettings = {
                    ConnectivityStatusHelper.openSimAndEsimSettings(context)
                    showConnectivitySheet = false
                }
            )
        }
    }

    if (isTabletExpanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    SideDrawerMenuContent(
                        isEs = isEs,
                        selectedDestination = selectedDestination,
                        onSelectDestination = { dest -> navigateTo(dest) }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopHeaderBar(
                        isEs = isEs,
                        showMenuButton = false,
                        activeTitle = if (isEs) selectedDestination.titleEs else selectedDestination.titleEn,
                        activeIcon = selectedDestination.iconSymbol,
                        connectionKind = connectivitySnapshot.kind,
                        onOpenDrawer = {},
                        onOpenConnectionSettings = { showConnectivitySheet = true }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                ) {
                    ActiveToolContent(
                        destination = selectedDestination,
                        isEs = isEs,
                        currentInstant = currentInstant,
                        prefs = prefs,
                        prefsRepository = prefsRepository,
                        onOpenPlanSheet = { showJsonPortabilitySheet = true },
                        onNavigateToDestination = { navigateTo(it) }
                    )
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(305.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    SideDrawerMenuContent(
                        isEs = isEs,
                        selectedDestination = selectedDestination,
                        onSelectDestination = { dest ->
                            navigateTo(dest)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopHeaderBar(
                        isEs = isEs,
                        showMenuButton = true,
                        activeTitle = if (isEs) selectedDestination.titleEs else selectedDestination.titleEn,
                        activeIcon = selectedDestination.iconSymbol,
                        connectionKind = connectivitySnapshot.kind,
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onOpenConnectionSettings = { showConnectivitySheet = true }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    ActiveToolContent(
                        destination = selectedDestination,
                        isEs = isEs,
                        currentInstant = currentInstant,
                        prefs = prefs,
                        prefsRepository = prefsRepository,
                        onOpenPlanSheet = { showJsonPortabilitySheet = true },
                        onNavigateToDestination = { navigateTo(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SideDrawerMenuContent(
    isEs: Boolean,
    selectedDestination: JapanDrawerDestination,
    onSelectDestination: (JapanDrawerDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = "⛩️", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Japan Travel Tools",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEs) "Menú de Herramientas Offline" else "Offline Travel Toolkit",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(10.dp))

        DrawerSection.entries.forEach { section ->
            Text(
                text = if (isEs) section.titleEs else section.titleEn,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            JapanDrawerDestination.entries
                .filter { it.section == section }
                .forEach { item ->
                    val isSelected = selectedDestination == item
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = if (isEs) item.titleEs else item.titleEn,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Text(text = item.iconSymbol, fontSize = 19.sp)
                        },
                        selected = isSelected,
                        onClick = { onSelectDestination(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(vertical = 2.dp)
                    )
                }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopHeaderBar(
    isEs: Boolean,
    showMenuButton: Boolean,
    activeTitle: String,
    activeIcon: String,
    connectionKind: ActiveConnectionKind,
    onOpenDrawer: () -> Unit,
    onOpenConnectionSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showMenuButton) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .height(42.dp)
                                .clickable(onClick = onOpenDrawer)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "☰",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isEs) "Menú" else "Menu",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    Text(
                        text = "$activeIcon $activeTitle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))
                ConnectionStatusPill(
                    isEs = isEs,
                    kind = connectionKind,
                    onClick = onOpenConnectionSettings
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ConnectionStatusPill(
    isEs: Boolean,
    kind: ActiveConnectionKind,
    onClick: () -> Unit
) {
    val label = "📡 ${if (isEs) kind.badgeEs.substringBefore(" ") else kind.badgeEn.substringBefore(" ")}"
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .height(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActiveToolContent(
    destination: JapanDrawerDestination,
    isEs: Boolean,
    currentInstant: Instant,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    onOpenPlanSheet: () -> Unit,
    onNavigateToDestination: (JapanDrawerDestination) -> Unit
) {
    when (destination) {
        JapanDrawerDestination.CURRENCY -> CurrencyToolScreen(isEs = isEs, prefs = prefs, prefsRepository = prefsRepository)
        JapanDrawerDestination.CLOCKS -> ClocksToolScreen(isEs = isEs, currentInstant = currentInstant, prefs = prefs, prefsRepository = prefsRepository)
        JapanDrawerDestination.WEATHER -> WeatherToolScreen(isEs = isEs)
        JapanDrawerDestination.TAX_FREE_RECEIPTS -> ShoppingReceiptsScreen(isEs = isEs, prefs = prefs)
        JapanDrawerDestination.PLANNER -> TripPlannerTabScreen(isEs = isEs, onOpenJsonPortability = onOpenPlanSheet)
        JapanDrawerDestination.TRANSPORT -> TransportTabScreen(isEs = isEs, onOpenJsonPortability = onOpenPlanSheet)
        JapanDrawerDestination.VOICE_TRANSLATOR -> VoiceTranslatorScreen(isEs = isEs, prefs = prefs)
        JapanDrawerDestination.AI_ASSISTANT -> AiAssistantScreen(
            isEs = isEs,
            prefs = prefs,
            prefsRepository = prefsRepository,
            onNavigateToPlanner = { onNavigateToDestination(JapanDrawerDestination.PLANNER) },
            onNavigateToSettings = { onNavigateToDestination(JapanDrawerDestination.SETTINGS) }
        )
        JapanDrawerDestination.PASSPORT_CUSTOMS -> PassportAndCustomsScreen(isEs = isEs, prefs = prefs, prefsRepository = prefsRepository)
        JapanDrawerDestination.BOOKINGS_VAULT -> BookingsVaultScreen(isEs = isEs)
        JapanDrawerDestination.TRAVEL_GUIDES -> TravelGuidesScreen(isEs = isEs)
        JapanDrawerDestination.SETTINGS -> SettingsPreferencesScreen(isEs = isEs, prefs = prefs, prefsRepository = prefsRepository)
        JapanDrawerDestination.ABOUT -> AboutAppScreen(isEs = isEs)
    }
}

// ============================================================================
// 1. CURRENCY CONVERTER SCREEN
// ============================================================================
@Composable
private fun CurrencyToolScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val rateSnapshot by SharedCurrencyRateStore.rateState.collectAsState()
    var isSyncingRates by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

    val displayStatusMessage = syncStatusMessage ?: run {
        val jpyRate = rateSnapshot.rates["JPY"] ?: 164.80
        if (rateSnapshot.isSeedData) {
            if (isEs) "Fuente: Datos Base Offline (1 EUR = $jpyRate JPY)"
            else "Source: Baseline Offline Rates (1 EUR = $jpyRate JPY)"
        } else {
            if (isEs) "1 EUR = $jpyRate JPY (BCE · ${rateSnapshot.rateDate})"
            else "1 EUR = $jpyRate JPY (ECB · ${rateSnapshot.rateDate})"
        }
    }

    var inputAmountText by rememberSaveable { mutableStateOf("1000") }
    var isJpyToBaseDirection by rememberSaveable { mutableStateOf(true) }

    val numericInput = inputAmountText.toDoubleOrNull() ?: 0.0
    val fromCurrency = if (isJpyToBaseDirection) prefs.targetCurrency else prefs.baseCurrency
    val toCurrency = if (isJpyToBaseDirection) prefs.baseCurrency else prefs.targetCurrency

    val convertedValue = remember(numericInput, fromCurrency, toCurrency, rateSnapshot) {
        ConvertCurrencyUseCase.convert(
            amount = numericInput,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            snapshot = rateSnapshot
        )
    }

    val availableCurrencies = listOf("EUR", "USD", "GBP", "MXN", "ARS", "COP", "CLP", "AUD")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Conversor de Yenes (JPY)" else "Yen Currency Converter (JPY)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "$inputAmountText $fromCurrency =",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${ConvertCurrencyUseCase.formatAmount(convertedValue, toCurrency)} $toCurrency",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputAmountText,
                    onValueChange = { newValue ->
                        inputAmountText = NumericInputHelper.filterDecimal(newValue)
                    },
                    label = {
                        Text(
                            text = if (isEs) "Importe en $fromCurrency" else "Amount in $fromCurrency",
                            fontSize = 15.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "Billetes y monedas frecuentes en Japón:" else "Common Yen amounts in Japan:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                val presets = listOf(100L, 500L, 1000L, 3000L, 5000L, 10000L)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.chunked(3).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPresets.forEach { yenBill ->
                                AppOptionButton(
                                    text = "¥%,d".format(yenBill),
                                    selected = isJpyToBaseDirection && inputAmountText == yenBill.toString(),
                                    onClick = {
                                        isJpyToBaseDirection = true
                                        inputAmountText = yenBill.toString()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "Moneda de referencia:" else "Home currency:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableCurrencies.chunked(4).forEach { rowCodes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCodes.forEach { code ->
                                AppOptionButton(
                                    text = code,
                                    selected = prefs.baseCurrency == code,
                                    onClick = { prefsRepository.setCurrencyPair(base = code, target = "JPY") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AppButtonRow(
                    primaryText = if (isSyncingRates) {
                        if (isEs) "Sincronizando..." else "Syncing..."
                    } else {
                        if (isEs) "🔄 Actualizar BCE" else "🔄 Update ECB"
                    },
                    onPrimaryClick = {
                        isSyncingRates = true
                        coroutineScope.launch {
                            SharedCurrencyRateStore.syncRatesOnline()
                                .onSuccess { fresh ->
                                    val jpyRate = fresh.rates["JPY"] ?: 164.80
                                    syncStatusMessage = "1 EUR = $jpyRate JPY (${fresh.rateDate})"
                                }
                                .onFailure {
                                    syncStatusMessage = if (isEs) "Modo Offline (${rateSnapshot.rateDate})" else "Offline (${rateSnapshot.rateDate})"
                                }
                            isSyncingRates = false
                        }
                    },
                    secondaryText = if (isEs) "⇄ Invertir Divisa" else "⇄ Swap Direction",
                    onSecondaryClick = { isJpyToBaseDirection = !isJpyToBaseDirection },
                    primaryEnabled = !isSyncingRates
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = displayStatusMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// 2. CLOCKS & TIMEZONES SCREEN
// ============================================================================
@Composable
private fun ClocksToolScreen(
    isEs: Boolean,
    currentInstant: Instant,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    val originZone = remember(prefs.originZoneId) {
        runCatching { ZoneId.of(prefs.originZoneId) }.getOrDefault(ZoneId.of("Europe/Madrid"))
    }
    val clockComparison = remember(currentInstant, originZone) {
        CalculateClockComparisonUseCase.calculate(
            instant = currentInstant,
            localZone = ZoneId.systemDefault(),
            originZone = originZone
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Hora en Japón y en Casa" else "Japan & Home Time",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🇯🇵 Tokio (JST)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = clockComparison.japanDateFormatted,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = clockComparison.japanTimeFormatted,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sign = if (clockComparison.hoursDifferenceTokyoVsOrigin >= 0) "+" else ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏠 ${clockComparison.originZoneId.substringAfterLast('/')}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${clockComparison.originDateFormatted} (${sign}${clockComparison.hoursDifferenceTokyoVsOrigin}h)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = clockComparison.originTimeFormatted,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isEs) "Selecciona tu país de origen:" else "Select your home country:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OfflineTimeZoneResolver.supportedOrigins.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { origin ->
                                AppOptionButton(
                                    text = "${origin.flagEmoji} ${if (isEs) origin.nameEs else origin.nameEn}",
                                    selected = prefs.originZoneId == origin.zoneId,
                                    onClick = { prefsRepository.setOriginZoneId(origin.zoneId) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. WEATHER TOOL SCREEN (OPEN-METEO WITH DYNAMIC CITIES CRUD & PRESETS)
// ============================================================================
@Composable
private fun WeatherToolScreen(isEs: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val allCities by SharedWeatherForecastStore.citiesState.collectAsState()
    val allForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()

    var selectedCityId by rememberSaveable { mutableStateOf("TOKYO") }
    var editingCity by remember { mutableStateOf<WeatherCityItem?>(null) }
    var isCreatingCity by remember { mutableStateOf(false) }
    var isSyncingWeather by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    LaunchedEffect(portablePlan.itineraryStops, portablePlan.transportSegments) {
        SharedWeatherForecastStore.syncCitiesFromPlan(
            stops = portablePlan.itineraryStops,
            segments = portablePlan.transportSegments,
            forceResetDeleted = false
        )
    }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    val selectedCity = remember(allCities, selectedCityId) {
        allCities.firstOrNull { it.id == selectedCityId } ?: allCities.firstOrNull() ?: WeatherCityCatalog.defaultCities().first()
    }

    val cityDays = remember(allForecasts, selectedCity) {
        allForecasts.filter { it.cityKey == selectedCity.id }.ifEmpty {
            OpenMeteoWeatherUseCase.generateSeedForCity(selectedCity.id)
        }
    }

    if (isCreatingCity || editingCity != null) {
        val initial = editingCity ?: WeatherCityItem(
            id = "CITY_${System.currentTimeMillis()}",
            nameEs = "",
            nameEn = "",
            latitude = 35.0,
            longitude = 135.0
        )
        CityWeatherEditorModalSheet(
            isEs = isEs,
            initialCity = initial,
            isNew = isCreatingCity,
            existingCount = allCities.size,
            onDismiss = {
                isCreatingCity = false
                editingCity = null
            },
            onSave = { saved ->
                SharedWeatherForecastStore.addCity(saved)
                selectedCityId = saved.id
                isCreatingCity = false
                editingCity = null
                statusMessage = if (isEs) "✅ Ciudad «${saved.nameEs}» guardada" else "✅ City «${saved.nameEn}» saved"
            },
            onDelete = { idToDelete ->
                SharedWeatherForecastStore.deleteCity(idToDelete)
                isCreatingCity = false
                editingCity = null
                selectedCityId = allCities.firstOrNull { it.id != idToDelete }?.id ?: "TOKYO"
                statusMessage = if (isEs) "🗑️ Ciudad eliminada" else "🗑️ City deleted"
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "Previsión Open-Meteo" else "Open-Meteo Forecast",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                SharedWeatherForecastStore.syncCitiesFromPlan(
                                    stops = portablePlan.itineraryStops,
                                    segments = portablePlan.transportSegments,
                                    forceResetDeleted = false
                                )
                                statusMessage = if (isEs) "🔄 Sincronizado con itinerario" else "🔄 Synced with trip plan"
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (isEs) "🔄 Plan" else "🔄 Plan", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                editingCity = null
                                isCreatingCity = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isEs) "➕ Ciudad" else "➕ City", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEs) {
                        "Previsión del tiempo obtenida automáticamente de los destinos de tus transportes e itinerarios. Puedes añadir, editar o eliminar ciudades con sus coordenadas."
                    } else {
                        "Weather forecast automatically obtained from your transport and itinerary destinations. Add, edit, or delete cities freely."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (statusMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "Ciudades Activas (${allCities.size}):" else "Active Cities (${allCities.size}):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCities.chunked(2).forEach { cityPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cityPair.forEach { city ->
                                AppOptionButton(
                                    text = if (isEs) city.nameEs else city.nameEn,
                                    selected = selectedCity.id == city.id,
                                    onClick = { selectedCityId = city.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (cityPair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Selected City Header Card with Edit & Delete actions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📍 ${if (isEs) selectedCity.nameEs else selectedCity.nameEn}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Coord: ${selectedCity.latitude}, ${selectedCity.longitude}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { editingCity = selectedCity }) {
                                Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 12.sp)
                            }
                            if (allCities.size > 1) {
                                OutlinedButton(onClick = {
                                    val cityName = if (isEs) selectedCity.nameEs else selectedCity.nameEn
                                    confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                        itemName = cityName,
                                        itemTypeEs = "ciudad",
                                        itemTypeEn = "city",
                                        isEs = isEs,
                                        onConfirm = {
                                            val toDelete = selectedCity.id
                                            val nextCity = allCities.firstOrNull { it.id != toDelete }?.id ?: "TOKYO"
                                            SharedWeatherForecastStore.deleteCity(toDelete)
                                            selectedCityId = nextCity
                                            statusMessage = if (isEs) "🗑️ Ciudad «$cityName» eliminada" else "🗑️ City «$cityName» deleted"
                                        }
                                    )
                                }) {
                                    Text("🗑️", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Forecast Days
                cityDays.take(5).forEach { day ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.condition.emoji} ${day.dateIso}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${day.tempMinC.toInt()}° / ${day.tempMaxC.toInt()}°C · ☔ ${day.precipitationProbabilityMax}%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (day.suitability == PlanningSuitability.OUTDOOR_OPTIMAL) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = if (isEs) day.suitability.badgeEs else day.suitability.badgeEn,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.suitability == PlanningSuitability.OUTDOOR_OPTIMAL) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isEs) day.userPlanningAdviceEs else day.userPlanningAdviceEn,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AppButtonRow(
                    primaryText = if (isSyncingWeather) {
                        if (isEs) "Actualizando..." else "Updating..."
                    } else {
                        if (isEs) "🔄 Actualizar Tiempo" else "🔄 Refresh Weather"
                    },
                    onPrimaryClick = {
                        isSyncingWeather = true
                        statusMessage = null
                        coroutineScope.launch {
                            OpenMeteoWeatherUseCase.fetchCoordinatesForecastOnline(
                                selectedCity.id,
                                selectedCity.latitude,
                                selectedCity.longitude
                            ).onSuccess { liveDays ->
                                SharedWeatherForecastStore.updateCityForecasts(selectedCity.id, liveDays)
                                statusMessage = if (isEs) "🌤️ Previsión en vivo actualizada para ${selectedCity.nameEs}" else "🌤️ Live forecast updated for ${selectedCity.nameEn}"
                            }.onFailure {
                                statusMessage = if (isEs) "ℹ️ Sin conexión: usando datos cacheados" else "ℹ️ Offline: using cached forecast"
                            }
                            isSyncingWeather = false
                        }
                    },
                    secondaryText = if (isEs) "✨ Aplicar a mi Ruta" else "✨ Apply to Route",
                    onSecondaryClick = {
                        val optimized = AiPlanBuilderEngine.optimizePlanWithWeather(
                            currentPlan = portablePlan,
                            forecasts = allForecasts
                        )
                        SharedTravelPlanStore.updatePlan(optimized)
                        statusMessage = optimized.aiOptimizationSummary
                    },
                    primaryEnabled = !isSyncingWeather
                )

                if (statusMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = statusMessage!!,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: CITY WEATHER EDITOR & DESTINATIONS PRESETS
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityWeatherEditorModalSheet(
    isEs: Boolean,
    initialCity: WeatherCityItem,
    isNew: Boolean,
    existingCount: Int,
    onDismiss: () -> Unit,
    onSave: (WeatherCityItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var nameEsText by rememberSaveable { mutableStateOf(initialCity.nameEs) }
    var nameEnText by rememberSaveable { mutableStateOf(initialCity.nameEn) }
    var latText by rememberSaveable { mutableStateOf(initialCity.latitude.toString()) }
    var lonText by rememberSaveable { mutableStateOf(initialCity.longitude.toString()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var geocodeNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val presets = remember { WeatherCityCatalog.destinationPresets() }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    var predictions by remember { mutableStateOf<List<GeocodedCity>>(emptyList()) }

    fun applyGeocodedCity(city: GeocodedCity) {
        nameEsText = city.nameEs
        nameEnText = city.nameEn
        latText = city.latitude.toString()
        lonText = city.longitude.toString()
        errorMessage = null
        geocodeNotice = if (isEs) {
            "📍 Coordenadas de Google Maps obtenidas (${city.source}): ${city.latitude}, ${city.longitude}"
        } else {
            "📍 Coordinates obtained from Google Maps (${city.source}): ${city.latitude}, ${city.longitude}"
        }
        predictions = emptyList()
    }

    fun updatePredictions(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            predictions = emptyList()
            return
        }
        val localMatches = CityGeocodingHelper.searchPredictions(trimmed, maxResults = 5)
        predictions = localMatches
        if (localMatches.isEmpty() && trimmed.length >= 3) {
            coroutineScope.launch {
                val onlineMatches = CityGeocodingHelper.searchOnlinePredictions(trimmed, maxResults = 4)
                if (onlineMatches.isNotEmpty()) {
                    predictions = onlineMatches
                }
            }
        }
    }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Añadir Ciudad a Previsión" else "➕ Add City Forecast"
                } else {
                    if (isEs) "✏️ Editar Ciudad" else "✏️ Edit City"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isEs) "🎌 Selecciona un destino popular o escribe sus coordenadas:" else "🎌 Pick a popular destination or enter coordinates:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Presets chips in rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { preset ->
                            OutlinedButton(
                                onClick = {
                                    nameEsText = preset.nameEs
                                    nameEnText = preset.nameEn
                                    latText = preset.latitude.toString()
                                    lonText = preset.longitude.toString()
                                    errorMessage = null
                                    geocodeNotice = if (isEs) {
                                        "📍 Coordenadas de destino popular aplicadas: ${preset.latitude}, ${preset.longitude}"
                                    } else {
                                        "📍 Destination coordinates applied: ${preset.latitude}, ${preset.longitude}"
                                    }
                                    predictions = emptyList()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(preset.nameEs, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = nameEsText,
                onValueChange = {
                    nameEsText = it
                    errorMessage = null
                    geocodeNotice = null
                    updatePredictions(it)
                },
                label = { Text(if (isEs) "Nombre de la ciudad (Español)" else "City name (Spanish)", fontSize = 15.sp) },
                placeholder = { Text(if (isEs) "Ej. Tokio, Kioto, Nara, Kobe, Madrid..." else "E.g. Tokyo, Kyoto, Nara, Kobe...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Predictive Suggestion Cards
            if (predictions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEs) "🔍 Destinos coincidentes (toca para autocompletar):" else "🔍 Matching destinations (tap to auto-fill):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        predictions.forEach { pred ->
                            Surface(
                                onClick = { applyGeocodedCity(pred) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📍", fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pred.nameEs} / ${pred.nameEn}" + if (pred.country.isNotBlank()) " · ${pred.country}" else "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Lat: ${pred.latitude} · Lon: ${pred.longitude} (${pred.source})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (isEs) "Elegir" else "Pick",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = nameEnText,
                onValueChange = { nameEnText = it; errorMessage = null },
                label = { Text(if (isEs) "Nombre de la ciudad (Inglés)" else "City name (English)", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Google Maps Geocoding Button
            OutlinedButton(
                onClick = {
                    if (nameEsText.isBlank() && nameEnText.isBlank()) {
                        errorMessage = if (isEs) "Escribe el nombre de la ciudad primero" else "Enter city name first"
                        return@OutlinedButton
                    }
                    val query = nameEsText.ifBlank { nameEnText }
                    isGeocoding = true
                    errorMessage = null
                    geocodeNotice = null
                    predictions = emptyList()
                    coroutineScope.launch {
                        val resolved = CityGeocodingHelper.geocodeCity(context, query)
                        isGeocoding = false
                        if (resolved != null) {
                            applyGeocodedCity(resolved)
                        } else {
                            errorMessage = if (isEs) {
                                "⚠️ No se encontraron coordenadas automáticas para '$query'. Introduce latitud y longitud manualmente."
                            } else {
                                "⚠️ Coordinates not found automatically for '$query'. Please enter manually."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeocoding
            ) {
                if (isGeocoding) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEs) "Buscando en Google Maps..." else "Searching Google Maps...", fontSize = 13.sp)
                } else {
                    Text(
                        if (isEs) "📍 Obtener coordenadas de Google Maps" else "📍 Get Google Maps Coordinates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (geocodeNotice != null) {
                Text(
                    text = geocodeNotice!!,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = NumericInputHelper.filterDecimal(it); errorMessage = null },
                    label = { Text("Latitud", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = NumericInputHelper.filterDecimal(it); errorMessage = null },
                    label = { Text("Longitud", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Ciudad" else "💾 Save City",
                onPrimaryClick = {
                    val lat = latText.toDoubleOrNull()
                    val lon = lonText.toDoubleOrNull()
                    if (nameEsText.isBlank()) {
                        errorMessage = if (isEs) "Introduce el nombre de la ciudad" else "Enter city name"
                        return@AppButtonRow
                    }
                    if (lat == null || lat < -90.0 || lat > 90.0) {
                        errorMessage = if (isEs) "Latitud inválida (-90.0 a 90.0)" else "Invalid latitude (-90 to 90)"
                        return@AppButtonRow
                    }
                    if (lon == null || lon < -180.0 || lon > 180.0) {
                        errorMessage = if (isEs) "Longitud inválida (-180.0 a 180.0)" else "Invalid longitude (-180 to 180)"
                        return@AppButtonRow
                    }

                    val cityId = if (isNew) {
                        nameEnText.trim().uppercase().replace(" ", "_").replace(Regex("[^A-Z0-9_]"), "")
                            .ifBlank { "CITY_${System.currentTimeMillis()}" }
                    } else {
                        initialCity.id
                    }

                    val saved = initialCity.copy(
                        id = cityId,
                        nameEs = nameEsText.trim(),
                        nameEn = nameEnText.trim().ifBlank { nameEsText.trim() },
                        latitude = lat,
                        longitude = lon
                    )
                    if (isNew) {
                        onSave(saved)
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = saved.nameEs,
                            isEs = isEs,
                            onConfirm = { onSave(saved) }
                        )
                    }
                },
                secondaryText = if (isNew) {
                    if (isEs) "Cancelar" else "Cancel"
                } else {
                    if (isEs) "🗑️ Eliminar" else "🗑️ Delete"
                },
                secondaryEnabled = isNew || existingCount > 1,
                onSecondaryClick = {
                    if (isNew) {
                        onDismiss()
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = if (isEs) initialCity.nameEs else initialCity.nameEn,
                            itemTypeEs = "ciudad",
                            itemTypeEn = "city",
                            isEs = isEs,
                            onConfirm = { onDelete(initialCity.id) }
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 4. TRIP PLANNER SCREEN (WITH FULL CREATE, EDIT & DELETE FOR STOPS AND DAYS)
// ============================================================================
@Composable
private fun TripPlannerTabScreen(
    isEs: Boolean,
    onOpenJsonPortability: () -> Unit
) {
    val context = LocalContext.current
    val allPlans by SharedTravelPlanStore.allPlansState.collectAsState()
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val weatherForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    val allStops = portablePlan.itineraryStops
    val availableDays = remember(allStops) {
        allStops.map { it.dayNumber }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedDay by rememberSaveable { mutableIntStateOf(1) }
    if (selectedDay !in availableDays) {
        selectedDay = availableDays.first()
    }

    var editingStop by remember { mutableStateOf<ItineraryStopItem?>(null) }
    var isCreatingStop by remember { mutableStateOf(false) }
    var isCreatingPlan by remember { mutableStateOf(false) }
    var isEditingPlan by remember { mutableStateOf<Boolean>(false) }
    var isSwitchingPlan by remember { mutableStateOf(false) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    val dayStops = remember(allStops, selectedDay) {
        TripPlannerUseCase.sortAndDetectConflicts(
            allStops.filter { it.dayNumber == selectedDay }
        )
    }
    val dayWeather = remember(weatherForecasts, selectedDay) {
        SharedWeatherForecastStore.forecastForDayNumber(selectedDay)
    }

    if (isCreatingPlan || isEditingPlan) {
        PlanEditorModalSheet(
            isEs = isEs,
            initialTitle = if (isEditingPlan) portablePlan.tripTitle else "",
            initialStartDate = if (isEditingPlan) portablePlan.startDateIso else "2026-11-11",
            initialDays = if (isEditingPlan) portablePlan.totalDays else 7,
            isNew = isCreatingPlan,
            onDismiss = {
                isCreatingPlan = false
                isEditingPlan = false
            },
            onSave = { title, startDate, days ->
                if (isCreatingPlan) {
                    SharedTravelPlanStore.createPlan(title, startDate, days)
                    selectedDay = 1
                } else {
                    SharedTravelPlanStore.editCurrentPlan(title, startDate, days)
                }
                isCreatingPlan = false
                isEditingPlan = false
            }
        )
    }

    if (isSwitchingPlan) {
        PlanSwitcherModalSheet(
            isEs = isEs,
            plans = allPlans,
            activePlanId = portablePlan.id,
            onDismiss = { isSwitchingPlan = false },
            onSelectPlan = { planId ->
                SharedTravelPlanStore.switchPlan(planId)
                selectedDay = 1
                isSwitchingPlan = false
            }
        )
    }

    if (isCreatingStop || editingStop != null) {
        val initialStop = editingStop ?: ItineraryStopItem(
            id = "stop-${System.currentTimeMillis()}",
            dayNumber = selectedDay,
            dateIso = String.format(Locale.US, "2026-11-%02d", (10 + selectedDay).coerceAtMost(30)),
            startTime = "10:00",
            endTime = "11:30",
            title = "",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "",
            latitude = 35.6762,
            longitude = 139.6503,
            notes = ""
        )
        ItineraryStopEditorModalSheet(
            isEs = isEs,
            initialStop = initialStop,
            isNew = isCreatingStop,
            onDismiss = {
                isCreatingStop = false
                editingStop = null
            },
            onSave = { saved ->
                SharedTravelPlanStore.upsertStop(saved)
                selectedDay = saved.dayNumber
                isCreatingStop = false
                editingStop = null
            },
            onDelete = { idToDelete ->
                SharedTravelPlanStore.deleteStop(idToDelete)
                isCreatingStop = false
                editingStop = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TOP CARD: ACTIVE TRAVEL PLAN & MULTI-PLAN MANAGEMENT
        KotlinConfCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🗓️ ${portablePlan.tripTitle}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Inicio: ${portablePlan.startDateIso} · ${availableDays.size} días · ${allStops.size} paradas",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (allPlans.size > 1) {
                    AppOptionButton(
                        text = "📑 ${allPlans.size} planes",
                        selected = false,
                        onClick = { isSwitchingPlan = true },
                        modifier = Modifier.width(110.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOptionButton(
                    text = if (isEs) "➕ Nuevo Plan" else "➕ New Plan",
                    selected = false,
                    onClick = { isCreatingPlan = true },
                    modifier = Modifier.weight(1f)
                )
                AppOptionButton(
                    text = if (isEs) "✏️ Editar Plan" else "✏️ Edit Plan",
                    selected = false,
                    onClick = { isEditingPlan = true },
                    modifier = Modifier.weight(1f)
                )
                if (allPlans.size > 1) {
                    AppOptionButton(
                        text = if (isEs) "📑 Cambiar" else "📑 Switch",
                        selected = false,
                        onClick = { isSwitchingPlan = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                AppOptionButton(
                    text = if (isEs) "🗑️ Borrar Plan" else "🗑️ Delete Plan",
                    selected = false,
                    onClick = {
                        confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                            itemName = portablePlan.tripTitle,
                            itemTypeEs = "plan de viaje completo",
                            itemTypeEn = "complete travel plan",
                            isEs = isEs,
                            onConfirm = { SharedTravelPlanStore.deleteCurrentPlan() }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Day selector row + Add Day button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableDays.forEach { dayNum ->
                AppOptionButton(
                    text = if (isEs) "Día $dayNum" else "Day $dayNum",
                    selected = selectedDay == dayNum,
                    onClick = { selectedDay = dayNum },
                    modifier = Modifier.width(92.dp)
                )
            }
            AppOptionButton(
                text = if (isEs) "➕ Día" else "➕ Day",
                selected = false,
                onClick = {
                    val nextDay = (availableDays.maxOrNull() ?: 0) + 1
                    selectedDay = nextDay
                    editingStop = ItineraryStopItem(
                        id = "stop-${System.currentTimeMillis()}",
                        dayNumber = nextDay,
                        dateIso = String.format(Locale.US, "2026-11-%02d", (10 + nextDay).coerceAtMost(30)),
                        startTime = "09:30",
                        endTime = "11:00",
                        title = "",
                        category = StopCategory.SIGHTSEEING,
                        placeNameJapanese = "",
                        latitude = 35.6762,
                        longitude = 139.6503,
                        notes = ""
                    )
                    isCreatingStop = true
                },
                modifier = Modifier.width(92.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Standardized 2-button toolbar: Add Stop + Plan Options / Clear
        AppButtonRow(
            primaryText = if (isEs) "➕ Añadir Parada" else "➕ Add Stop",
            onPrimaryClick = {
                editingStop = null
                isCreatingStop = true
            },
            secondaryText = if (isEs) "⚙️ JSON / Vaciar" else "⚙️ JSON / Clear",
            onSecondaryClick = onOpenJsonPortability
        )

        if (dayStops.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoTagPill(
                    text = "${dayWeather.condition.emoji} ${dayWeather.tempMinC.toInt()}°/${dayWeather.tempMaxC.toInt()}°C · ☔ ${dayWeather.precipitationProbabilityMax}%"
                )
                AppOptionButton(
                    text = if (isEs) "🗑️ Borrar Día $selectedDay" else "🗑️ Delete Day $selectedDay",
                    selected = false,
                    onClick = {
                        confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                            itemName = if (isEs) "Día $selectedDay (${dayStops.size} paradas)" else "Day $selectedDay (${dayStops.size} stops)",
                            itemTypeEs = "día completo del itinerario",
                            itemTypeEn = "full itinerary day",
                            isEs = isEs,
                            onConfirm = { SharedTravelPlanStore.deleteDay(selectedDay) }
                        )
                    },
                    modifier = Modifier.width(150.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (dayStops.isEmpty()) {
            KotlinConfCard {
                Text(
                    text = if (isEs) "No hay paradas en este día" else "No stops on this day",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isEs) {
                        "Pulsa «➕ Añadir Parada» para crear tu propio itinerario paso a paso, o «⚙️ JSON / Vaciar» para importar un plan o cargar la demo."
                    } else {
                        "Tap «➕ Add Stop» to create your own itinerary, or «⚙️ JSON / Clear» to import a plan or load demo data."
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AppButtonRow(
                    primaryText = if (isEs) "➕ Crear Parada" else "➕ Create Stop",
                    onPrimaryClick = {
                        editingStop = null
                        isCreatingStop = true
                    },
                    secondaryText = if (isEs) "🔄 Cargar Demo" else "🔄 Load Demo",
                    onSecondaryClick = {
                        confirmationDialogState = ConfirmationDialogState(
                            title = if (isEs) "¿Cargar datos demo?" else "Load demo data?",
                            message = if (isEs) {
                                "Se sobrescribirá el itinerario actual con los datos de prueba. ¿Deseas continuar?"
                            } else {
                                "The current itinerary will be overwritten with sample data. Continue?"
                            },
                            confirmButtonText = if (isEs) "🔄 Cargar" else "🔄 Load",
                            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
                            isDestructive = true,
                            onConfirm = { SharedTravelPlanStore.resetToDefaults() }
                        )
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(dayStops, key = { it.item.id }) { evaluated ->
                    ItineraryStopCard(
                        stop = evaluated.item,
                        hasConflict = evaluated.hasTimeConflict,
                        isEs = isEs,
                        onEditStop = { stopToEdit ->
                            isCreatingStop = false
                            editingStop = stopToEdit
                        },
                        onDeleteStop = { stopToDelete ->
                            confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                itemName = stopToDelete.title,
                                itemTypeEs = "parada",
                                itemTypeEn = "stop",
                                isEs = isEs,
                                onConfirm = { SharedTravelPlanStore.deleteStop(stopToDelete.id) }
                            )
                        },
                        onOpenGoogleMaps = { stop ->
                            val geoUri = Uri.parse(
                                "geo:${stop.latitude},${stop.longitude}?q=${Uri.encode(stop.placeNameJapanese.ifBlank { stop.title })}"
                            )
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                            }
                        },
                        onExportCalendar = { stop ->
                            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, "🇯🇵 ${stop.title}")
                                putExtra(CalendarContract.Events.EVENT_LOCATION, stop.placeNameJapanese)
                                putExtra(CalendarContract.Events.DESCRIPTION, stop.notes)
                            }
                            runCatching { context.startActivity(calIntent) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItineraryStopCard(
    stop: ItineraryStopItem,
    hasConflict: Boolean,
    isEs: Boolean,
    onEditStop: (ItineraryStopItem) -> Unit,
    onDeleteStop: (ItineraryStopItem) -> Unit,
    onOpenGoogleMaps: (ItineraryStopItem) -> Unit,
    onExportCalendar: (ItineraryStopItem) -> Unit
) {
    KotlinConfCard {
        val fullDate = TravelDateTimeFormatter.formatFullLocalDate(stop.dateIso)
        Text(
            text = "📅 $fullDate",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🕒 ${stop.startTime} – ${stop.endTime}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (hasConflict) {
                InfoTagPill(
                    text = if (isEs) "⚠️ Solapamiento" else "⚠️ Overlap",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                InfoTagPill(text = if (isEs) stop.category.labelEs else stop.category.labelEn)
            }
        }

        val offsetId = runCatching {
            java.time.ZoneId.of(stop.timeZoneId).rules.getOffset(java.time.Instant.now()).id
        }.getOrDefault("+09:00")
        val utcOffset = TravelDateTimeFormatter.formatUtcOffsetString(offsetId)
        val zoneCity = when (stop.timeZoneId) {
            "Europe/Madrid" -> "Madrid"
            "Europe/Berlin" -> "Múnich"
            "Asia/Tokyo" -> "Japón"
            else -> stop.timeZoneId.substringAfter('/')
        }
        val localZoneText = if (isEs) "🌍 Hora local de $zoneCity ($utcOffset)" else "🌍 Local time: $zoneCity ($utcOffset)"
        Spacer(Modifier.height(3.dp))
        Text(
            text = localZoneText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = stop.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (stop.placeNameJapanese.isNotBlank()) {
            Text(
                text = stop.placeNameJapanese,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (stop.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stop.notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onOpenGoogleMaps(stop) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(34.dp)
                    .weight(1.2f)
            ) {
                Text(if (isEs) "🗺️ Mapa" else "🗺️ Map", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = { onExportCalendar(stop) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(34.dp)
                    .weight(1.1f)
            ) {
                Text(if (isEs) "📅 Cal." else "📅 Cal.", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = { onEditStop(stop) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(34.dp)
                    .weight(1.1f)
            ) {
                Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = { onDeleteStop(stop) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(34.dp)
            ) {
                Text("🗑️", fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItineraryStopEditorModalSheet(
    isEs: Boolean,
    initialStop: ItineraryStopItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ItineraryStopItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var stopDateText by rememberSaveable(initialStop.id) {
        mutableStateOf(if (isNew) "" else initialStop.dateIso)
    }
    var dayText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.dayNumber.toString()) }
    var startTimeText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.startTime) }
    var endTimeText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.endTime) }
    var titleText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.title) }
    var japaneseNameText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.placeNameJapanese) }
    var notesText by rememberSaveable(initialStop.id) { mutableStateOf(if (isNew) "" else initialStop.notes) }
    var selectedCategory by rememberSaveable(initialStop.id) { mutableStateOf(initialStop.category) }
    var selectedZoneId by rememberSaveable(initialStop.id) { mutableStateOf(initialStop.timeZoneId) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Añadir Parada al Plan" else "➕ Add Stop to Plan"
                } else {
                    if (isEs) "✏️ Editar Parada" else "✏️ Edit Stop"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDatePickerField(
                    label = if (isEs) "Fecha Actividad" else "Activity Date",
                    dateIso = stopDateText,
                    onDateSelected = { stopDateText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1.1f)
                )
                AppNumberStepperField(
                    label = if (isEs) "Día" else "Day",
                    value = dayText,
                    onValueChange = { dayText = it },
                    minValue = 1,
                    maxValue = 99,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTimePickerField(
                    label = if (isEs) "Hora Inicio" else "Start Time",
                    timeText = startTimeText,
                    onTimeSelected = { startTimeText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1f)
                )
                AppTimePickerField(
                    label = if (isEs) "Hora Fin" else "End Time",
                    timeText = endTimeText,
                    onTimeSelected = { endTimeText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEs) "Nombre del lugar o actividad" else "Place or activity title", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = japaneseNameText,
                onValueChange = { japaneseNameText = it },
                label = { Text(if (isEs) "Nombre en japonés (opcional)" else "Japanese name (optional)", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(if (isEs) "Notas, línea de metro o reserva" else "Notes, subway line or booking", fontSize = 15.sp) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StopCategory.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { cat ->
                            AppOptionButton(
                                text = if (isEs) cat.labelEs else cat.labelEn,
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEs) "Zona Horaria Local" else "Local Time Zone",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOptionButton(
                    text = "Japón (UTC+9)",
                    selected = selectedZoneId == "Asia/Tokyo",
                    onClick = { selectedZoneId = "Asia/Tokyo" },
                    modifier = Modifier.weight(1f)
                )
                AppOptionButton(
                    text = "Múnich (UTC+1)",
                    selected = selectedZoneId == "Europe/Berlin",
                    onClick = { selectedZoneId = "Europe/Berlin" },
                    modifier = Modifier.weight(1f)
                )
                AppOptionButton(
                    text = "Madrid (UTC+1)",
                    selected = selectedZoneId == "Europe/Madrid",
                    onClick = { selectedZoneId = "Europe/Madrid" },
                    modifier = Modifier.weight(1f)
                )
            }

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Parada" else "💾 Save Stop",
                onPrimaryClick = {
                    val safeDay = dayText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val finalDateIso = stopDateText.trim().ifBlank {
                        String.format(Locale.US, "2026-11-%02d", (10 + safeDay).coerceAtMost(30))
                    }
                    val startClean = startTimeText.ifBlank { "10:00" }
                    val endClean = endTimeText.ifBlank { "11:30" }
                    val finalZoneId = selectedZoneId.ifBlank {
                        TravelDateTimeFormatter.inferZoneIdFromCoordinatesOrLocation(
                            initialStop.latitude,
                            initialStop.longitude,
                            "$titleText $notesText"
                        )
                    }
                    val startUtc = TravelDateTimeFormatter.localToUtcIso(finalDateIso, startClean, finalZoneId)
                    val endUtc = TravelDateTimeFormatter.localToUtcIso(finalDateIso, endClean, finalZoneId)
                    val updated = initialStop.copy(
                        dayNumber = safeDay,
                        dateIso = finalDateIso,
                        startTime = startClean,
                        endTime = endClean,
                        title = titleText.ifBlank { if (isEs) "Nueva Parada" else "New Stop" },
                        placeNameJapanese = japaneseNameText,
                        notes = notesText,
                        category = selectedCategory,
                        timeZoneId = finalZoneId,
                        startTimeUtcIso = startUtc,
                        endTimeUtcIso = endUtc
                    )
                    if (isNew) {
                        onSave(updated)
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = updated.title,
                            isEs = isEs,
                            onConfirm = { onSave(updated) }
                        )
                    }
                },
                secondaryText = if (isNew) {
                    if (isEs) "Cancelar" else "Cancel"
                } else {
                    if (isEs) "🗑️ Eliminar Parada" else "🗑️ Delete Stop"
                },
                onSecondaryClick = {
                    if (isNew) {
                        onDismiss()
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = initialStop.title,
                            itemTypeEs = "parada",
                            itemTypeEn = "stop",
                            isEs = isEs,
                            onConfirm = { onDelete(initialStop.id) }
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditorModalSheet(
    isEs: Boolean,
    initialTitle: String,
    initialStartDate: String,
    initialDays: Int,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, startDate: String, days: Int) -> Unit
) {
    var titleText by rememberSaveable { mutableStateOf(initialTitle) }
    var startDateText by rememberSaveable { mutableStateOf(initialStartDate) }
    var daysText by rememberSaveable { mutableStateOf(initialDays.toString()) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Crear Nuevo Plan de Viaje" else "➕ Create New Travel Plan"
                } else {
                    if (isEs) "✏️ Editar Plan de Viaje" else "✏️ Edit Travel Plan"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEs) "Nombre del Viaje (ej. Primavera Kioto 2027)" else "Trip Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDatePickerField(
                    label = if (isEs) "Fecha Inicio" else "Start Date",
                    dateIso = startDateText,
                    onDateSelected = { startDateText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1.1f)
                )
                AppNumberStepperField(
                    label = if (isEs) "Días" else "Days",
                    value = daysText,
                    onValueChange = { daysText = it },
                    minValue = 1,
                    maxValue = 90,
                    modifier = Modifier.weight(0.9f)
                )
            }

            AppButtonRow(
                primaryText = if (isNew) {
                    if (isEs) "🚀 Crear Plan" else "🚀 Create Plan"
                } else {
                    if (isEs) "💾 Guardar" else "💾 Save"
                },
                onPrimaryClick = {
                    val daysInt = daysText.toIntOrNull()?.coerceIn(1, 45) ?: 7
                    val finalTitle = titleText.ifBlank { if (isEs) "Nuevo Viaje a Japón" else "New Japan Trip" }
                    val finalDate = startDateText.ifBlank { "2026-11-11" }
                    if (isNew) {
                        onSave(finalTitle, finalDate, daysInt)
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = finalTitle,
                            isEs = isEs,
                            onConfirm = { onSave(finalTitle, finalDate, daysInt) }
                        )
                    }
                },
                secondaryText = if (isEs) "Cancelar" else "Cancel",
                onSecondaryClick = onDismiss
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanSwitcherModalSheet(
    isEs: Boolean,
    plans: List<PortableTravelPlanBundle>,
    activePlanId: String,
    onDismiss: () -> Unit,
    onSelectPlan: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEs) "📑 Mis Planes de Viaje Guardados" else "📑 Saved Travel Plans",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            plans.forEach { plan ->
                val isSelected = plan.id == activePlanId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlan(plan.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.tripTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "📅 ${plan.startDateIso} · ${plan.totalDays} días · ${plan.itineraryStops.size} paradas",
                                fontSize = 13.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Text("✓ Activo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 5. TRANSPORT & TRAINS SCREEN (WITH FULL CREATE, EDIT & DELETE)
// ============================================================================
@Composable
private fun TransportTabScreen(
    isEs: Boolean,
    onOpenJsonPortability: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val segments = portablePlan.transportSegments
    val alerts by SharedTransportAlertStore.alertsState.collectAsState()

    var selectedFilter by rememberSaveable { mutableStateOf<TransportMode?>(null) }
    val filteredSegments = remember(segments, selectedFilter) {
        if (selectedFilter == null) segments else segments.filter { it.type == selectedFilter }
    }

    var editingSegment by remember { mutableStateOf<TransportSegmentItem?>(null) }
    var isCreatingSegment by remember { mutableStateOf(false) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    LaunchedEffect(segments, isEs) {
        SharedTransportAlertStore.evaluate(segments, context, isEs)
        while (true) {
            delay(30_000L)
            SharedTransportAlertStore.evaluate(segments, context, isEs)
        }
    }

    if (isCreatingSegment || editingSegment != null) {
        val initialSeg = editingSegment ?: TransportPlannerUseCase.createEmptySegment(
            id = "seg-${System.currentTimeMillis()}",
            order = segments.size + 1,
            initialMode = selectedFilter ?: TransportMode.TRAIN_SHINKANSEN
        )
        TransportSegmentEditorModalSheet(
            isEs = isEs,
            initialSegment = initialSeg,
            isNew = isCreatingSegment,
            onDismiss = {
                isCreatingSegment = false
                editingSegment = null
            },
            onSave = { saved ->
                SharedTravelPlanStore.upsertTransportSegment(saved)
                isCreatingSegment = false
                editingSegment = null
            },
            onDelete = { idToDelete ->
                SharedTravelPlanStore.deleteTransportSegment(idToDelete)
                isCreatingSegment = false
                editingSegment = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. ACTIVE TRANSPORT ALERTS BANNER (VISUAL WARNING & SYSTEM NOTIFICATION DISPATCH)
        if (alerts.isNotEmpty()) {
            items(alerts, key = { it.uniqueKey }) { alert ->
                val isUrgent = alert.severity == TransportAlertSeverity.URGENT
                val borderCol = if (isUrgent) Color(0xFFC9171E) else Color(0xFFE5A100)
                val bgCol = if (isUrgent) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                val textCol = if (isUrgent) Color(0xFFB71C1C) else Color(0xFF5D4037)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, borderCol),
                    color = bgCol
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEs) alert.titleEs else alert.titleEn,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = borderCol
                            ) {
                                Text(
                                    text = if (alert.minutesUntilEvent <= 0) (if (isEs) "¡AHORA!" else "NOW!") else "⏰ ${alert.minutesUntilEvent} min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isEs) alert.messageEs else alert.messageEn,
                            fontSize = 14.sp,
                            color = textCol
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${alert.targetGateOrCounter}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textCol.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        TransportAlertNotificationHelper.dispatchAlertNotification(context, alert, isEs)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isEs) "🔔 Notificar" else "🔔 Push", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        SharedTransportAlertStore.dismissAlert(alert.uniqueKey)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("✕", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. ALERT TEST & SIMULATION CONTROL CARD
        item {
            KotlinConfCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "🔔 Avisos de Facturación y Embarque" else "🔔 Flight & Transport Alerts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    InfoTagPill(text = if (alerts.isNotEmpty()) "${alerts.size} activas" else if (isEs) "Activo" else "Active")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEs) {
                        "La app detecta automáticamente la proximidad de la facturación, el embarque y la salida de tus vuelos y trenes, mostrando advertencias visuales y notificaciones al móvil."
                    } else {
                        "The app automatically alerts you when check-in, boarding or departure approaches with visual warnings and push notifications."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isEs) "Probar aviso en este dispositivo:" else "Test alert on this device:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            SharedTransportAlertStore.triggerSimulation(context, TransportAlertType.BOARDING_NOW, isEs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(if (isEs) "🚨 Embarque" else "🚨 Boarding", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            SharedTransportAlertStore.triggerSimulation(context, TransportAlertType.CHECK_IN_APPROACHING, isEs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(if (isEs) "🧳 Facturación" else "🧳 Check-in", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            SharedTransportAlertStore.triggerSimulation(context, TransportAlertType.DEPARTURE_IMMINENT, isEs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(if (isEs) "🚄 Tren Salida" else "🚄 Train Dep", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // 3. TRANSPORT FILTER BUTTONS & ACTIONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOptionButton(
                        text = if (isEs) "Todos" else "All",
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        modifier = Modifier.weight(1f)
                    )
                    AppOptionButton(
                        text = if (isEs) "✈️ Vuelos" else "✈️ Flights",
                        selected = selectedFilter == TransportMode.FLIGHT,
                        onClick = { selectedFilter = TransportMode.FLIGHT },
                        modifier = Modifier.weight(1f)
                    )
                    AppOptionButton(
                        text = if (isEs) "🚄 Trenes" else "🚄 Trains",
                        selected = selectedFilter == TransportMode.TRAIN_SHINKANSEN,
                        onClick = { selectedFilter = TransportMode.TRAIN_SHINKANSEN },
                        modifier = Modifier.weight(1f)
                    )
                }

                AppButtonRow(
                    primaryText = if (isEs) "➕ Añadir Transporte" else "➕ Add Transport",
                    onPrimaryClick = {
                        editingSegment = null
                        isCreatingSegment = true
                    },
                    secondaryText = if (isEs) "⚙️ JSON / Vaciar" else "⚙️ JSON / Clear",
                    onSecondaryClick = onOpenJsonPortability
                )
            }
        }

        if (filteredSegments.isEmpty()) {
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "No hay tramos de transporte" else "No transport segments",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isEs) {
                            "Pulsa «➕ Añadir Transporte» para registrar tus vuelos, trenes Shinkansen, metros o autobuses."
                        } else {
                            "Tap «➕ Add Transport» to log your flights, Shinkansen trains, subways or buses."
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredSegments, key = { it.id }) { seg ->
                val durationMins = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg)
                val segAlert = alerts.firstOrNull { it.segmentId == seg.id }
                val isUrgentSeg = segAlert?.severity == TransportAlertSeverity.URGENT
                val cardBorderColor = if (isUrgentSeg) {
                    Color(0xFFC9171E)
                } else if (segAlert != null) {
                    Color(0xFFE5A100)
                } else {
                    MaterialTheme.colorScheme.outline
                }

                KotlinConfCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorderColor = cardBorderColor
                ) {
                    if (segAlert != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUrgentSeg) Color(0xFFC9171E) else Color(0xFFE5A100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEs) segAlert.titleEs else segAlert.titleEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    val depDate = seg.departureTimeIso.substringBefore('T')
                    val arrDate = seg.arrivalTimeIso.substringBefore('T')
                    val depFormattedDate = TravelDateTimeFormatter.formatFullLocalDate(depDate)
                    Text(
                        text = "📅 $depFormattedDate",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${seg.operatorName} · ${seg.lineOrNumber}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        InfoTagPill(text = seg.passOrPaymentBadge)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${seg.departureCode} (${seg.departureName}) ➔ ${seg.arrivalCode} (${seg.arrivalName})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val depOffsetId = runCatching {
                        java.time.ZoneId.of(seg.departureZoneId).rules.getOffset(java.time.Instant.now()).id
                    }.getOrDefault("+01:00")
                    val arrOffsetId = runCatching {
                        java.time.ZoneId.of(seg.arrivalZoneId).rules.getOffset(java.time.Instant.now()).id
                    }.getOrDefault("+09:00")
                    val depOffset = TravelDateTimeFormatter.formatUtcOffsetString(depOffsetId)
                    val arrOffset = TravelDateTimeFormatter.formatUtcOffsetString(arrOffsetId)
                    val isNextDay = depDate != arrDate
                    val nextDaySuffix = if (isNextDay) " (+1d)" else ""

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🕒 ${seg.departureTimeIso.substringAfter('T')} ($depOffset) ➔ ${seg.arrivalTimeIso.substringAfter('T')}$nextDaySuffix ($arrOffset) · ${TransportPlannerUseCase.formatDurationHoursMinutes(durationMins)}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    val depCity = seg.departureName.substringBefore('-').trim()
                    val arrCity = seg.arrivalName.substringBefore('-').trim()
                    val localZoneDesc = if (isEs) {
                        "🌍 Salida: $depCity ($depOffset) · Llegada: $arrCity ($arrOffset)"
                    } else {
                        "🌍 Dep: $depCity ($depOffset) · Arr: $arrCity ($arrOffset)"
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = localZoneDesc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )

                    if (seg.checkInTime != null || seg.boardingTime != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            seg.checkInTime?.let {
                                InfoTagPill(text = "🧳 " + (if (isEs) "Facturación: " else "Check-in: ") + it.substringAfter('T'))
                            }
                            seg.boardingTime?.let {
                                InfoTagPill(text = "🚪 " + (if (isEs) "Embarque: " else "Boarding: ") + it.substringAfter('T'))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "📍 ${seg.departureTerminalOrPlatform} · 💺 ${seg.seatOrCarInfo}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isCreatingSegment = false
                                editingSegment = seg
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(1f)
                        ) {
                            Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                    itemName = "${seg.operatorName} ${seg.lineOrNumber}",
                                    itemTypeEs = "transporte",
                                    itemTypeEn = "transport",
                                    isEs = isEs,
                                    onConfirm = {
                                        SharedTravelPlanStore.deleteTransportSegment(seg.id)
                                    }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(if (isEs) "🗑️ Eliminar" else "🗑️ Delete", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportSegmentEditorModalSheet(
    isEs: Boolean,
    initialSegment: TransportSegmentItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransportSegmentItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var selectedType by rememberSaveable(initialSegment.id) { mutableStateOf(initialSegment.type) }
    var operatorText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.operatorName) }
    var lineNumberText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.lineOrNumber) }
    var depNameText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.departureName) }
    var arrNameText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.arrivalName) }
    var depDateText by rememberSaveable(initialSegment.id) {
        mutableStateOf(
            if (isNew) {
                ""
            } else if (initialSegment.departureTimeIso.contains('T')) {
                initialSegment.departureTimeIso.substringBefore('T')
            } else {
                initialSegment.departureTimeIso
            }
        )
    }
    var depTimeText by rememberSaveable(initialSegment.id) {
        mutableStateOf(if (isNew || !initialSegment.departureTimeIso.contains('T')) "" else initialSegment.departureTimeIso.substringAfter('T'))
    }
    var arrTimeText by rememberSaveable(initialSegment.id) {
        mutableStateOf(if (isNew || !initialSegment.arrivalTimeIso.contains('T')) "" else initialSegment.arrivalTimeIso.substringAfter('T'))
    }
    var checkInTimeText by rememberSaveable(initialSegment.id) {
        mutableStateOf(if (isNew || initialSegment.checkInTime == null || !initialSegment.checkInTime.contains('T')) "" else initialSegment.checkInTime.substringAfter('T'))
    }
    var boardingTimeText by rememberSaveable(initialSegment.id) {
        mutableStateOf(if (isNew || initialSegment.boardingTime == null || !initialSegment.boardingTime.contains('T')) "" else initialSegment.boardingTime.substringAfter('T'))
    }
    var platformText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.departureTerminalOrPlatform) }
    var seatText by rememberSaveable(initialSegment.id) { mutableStateOf(if (isNew) "" else initialSegment.seatOrCarInfo) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Añadir Transporte" else "➕ Add Transport"
                } else {
                    if (isEs) "✏️ Editar Transporte" else "✏️ Edit Transport"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(TransportMode.TRAIN_SHINKANSEN, TransportMode.FLIGHT, TransportMode.SUBWAY_METRO, TransportMode.BUS).forEach { mode ->
                    AppOptionButton(
                        text = if (isEs) mode.labelEs else mode.labelEn,
                        selected = selectedType == mode,
                        onClick = { selectedType = mode },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = operatorText,
                    onValueChange = { operatorText = it },
                    label = { Text(if (isEs) "Compañía (ej. JR, ANA)" else "Operator (e.g. JR, ANA)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lineNumberText,
                    onValueChange = { lineNumberText = it },
                    label = { Text(if (isEs) "Tren/Vuelo (ej. Nozomi)" else "Line/Flight #", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = depNameText,
                    onValueChange = { depNameText = it },
                    label = { Text(if (isEs) "Origen (ej. Tokyo)" else "From (e.g. Tokyo)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = arrNameText,
                    onValueChange = { arrNameText = it },
                    label = { Text(if (isEs) "Destino (ej. Kyoto)" else "To (e.g. Kyoto)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            AppDatePickerField(
                label = if (isEs) "Fecha del Trayecto" else "Journey Date",
                dateIso = depDateText,
                onDateSelected = { depDateText = it },
                isEs = isEs,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTimePickerField(
                    label = if (isEs) "Hora Salida" else "Dep Time",
                    timeText = depTimeText,
                    onTimeSelected = { depTimeText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1f)
                )
                AppTimePickerField(
                    label = if (isEs) "Hora Llegada" else "Arr Time",
                    timeText = arrTimeText,
                    onTimeSelected = { arrTimeText = it },
                    isEs = isEs,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTimePickerField(
                    label = if (isEs) "Facturación (opc)" else "Check-in (opt)",
                    timeText = checkInTimeText,
                    onTimeSelected = { checkInTimeText = it },
                    isEs = isEs,
                    isOptional = true,
                    onClear = { checkInTimeText = "" },
                    modifier = Modifier.weight(1f)
                )
                AppTimePickerField(
                    label = if (isEs) "Embarque (opc)" else "Boarding (opt)",
                    timeText = boardingTimeText,
                    onTimeSelected = { boardingTimeText = it },
                    isEs = isEs,
                    isOptional = true,
                    onClear = { boardingTimeText = "" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = platformText,
                    onValueChange = { platformText = it },
                    label = { Text(if (isEs) "Andén / Terminal" else "Platform / Terminal", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = seatText,
                    onValueChange = { seatText = it },
                    label = { Text(if (isEs) "Vagón / Asiento" else "Car / Seat", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Transporte" else "💾 Save Transport",
                onPrimaryClick = {
                    val depCode = depNameText.trim().take(3).uppercase(Locale.ROOT).ifBlank { "TYO" }
                    val arrCode = arrNameText.trim().take(3).uppercase(Locale.ROOT).ifBlank { "KYO" }
                    val baseDate = depDateText.trim().ifBlank {
                        if (initialSegment.departureTimeIso.contains('T')) {
                            initialSegment.departureTimeIso.substringBefore('T')
                        } else "2026-11-12"
                    }
                    val defaultOp = when (selectedType) {
                        TransportMode.FLIGHT -> if (isEs) "Vuelo" else "Flight"
                        TransportMode.TRAIN_SHINKANSEN -> if (isEs) "Shinkansen / Tren" else "Shinkansen / Train"
                        TransportMode.SUBWAY_METRO -> if (isEs) "Metro" else "Subway"
                        TransportMode.BUS -> if (isEs) "Autobús" else "Bus"
                    }
                    val depTimeClean = depTimeText.trim().ifBlank { "09:00" }
                    val arrTimeClean = arrTimeText.trim().ifBlank { "11:00" }
                    val depUtc = runCatching {
                        TravelDateTimeFormatter.localToUtcIso(baseDate, depTimeClean, initialSegment.departureZoneId)
                    }.getOrDefault("${baseDate}T$depTimeClean:00Z")
                    val arrUtc = runCatching {
                        TravelDateTimeFormatter.localToUtcIso(baseDate, arrTimeClean, initialSegment.arrivalZoneId)
                    }.getOrDefault("${baseDate}T$arrTimeClean:00Z")
                    val updated = initialSegment.copy(
                        type = selectedType,
                        operatorName = operatorText.trim().ifBlank { defaultOp },
                        lineOrNumber = lineNumberText.trim(),
                        departureCode = depCode,
                        departureName = depNameText.trim().ifBlank { if (isEs) "Origen" else "Origin" },
                        departureTimeIso = "${baseDate}T$depTimeClean",
                        checkInTime = if (checkInTimeText.isNotBlank()) "${baseDate}T${checkInTimeText.trim()}" else null,
                        boardingTime = if (boardingTimeText.isNotBlank()) "${baseDate}T${boardingTimeText.trim()}" else null,
                        arrivalCode = arrCode,
                        arrivalName = arrNameText.trim().ifBlank { if (isEs) "Destino" else "Destination" },
                        arrivalTimeIso = "${baseDate}T$arrTimeClean",
                        departureTerminalOrPlatform = platformText.trim(),
                        seatOrCarInfo = seatText.trim(),
                        passOrPaymentBadge = if (selectedType == TransportMode.FLIGHT) "Flight" else "IC Card / JR",
                        departureTimeUtcIso = depUtc,
                        arrivalTimeUtcIso = arrUtc
                    )
                    if (isNew) {
                        onSave(updated)
                    } else {
                        val segName = "${updated.operatorName} ${updated.lineOrNumber}".trim().ifBlank { updated.departureName }
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = segName,
                            isEs = isEs,
                            onConfirm = { onSave(updated) }
                        )
                    }
                },
                secondaryText = if (isNew) {
                    if (isEs) "Cancelar" else "Cancel"
                } else {
                    if (isEs) "🗑️ Eliminar" else "🗑️ Delete"
                },
                onSecondaryClick = {
                    if (isNew) {
                        onDismiss()
                    } else {
                        val segName = "${initialSegment.operatorName} ${initialSegment.lineOrNumber}".trim().ifBlank { initialSegment.departureName }
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = segName,
                            itemTypeEs = "transporte",
                            itemTypeEn = "transport",
                            isEs = isEs,
                            onConfirm = { onDelete(initialSegment.id) }
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 6. VOICE & CAMERA TRANSLATOR SCREEN (BIDIRECTIONAL STT + CAMERA OCR)
// ============================================================================
private enum class CropDragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT,
    MOVE_BOX
}

@Composable
private fun SelectImageRegionDialog(
    bitmap: Bitmap,
    initialPreset: ImageCropPreset,
    initialLeft: Float,
    initialTop: Float,
    initialRight: Float,
    initialBottom: Float,
    initialSelectedZones: Set<String> = emptySet(),
    isEs: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (preset: ImageCropPreset, left: Float, top: Float, right: Float, bottom: Float, selectedZones: Set<String>) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(initialPreset) }
    var left by remember { mutableFloatStateOf(initialLeft) }
    var top by remember { mutableFloatStateOf(initialTop) }
    var right by remember { mutableFloatStateOf(initialRight) }
    var bottom by remember { mutableFloatStateOf(initialBottom) }
    var multiZoneMode by remember { mutableStateOf(initialSelectedZones.size > 1) }
    val selectedZones = remember { mutableStateListOf<String>().apply { addAll(initialSelectedZones) } }
    var activeDragHandle by remember { mutableStateOf(CropDragHandle.NONE) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "📸 Selector de Zona (Arrastre Táctil)" else "📸 Region Selector (Touch Drag)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isEs) {
                        "Arrastra con el dedo las esquinas o bordes para encuadrar el cartel sin recortar la foto original:"
                    } else {
                        "Drag corners or edges with your finger to frame the sign without cropping original photo:"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // Previsualización no destructiva maximizada estilo Google Photos (con arrastre táctil)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141414),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                val boxW = size.width.toFloat()
                                val boxH = size.height.toFloat()
                                val aspectBitmap = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                                val aspectBox = boxW / boxH.coerceAtLeast(1f)

                                val imgW: Float
                                val imgH: Float
                                val imgX: Float
                                val imgY: Float
                                if (aspectBox > aspectBitmap) {
                                    imgH = boxH
                                    imgW = imgH * aspectBitmap
                                    imgX = (boxW - imgW) / 2f
                                    imgY = 0f
                                } else {
                                    imgW = boxW
                                    imgH = imgW / aspectBitmap
                                    imgX = 0f
                                    imgY = (boxH - imgH) / 2f
                                }

                                val cornerSlop = 40.dp.toPx()
                                val edgeSlop = 28.dp.toPx()

                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val cropL = imgX + imgW * left
                                        val cropT = imgY + imgH * top
                                        val cropR = imgX + imgW * right
                                        val cropB = imgY + imgH * bottom

                                        val tx = offset.x
                                        val ty = offset.y

                                        activeDragHandle = when {
                                            // 1. Esquinas (prioridad táctil máxima)
                                            hypot(tx - cropL, ty - cropT) <= cornerSlop -> CropDragHandle.TOP_LEFT
                                            hypot(tx - cropR, ty - cropT) <= cornerSlop -> CropDragHandle.TOP_RIGHT
                                            hypot(tx - cropL, ty - cropB) <= cornerSlop -> CropDragHandle.BOTTOM_LEFT
                                            hypot(tx - cropR, ty - cropB) <= cornerSlop -> CropDragHandle.BOTTOM_RIGHT

                                            // 2. Bordes
                                            abs(ty - cropT) <= edgeSlop && tx >= (cropL - 10f) && tx <= (cropR + 10f) -> CropDragHandle.EDGE_TOP
                                            abs(ty - cropB) <= edgeSlop && tx >= (cropL - 10f) && tx <= (cropR + 10f) -> CropDragHandle.EDGE_BOTTOM
                                            abs(tx - cropL) <= edgeSlop && ty >= (cropT - 10f) && ty <= (cropB + 10f) -> CropDragHandle.EDGE_LEFT
                                            abs(tx - cropR) <= edgeSlop && ty >= (cropT - 10f) && ty <= (cropB + 10f) -> CropDragHandle.EDGE_RIGHT

                                            // 3. Interior del recuadro para desplazar/panear
                                            tx in cropL..cropR && ty in cropT..cropB -> CropDragHandle.MOVE_BOX

                                            else -> CropDragHandle.NONE
                                        }
                                        if (activeDragHandle != CropDragHandle.NONE) {
                                            selectedPreset = ImageCropPreset.CUSTOM
                                        }
                                    },
                                    onDragEnd = {
                                        activeDragHandle = CropDragHandle.NONE
                                    },
                                    onDragCancel = {
                                        activeDragHandle = CropDragHandle.NONE
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (activeDragHandle == CropDragHandle.NONE) return@detectDragGestures
                                        change.consume()

                                        val dx = if (imgW > 0f) dragAmount.x / imgW else 0f
                                        val dy = if (imgH > 0f) dragAmount.y / imgH else 0f
                                        val minSpan = 0.08f

                                        when (activeDragHandle) {
                                            CropDragHandle.TOP_LEFT -> {
                                                left = (left + dx).coerceIn(0f, right - minSpan)
                                                top = (top + dy).coerceIn(0f, bottom - minSpan)
                                            }
                                            CropDragHandle.TOP_RIGHT -> {
                                                right = (right + dx).coerceIn(left + minSpan, 1f)
                                                top = (top + dy).coerceIn(0f, bottom - minSpan)
                                            }
                                            CropDragHandle.BOTTOM_LEFT -> {
                                                left = (left + dx).coerceIn(0f, right - minSpan)
                                                bottom = (bottom + dy).coerceIn(top + minSpan, 1f)
                                            }
                                            CropDragHandle.BOTTOM_RIGHT -> {
                                                right = (right + dx).coerceIn(left + minSpan, 1f)
                                                bottom = (bottom + dy).coerceIn(top + minSpan, 1f)
                                            }
                                            CropDragHandle.EDGE_TOP -> {
                                                top = (top + dy).coerceIn(0f, bottom - minSpan)
                                            }
                                            CropDragHandle.EDGE_BOTTOM -> {
                                                bottom = (bottom + dy).coerceIn(top + minSpan, 1f)
                                            }
                                            CropDragHandle.EDGE_LEFT -> {
                                                left = (left + dx).coerceIn(0f, right - minSpan)
                                            }
                                            CropDragHandle.EDGE_RIGHT -> {
                                                right = (right + dx).coerceIn(left + minSpan, 1f)
                                            }
                                            CropDragHandle.MOVE_BOX -> {
                                                val curW = right - left
                                                val curH = bottom - top
                                                var newL = left + dx
                                                var newT = top + dy
                                                if (newL < 0f) newL = 0f
                                                if (newL + curW > 1f) newL = 1f - curW
                                                if (newT < 0f) newT = 0f
                                                if (newT + curH > 1f) newT = 1f - curH
                                                left = newL
                                                right = newL + curW
                                                top = newT
                                                bottom = newT + curH
                                            }
                                            CropDragHandle.NONE -> {}
                                        }
                                    }
                                )
                            }
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Previsualización Google Photos",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val boxW = size.width
                            val boxH = size.height
                            val aspectBitmap = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                            val aspectBox = boxW / boxH.coerceAtLeast(1f)

                            val imgW: Float
                            val imgH: Float
                            val imgX: Float
                            val imgY: Float
                            if (aspectBox > aspectBitmap) {
                                imgH = boxH
                                imgW = imgH * aspectBitmap
                                imgX = (boxW - imgW) / 2f
                                imgY = 0f
                            } else {
                                imgW = boxW
                                imgH = imgW / aspectBitmap
                                imgX = 0f
                                imgY = (boxH - imgH) / 2f
                            }

                            val cropL = imgX + imgW * left
                            val cropT = imgY + imgH * top
                            val cropR = imgX + imgW * right
                            val cropB = imgY + imgH * bottom

                            // 1. Scrim translúcido (oscurece todo lo exterior al encuadre)
                            val scrimColor = Color(0x99000000)
                            if (cropT > imgY) {
                                drawRect(scrimColor, topLeft = Offset(imgX, imgY), size = Size(imgW, cropT - imgY))
                            }
                            if (imgY + imgH > cropB) {
                                drawRect(scrimColor, topLeft = Offset(imgX, cropB), size = Size(imgW, (imgY + imgH) - cropB))
                            }
                            if (cropL > imgX) {
                                drawRect(scrimColor, topLeft = Offset(imgX, cropT), size = Size(cropL - imgX, cropB - cropT))
                            }
                            if (imgX + imgW > cropR) {
                                drawRect(scrimColor, topLeft = Offset(cropR, cropT), size = Size((imgX + imgW) - cropR, cropB - cropT))
                            }

                            // 2. Líneas de guía de tercios (Rule of Thirds)
                            val cropW = (cropR - cropL).coerceAtLeast(1f)
                            val cropH = (cropB - cropT).coerceAtLeast(1f)
                            val gridColor = Color.White.copy(alpha = 0.35f)
                            val gridStroke = 1.dp.toPx()
                            drawLine(gridColor, Offset(cropL + cropW / 3f, cropT), Offset(cropL + cropW / 3f, cropB), strokeWidth = gridStroke)
                            drawLine(gridColor, Offset(cropL + 2f * cropW / 3f, cropT), Offset(cropL + 2f * cropW / 3f, cropB), strokeWidth = gridStroke)
                            drawLine(gridColor, Offset(cropL, cropT + cropH / 3f), Offset(cropR, cropT + cropH / 3f), strokeWidth = gridStroke)
                            drawLine(gridColor, Offset(cropL, cropT + 2f * cropH / 3f), Offset(cropR, cropT + 2f * cropH / 3f), strokeWidth = gridStroke)

                            // 3. Marco de recorte blanco
                            drawRect(
                                color = Color.White.copy(alpha = 0.85f),
                                topLeft = Offset(cropL, cropT),
                                size = Size(cropW, cropH),
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // 4. Brackets de esquina icónicos estilo Google Photos ┌ ┐ └ ┘ con puntos de toque
                            val bracketLen = 22.dp.toPx().coerceAtMost(cropW / 3f).coerceAtMost(cropH / 3f)
                            val bracketStroke = 4.dp.toPx()
                            val bracketColor = Color.White
                            val handleCircleRadius = 5.dp.toPx()

                            // Top-Left ┌
                            drawLine(bracketColor, Offset(cropL, cropT), Offset(cropL + bracketLen, cropT), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropL, cropT), Offset(cropL, cropT + bracketLen), strokeWidth = bracketStroke)
                            drawCircle(bracketColor, radius = handleCircleRadius, center = Offset(cropL, cropT))

                            // Top-Right ┐
                            drawLine(bracketColor, Offset(cropR, cropT), Offset(cropR - bracketLen, cropT), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropR, cropT), Offset(cropR, cropT + bracketLen), strokeWidth = bracketStroke)
                            drawCircle(bracketColor, radius = handleCircleRadius, center = Offset(cropR, cropT))

                            // Bottom-Left └
                            drawLine(bracketColor, Offset(cropL, cropB), Offset(cropL + bracketLen, cropB), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropL, cropB), Offset(cropL, cropB - bracketLen), strokeWidth = bracketStroke)
                            drawCircle(bracketColor, radius = handleCircleRadius, center = Offset(cropL, cropB))

                            // Bottom-Right ┘
                            drawLine(bracketColor, Offset(cropR, cropB), Offset(cropR - bracketLen, cropB), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropR, cropB), Offset(cropR, cropB - bracketLen), strokeWidth = bracketStroke)
                            drawCircle(bracketColor, radius = handleCircleRadius, center = Offset(cropR, cropB))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Indicador táctil de ayuda
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEs) {
                            "👆 Arrastra con el dedo las esquinas/bordes para cambiar el tamaño. Toca y arrastra el centro para mover el recuadro."
                        } else {
                            "👆 Drag corners/edges with your finger to resize. Tap and drag inside the box to move it."
                        },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Selector de modo Multizona
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { multiZoneMode = !multiZoneMode }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEs) "✨ Modo Multizona" else "✨ Multi-Zone Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEs) "Traduce múltiples bloques de carteles simultáneamente" else "Translate multiple sign zones simultaneously",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = multiZoneMode,
                        onCheckedChange = { multiZoneMode = it }
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (isEs) "Zonas Rápidas y Presets:" else "Quick Zones & Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        ImageCropPreset.FULL,
                        ImageCropPreset.TOP_HALF,
                        ImageCropPreset.CENTER,
                        ImageCropPreset.BOTTOM_HALF,
                        ImageCropPreset.LEFT_HALF,
                        ImageCropPreset.RIGHT_HALF
                    )
                    presets.forEach { preset ->
                        val isSel = if (multiZoneMode) {
                            preset.id in selectedZones
                        } else {
                            selectedPreset == preset
                        }
                        AppOptionButton(
                            text = if (isEs) preset.labelEs else preset.labelEn,
                            selected = isSel,
                            onClick = {
                                if (multiZoneMode) {
                                    if (preset == ImageCropPreset.FULL) {
                                        selectedZones.clear()
                                        selectedZones.addAll(listOf("zone_top", "zone_center", "zone_bottom"))
                                    } else {
                                        val zoneId = when (preset) {
                                            ImageCropPreset.TOP_HALF -> "zone_top"
                                            ImageCropPreset.CENTER -> "zone_center"
                                            ImageCropPreset.BOTTOM_HALF -> "zone_bottom"
                                            ImageCropPreset.LEFT_HALF -> "zone_left"
                                            ImageCropPreset.RIGHT_HALF -> "zone_right"
                                            else -> preset.id
                                        }
                                        if (zoneId in selectedZones) {
                                            selectedZones.remove(zoneId)
                                        } else {
                                            selectedZones.add(zoneId)
                                        }
                                    }
                                } else {
                                    selectedPreset = preset
                                    left = preset.leftFraction
                                    top = preset.topFraction
                                    right = preset.rightFraction
                                    bottom = preset.bottomFraction
                                }
                            },
                            fontSize = 11.5.sp,
                            horizontalPadding = 6.dp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "Cancelar" else "Cancel")
                    }
                    Button(
                        onClick = {
                            val zonesToReturn = if (multiZoneMode) selectedZones.toSet() else emptySet()
                            onConfirm(selectedPreset, left, top, right, bottom, zonesToReturn)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (multiZoneMode && selectedZones.size > 1) {
                                if (isEs) "🔍 Traducir (${selectedZones.size} zonas)" else "🔍 Translate (${selectedZones.size} zones)"
                            } else {
                                if (isEs) "🔍 Traducir" else "🔍 Translate"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceTranslatorScreen(isEs: Boolean, prefs: UserPreferences? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var translatorMode by rememberSaveable { mutableIntStateOf(0) } // 0: Voz, 1: Texto, 2: Cámara / Carteles

    // --- MODO 0: VOZ SIMULTÁNEA ---
    var phraseInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    var japaneseHeardInput by rememberSaveable {
        mutableStateOf("いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？")
    }
    var customOnlineTranslationJaToEs by rememberSaveable { mutableStateOf<String?>(null) }
    var customOnlineTranslationEsToJa by rememberSaveable { mutableStateOf<String?>(null) }
    var rotateJapaneseCard180 by rememberSaveable { mutableStateOf(false) }

    // --- MODO 1: TRADUCTOR DE TEXTO (ESPAÑOL ➔ JAPONÉS) ---
    var textTranslatorInput by rememberSaveable { mutableStateOf("") }
    var textTranslatedItem by rememberSaveable { mutableStateOf<SimultaneousTranslationItem?>(null) }
    var textOnlineTranslationJa by rememberSaveable { mutableStateOf<String?>(null) }

    // --- MODO 2: CÁMARA & OCR DE CARTELES ---
    var originalCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeCropPreset by remember { mutableStateOf(ImageCropPreset.FULL) }
    var showRegionCropDialog by remember { mutableStateOf(false) }
    var customCropLeft by remember { mutableFloatStateOf(0f) }
    var customCropTop by remember { mutableFloatStateOf(0f) }
    var customCropRight by remember { mutableFloatStateOf(1f) }
    var customCropBottom by remember { mutableFloatStateOf(1f) }
    var activeSelectedZones by remember { mutableStateOf(setOf<String>()) }
    var ocrMultiZoneResults by remember { mutableStateOf<List<Pair<String, com.laralbir.japantraveltools.feature.ai.CameraOcrResult>>>(emptyList()) }
    var ocrOnlineTranslation by remember { mutableStateOf<String?>(null) }
    var isProcessingOnlineOcr by remember { mutableStateOf(false) }

    var isProcessingOcr by remember { mutableStateOf(false) }
    var ocrRawText by rememberSaveable {
        mutableStateOf(OfflineTranslatorEngine.sampleJapaneseSigns.first().sampleJapaneseOcr)
    }
    var ocrErrorMessage by remember { mutableStateOf<String?>(null) }

    val ocrResult = remember(ocrRawText, isEs) {
        OfflineTranslatorEngine.translateOcrJapaneseText(ocrRawText, isEs)
    }

    val ocrRecognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    DisposableEffect(ocrRecognizer) {
        onDispose {
            ocrRecognizer.close()
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f || degrees % 360f == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun decodeAndOrientBitmap(ctx: Context, uri: Uri): Bitmap? {
        return runCatching {
            val rotationDegrees = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val rawBitmap = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            if (rotationDegrees != 0f) {
                rotateBitmap(rawBitmap, rotationDegrees)
            } else {
                rawBitmap
            }
        }.getOrNull()
    }

    // Motor TTS local
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        lateinit var instance: TextToSpeech
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine = instance
            }
        }
        onDispose {
            instance.stop()
            instance.shutdown()
        }
    }

    fun speakOutLoud(text: String, locale: Locale) {
        val engine = ttsEngine ?: return
        val res = engine.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (locale.language == "es") {
                engine.setLanguage(Locale.forLanguageTag("es"))
            } else if (locale.language == "ja") {
                engine.setLanguage(Locale.JAPANESE)
            }
        }
        engine.setSpeechRate(0.95f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
    }

    fun performTextTranslation(input: String, autoSpeak: Boolean = false) {
        val clean = input.trim()
        if (clean.isBlank()) return
        val offline = OfflineTranslatorEngine.translateToJapanese(clean)
        textTranslatedItem = offline
        textOnlineTranslationJa = null
        if (autoSpeak) {
            speakOutLoud(offline.japaneseText, Locale.JAPAN)
        }
        if (prefs?.forceOfflineMode != true) {
            coroutineScope.launch(Dispatchers.IO) {
                val online = OnlineTranslatorClient.translate(clean, if (isEs) "es" else "en", "ja")
                withContext(Dispatchers.Main) {
                    if (!online.isNullOrBlank() && online != offline.japaneseText) {
                        textOnlineTranslationJa = online
                        if (autoSpeak) {
                            speakOutLoud(online, Locale.JAPAN)
                        }
                    }
                }
            }
        }
    }

    fun processOcrImage(inputImage: InputImage, previewBitmap: Bitmap?) {
        isProcessingOcr = true
        ocrErrorMessage = null
        ocrOnlineTranslation = null
        activeCroppedBitmap = previewBitmap
        ocrRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                isProcessingOcr = false
                val recognizedText = visionText.text
                if (recognizedText.isNotBlank()) {
                    ocrRawText = recognizedText
                    // Traducción online en segundo plano para enriquecer el resultado offline
                    if (prefs?.forceOfflineMode != true) {
                        isProcessingOnlineOcr = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val online = OnlineTranslatorClient.translate(recognizedText, "ja", if (isEs) "es" else "en")
                            withContext(Dispatchers.Main) {
                                isProcessingOnlineOcr = false
                                if (!online.isNullOrBlank()) {
                                    ocrOnlineTranslation = online
                                }
                            }
                        }
                    }
                } else {
                    ocrErrorMessage = if (isEs) {
                        "No se detectó texto en la zona seleccionada. Intenta seleccionar otra área o enfocar más de cerca el cartel o menú."
                    } else {
                        "No text detected in the selected area. Please select another area or focus closer to the sign or menu."
                    }
                }
            }
            .addOnFailureListener { e ->
                isProcessingOcr = false
                ocrErrorMessage = if (isEs) {
                    "Error al analizar la imagen: ${e.localizedMessage}"
                } else {
                    "Error analyzing image: ${e.localizedMessage}"
                }
            }
    }

    fun runMultiZoneOcr(zoneIds: Set<String>) {
        val orig = originalCapturedBitmap ?: return
        isProcessingOcr = true
        ocrErrorMessage = null
        ocrOnlineTranslation = null
        ocrMultiZoneResults = emptyList()

        val standardZones = OfflineTranslatorEngine.getStandardMultiZones().filter { it.id in zoneIds }
        if (standardZones.isEmpty()) {
            return
        }

        val collectedResults = mutableListOf<Pair<String, com.laralbir.japantraveltools.feature.ai.CameraOcrResult>>()
        val allTexts = StringBuilder()
        var completedCount = 0

        standardZones.forEach { zone ->
            val bounds = OfflineTranslatorEngine.calculateCropBounds(
                orig.width, orig.height,
                zone.leftFraction, zone.topFraction,
                zone.rightFraction, zone.bottomFraction
            )
            val cropped = Bitmap.createBitmap(orig, bounds.left, bounds.top, bounds.width, bounds.height)
            val inputImage = InputImage.fromBitmap(cropped, 0)
            ocrRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    completedCount++
                    val text = visionText.text.trim()
                    if (text.isNotBlank()) {
                        val zoneResult = OfflineTranslatorEngine.translateOcrJapaneseText(text, isEs)
                        val label = if (isEs) zone.labelEs else zone.labelEn
                        collectedResults.add(label to zoneResult)
                        if (allTexts.isNotEmpty()) allTexts.append("\n---\n")
                        allTexts.append(text)
                    }
                    if (completedCount == standardZones.size) {
                        isProcessingOcr = false
                        ocrMultiZoneResults = collectedResults.toList()
                        if (collectedResults.isNotEmpty()) {
                            ocrRawText = allTexts.toString()
                            if (prefs?.forceOfflineMode != true) {
                                isProcessingOnlineOcr = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val online = OnlineTranslatorClient.translate(allTexts.toString(), "ja", if (isEs) "es" else "en")
                                    withContext(Dispatchers.Main) {
                                        isProcessingOnlineOcr = false
                                        if (!online.isNullOrBlank()) {
                                            ocrOnlineTranslation = online
                                        }
                                    }
                                }
                            }
                        } else {
                            ocrErrorMessage = if (isEs) {
                                "No se detectó texto en ninguna de las zonas seleccionadas."
                            } else {
                                "No text detected in any of the selected zones."
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == standardZones.size) {
                        isProcessingOcr = false
                        ocrMultiZoneResults = collectedResults.toList()
                    }
                }
        }
    }

    fun applyCropAndRunOcr(
        preset: ImageCropPreset,
        leftFrac: Float = if (preset == ImageCropPreset.CUSTOM) customCropLeft else preset.leftFraction,
        topFrac: Float = if (preset == ImageCropPreset.CUSTOM) customCropTop else preset.topFraction,
        rightFrac: Float = if (preset == ImageCropPreset.CUSTOM) customCropRight else preset.rightFraction,
        bottomFrac: Float = if (preset == ImageCropPreset.CUSTOM) customCropBottom else preset.bottomFraction,
        selectedZones: Set<String> = emptySet()
    ) {
        activeCropPreset = preset
        activeSelectedZones = selectedZones
        if (selectedZones.size > 1) {
            runMultiZoneOcr(selectedZones)
            return
        }
        ocrMultiZoneResults = emptyList()
        if (preset == ImageCropPreset.CUSTOM) {
            customCropLeft = leftFrac
            customCropTop = topFrac
            customCropRight = rightFrac
            customCropBottom = bottomFrac
        }
        val orig = originalCapturedBitmap ?: return
        val bounds = OfflineTranslatorEngine.calculateCropBounds(
            imageWidth = orig.width,
            imageHeight = orig.height,
            leftFraction = leftFrac,
            topFraction = topFrac,
            rightFraction = rightFrac,
            bottomFraction = bottomFrac
        )
        val cropped = if (bounds.left == 0 && bounds.top == 0 && bounds.width == orig.width && bounds.height == orig.height) {
            orig
        } else {
            Bitmap.createBitmap(orig, bounds.left, bounds.top, bounds.width, bounds.height)
        }
        activeCroppedBitmap = cropped
        val inputImage = InputImage.fromBitmap(cropped, 0)
        processOcrImage(inputImage, cropped)
    }

    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempCameraImageUri
            if (uri != null) {
                val orientedBitmap = decodeAndOrientBitmap(context, uri)
                if (orientedBitmap != null) {
                    originalCapturedBitmap = orientedBitmap
                    applyCropAndRunOcr(ImageCropPreset.FULL)
                }
            }
        }
    }

    val takePhotoPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            originalCapturedBitmap = bitmap
            applyCropAndRunOcr(ImageCropPreset.FULL)
        }
    }

    fun launchNativeCamera() {
        runCatching {
            val photoFile = File(context.cacheDir, "camera_ocr_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraImageUri = uri
            takePictureLauncher.launch(uri)
        }.onFailure {
            runCatching {
                takePhotoPreviewLauncher.launch(null)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchNativeCamera()
        } else {
            ocrErrorMessage = if (isEs) {
                "Permiso de cámara no concedido. Puedes habilitarlo en Ajustes o seleccionar una foto de la galería."
            } else {
                "Camera permission not granted. You can enable it in Settings or select a photo from gallery."
            }
        }
    }

    fun checkAndLaunchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchNativeCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val orientedBitmap = decodeAndOrientBitmap(context, uri)
            if (orientedBitmap != null) {
                originalCapturedBitmap = orientedBitmap
                applyCropAndRunOcr(ImageCropPreset.FULL)
            }
        }
    }

    val translation = remember(phraseInput) {
        OfflineTranslatorEngine.translateToJapanese(phraseInput)
    }
    val japaneseReplyTranslation = remember(japaneseHeardInput) {
        OfflineTranslatorEngine.translateFromJapaneseSpeech(japaneseHeardInput)
    }

    val travelerSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                if (OfflineTranslatorEngine.isJapaneseText(spokenText)) {
                    // Si se detecta voz japonesa en el micrófono del viajero
                    japaneseHeardInput = spokenText
                    customOnlineTranslationJaToEs = null
                    val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech(spokenText)
                    val targetSpeech = if (isEs) reply.spanishSpokenText else reply.englishSpokenText
                    speakOutLoud(targetSpeech, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                    if (prefs?.forceOfflineMode != true) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val online = OnlineTranslatorClient.translate(spokenText, "ja", if (isEs) "es" else "en")
                            if (!online.isNullOrBlank() && online != targetSpeech) {
                                withContext(Dispatchers.Main) {
                                    customOnlineTranslationJaToEs = online
                                }
                            }
                        }
                    }
                } else {
                    // Flujo 2: Habla español -> se captura texto en español -> se traduce al japonés -> se habla el texto traducido en japonés
                    phraseInput = spokenText
                    customOnlineTranslationEsToJa = null
                    val translated = OfflineTranslatorEngine.translateToJapanese(spokenText)
                    speakOutLoud(translated.japaneseText, Locale.JAPAN)
                    if (prefs?.forceOfflineMode != true) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val online = OnlineTranslatorClient.translate(spokenText, if (isEs) "es" else "en", "ja")
                            if (!online.isNullOrBlank() && online != translated.japaneseText) {
                                withContext(Dispatchers.Main) {
                                    customOnlineTranslationEsToJa = online
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val japaneseSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heardJa = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heardJa.isNullOrBlank()) {
                if (!OfflineTranslatorEngine.isJapaneseText(heardJa) && heardJa.any { it.isLetter() }) {
                    // Si se detecta voz en español en el micrófono japonés
                    phraseInput = heardJa
                    customOnlineTranslationEsToJa = null
                    val translated = OfflineTranslatorEngine.translateToJapanese(heardJa)
                    speakOutLoud(translated.japaneseText, Locale.JAPAN)
                    if (prefs?.forceOfflineMode != true) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val online = OnlineTranslatorClient.translate(heardJa, if (isEs) "es" else "en", "ja")
                            if (!online.isNullOrBlank() && online != translated.japaneseText) {
                                withContext(Dispatchers.Main) {
                                    customOnlineTranslationEsToJa = online
                                }
                            }
                        }
                    }
                } else {
                    // Flujo 1: Habla japonés -> se captura texto japonés -> se traduce el texto al español -> se habla el texto traducido en español
                    japaneseHeardInput = heardJa
                    customOnlineTranslationJaToEs = null
                    val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech(heardJa)
                    val targetSpeech = if (isEs) reply.spanishSpokenText else reply.englishSpokenText
                    speakOutLoud(targetSpeech, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                    if (prefs?.forceOfflineMode != true) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val online = OnlineTranslatorClient.translate(heardJa, "ja", if (isEs) "es" else "en")
                            if (!online.isNullOrBlank() && online != targetSpeech) {
                                withContext(Dispatchers.Main) {
                                    customOnlineTranslationJaToEs = online
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SELECTOR DE MODO: VOZ vs TEXTO vs CÁMARA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppOptionButton(
                    text = if (isEs) "🎙️ Voz" else "🎙️ Voice",
                    selected = translatorMode == 0,
                    onClick = { translatorMode = 0 },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    horizontalPadding = 4.dp
                )
                AppOptionButton(
                    text = if (isEs) "✍️ Texto" else "✍️ Text",
                    selected = translatorMode == 1,
                    onClick = { translatorMode = 1 },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    horizontalPadding = 4.dp
                )
                AppOptionButton(
                    text = if (isEs) "📷 Carteles / Cámara" else "📷 Camera / Signs",
                    selected = translatorMode == 2,
                    onClick = { translatorMode = 2 },
                    modifier = Modifier.weight(1.35f),
                    fontSize = 12.sp,
                    horizontalPadding = 4.dp
                )
            }
        }

        when (translatorMode) {
            0 -> {
                // MODO VOZ - TARJETA 1: HABLA JAPONÉS ➔ SE CAPTURA TEXTO JAPONÉS ➔ SE TRADUCE AL ESPAÑOL ➔ SE HABLA EN ESPAÑOL
                item {
                    KotlinConfCard {
                        Text(
                            text = if (isEs) "1. Habla Japonés ➔ Traduce y Habla en Español" else "1. Speaks Japanese ➔ Translates and Speaks in Spanish",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    AppButtonRow(
                        primaryText = if (isEs) "🎙️ Hablar en Japonés" else "🎙️ Speak Japanese",
                        onPrimaryClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                                }
                                japaneseSpeechLauncher.launch(intent)
                            }.onFailure {
                                val spoken = customOnlineTranslationJaToEs ?: if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
                                speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                            }
                        },
                        secondaryText = if (isEs) "🔊 Hablar en Español" else "🔊 Speak in Spanish",
                        onSecondaryClick = {
                            val spoken = customOnlineTranslationJaToEs ?: if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
                            speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = japaneseHeardInput.ifBlank { if (isEs) "Pulsa el micro para hablar en japonés 🇯🇵" else "Tap mic to speak in Japanese 🇯🇵" },
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (japaneseHeardInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            speakOutLoud(japaneseHeardInput, Locale.JAPAN)
                                        }
                                    ) {
                                        Text("🔊", fontSize = 20.sp)
                                    }
                                }
                            }
                            if (japaneseReplyTranslation.romaji.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "🗣️ ${japaneseReplyTranslation.romaji}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))
                            val displayTranslation = customOnlineTranslationJaToEs ?: if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🇪🇸 $displayTranslation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (displayTranslation.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            speakOutLoud(displayTranslation, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                                        }
                                    ) {
                                        Text("🔊", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MODO VOZ - TARJETA 2: HABLA ESPAÑOL ➔ SE CAPTURA TEXTO EN ESPAÑOL ➔ SE TRADUCE AL JAPONÉS ➔ SE HABLA EN JAPONÉS
            item {
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "2. Habla Español ➔ Traduce y Habla en Japonés" else "2. Speaks Spanish ➔ Translates and Speaks in Japanese",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        AppOptionButton(
                            text = if (rotateJapaneseCard180) "Normal" else "🔄 180°",
                            selected = rotateJapaneseCard180,
                            onClick = { rotateJapaneseCard180 = !rotateJapaneseCard180 },
                            modifier = Modifier.width(96.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    AppButtonRow(
                        primaryText = if (isEs) "🎙️ Hablar en Español" else "🎙️ Speak Spanish",
                        onPrimaryClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEs) "es-ES" else "en-US")
                                }
                                travelerSpeechLauncher.launch(intent)
                            }.onFailure {
                                val target = customOnlineTranslationEsToJa ?: translation.japaneseText
                                speakOutLoud(target, Locale.JAPAN)
                            }
                        },
                        secondaryText = if (isEs) "🔊 Hablar en Japonés" else "🔊 Speak in Japanese",
                        onSecondaryClick = {
                            val target = customOnlineTranslationEsToJa ?: translation.japaneseText
                            speakOutLoud(target, Locale.JAPAN)
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .rotate(if (rotateJapaneseCard180) 180f else 0f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = phraseInput.ifBlank { if (isEs) "Pulsa el micro para hablar en español 🇪🇸" else "Tap mic to speak 🇺🇸" },
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            val displayJa = customOnlineTranslationEsToJa ?: translation.japaneseText
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayJa,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (displayJa.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            speakOutLoud(displayJa, Locale.JAPAN)
                                        }
                                    ) {
                                        Text("🔊", fontSize = 20.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "🗣️ ${translation.romaji}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SECCIÓN UNIFICADA AL FINAL: FRASES HABITUALES DE VIAJE EN JAPONÉS (DESDUPLICADAS CON AUDIO)
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "🗣️ Frases Habituales de Viaje en Japonés" else "🗣️ Common Japanese Travel Phrases",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isEs) {
                            "Frases hechas para hablar en japonés con significado en español, lectura fonética y locución por voz."
                        } else {
                            "Ready-made phrases to speak in Japanese with English meaning, phonetic romaji and voice audio."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
                    val categories = remember {
                        listOf("TODAS") + OfflineTranslatorEngine.travelerQuickPhrases.map { it.categoryBadge }.distinct()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = (selectedCategoryFilter == null && category == "TODAS") || selectedCategoryFilter == category
                            AppOptionButton(
                                text = category,
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (category == "TODAS") null else category
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val filteredPhrases = remember(selectedCategoryFilter) {
                        if (selectedCategoryFilter == null) OfflineTranslatorEngine.travelerQuickPhrases
                        else OfflineTranslatorEngine.travelerQuickPhrases.filter { it.categoryBadge == selectedCategoryFilter }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredPhrases.forEach { phrase ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        speakOutLoud(phrase.japaneseText, Locale.JAPAN)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = phrase.categoryBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isEs) phrase.sourceEs else phrase.sourceEn,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = phrase.japaneseText,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "🗣️ ${phrase.romaji}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            speakOutLoud(phrase.japaneseText, Locale.JAPAN)
                                        }
                                    ) {
                                        Text("🔊", fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        1 -> {
                // ==================== MODO 1: TEXTO ====================
                // SECCIÓN / TARJETA DEDICADA: TRADUCTOR DE TEXTO (ESPAÑOL ➔ JAPONÉS)
                item {
                    KotlinConfCard {
                        Text(
                            text = if (isEs) "Traductor de Texto (Español ➔ Japonés)" else "Text Translator (Spanish ➔ Japanese)",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isEs) {
                                "Escribe frases o preguntas en español (o inglés) para traducirlas a caracteres japoneses (Kanji/Kana) con fonética Romaji y locución en audio japonés."
                            } else {
                                "Type phrases or questions in Spanish (or English) to translate into Japanese (Kanji/Kana) with Romaji pronunciation and Japanese audio speech."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = textTranslatorInput,
                            onValueChange = { textTranslatorInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (isEs) "Escribe en español o inglés" else "Type in Spanish or English")
                            },
                            placeholder = {
                                Text(if (isEs) "Ej. ¿Dónde está el baño? / La cuenta, por favor" else "E.g. Where is the restroom? / The bill, please")
                            },
                            trailingIcon = {
                                if (textTranslatorInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            textTranslatorInput = ""
                                            textTranslatedItem = null
                                            textOnlineTranslationJa = null
                                        }
                                    ) {
                                        Text(
                                            "✕",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (textTranslatorInput.isNotBlank()) {
                                        performTextTranslation(textTranslatorInput)
                                    }
                                }
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        AppButtonRow(
                            primaryText = if (isEs) "🇯🇵 Traducir a Japonés" else "🇯🇵 Translate to Japanese",
                            onPrimaryClick = {
                                if (textTranslatorInput.isNotBlank()) {
                                    performTextTranslation(textTranslatorInput)
                                }
                            },
                            secondaryText = if (isEs) "🔊 Escuchar en Japonés" else "🔊 Speak in Japanese",
                            onSecondaryClick = {
                                val target = textOnlineTranslationJa ?: textTranslatedItem?.japaneseText
                                if (!target.isNullOrBlank()) {
                                    speakOutLoud(target, Locale.JAPAN)
                                } else if (textTranslatorInput.isNotBlank()) {
                                    performTextTranslation(textTranslatorInput, autoSpeak = true)
                                }
                            }
                        )

                        // Visualización del resultado con traducción en japonés (Kanji/Kana), pronunciación Romaji y botón de altavoz
                        val displayJa = textOnlineTranslationJa ?: textTranslatedItem?.japaneseText
                        if (!displayJa.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val badge = textTranslatedItem?.categoryBadge
                                        if (!badge.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = badge,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.width(1.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                speakOutLoud(displayJa, Locale.JAPAN)
                                            }
                                        ) {
                                            Text("🔊", fontSize = 20.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = displayJa,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val romaji = textTranslatedItem?.romaji
                                    if (!romaji.isNullOrBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "🗣️ $romaji",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            speakOutLoud(displayJa, Locale.JAPAN)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = if (isEs) "🔊 Escuchar en Japonés" else "🔊 Speak in Japanese",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FRASES RÁPIDAS DE VIAJE PARA INSERTAR Y TRADUCIR
                item {
                    KotlinConfCard {
                        Text(
                            text = if (isEs) "💡 Frases Rápidas para Traducir" else "💡 Quick Phrases to Translate",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isEs) {
                                "Toca cualquier frase para insertarla en el traductor de texto, ver su traducción y escuchar su pronunciación:"
                            } else {
                                "Tap any phrase to insert it into the text translator, view translation and hear pronunciation:"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))

                        var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
                        val categories = remember {
                            listOf("TODAS") + OfflineTranslatorEngine.travelerQuickPhrases.map { it.categoryBadge }.distinct()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                val isSelected = (selectedCategoryFilter == null && category == "TODAS") || selectedCategoryFilter == category
                                AppOptionButton(
                                    text = category,
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategoryFilter = if (category == "TODAS") null else category
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        val filteredPhrases = remember(selectedCategoryFilter) {
                            if (selectedCategoryFilter == null) OfflineTranslatorEngine.travelerQuickPhrases
                            else OfflineTranslatorEngine.travelerQuickPhrases.filter { it.categoryBadge == selectedCategoryFilter }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredPhrases.forEach { phrase ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            textTranslatorInput = phrase.sourceEs
                                            performTextTranslation(phrase.sourceEs, autoSpeak = true)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Text(
                                                    text = phrase.categoryBadge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isEs) phrase.sourceEs else phrase.sourceEn,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = phrase.japaneseText,
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "🗣️ ${phrase.romaji}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                speakOutLoud(phrase.japaneseText, Locale.JAPAN)
                                            }
                                        ) {
                                            Text("🔊", fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // ==================== MODO 2: CÁMARA & CARTELES ====================
                // MODO CÁMARA & CARTELES - CARD 1: CAPTURA Y FOTO
                item {
                    KotlinConfCard {
                        Text(
                            text = if (isEs) "📷 Traductor de Carteles y Menús" else "📷 Sign & Menu Camera Translator",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isEs) {
                                "Reconoce caracteres japoneses (Kanji, Hiragana, Katakana), platos, precios en Yenes y carteles 100% offline."
                            } else {
                                "Recognizes Japanese text (Kanji, Hiragana, Katakana), dish names, Yen prices, and station signs 100% offline."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        AppButtonRow(
                            primaryText = if (isEs) "📷 Abrir Cámara" else "📷 Open Camera",
                            onPrimaryClick = {
                                checkAndLaunchCamera()
                            },
                            secondaryText = if (isEs) "🖼️ Elegir Imagen" else "🖼️ Pick Image",
                            onSecondaryClick = {
                                runCatching {
                                    pickImageLauncher.launch("image/*")
                                }
                            }
                        )

                        val currentPhoto = activeCroppedBitmap ?: originalCapturedBitmap
                        if (currentPhoto != null) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = currentPhoto.asImageBitmap(),
                                        contentDescription = "Foto capturada",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    // Acciones directas sobre la foto: Girar 90°, Recortar y Quitar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AppOptionButton(
                                            text = if (isEs) "🔄 Girar 90°" else "🔄 Rotate 90°",
                                            selected = false,
                                            onClick = {
                                                val orig = originalCapturedBitmap
                                                if (orig != null) {
                                                    val rotated = rotateBitmap(orig, 90f)
                                                    originalCapturedBitmap = rotated
                                                    applyCropAndRunOcr(activeCropPreset)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            fontSize = 12.sp,
                                            horizontalPadding = 6.dp
                                        )
                                        AppOptionButton(
                                            text = if (isEs) "✂️ Recortar Área" else "✂️ Crop Area",
                                            selected = activeCropPreset != ImageCropPreset.FULL,
                                            onClick = {
                                                showRegionCropDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            fontSize = 12.sp,
                                            horizontalPadding = 6.dp
                                        )
                                        AppOptionButton(
                                            text = if (isEs) "🗑️ Quitar" else "🗑️ Clear",
                                            selected = false,
                                            onClick = {
                                                originalCapturedBitmap = null
                                                activeCroppedBitmap = null
                                                activeCropPreset = ImageCropPreset.FULL
                                            },
                                            modifier = Modifier.weight(0.8f),
                                            fontSize = 12.sp,
                                            horizontalPadding = 6.dp
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Selector rápido de presets de área a traducir
                                    Text(
                                        text = if (isEs) "Zona a traducir:" else "Zone to translate:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ImageCropPreset.entries.forEach { preset ->
                                            val isSelected = activeCropPreset == preset
                                            AppOptionButton(
                                                text = if (isEs) preset.labelEs else preset.labelEn,
                                                selected = isSelected,
                                                onClick = {
                                                    if (preset == ImageCropPreset.CUSTOM) {
                                                        showRegionCropDialog = true
                                                    } else {
                                                        applyCropAndRunOcr(preset)
                                                    }
                                                },
                                                fontSize = 11.5.sp,
                                                horizontalPadding = 6.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    if (isProcessingOcr) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isEs) "Analizando caracteres japoneses..." else "Analyzing Japanese characters...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (ocrErrorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ocrErrorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // MODO CÁMARA & CARTELES - CARD 2: RESULTADO DE LA TRADUCCIÓN
            item {
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "🇯🇵 Traducción Detectada" else "🇯🇵 Detected Translation",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (ocrResult.lines.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${ocrResult.lines.size} ${if (isEs) "líneas" else "lines"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (ocrMultiZoneResults.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ocrMultiZoneResults.forEach { (zoneLabel, zoneRes) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📍 $zoneLabel",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    zoneRes.lines.forEach { line ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = line.japaneseText,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (line.romaji.isNotBlank()) {
                                                    Spacer(Modifier.height(3.dp))
                                                    Text(
                                                        text = "🗣️ ${line.romaji}",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "✅ ${line.translatedText}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (ocrResult.lines.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ocrResult.lines.forEach { line ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = line.japaneseText,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (line.romaji.isNotBlank()) {
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "🗣️ ${line.romaji}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "✅ ${line.translatedText}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ocrResult.fullTranslation,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (isProcessingOnlineOcr) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isEs) "🌐 Obteniendo traducción online contextual..." else "🌐 Fetching contextual online translation...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (ocrOnlineTranslation != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEs) "🌐 Traducción Online Contextual" else "🌐 Contextual Online Translation",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            speakOutLoud(
                                                ocrOnlineTranslation!!,
                                                if (isEs) Locale.forLanguageTag("es-ES") else Locale.US
                                            )
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("🔊", fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = ocrOnlineTranslation!!,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (ocrResult.lines.isNotEmpty() || ocrMultiZoneResults.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))

                        AppButtonRow(
                            primaryText = if (isEs) "🔊 Oír en Español" else "🔊 Hear in English",
                            onPrimaryClick = {
                                val textToSpeak = ocrOnlineTranslation ?: if (ocrMultiZoneResults.isNotEmpty()) {
                                    ocrMultiZoneResults.flatMap { it.second.lines }.joinToString(". ") { it.translatedText }
                                } else {
                                    ocrResult.lines.joinToString(". ") { it.translatedText }
                                }
                                speakOutLoud(
                                    textToSpeak,
                                    if (isEs) Locale.forLanguageTag("es-ES") else Locale.US
                                )
                            },
                            secondaryText = if (isEs) "🔊 Oír en Japonés" else "🔊 Hear Japanese",
                            onSecondaryClick = {
                                val jaToSpeak = if (ocrMultiZoneResults.isNotEmpty()) {
                                    ocrMultiZoneResults.flatMap { it.second.lines }.joinToString(" ") { it.japaneseText }
                                } else {
                                    ocrResult.lines.joinToString(" ") { it.japaneseText }
                                }
                                speakOutLoud(
                                    jaToSpeak,
                                    Locale.JAPAN
                                )
                            }
                        )
                    }
                }
            }

            // MODO CÁMARA & CARTELES - CARD 3: EJEMPLOS DE CARTELES TÍPICOS
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "💡 Ejemplos de Carteles Japoneses:" else "💡 Sample Japanese Signs:",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) {
                            "Pulsa cualquiera para probar al instante la traducción de platos, precios y carteles de estaciones:"
                        } else {
                            "Tap any sample to instantly test offline translation for dishes, prices and signs:"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfflineTranslatorEngine.sampleJapaneseSigns.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { sample ->
                                    AppOptionButton(
                                        text = sample.label,
                                        selected = ocrRawText == sample.sampleJapaneseOcr,
                                        onClick = {
                                            originalCapturedBitmap = null
                                            activeCroppedBitmap = null
                                            activeCropPreset = ImageCropPreset.FULL
                                            ocrErrorMessage = null
                                            ocrRawText = sample.sampleJapaneseOcr
                                            speakOutLoud(
                                                OfflineTranslatorEngine.translateOcrJapaneseText(sample.sampleJapaneseOcr, isEs)
                                                    .lines.firstOrNull()?.translatedText ?: "",
                                                if (isEs) Locale.forLanguageTag("es-ES") else Locale.US
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // MODO CÁMARA & CARTELES - CARD 4: MODO CÁMARA EN VIVO / GOOGLE LENS
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "🌐 Cámara en Vivo / Google Lens" else "🌐 Live Camera / Google Lens",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) {
                            "Para realidad aumentada con superposición en tiempo real, puedes abrir directamente Google Lens, Google Traductor o la cámara del sistema:"
                        } else {
                            "For live augmented reality overlay translation, you can directly launch Google Lens, Google Translate, or the system camera:"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    AppButtonRow(
                        primaryText = if (isEs) "🌐 Abrir Google Lens" else "🌐 Open Google Lens",
                        onPrimaryClick = {
                            val lensIntent = context.packageManager.getLaunchIntentForPackage("com.google.ar.lens")
                                ?: Intent(Intent.ACTION_VIEW).apply {
                                    setPackage("com.google.ar.lens")
                                    data = Uri.parse("googlelens://v1")
                                }
                            runCatching {
                                context.startActivity(lensIntent)
                            }.onFailure {
                                val gTranslateIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.translate")
                                if (gTranslateIntent != null) {
                                    runCatching { context.startActivity(gTranslateIntent) }
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                                    }.onFailure {
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://translate.google.com/?sl=ja&tl=es&op=images")
                                        )
                                        runCatching { context.startActivity(webIntent) }
                                    }
                                }
                            }
                        },
                        secondaryText = if (isEs) "🌐 Google Traductor" else "🌐 Google Translate",
                        onSecondaryClick = {
                            val gTranslateIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.translate")
                                ?: Intent(Intent.ACTION_VIEW).apply {
                                    setPackage("com.google.android.apps.translate")
                                    data = Uri.parse("https://translate.google.com")
                                }
                            runCatching {
                                context.startActivity(gTranslateIntent)
                            }.onFailure {
                                val webIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://translate.google.com/?sl=ja&tl=es&op=images")
                                )
                                runCatching { context.startActivity(webIntent) }
                            }
                        }
                    )
                }
            }
        }
    }
}

    if (showRegionCropDialog && originalCapturedBitmap != null) {
        SelectImageRegionDialog(
            bitmap = originalCapturedBitmap!!,
            initialPreset = activeCropPreset,
            initialLeft = customCropLeft,
            initialTop = customCropTop,
            initialRight = customCropRight,
            initialBottom = customCropBottom,
            initialSelectedZones = activeSelectedZones,
            isEs = isEs,
            onDismiss = { showRegionCropDialog = false },
            onConfirm = { preset, l, t, r, b, zones ->
                showRegionCropDialog = false
                applyCropAndRunOcr(preset, l, t, r, b, zones)
            }
        )
    }
}

// ============================================================================
// 7. AI TRAVEL ASSISTANT SCREEN (SMART ANSWERS + ROUTE BUILDER)
// ============================================================================
@Composable
private fun AiAssistantScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository,
    onNavigateToPlanner: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var geminiPrompt by rememberSaveable { mutableStateOf("") }
    var assistantAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantSource by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantErrorDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantTime by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var actionStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var activeQueryJob by remember { mutableStateOf<Job?>(null) }

    fun performQuery(promptToQuery: String) {
        val clean = promptToQuery.trim()
        if (clean.isBlank() || isLoading) return
        keyboardController?.hide()
        focusManager.clearFocus()
        isLoading = true
        actionStatusMessage = null
        assistantErrorDetail = null
        activeQueryJob?.cancel()
        activeQueryJob = coroutineScope.launch {
            try {
                val result = GeminiAssistantEngine.queryAssistant(
                    prompt = clean,
                    apiKey = prefs.geminiApiKey,
                    eurToJpyRate = 164.80,
                    currentPlan = portablePlan,
                    modelId = prefs.geminiModelId,
                    isEs = isEs
                )
                assistantAnswer = result.answer
                assistantSource = result.source.name
                assistantErrorDetail = result.cloudErrorDetail
                assistantTime = result.timestampFormatted
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelQuery() {
        activeQueryJob?.cancel()
        activeQueryJob = null
        isLoading = false
        actionStatusMessage = if (isEs) "🛑 Consulta cancelada por el usuario" else "🛑 Query cancelled by user"
    }

    val quickTopics = listOf(
        "💳 Tarjeta Suica" to "¿Cómo funciona la Suica y el metro?",
        "🚄 Shinkansen" to "¿Cómo reservar Shinkansen y maletas grandes?",
        "🍜 Carta Ramen ¥" to "Traducir carta de Ramen con precios en ¥",
        "🛍️ Tax-Free" to "¿Cómo funciona el Tax-Free en Don Quijote y pasaporte?",
        "📶 eSIM / Wi-Fi" to "¿Cómo tener internet en Japón con eSIM o Pocket Wi-Fi?",
        "🚫 Propinas y Modales" to "¿Hay que dejar propina en restaurantes de Japón?",
        "♨️ Normas Onsen" to "¿Puedo entrar a un onsen si tengo tatuajes?",
        "🚅 JR Pass" to "¿Merece la pena comprar el JR Pass nacional?",
        "🔌 Enchufes y Voltaje" to "¿Qué enchufes y voltaje se usan en Japón?",
        "🦌 Ruta Nara" to "Añadir excursión a Nara y ciervos",
        "🗻 Ruta Hakone" to "Añadir excursión a Hakone y Monte Fuji",
        "🐙 Ruta Osaka" to "Añadir día en Osaka y Dotonbori"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Asistente IA de Viaje a Japón" else "Japan AI Travel Assistant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEs) {
                        "Haz cualquier pregunta sobre transportes, etiqueta, compras o excursiones. Funciona con Google Gemini en la nube o con el motor experto 100% offline."
                    } else {
                        "Ask anything about transport, etiquette, shopping, or day trips. Works with cloud Gemini or 100% offline expert engine."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Engine mode & Settings link banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasKey = !prefs.geminiApiKey.isNullOrBlank()
                    val modelName = remember(prefs.geminiModelId) {
                        com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper.resolveModelName(prefs.geminiModelId)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (hasKey) {
                                "✨ $modelName (Cloud)"
                            } else {
                                if (isEs) "⚡ Motor Experto (100% Offline)" else "⚡ Expert Engine (Offline)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(onClick = onNavigateToSettings) {
                        Text(
                            text = if (isEs) "⚙️ Configurar en Ajustes" else "⚙️ Configure in Settings",
                            fontSize = 13.sp
                        )
                    }
                }

                // Direct Model Tier Switcher: Flash vs PRO
                val currentIsPro = com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper.isProModel(prefs.geminiModelId)
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "Nivel de Inteligencia:" else "Intelligence Tier:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (currentIsPro) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (currentIsPro) {
                                    if (isEs) "🧠 PRO ACTIVO" else "🧠 PRO ACTIVE"
                                } else {
                                    if (isEs) "⚡ FLASH ACTIVO" else "⚡ FLASH ACTIVE"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentIsPro) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !currentIsPro,
                            onClick = {
                                prefsRepository.setGeminiModelId(com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper.FLASH_DEFAULT_MODEL_ID)
                            },
                            leadingIcon = { Text("⚡", fontSize = 13.sp) },
                            label = {
                                Text(
                                    text = if (isEs) "Flash (Rápido)" else "Flash (Fast)",
                                    fontSize = 12.sp,
                                    fontWeight = if (!currentIsPro) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentIsPro,
                            onClick = {
                                prefsRepository.setGeminiModelId(com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper.PRO_DEFAULT_MODEL_ID)
                            },
                            leadingIcon = { Text("🧠", fontSize = 13.sp) },
                            label = {
                                Text(
                                    text = if (isEs) "PRO (Razonamiento)" else "PRO (Reasoning)",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentIsPro) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (currentIsPro) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.laralbir.japantraveltools.feature.ai.GeminiApiKeyHelper.getProModels().forEach { proOpt ->
                                val isSelected = prefs.geminiModelId == proOpt.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { prefsRepository.setGeminiModelId(proOpt.id) },
                                    label = { Text(proOpt.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "Temas Rápidos Populares:" else "Popular Quick Topics:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickTopics.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { (shortLabel, fullQuery) ->
                                AppOptionButton(
                                    text = shortLabel,
                                    selected = geminiPrompt == fullQuery,
                                    onClick = {
                                        geminiPrompt = fullQuery
                                        performQuery(fullQuery)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = geminiPrompt,
                    onValueChange = { geminiPrompt = it },
                    label = {
                        Text(
                            text = if (isEs) "Escribe tu consulta o tema de viaje..." else "Type your travel question...",
                            fontSize = 15.sp
                        )
                    },
                    trailingIcon = {
                        if (geminiPrompt.isNotBlank()) {
                            IconButton(onClick = { geminiPrompt = "" }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (geminiPrompt.isNotBlank() && !isLoading) {
                                performQuery(geminiPrompt)
                            }
                        }
                    ),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                if (isLoading) {
                    OutlinedButton(
                        onClick = { cancelQuery() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isEs) "❌ Cancelar consulta en curso" else "❌ Cancel query in progress",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { performQuery(geminiPrompt) },
                            enabled = geminiPrompt.isNotBlank(),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(if (isEs) "✨ Consultar IA" else "✨ Ask AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (geminiPrompt.isNotBlank()) {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    val updated = AiPlanBuilderEngine.improveAndExpandPlan(
                                        currentPlan = portablePlan,
                                        userInstruction = geminiPrompt
                                    )
                                    SharedTravelPlanStore.updatePlan(updated)
                                    assistantAnswer = updated.aiOptimizationSummary
                                    assistantSource = AssistantSource.OFFLINE_EXPERT.name
                                    assistantTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                    actionStatusMessage = if (isEs) "🗓️ Itinerario actualizado con paradas" else "🗓️ Route updated with stops"
                                }
                            },
                            enabled = geminiPrompt.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEs) "🗓️ Aplicar" else "🗓️ Apply", fontSize = 14.sp)
                        }
                    }
                }

                if (actionStatusMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = actionStatusMessage ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Answer / Loading / Greeting display
                if (isLoading) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Consultando al Asistente IA..." else "Consulting AI Assistant...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (prefs.geminiApiKey.isNullOrBlank()) {
                                            if (isEs) "Consultando motor experto de viaje a Japón (100% offline)..." else "Querying offline Japan expert engine..."
                                        } else {
                                            if (isEs) "Conectando con Google Gemini Cloud..." else "Connecting to Google Gemini Cloud..."
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { cancelQuery() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isEs) "❌ Cancelar consulta" else "❌ Cancel query", fontSize = 13.sp)
                            }
                        }
                    }
                } else if (assistantAnswer != null) {
                    val currentAnswer = assistantAnswer ?: ""
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (assistantSource == AssistantSource.GEMINI_CLOUD.name) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = if (assistantSource == AssistantSource.GEMINI_CLOUD.name) {
                                            "✨ Gemini Cloud"
                                        } else {
                                            "⚡ Experto Japón Offline"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (assistantSource == AssistantSource.GEMINI_CLOUD.name) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (assistantTime != null) {
                                    Text(
                                        text = "🕐 $assistantTime",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (assistantErrorDetail != null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("⚠️", fontSize = 16.sp)
                                            Text(
                                                text = if (isEs) "Fallo al conectar con Gemini Cloud" else "Failed to connect to Gemini Cloud",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = assistantErrorDetail!!,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isEs) {
                                                "ℹ️ Se muestra a continuación la respuesta del Motor Experto Offline como respaldo."
                                            } else {
                                                "ℹ️ Showing response from Offline Expert Engine as fallback."
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            MarkdownText(
                                markdown = currentAnswer,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))

                            // Action buttons: Copy, Save to Guides, Apply to Planner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("IA Japón", currentAnswer))
                                        actionStatusMessage = if (isEs) "📋 Respuesta copiada al portapapeles" else "📋 Copied to clipboard"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEs) "📋 Copiar" else "📋 Copy", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val guideTitle = geminiPrompt.take(40).ifBlank { "Consejo IA Japón" }
                                        val guide = TravelGuideArticle(
                                            id = "guide-ai-${System.currentTimeMillis()}",
                                            title = guideTitle,
                                            cityRegion = "Japón",
                                            tags = listOf("IA", "Consejos", "Offline"),
                                            markdownSummary = currentAnswer,
                                            isPinned = true
                                        )
                                        SharedDocumentsAndGuidesStore.upsertGuide(guide)
                                        actionStatusMessage = if (isEs) "📖 ¡Guardado en Bóveda de Guías!" else "📖 Saved to Travel Guides!"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEs) "📖 Guardar Guía" else "📖 Save Guide", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🇯🇵 ¡Hola! Soy tu Asistente IA de Viaje a Japón.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isEs) {
                                    "Toca cualquiera de los temas rápidos arriba o escribe tu duda en el campo de texto (transporte, tarjetas Suica, Shinkansen, tax-free, qué ropa llevar, propinas, normas Onsen o precios en yenes)."
                                } else {
                                    "Tap any quick topic above or type your question (transport, Suica, Shinkansen, tax-free, clothing, tipping, onsen rules, or yen prices)."
                                },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                AppSecondaryButton(
                    text = if (isEs) "🗓️ Abrir Planificador de Viaje" else "🗓️ Open Trip Planner",
                    onClick = onNavigateToPlanner,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// 8. PASSPORT & CUSTOMS SCREEN (REAL BIOMETRIC PROMPT + REAL PHOTO VAULT)
// ============================================================================
@Composable
private fun PassportAndCustomsScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricAvailable = remember(context) {
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    val isLockMandatory = VaultSecurityPolicy.isLockMandatory(biometricAvailable)

    var isLaunchingExternalPicker by rememberSaveable { mutableStateOf(false) }

    // Marca temporal de sesión activa en rememberSaveable para evitar rebloqueos al rotar o invocar cámara/SAF
    var lastUnlockTimestamp by rememberSaveable {
        mutableStateOf(
            if (VaultSecurityPolicy.shouldStartUnlocked(biometricAvailable)) System.currentTimeMillis() else 0L
        )
    }

    val isSessionValid = remember(lastUnlockTimestamp, isLockMandatory) {
        !isLockMandatory || !VaultSecurityPolicy.isSessionExpired(lastUnlockTimestamp, System.currentTimeMillis())
    }
    val vaultUnlocked = isSessionValid
    var authErrorText by remember { mutableStateOf<String?>(null) }
    var refreshPhotoCounter by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isLockMandatory) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isLockMandatory && !isLaunchingExternalPicker) {
                    lastUnlockTimestamp = 0L
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isLockMandatory && !isLaunchingExternalPicker) {
                lastUnlockTimestamp = 0L
            }
        }
    }

    // Auto-bloqueo por tiempo prudencial de seguridad (120 segundos)
    LaunchedEffect(lastUnlockTimestamp, isLockMandatory) {
        if (vaultUnlocked && isLockMandatory && lastUnlockTimestamp > 0L) {
            val elapsed = System.currentTimeMillis() - lastUnlockTimestamp
            val remaining = (VaultSecurityPolicy.AUTO_LOCK_TIMEOUT_MILLIS - elapsed).coerceAtLeast(0L)
            delay(remaining)
            lastUnlockTimestamp = 0L
        }
    }

    val customDocs by SharedDocumentsAndGuidesStore.customIdDocsState.collectAsState()
    var editingCustomDoc by remember { mutableStateOf<CustomIdDocumentItem?>(null) }
    var isCreatingCustomDoc by remember { mutableStateOf(false) }

    fun launchRealBiometricPrompt(
        onSuccess: () -> Unit = {
            lastUnlockTimestamp = System.currentTimeMillis()
            authErrorText = null
        },
        onError: ((String) -> Unit)? = null
    ) {
        if (activity == null) {
            onSuccess()
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authErrorText = null
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authErrorText = errString.toString()
                    onError?.invoke(errString.toString())
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isEs) "Bóveda de Pasaporte y DNI" else "Passport & ID Vault")
            .setSubtitle(
                if (isEs) "Autentícate con tu huella, rostro o PIN del dispositivo"
                else "Authenticate with fingerprint, face or device PIN"
            )
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(isLockMandatory, vaultUnlocked) {
        if (isLockMandatory && !vaultUnlocked && !isLaunchingExternalPicker) {
            launchRealBiometricPrompt()
        }
    }

    if (isCreatingCustomDoc || editingCustomDoc != null) {
        CustomIdDocumentEditorModalSheet(
            isEs = isEs,
            initialDoc = editingCustomDoc,
            onStartExternalPicker = { isLaunchingExternalPicker = true },
            onEndExternalPicker = { isLaunchingExternalPicker = false },
            onDismiss = {
                isCreatingCustomDoc = false
                editingCustomDoc = null
            },
            onSave = { title, subtitle, fileUri, originalName ->
                val docToEdit = editingCustomDoc
                if (docToEdit != null) {
                    val updated = docToEdit.copy(title = title, subtitle = subtitle)
                    SharedDocumentsAndGuidesStore.upsertCustomIdDoc(updated)
                    if (fileUri != null) {
                        SharedDocumentsAndGuidesStore.updateCustomIdDocumentFile(context, docToEdit.id, fileUri, originalName)
                    }
                    refreshPhotoCounter++
                } else {
                    if (fileUri != null) {
                        SharedDocumentsAndGuidesStore.addCustomIdDocumentWithFile(context, title, subtitle, fileUri, originalName)
                    } else {
                        val newId = "custom_${System.currentTimeMillis()}"
                        val newDoc = CustomIdDocumentItem(
                            id = newId,
                            title = title,
                            subtitle = subtitle,
                            fileName = "${newId}.jpg",
                            createdAtEpochMs = System.currentTimeMillis()
                        )
                        SharedDocumentsAndGuidesStore.upsertCustomIdDoc(newDoc)
                    }
                    refreshPhotoCounter++
                }
                isCreatingCustomDoc = false
                editingCustomDoc = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Bóveda de Documentos e Identidad" else "Identity & Documents Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (isEs) {
                        "Almacenamiento offline privado y seguro en tu dispositivo para pasaportes, DNI y documentos oficiales del viaje."
                    } else {
                        "Private and secure offline storage on your device for passports, national ID, and official travel documents."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

                if (biometricAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔒", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Protección Biométrica Obligatoria" else "Mandatory Biometric Protection",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (isEs) {
                                            "Huella, rostro o PIN requerido para acceder a tus documentos confidenciales"
                                        } else {
                                            "Fingerprint, face or PIN required to access your sensitive documents"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (vaultUnlocked) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEs) "Bóveda desbloqueada (auto-bloqueo en 2 min)" else "Vault unlocked (auto-lock in 2 min)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            lastUnlockTimestamp = 0L
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isEs) "🔒 Bloquear" else "🔒 Lock",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isEs) "Sin Bloqueo de Pantalla Configurado" else "No Device Screen Lock Set",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isEs) {
                                        "Configura huella o PIN en tu dispositivo para proteger tus documentos de viaje."
                                    } else {
                                        "Set up fingerprint or PIN on your device to protect your travel documents."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (!vaultUnlocked && isLockMandatory) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = if (isEs) "🔒 Identificación Bloqueada" else "🔒 ID Vault Locked",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isEs) {
                                    "Por seguridad, tus fotos de Pasaporte, DNI y documentos están protegidas en el almacenamiento privado. Autentícate para verlas."
                                } else {
                                    "Your Passport, ID and documents photos are protected in private storage. Authenticate to view."
                                },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (authErrorText != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ $authErrorText",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            AppPrimaryButton(
                                text = if (isEs) "🔓 Desbloquear con Biometría / PIN" else "🔓 Unlock with Biometrics / PIN",
                                onClick = { launchRealBiometricPrompt() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))

                    // 1. PASSPORT SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🛂 Pasaporte (Página de Datos)" else "🛂 Passport (Data Page)",
                        subtitle = if (isEs) "Foto o PDF listo para mostrar en hoteles y compras Tax-Free" else "Photo or PDF ready for hotels and Tax-Free shops",
                        fileName = "passport_data_page",
                        refreshTrigger = refreshPhotoCounter,
                        onStartExternalPicker = { isLaunchingExternalPicker = true },
                        onEndExternalPicker = { isLaunchingExternalPicker = false },
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )

                    Spacer(Modifier.height(14.dp))

                    // 2. DNI FRONT SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🪪 DNI / ID (Anverso)" else "🪪 National ID (Front)",
                        subtitle = if (isEs) "Copia frontal de tu documento nacional (Foto o PDF)" else "Front copy of your national identity card (Photo or PDF)",
                        fileName = "dni_front",
                        refreshTrigger = refreshPhotoCounter,
                        onStartExternalPicker = { isLaunchingExternalPicker = true },
                        onEndExternalPicker = { isLaunchingExternalPicker = false },
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )

                    Spacer(Modifier.height(14.dp))

                    // 3. DNI BACK SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🪪 DNI / ID (Reverso)" else "🪪 National ID (Back)",
                        subtitle = if (isEs) "Reverso con dirección y datos de expedición (Foto o PDF)" else "Back side with address and expiration (Photo or PDF)",
                        fileName = "dni_back",
                        refreshTrigger = refreshPhotoCounter,
                        onStartExternalPicker = { isLaunchingExternalPicker = true },
                        onEndExternalPicker = { isLaunchingExternalPicker = false },
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )

                    Spacer(Modifier.height(18.dp))

                    // 4. CUSTOM DOCUMENTS SECTION (VACCINES, PRESCRIPTIONS, PERMITS, ETC.)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (isEs) "📋 Documentos Personalizados" else "📋 Custom Documents",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (isEs) {
                                            "Certificados de vacunas, recetas médicas, carnet internacional, seguros o visados"
                                        } else {
                                            "Vaccines, prescriptions, international driving permit, insurance, visas"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { isCreatingCustomDoc = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "➕ Añadir" else "➕ Add",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (customDocs.isEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📄", fontSize = 28.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isEs) "Sin documentos adicionales guardados" else "No additional documents saved",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = if (isEs) {
                                                "Añade cualquier documento oficial con foto o PDF (ej. certificado de vacunas, receta médica para aduanas japonesas, carnet internacional de conducir, póliza de seguro o visado) con el botón de añadir arriba."
                                            } else {
                                                "Add any official photo or PDF document (e.g. vaccine certificate, prescription for Japanese customs, international permit, insurance policy or visa) with the add button above."
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    customDocs.forEach { customDoc ->
                                        CustomIdDocumentItemCard(
                                            isEs = isEs,
                                            item = customDoc,
                                            refreshTrigger = refreshPhotoCounter,
                                            onStartExternalPicker = { isLaunchingExternalPicker = true },
                                            onEndExternalPicker = { isLaunchingExternalPicker = false },
                                            onEdit = { editingCustomDoc = customDoc },
                                            onDelete = {
                                                SharedDocumentsAndGuidesStore.deleteCustomIdDoc(customDoc.id, context)
                                                refreshPhotoCounter++
                                            },
                                            onPhotoChanged = { refreshPhotoCounter++ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        }
    }
    return uri.lastPathSegment
}

@Composable
private fun IdDocumentPhotoSection(
    isEs: Boolean,
    title: String,
    subtitle: String,
    fileName: String,
    refreshTrigger: Int,
    onStartExternalPicker: () -> Unit = {},
    onEndExternalPicker: () -> Unit = {},
    onPhotoChanged: () -> Unit
) {
    val context = LocalContext.current
    val docFile = remember(fileName, refreshTrigger) {
        SharedDocumentsAndGuidesStore.getIdDocumentFile(context, fileName)
    }
    val isPdf = remember(docFile) {
        SharedDocumentsAndGuidesStore.isPdfDocumentFile(docFile)
    }
    val photoBitmap = remember(docFile, isPdf) {
        if (!isPdf) {
            docFile?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }.getOrNull() }
        } else null
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        onEndExternalPicker()
        if (success && pendingCameraUri != null) {
            SharedDocumentsAndGuidesStore.saveIdDocument(context, pendingCameraUri!!, fileName, "camera.jpg")
            onPhotoChanged()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onEndExternalPicker()
        if (uri != null) {
            val originalName = queryFileName(context, uri)
            SharedDocumentsAndGuidesStore.saveIdDocument(context, uri, fileName, originalName)
            onPhotoChanged()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onEndExternalPicker()
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val originalName = queryFileName(context, uri) ?: "document.pdf"
            SharedDocumentsAndGuidesStore.saveIdDocument(context, uri, fileName, originalName)
            onPhotoChanged()
        }
    }

    fun launchCamera() {
        onStartExternalPicker()
        runCatching {
            val tempFile = File(context.cacheDir, "camera_id_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { onEndExternalPicker() }
    }

    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    var isViewingFullScreen by remember { mutableStateOf(false) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    if (photoBitmap != null && isViewingFullScreen) {
        FullScreenZoomableImageViewerDialog(
            title = title,
            bitmap = photoBitmap,
            isEs = isEs,
            onDismiss = { isViewingFullScreen = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (isPdf && docFile != null && docFile.exists() && docFile.length() > 0L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStartExternalPicker()
                            openDocumentFile(context, docFile.absolutePath)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📄", fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEs) "Documento PDF Adjunto" else "Attached PDF Document",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            val sizeKb = (docFile.length() / 1024L).coerceAtLeast(1L)
                            Text(
                                text = "${docFile.name} · ${sizeKb} KB · " + if (isEs) "Toca para abrir ↗️" else "Tap to open ↗️",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("↗️", fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "🖼️ Galería" else "🖼️ Gallery", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            pdfPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📄 Cambiar" else "📄 Change", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                                itemName = title,
                                itemTypeEs = "documento de identidad",
                                itemTypeEn = "identity document",
                                isEs = isEs,
                                onConfirm = {
                                    SharedDocumentsAndGuidesStore.deleteIdDocument(context, fileName)
                                    onPhotoChanged()
                                }
                            )
                        }
                    ) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            } else if (photoBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clickable { isViewingFullScreen = true }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🔍", fontSize = 13.sp)
                                Text(
                                    text = if (isEs) "Toca para pantalla completa y zoom" else "Tap for full screen & zoom",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "🖼️ Galería" else "🖼️ Gallery", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            pdfPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📄 PDF" else "📄 PDF", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                                itemName = title,
                                itemTypeEs = "foto de identidad oficial",
                                itemTypeEn = "official identity photo",
                                isEs = isEs,
                                onConfirm = {
                                    SharedDocumentsAndGuidesStore.deleteIdDocument(context, fileName)
                                    onPhotoChanged()
                                }
                            )
                        }
                    ) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppPrimaryButton(
                        text = if (isEs) "📸 Cámara" else "📸 Camera",
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "🖼️ Galería" else "🖼️ Gallery", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            pdfPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📄 PDF" else "📄 PDF", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomIdDocumentItemCard(
    isEs: Boolean,
    item: CustomIdDocumentItem,
    refreshTrigger: Int,
    onStartExternalPicker: () -> Unit = {},
    onEndExternalPicker: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoChanged: () -> Unit
) {
    val context = LocalContext.current
    val photoFile = remember(item.fileName, refreshTrigger) {
        SharedDocumentsAndGuidesStore.getIdPhotoFile(context, item.fileName)
    }
    val isPdf = item.isPdf
    val photoBitmap = remember(photoFile, isPdf) {
        if (!isPdf) {
            photoFile?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }.getOrNull() }
        } else null
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        onEndExternalPicker()
        if (success && pendingCameraUri != null) {
            SharedDocumentsAndGuidesStore.updateCustomIdDocumentFile(context, item.id, pendingCameraUri!!, "camera.jpg")
            onPhotoChanged()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onEndExternalPicker()
        if (uri != null) {
            val originalName = queryFileName(context, uri)
            SharedDocumentsAndGuidesStore.updateCustomIdDocumentFile(context, item.id, uri, originalName)
            onPhotoChanged()
        }
    }

    fun launchCamera() {
        onStartExternalPicker()
        runCatching {
            val tempFile = File(context.cacheDir, "camera_custom_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { onEndExternalPicker() }
    }

    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    var isViewingFullScreen by remember { mutableStateOf(false) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    if (photoBitmap != null && isViewingFullScreen) {
        FullScreenZoomableImageViewerDialog(
            title = item.title,
            bitmap = photoBitmap,
            isEs = isEs,
            onDismiss = { isViewingFullScreen = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(text = item.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (item.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Text("✏️", fontSize = 16.sp)
                    }
                    IconButton(onClick = {
                        confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                            itemName = item.title,
                            itemTypeEs = "documento de identidad",
                            itemTypeEn = "identity document",
                            isEs = isEs,
                            onConfirm = onDelete
                        )
                    }) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (isPdf && photoFile != null && photoFile.exists() && photoFile.length() > 0L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            openDocumentFile(context, photoFile.absolutePath)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📄", fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEs) "Documento PDF Adjunto" else "Attached PDF Document",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isEs) "Toca para abrir en visor de PDF externo" else "Tap to open in PDF viewer",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("↗️", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📄 Cambiar" else "📄 Change", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                                itemName = item.title,
                                itemTypeEs = "archivo del documento",
                                itemTypeEn = "document file",
                                isEs = isEs,
                                onConfirm = {
                                    SharedDocumentsAndGuidesStore.deleteIdPhoto(context, item.fileName)
                                    onPhotoChanged()
                                }
                            )
                        }
                    ) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            } else if (photoBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clickable { isViewingFullScreen = true }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🔍", fontSize = 13.sp)
                                Text(
                                    text = if (isEs) "Toca para pantalla completa y zoom" else "Tap for full screen & zoom",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStartExternalPicker()
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📄 Cambiar" else "📄 Change", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                                itemName = item.title,
                                itemTypeEs = "foto del documento",
                                itemTypeEn = "document photo",
                                isEs = isEs,
                                onConfirm = {
                                    SharedDocumentsAndGuidesStore.deleteIdPhoto(context, item.fileName)
                                    onPhotoChanged()
                                }
                            )
                        }
                    ) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEs) "Sin foto ni archivo adjunto todavía" else "No photo or file attached yet",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPrimaryButton(
                                text = if (isEs) "📸 Foto Cámara" else "📸 Camera Photo",
                                onClick = { launchCamera() },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = if (isEs) "📄 Adjuntar Archivo/PDF" else "📄 Attach File/PDF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomIdDocumentEditorModalSheet(
    isEs: Boolean,
    initialDoc: CustomIdDocumentItem?,
    onStartExternalPicker: () -> Unit = {},
    onEndExternalPicker: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (title: String, subtitle: String, fileUri: Uri?, originalName: String?) -> Unit
) {
    var titleText by rememberSaveable { mutableStateOf(initialDoc?.title ?: "") }
    var subtitleText by rememberSaveable { mutableStateOf(initialDoc?.subtitle ?: "") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val isNew = initialDoc == null

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        onEndExternalPicker()
        if (success && pendingCameraUri != null) {
            selectedFileUri = pendingCameraUri
            selectedFileName = "camera_capture.jpg"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onEndExternalPicker()
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = queryFileName(context, uri) ?: "document"
        }
    }

    fun launchCamera() {
        onStartExternalPicker()
        runCatching {
            val tempFile = File(context.cacheDir, "camera_edit_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { onEndExternalPicker() }
    }

    val presets = remember(isEs) {
        if (isEs) {
            listOf(
                "💉 Certificado de Vacunas",
                "💊 Receta Médica",
                "🚗 Carnet Internacional",
                "🏥 Seguro Médico",
                "🛂 Visado / QR",
                "📄 Documento Personal"
            )
        } else {
            listOf(
                "💉 Vaccine Certificate",
                "💊 Medical Prescription",
                "🚗 International Driving Permit",
                "🏥 Health Insurance",
                "🛂 Visa / Entry QR",
                "📄 Personal Document"
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Añadir Documento de Identidad" else "➕ Add Identity Document"
                } else {
                    if (isEs) "✏️ Editar Documento" else "✏️ Edit Document"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isEs) {
                    "Guarda certificados de vacunación, recetas médicas para aduanas, carnet internacional de conducir, visados o cualquier otro documento sensible en foto o PDF."
                } else {
                    "Store vaccination certificates, prescriptions for customs, international permits, visas or sensitive documents in photo or PDF."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isEs) "Sugerencias rápidas:" else "Quick suggestions:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val cleanName = preset.substringAfter(" ").trim()
                            titleText = cleanName
                        }
                    ) {
                        Text(
                            text = preset,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEs) "Nombre del Documento *" else "Document Title *") },
                placeholder = { Text(if (isEs) "ej. Certificado Vacunas Fiebre Amarilla" else "e.g. Yellow Fever Vaccine") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subtitleText,
                onValueChange = { subtitleText = it },
                label = { Text(if (isEs) "Notas / Referencia / Caducidad (Opcional)" else "Notes / Expiration (Optional)") },
                placeholder = { Text(if (isEs) "ej. Hospital Carlos III · Válido hasta 2028" else "e.g. Valid until 2028") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isEs) "Foto o Archivo del Documento" else "Document Photo or File",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (selectedFileUri != null) {
                            if (isEs) "✅ Seleccionado: ${selectedFileName ?: "Archivo"}" else "✅ Selected: ${selectedFileName ?: "File"}"
                        } else if (initialDoc != null) {
                            if (isEs) "ℹ️ Archivo actual: ${initialDoc.fileName}" else "ℹ️ Current file: ${initialDoc.fileName}"
                        } else {
                            if (isEs) "Puedes tomar una foto con la cámara o adjuntar PDF / imagen" else "Take a camera photo or attach a PDF / image"
                        },
                        fontSize = 12.sp,
                        color = if (selectedFileUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { launchCamera() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                        }
                        AppPrimaryButton(
                            text = if (isEs) "📄 PDF / Galería" else "📄 PDF / Gallery",
                            onClick = {
                                onStartExternalPicker()
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Documento" else "💾 Save Document",
                onPrimaryClick = {
                    if (titleText.isNotBlank()) {
                        onSave(titleText, subtitleText, selectedFileUri, selectedFileName)
                    }
                },
                secondaryText = if (isEs) "Cancelar" else "Cancel",
                onSecondaryClick = onDismiss,
                primaryEnabled = titleText.isNotBlank()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 9. BOOKINGS & TICKETS SCREEN (WITH TICKETS CATEGORY & FULL CRUD)
// ============================================================================

private fun openDocumentFile(context: android.content.Context, filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when {
            filePath.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            filePath.endsWith(".png", ignoreCase = true) -> "image/png"
            filePath.endsWith(".jpg", ignoreCase = true) || filePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

@Composable
private fun BookingsVaultScreen(isEs: Boolean) {
    val context = LocalContext.current
    val allDocuments by SharedDocumentsAndGuidesStore.documentsState.collectAsState()
    var selectedCategory by rememberSaveable { mutableStateOf<DocumentCategory?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredDocs = remember(allDocuments, selectedCategory, searchQuery) {
        DocumentsFilterUseCase.filterDocuments(allDocuments, selectedCategory, searchQuery)
    }

    var editingDoc by remember { mutableStateOf<TravelDocumentItem?>(null) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var viewingPassDoc by remember { mutableStateOf<TravelDocumentItem?>(null) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    if (viewingPassDoc != null) {
        WalletTicketPassDialog(
            doc = viewingPassDoc!!,
            isEs = isEs,
            onDismiss = { viewingPassDoc = null }
        )
    }

    if (isCreatingDoc || editingDoc != null) {
        val initialDoc = editingDoc ?: TravelDocumentItem(
            id = "doc-${System.currentTimeMillis()}",
            title = "",
            category = selectedCategory ?: DocumentCategory.TICKET,
            referenceOrNotes = ""
        )
        DocumentEditorModalSheet(
            isEs = isEs,
            initialDoc = initialDoc,
            isNew = isCreatingDoc,
            onDismiss = {
                isCreatingDoc = false
                editingDoc = null
            },
            onSave = { saved ->
                SharedDocumentsAndGuidesStore.upsertDocument(saved)
                isCreatingDoc = false
                editingDoc = null
            },
            onDelete = { idToDelete ->
                SharedDocumentsAndGuidesStore.deleteDocument(idToDelete)
                isCreatingDoc = false
                editingDoc = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOptionButton(
                        text = if (isEs) "Todos" else "All",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        modifier = Modifier.width(90.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "🎟️ Entradas (USJ/Ghibli)" else "🎟️ Tickets (USJ/Ghibli)",
                        selected = selectedCategory == DocumentCategory.TICKET,
                        onClick = { selectedCategory = DocumentCategory.TICKET },
                        modifier = Modifier.width(180.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "✈️ Vuelos" else "✈️ Flights",
                        selected = selectedCategory == DocumentCategory.FLIGHT,
                        onClick = { selectedCategory = DocumentCategory.FLIGHT },
                        modifier = Modifier.width(110.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "🏨 Hoteles" else "🏨 Hotels",
                        selected = selectedCategory == DocumentCategory.HOTEL,
                        onClick = { selectedCategory = DocumentCategory.HOTEL },
                        modifier = Modifier.width(110.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "🚄 Pases JR" else "🚄 JR Passes",
                        selected = selectedCategory == DocumentCategory.TRANSPORT,
                        onClick = { selectedCategory = DocumentCategory.TRANSPORT },
                        modifier = Modifier.width(110.dp)
                    )
                }

                // Add button & Search
                AppPrimaryButton(
                    text = if (isEs) "➕ Añadir Entrada o Reserva" else "➕ Add Ticket or Booking",
                    onClick = {
                        editingDoc = null
                        isCreatingDoc = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (isEs) "🔍 Buscar entradas, vuelos, hoteles, QR..." else "🔍 Search tickets, flights, hotels...", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredDocs.isEmpty()) {
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "No hay documentos en esta sección" else "No documents found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) {
                            "Pulsa «➕ Añadir Entrada o Reserva» para guardar tus billetes, entradas de parques o museos."
                        } else {
                            "Tap «➕ Add Ticket or Booking» to store your tickets or reservations."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredDocs, key = { it.id }) { doc ->
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (doc.isPinned) "📌 ${doc.title}" else doc.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        InfoTagPill(text = doc.category.badgeCode)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = doc.referenceOrNotes,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!doc.locatorCode.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "🎫 ${doc.locatorCode}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val attachedPath = doc.fileUri ?: doc.internalUriPath
                    if (attachedPath != null) {
                        Spacer(Modifier.height(8.dp))
                        val isImage = attachedPath.endsWith(".png", ignoreCase = true) ||
                                      attachedPath.endsWith(".jpg", ignoreCase = true) ||
                                      attachedPath.endsWith(".jpeg", ignoreCase = true)
                        if (isImage) {
                            val bitmap = remember(attachedPath) {
                                runCatching { BitmapFactory.decodeFile(attachedPath)?.asImageBitmap() }.getOrNull()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = doc.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (attachedPath != null) {
                            OutlinedButton(
                                onClick = { openDocumentFile(context, attachedPath) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .weight(1.2f)
                            ) {
                                Text(if (isEs) "📄 Archivo" else "📄 File", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        if (!doc.locatorCode.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { viewingPassDoc = doc },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .weight(1.2f)
                            ) {
                                Text(if (isEs) "🎫 Pase QR" else "🎫 Pass QR", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                isCreatingDoc = false
                                editingDoc = doc
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(1.1f)
                        ) {
                            Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 12.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                    itemName = doc.title,
                                    itemTypeEs = "documento",
                                    itemTypeEn = "document",
                                    isEs = isEs,
                                    onConfirm = { SharedDocumentsAndGuidesStore.deleteDocument(doc.id) }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("🗑️", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTicketPassDialog(
    doc: TravelDocumentItem,
    isEs: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoTagPill(
                        text = doc.category.badgeCode,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isEs) "Pase Digital Offline" else "Digital Offline Pass",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = doc.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (doc.referenceOrNotes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = doc.referenceOrNotes,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎫 █║▌║█║▌│║▌║▌█║ 🎫",
                            fontSize = 24.sp,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = doc.locatorCode ?: doc.id,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "💡 Sube el brillo de la pantalla para escanear en tornos o controles" else "💡 Increase screen brightness when scanning at gates",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isEs) "Cerrar Pase" else "Close Pass", fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentEditorModalSheet(
    isEs: Boolean,
    initialDoc: TravelDocumentItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (TravelDocumentItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var titleText by rememberSaveable { mutableStateOf(initialDoc.title) }
    var notesText by rememberSaveable { mutableStateOf(initialDoc.referenceOrNotes) }
    var locatorText by rememberSaveable { mutableStateOf(initialDoc.locatorCode ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(initialDoc.category) }
    var isPinned by rememberSaveable { mutableStateOf(initialDoc.isPinned) }
    var attachedFilePath by rememberSaveable { mutableStateOf(initialDoc.fileUri ?: initialDoc.internalUriPath) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            val saved = SharedDocumentsAndGuidesStore.saveDocumentAttachment(context, pendingCameraUri!!, "ticket_camera.jpg")
            if (saved != null) {
                attachedFilePath = saved
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val originalName = queryFileName(context, uri) ?: uri.lastPathSegment
            val saved = SharedDocumentsAndGuidesStore.saveDocumentAttachment(context, uri, originalName)
            if (saved != null) {
                attachedFilePath = saved
            }
        }
    }

    fun launchCamera() {
        runCatching {
            val tempFile = File(context.cacheDir, "camera_ticket_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Nueva Entrada o Reserva" else "➕ New Ticket or Booking"
                } else {
                    if (isEs) "✏️ Editar Documento / Entrada" else "✏️ Edit Document / Ticket"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEs) "Título (ej. Entrada teamLab / Hotel)" else "Title", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = locatorText,
                onValueChange = { locatorText = it },
                label = { Text(if (isEs) "Localizador / Código QR (Opcional)" else "Locator / QR Code (Optional)", fontSize = 15.sp) },
                placeholder = { Text("ej. PNR-789012 o JR-PASS-2026", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (isEs) "Categoría:" else "Category:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DocumentCategory.TICKET,
                    DocumentCategory.FLIGHT,
                    DocumentCategory.HOTEL,
                    DocumentCategory.TRANSPORT,
                    DocumentCategory.INSURANCE,
                    DocumentCategory.GUIDE
                ).chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { cat ->
                            AppOptionButton(
                                text = if (isEs) cat.labelEs else cat.labelEn,
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(if (isEs) "Notas, horario de entrada o asientos" else "Notes, entry time or seats", fontSize = 15.sp) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "📌 Fijar en la parte superior" else "📌 Pin to top",
                    fontSize = 15.sp
                )
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }

            Text(
                text = if (isEs) "Archivo o Billete Adjunto (PDF o Foto):" else "Attached Ticket/File (PDF or Photo):",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (attachedFilePath != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📎 ${File(attachedFilePath!!).name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        AppOptionButton(
                            text = if (isEs) "🗑️ Quitar" else "🗑️ Remove",
                            selected = false,
                            onClick = { attachedFilePath = null },
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isEs) "📸 Cámara" else "📸 Camera", fontSize = 12.sp)
                    }
                    AppPrimaryButton(
                        text = if (isEs) "📄 PDF / Foto" else "📄 PDF / Photo",
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar" else "💾 Save",
                onPrimaryClick = {
                    val updated = initialDoc.copy(
                        title = titleText.ifBlank { if (isEs) "Documento" else "Document" },
                        referenceOrNotes = notesText,
                        locatorCode = locatorText.trim().ifBlank { null },
                        category = selectedCategory,
                        isPinned = isPinned,
                        fileUri = attachedFilePath,
                        internalUriPath = attachedFilePath
                    )
                    if (isNew) {
                        onSave(updated)
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = updated.title,
                            isEs = isEs,
                            onConfirm = { onSave(updated) }
                        )
                    }
                },
                secondaryText = if (isNew) {
                    if (isEs) "Cancelar" else "Cancel"
                } else {
                    if (isEs) "🗑️ Eliminar" else "🗑️ Delete"
                },
                onSecondaryClick = {
                    if (isNew) {
                        onDismiss()
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = initialDoc.title,
                            itemTypeEs = "documento",
                            itemTypeEn = "document",
                            isEs = isEs,
                            onConfirm = { onDelete(initialDoc.id) }
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 10. TRAVEL GUIDES SCREEN (WITH FULL CRUD)
// ============================================================================
@Composable
private fun TravelGuidesScreen(isEs: Boolean) {
    val guides by SharedDocumentsAndGuidesStore.guidesState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSortOrder by rememberSaveable { mutableStateOf(GuideSortOrder.PINNED_FIRST) }
    var selectedCityFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val filteredGuides = remember(guides, searchQuery, selectedCityFilter, selectedSortOrder) {
        DocumentsFilterUseCase.filterGuides(guides, selectedCityFilter, searchQuery, selectedSortOrder)
    }

    var editingGuide by remember { mutableStateOf<TravelGuideArticle?>(null) }
    var viewingFullGuide by remember { mutableStateOf<TravelGuideArticle?>(null) }
    var isCreatingGuide by remember { mutableStateOf(false) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    if (viewingFullGuide != null) {
        FullGuideReaderDialog(
            guide = viewingFullGuide!!,
            isEs = isEs,
            onDismiss = { viewingFullGuide = null }
        )
    }

    if (isCreatingGuide || editingGuide != null) {
        val initialGuide = editingGuide ?: TravelGuideArticle(
            id = "guide-${System.currentTimeMillis()}",
            title = "",
            cityRegion = "Tokyo",
            tags = listOf("Recomendaciones"),
            markdownSummary = ""
        )
        GuideEditorModalSheet(
            isEs = isEs,
            initialGuide = initialGuide,
            isNew = isCreatingGuide,
            onDismiss = {
                isCreatingGuide = false
                editingGuide = null
            },
            onSave = { saved ->
                SharedDocumentsAndGuidesStore.upsertGuide(saved)
                isCreatingGuide = false
                editingGuide = null
            },
            onDelete = { idToDelete ->
                SharedDocumentsAndGuidesStore.deleteGuide(idToDelete)
                isCreatingGuide = false
                editingGuide = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Sorting & Filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOptionButton(
                        text = if (isEs) "📌 Fijadas" else "📌 Pinned",
                        selected = selectedSortOrder == GuideSortOrder.PINNED_FIRST,
                        onClick = { selectedSortOrder = GuideSortOrder.PINNED_FIRST },
                        modifier = Modifier.width(105.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "🔤 A - Z" else "🔤 A - Z",
                        selected = selectedSortOrder == GuideSortOrder.TITLE_AZ,
                        onClick = { selectedSortOrder = GuideSortOrder.TITLE_AZ },
                        modifier = Modifier.width(95.dp)
                    )
                    AppOptionButton(
                        text = if (isEs) "🏙️ Por Ciudad" else "🏙️ By City",
                        selected = selectedSortOrder == GuideSortOrder.CITY_REGION,
                        onClick = { selectedSortOrder = GuideSortOrder.CITY_REGION },
                        modifier = Modifier.width(125.dp)
                    )
                }

                AppPrimaryButton(
                    text = if (isEs) "➕ Crear Nueva Guía" else "➕ Create New Guide",
                    onClick = {
                        editingGuide = null
                        isCreatingGuide = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (isEs) "🔍 Buscar consejos y guías..." else "🔍 Search guides & tips...", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredGuides.isEmpty()) {
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "No hay guías disponibles" else "No guides found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) "Pulsa «➕ Crear Nueva Guía» para redactar tus propios apuntes offline de Japón."
                        else "Tap «➕ Create New Guide» to write your own offline Japan travel notes.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredGuides, key = { it.id }) { guide ->
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (guide.isPinned) "📌 ${guide.title}" else guide.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        InfoTagPill(text = guide.cityRegion)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = guide.markdownSummary,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewingFullGuide = guide },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(1.3f)
                        ) {
                            Text(if (isEs) "📖 Leer Guía" else "📖 Read Guide", fontSize = 12.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                isCreatingGuide = false
                                editingGuide = guide
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(1.1f)
                        ) {
                            Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 12.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                    itemName = guide.title,
                                    itemTypeEs = "guía de viaje",
                                    itemTypeEn = "travel guide",
                                    isEs = isEs,
                                    onConfirm = { SharedDocumentsAndGuidesStore.deleteGuide(guide.id) }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("🗑️", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullGuideReaderDialog(
    guide: TravelGuideArticle,
    isEs: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoTagPill(text = guide.cityRegion)
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = guide.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (guide.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = guide.tags.joinToString(" · ") { "#$it" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = guide.markdownSummary,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(if (isEs) "Cerrar Lectura" else "Close Reader", fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideEditorModalSheet(
    isEs: Boolean,
    initialGuide: TravelGuideArticle,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (TravelGuideArticle) -> Unit,
    onDelete: (String) -> Unit
) {
    var titleText by rememberSaveable { mutableStateOf(initialGuide.title) }
    var cityText by rememberSaveable { mutableStateOf(initialGuide.cityRegion) }
    var contentText by rememberSaveable { mutableStateOf(initialGuide.markdownSummary) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) {
                    if (isEs) "➕ Crear Guía de Viaje" else "➕ Create Travel Guide"
                } else {
                    if (isEs) "✏️ Editar Guía de Viaje" else "✏️ Edit Travel Guide"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEs) "Título de la guía" else "Guide title", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cityText,
                onValueChange = { cityText = it },
                label = { Text(if (isEs) "Ciudad o región (Tokio, Kioto, Osaka...)" else "City or region", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text(if (isEs) "Consejos, recomendaciones y apuntes" else "Tips & notes", fontSize = 15.sp) },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Guía" else "💾 Save Guide",
                onPrimaryClick = {
                    val updated = initialGuide.copy(
                        title = titleText.ifBlank { if (isEs) "Guía de Japón" else "Japan Guide" },
                        cityRegion = cityText.ifBlank { "Japón" },
                        markdownSummary = contentText
                    )
                    if (isNew) {
                        onSave(updated)
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildEditConfirmation(
                            itemName = updated.title,
                            isEs = isEs,
                            onConfirm = { onSave(updated) }
                        )
                    }
                },
                secondaryText = if (isNew) {
                    if (isEs) "Cancelar" else "Cancel"
                } else {
                    if (isEs) "🗑️ Eliminar" else "🗑️ Delete"
                },
                onSecondaryClick = {
                    if (isNew) {
                        onDismiss()
                    } else {
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = initialGuide.title,
                            itemTypeEs = "guía de viaje",
                            itemTypeEn = "travel guide",
                            isEs = isEs,
                            onConfirm = { onDelete(initialGuide.id) }
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// 11. SETTINGS & SYNC SCREEN
// ============================================================================
@Composable
private fun SettingsPreferencesScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Idioma y Tema Visual" else "Language & Visual Theme",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (isEs) "Idioma de la aplicación" else "App Language",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguagePreference.entries.forEach { lang ->
                        AppOptionButton(
                            text = when (lang) {
                                AppLanguagePreference.SYSTEM -> if (isEs) "Sistema" else "System"
                                AppLanguagePreference.ENGLISH -> "English"
                                AppLanguagePreference.SPANISH -> "Español"
                            },
                            selected = prefs.language == lang,
                            onClick = { prefsRepository.setLanguage(lang) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isEs) "Tema de color" else "Color Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        AppOptionButton(
                            text = when (mode) {
                                AppThemeMode.SYSTEM -> if (isEs) "Sistema" else "System"
                                AppThemeMode.LIGHT -> if (isEs) "☀️ Claro" else "☀️ Light"
                                AppThemeMode.DARK -> if (isEs) "🌙 Oscuro" else "🌙 Dark"
                            },
                            selected = prefs.themeMode == mode,
                            onClick = { prefsRepository.setThemeMode(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            GoogleDriveSyncCard(
                isEs = isEs,
                prefs = prefs,
                prefsRepository = prefsRepository
            )
        }

        item {
            JsonDataPortabilityCard(
                isEs = isEs,
                prefs = prefs,
                prefsRepository = prefsRepository
            )
        }

        item {
            GeminiApiKeySetupCard(
                isEs = isEs,
                prefs = prefs,
                prefsRepository = prefsRepository
            )
        }
    }
}

// ============================================================================
// 12. ABOUT APP SCREEN
// ============================================================================
@Composable
private fun AboutAppScreen(isEs: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = "Japan Travel Tools · v${OpenSourceLibrariesCatalog.APP_VERSION_NAME} (build ${OpenSourceLibrariesCatalog.APP_VERSION_CODE})",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                OpenSourceLibrariesCatalog.thirdPartyLibraries.forEach { lib ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "${lib.name} (${lib.version})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${lib.artifactId} · ${lib.license}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(text = if (isEs) lib.purposeEs else lib.purposeEn, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: JSON PORTABILITY, AI ROUTE BUILDER & DEMO DATA MANAGER
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonPortabilityModalSheet(
    isEs: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPlan by SharedTravelPlanStore.planState.collectAsState()
    var jsonEditorText by remember(currentPlan) {
        mutableStateOf(SharedTravelPlanStore.exportCurrentJson())
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = if (isEs) "Gestión de Plan: Demo, IA y JSON" else "Plan Manager: Demo, AI & JSON",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isEs) {
                    "Los planes iniciales son de demostración. Puedes vaciarlos para empezar de cero, restaurarlos o importar/exportar JSON."
                } else {
                    "Initial plans are demo seed data. You can clear them, restore them, or import/export JSON."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            AppButtonRow(
                primaryText = if (isEs) "✨ Añadir Osaka (IA)" else "✨ Add Osaka (AI)",
                onPrimaryClick = {
                    val improved = AiPlanBuilderEngine.improveAndExpandPlan(
                        currentPlan = currentPlan,
                        userInstruction = "Añadir Osaka"
                    )
                    SharedTravelPlanStore.updatePlan(improved)
                    jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                    statusMessage = improved.aiOptimizationSummary
                },
                secondaryText = if (currentPlan.itineraryStops.isNotEmpty()) {
                    if (isEs) "🗑️ Vaciar Demo" else "🗑️ Clear Demo"
                } else {
                    if (isEs) "🔄 Cargar Demo" else "🔄 Load Demo"
                },
                onSecondaryClick = {
                    if (currentPlan.itineraryStops.isNotEmpty()) {
                        confirmationDialogState = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
                            itemName = currentPlan.tripTitle.ifBlank { if (isEs) "Itinerario activo" else "Active itinerary" },
                            itemTypeEs = "todas las paradas y tramos del itinerario",
                            itemTypeEn = "all stops and transport segments in itinerary",
                            isEs = isEs,
                            onConfirm = {
                                SharedTravelPlanStore.updatePlan(
                                    PortableTravelPlanBundle(
                                        schemaVersion = 1,
                                        tripTitle = currentPlan.tripTitle.ifBlank { "Mi Viaje a Japón" },
                                        itineraryStops = emptyList(),
                                        transportSegments = emptyList()
                                    )
                                )
                                jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                                statusMessage = if (isEs) "✅ Datos eliminados." else "✅ Data cleared."
                            }
                        )
                    } else {
                        confirmationDialogState = ConfirmationDialogState(
                            title = if (isEs) "¿Cargar datos demo?" else "Load demo data?",
                            message = if (isEs) {
                                "Se restaurará el itinerario de prueba inicial con paradas en Tokio y Kioto."
                            } else {
                                "Initial demo itinerary with Tokyo and Kyoto stops will be restored."
                            },
                            confirmButtonText = if (isEs) "🔄 Cargar" else "🔄 Load",
                            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
                            isDestructive = false,
                            onConfirm = {
                                SharedTravelPlanStore.resetToDefaults()
                                jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                                statusMessage = if (isEs) "✅ Datos demo restaurados." else "✅ Demo data restored."
                            }
                        )
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            AppButtonRow(
                primaryText = if (isEs) "📥 Importar JSON" else "📥 Import JSON",
                onPrimaryClick = {
                    confirmationDialogState = ConfirmationDialogState(
                        title = if (isEs) "¿Importar plan JSON?" else "Import JSON plan?",
                        message = if (isEs) {
                            "Se sobrescribirá el itinerario actual con los datos del JSON introducido. ¿Deseas continuar?"
                        } else {
                            "The current itinerary will be replaced with the JSON data entered. Continue?"
                        },
                        confirmButtonText = if (isEs) "📥 Importar" else "📥 Import",
                        dismissButtonText = if (isEs) "Cancelar" else "Cancel",
                        isDestructive = true,
                        onConfirm = {
                            SharedTravelPlanStore.importJson(jsonEditorText, mergeWithExisting = false)
                                .onSuccess {
                                    statusMessage = if (isEs) "✅ Plan JSON importado." else "✅ JSON plan imported."
                                }
                                .onFailure {
                                    statusMessage = if (isEs) "⚠️ JSON no válido." else "⚠️ Invalid JSON."
                                }
                        }
                    )
                },
                secondaryText = if (isEs) "📤 Compartir JSON" else "📤 Share JSON",
                onSecondaryClick = {
                    val exported = SharedTravelPlanStore.exportCurrentJson()
                    jsonEditorText = exported
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "Japan Travel Plan JSON")
                        putExtra(Intent.EXTRA_TEXT, exported)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(sendIntent, "Export Travel Plan JSON"))
                    }
                }
            )

            if (statusMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = statusMessage!!,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jsonEditorText,
                onValueChange = { jsonEditorText = it },
                label = { Text("JSON (japan-travel-plan.v1)", fontSize = 14.sp) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                minLines = 6,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickConnectivitySwitcherContent(
    isEs: Boolean,
    kind: ActiveConnectionKind,
    forceOffline: Boolean,
    onToggleForceOffline: (Boolean) -> Unit,
    onOpenInternetPanel: () -> Unit,
    onOpenSimSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isEs) "📡 Conexión Activa: ${kind.badgeEs}" else "📡 Active Connection: ${kind.badgeEn}",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        AppPrimaryButton(
            text = if (isEs) "📶 Abrir Panel Wi-Fi / Datos" else "📶 Open Wi-Fi / Cellular Panel",
            onClick = onOpenInternetPanel,
            modifier = Modifier.fillMaxWidth()
        )

        AppSecondaryButton(
            text = if (isEs) "📲 Cambiar entre SIM y eSIM" else "📲 Switch SIM / eSIM",
            onClick = onOpenSimSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "Modo Solo Offline (Ahorro eSIM)" else "Offline-Only Mode",
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = forceOffline, onCheckedChange = onToggleForceOffline)
        }
        Spacer(Modifier.height(12.dp))
    }
}
