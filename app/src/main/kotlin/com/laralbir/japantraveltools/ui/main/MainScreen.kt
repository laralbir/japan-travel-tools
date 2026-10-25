package com.laralbir.japantraveltools.ui.main

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
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
import com.laralbir.japantraveltools.feature.clock.CalculateClockComparisonUseCase
import com.laralbir.japantraveltools.feature.clock.OfflineTimeZoneResolver
import com.laralbir.japantraveltools.feature.currency.ConvertCurrencyUseCase
import com.laralbir.japantraveltools.feature.currency.FrankfurterJsonParser
import com.laralbir.japantraveltools.feature.currency.SeedExchangeRates
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
import com.laralbir.japantraveltools.feature.documents.SharedDocumentsAndGuidesStore
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.DocumentsFilterUseCase
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

    PLANNER(DrawerSection.PLANNING, "Planificador de Viaje", "Trip Planner", "🗓️"),
    TRANSPORT(DrawerSection.PLANNING, "Transporte y Trenes", "Trains & Flights", "🚄"),

    VOICE_TRANSLATOR(DrawerSection.VOICE_AI, "Traductor por Voz", "Voice Translator", "🎙️"),
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
private fun AppPrimaryButton(
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
private fun AppSecondaryButton(
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
    modifier: Modifier = Modifier
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
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
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
    }
    val configuration = LocalConfiguration.current
    val isTabletExpanded = configuration.screenWidthDp >= 720

    val effectiveLang = LanguageResolver.resolveEffectiveLanguage(prefs.language)
    val isEs = effectiveLang == EffectiveLanguage.SPANISH

    var selectedDestination by rememberSaveable { mutableStateOf(JapanDrawerDestination.CURRENCY) }
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
                        onSelectDestination = { dest -> selectedDestination = dest }
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
                ) {
                    ActiveToolContent(
                        destination = selectedDestination,
                        isEs = isEs,
                        currentInstant = currentInstant,
                        prefs = prefs,
                        prefsRepository = prefsRepository,
                        onOpenPlanSheet = { showJsonPortabilitySheet = true },
                        onNavigateToDestination = { selectedDestination = it }
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
                            selectedDestination = dest
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
                ) {
                    ActiveToolContent(
                        destination = selectedDestination,
                        isEs = isEs,
                        currentInstant = currentInstant,
                        prefs = prefs,
                        prefsRepository = prefsRepository,
                        onOpenPlanSheet = { showJsonPortabilitySheet = true },
                        onNavigateToDestination = { selectedDestination = it }
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
        JapanDrawerDestination.PLANNER -> TripPlannerTabScreen(isEs = isEs, onOpenJsonPortability = onOpenPlanSheet)
        JapanDrawerDestination.TRANSPORT -> TransportTabScreen(isEs = isEs, onOpenJsonPortability = onOpenPlanSheet)
        JapanDrawerDestination.VOICE_TRANSLATOR -> VoiceTranslatorScreen(isEs = isEs)
        JapanDrawerDestination.AI_ASSISTANT -> AiAssistantScreen(
            isEs = isEs,
            prefs = prefs,
            prefsRepository = prefsRepository,
            onNavigateToPlanner = { onNavigateToDestination(JapanDrawerDestination.PLANNER) }
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
    var rateSnapshot by remember { mutableStateOf(SeedExchangeRates.defaultSnapshot()) }
    var isSyncingRates by remember { mutableStateOf(false) }
    var syncStatusMessage by remember {
        mutableStateOf(
            if (isEs) "Fuente: Frankfurter API (BCE) · Offline listo"
            else "Source: Frankfurter API (ECB) · Offline ready"
        )
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
                        inputAmountText = newValue.filter { it.isDigit() || it == '.' }
                    },
                    label = {
                        Text(
                            text = if (isEs) "Importe en $fromCurrency" else "Amount in $fromCurrency",
                            fontSize = 15.sp
                        )
                    },
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
                            FrankfurterJsonParser.fetchFromFrankfurterApi()
                                .onSuccess { fresh ->
                                    rateSnapshot = fresh
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
                    text = syncStatusMessage,
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
                    Button(onClick = {
                        editingCity = null
                        isCreatingCity = true
                    }) {
                        Text(if (isEs) "➕ Ciudad" else "➕ City", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEs) {
                        "Previsión del tiempo para tus destinos en Japón. Puedes añadir, editar o eliminar ciudades con sus coordenadas."
                    } else {
                        "Weather forecast for your Japan destinations. Add, edit, or delete cities with custom coordinates."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

    val presets = remember { WeatherCityCatalog.destinationPresets() }
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
                .navigationBarsPadding(),
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
                onValueChange = { nameEsText = it; errorMessage = null },
                label = { Text(if (isEs) "Nombre de la ciudad (Español)" else "City name (Spanish)", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nameEnText,
                onValueChange = { nameEnText = it; errorMessage = null },
                label = { Text(if (isEs) "Nombre de la ciudad (Inglés)" else "City name (English)", fontSize = 15.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; errorMessage = null },
                    label = { Text("Latitud", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it; errorMessage = null },
                    label = { Text("Longitud", fontSize = 14.sp) },
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
                        confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                            itemName = portablePlan.tripTitle,
                            itemTypeEs = "plan de viaje",
                            itemTypeEn = "travel plan",
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
                    onClick = { SharedTravelPlanStore.deleteDay(selectedDay) },
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

        Spacer(Modifier.height(8.dp))
        Text(
            text = stop.title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        if (stop.placeNameJapanese.isNotBlank()) {
            Text(
                text = stop.placeNameJapanese,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (stop.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stop.notes,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        AppButtonRow(
            primaryText = if (isEs) "✏️ Editar" else "✏️ Edit",
            onPrimaryClick = { onEditStop(stop) },
            secondaryText = if (isEs) "🗑️ Eliminar" else "🗑️ Delete",
            onSecondaryClick = { onDeleteStop(stop) }
        )

        Spacer(Modifier.height(10.dp))
        AppButtonRow(
            primaryText = if (isEs) "🗺️ Abrir Mapa" else "🗺️ Open Map",
            onPrimaryClick = { onOpenGoogleMaps(stop) },
            secondaryText = if (isEs) "📅 Al Calendario" else "📅 To Calendar",
            onSecondaryClick = { onExportCalendar(stop) }
        )
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
    var dayText by rememberSaveable { mutableStateOf(initialStop.dayNumber.toString()) }
    var startTimeText by rememberSaveable { mutableStateOf(initialStop.startTime) }
    var endTimeText by rememberSaveable { mutableStateOf(initialStop.endTime) }
    var titleText by rememberSaveable { mutableStateOf(initialStop.title) }
    var japaneseNameText by rememberSaveable { mutableStateOf(initialStop.placeNameJapanese) }
    var notesText by rememberSaveable { mutableStateOf(initialStop.notes) }
    var selectedCategory by rememberSaveable { mutableStateOf(initialStop.category) }
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
                .navigationBarsPadding(),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isEs) "Día" else "Day", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = { startTimeText = it },
                    label = { Text(if (isEs) "Inicio (HH:mm)" else "Start (HH:mm)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f)
                )
                OutlinedTextField(
                    value = endTimeText,
                    onValueChange = { endTimeText = it },
                    label = { Text(if (isEs) "Fin (HH:mm)" else "End (HH:mm)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f)
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

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar Parada" else "💾 Save Stop",
                onPrimaryClick = {
                    val safeDay = dayText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val updated = initialStop.copy(
                        dayNumber = safeDay,
                        dateIso = String.format(Locale.US, "2026-11-%02d", (10 + safeDay).coerceAtMost(30)),
                        startTime = startTimeText.ifBlank { "10:00" },
                        endTime = endTimeText.ifBlank { "11:30" },
                        title = titleText.ifBlank { if (isEs) "Nueva Parada" else "New Stop" },
                        placeNameJapanese = japaneseNameText,
                        notes = notesText,
                        category = selectedCategory
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
                .navigationBarsPadding(),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    label = { Text(if (isEs) "Fecha Inicio (YYYY-MM-DD)" else "Start Date") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isEs) "Días" else "Days") },
                    singleLine = true,
                    modifier = Modifier.weight(0.7f)
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
                .navigationBarsPadding(),
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

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${seg.departureCode} (${seg.departureName}) ➔ ${seg.arrivalCode} (${seg.arrivalName})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🕒 ${seg.departureTimeIso.substringAfter('T')} ➔ ${seg.arrivalTimeIso.substringAfter('T')} (${TransportPlannerUseCase.formatDurationHoursMinutes(durationMins)})",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    if (seg.checkInTime != null || seg.boardingTime != null) {
                        Spacer(Modifier.height(4.dp))
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

                    Spacer(Modifier.height(14.dp))
                    AppButtonRow(
                        primaryText = if (isEs) "✏️ Editar" else "✏️ Edit",
                        onPrimaryClick = {
                            isCreatingSegment = false
                            editingSegment = seg
                        },
                        secondaryText = if (isEs) "🗑️ Eliminar" else "🗑️ Delete",
                        onSecondaryClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                itemName = "${seg.operatorName} ${seg.lineOrNumber}",
                                itemTypeEs = "transporte",
                                itemTypeEn = "transport",
                                isEs = isEs,
                                onConfirm = {
                                    SharedTravelPlanStore.deleteTransportSegment(seg.id)
                                }
                            )
                        }
                    )
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
                .navigationBarsPadding(),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = depTimeText,
                    onValueChange = { depTimeText = it },
                    label = { Text(if (isEs) "Salida (HH:mm)" else "Dep (HH:mm)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = arrTimeText,
                    onValueChange = { arrTimeText = it },
                    label = { Text(if (isEs) "Llegada (HH:mm)" else "Arr (HH:mm)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = checkInTimeText,
                    onValueChange = { checkInTimeText = it },
                    label = { Text(if (isEs) "Facturación (HH:mm, opc)" else "Check-in (HH:mm, opt)", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = boardingTimeText,
                    onValueChange = { boardingTimeText = it },
                    label = { Text(if (isEs) "Embarque (HH:mm, opc)" else "Boarding (HH:mm, opt)", fontSize = 14.sp) },
                    singleLine = true,
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
                    val baseDate = if (initialSegment.departureTimeIso.contains('T')) {
                        initialSegment.departureTimeIso.substringBefore('T')
                    } else "2026-11-12"
                    val defaultOp = when (selectedType) {
                        TransportMode.FLIGHT -> if (isEs) "Vuelo" else "Flight"
                        TransportMode.TRAIN_SHINKANSEN -> if (isEs) "Shinkansen / Tren" else "Shinkansen / Train"
                        TransportMode.SUBWAY_METRO -> if (isEs) "Metro" else "Subway"
                        TransportMode.BUS -> if (isEs) "Autobús" else "Bus"
                    }
                    val depTimeClean = depTimeText.trim().ifBlank { "09:00" }
                    val arrTimeClean = arrTimeText.trim().ifBlank { "11:00" }
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
                        passOrPaymentBadge = if (selectedType == TransportMode.FLIGHT) "Flight" else "IC Card / JR"
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
@Composable
private fun VoiceTranslatorScreen(isEs: Boolean) {
    val context = LocalContext.current
    var translatorMode by rememberSaveable { mutableIntStateOf(0) } // 0: Voz, 1: Cámara

    // --- MODO 0: VOZ SIMULTÁNEA ---
    var phraseInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    var japaneseHeardInput by rememberSaveable {
        mutableStateOf("いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？")
    }
    var rotateJapaneseCard180 by rememberSaveable { mutableStateOf(false) }

    // --- MODO 1: CÁMARA & OCR DE CARTELES ---
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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

    fun processOcrImage(inputImage: InputImage, previewBitmap: Bitmap?) {
        isProcessingOcr = true
        ocrErrorMessage = null
        capturedBitmap = previewBitmap
        ocrRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                isProcessingOcr = false
                val recognizedText = visionText.text
                if (recognizedText.isNotBlank()) {
                    ocrRawText = recognizedText
                } else {
                    ocrErrorMessage = if (isEs) {
                        "No se detectó texto en la imagen. Intenta enfocar más de cerca el cartel o menú."
                    } else {
                        "No text detected in the image. Please focus closer to the sign or menu."
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

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            processOcrImage(inputImage, bitmap)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val decodedBitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            if (decodedBitmap != null) {
                val inputImage = InputImage.fromBitmap(decodedBitmap, 0)
                processOcrImage(inputImage, decodedBitmap)
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
                    japaneseHeardInput = spokenText
                    val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech(spokenText)
                    val targetSpeech = if (isEs) reply.spanishSpokenText else reply.englishSpokenText
                    speakOutLoud(targetSpeech, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                } else {
                    phraseInput = spokenText
                    val translated = OfflineTranslatorEngine.translateToJapanese(spokenText)
                    speakOutLoud(translated.japaneseText, Locale.JAPAN)
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
                    phraseInput = heardJa
                    val translated = OfflineTranslatorEngine.translateToJapanese(heardJa)
                    speakOutLoud(translated.japaneseText, Locale.JAPAN)
                } else {
                    japaneseHeardInput = heardJa
                    val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech(heardJa)
                    val targetSpeech = if (isEs) reply.spanishSpokenText else reply.englishSpokenText
                    speakOutLoud(targetSpeech, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SELECTOR DE MODO: VOZ vs CÁMARA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppOptionButton(
                    text = if (isEs) "🎙️ Voz Simultánea" else "🎙️ Voice Interpreter",
                    selected = translatorMode == 0,
                    onClick = { translatorMode = 0 },
                    modifier = Modifier.weight(1f)
                )
                AppOptionButton(
                    text = if (isEs) "📷 Cámara / Carteles" else "📷 Camera / Signs",
                    selected = translatorMode == 1,
                    onClick = { translatorMode = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (translatorMode == 0) {
            // MODO VOZ - CARD 1: YOU SPEAK -> PHONE SPEAKS JAPANESE
            item {
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "1. Tú hablas ➔ Suena Japonés" else "1. You speak ➔ Plays Japanese",
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
                        primaryText = if (isEs) "🎙️ Hablar por Micro" else "🎙️ Speak Mic",
                        onPrimaryClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEs) "es-ES" else "en-US")
                                }
                                travelerSpeechLauncher.launch(intent)
                            }.onFailure {
                                speakOutLoud(translation.japaneseText, Locale.JAPAN)
                            }
                        },
                        secondaryText = if (isEs) "🔊 Sonar en Japonés" else "🔊 Play Japanese",
                        onSecondaryClick = { speakOutLoud(translation.japaneseText, Locale.JAPAN) }
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
                                text = phraseInput,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = translation.japaneseText,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "🗣️ ${translation.romaji}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (isEs) "Frases rápidas con audio japonés:" else "Quick Japanese audio phrases:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfflineTranslatorEngine.essentialPhrases.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { preset ->
                                    AppOptionButton(
                                        text = "🔊 ${preset.categoryBadge}",
                                        selected = phraseInput == preset.sourceText,
                                        onClick = {
                                            phraseInput = preset.sourceText
                                            speakOutLoud(preset.japaneseText, Locale.JAPAN)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MODO VOZ - CARD 2: JAPANESE PERSON SPEAKS -> PHONE SPEAKS SPANISH TO YOU
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "2. Habla el Japonés ➔ Te habla en Español" else "2. Japanese speaks ➔ Speaks English to you",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    AppButtonRow(
                        primaryText = if (isEs) "🎙️ Escuchar Japonés" else "🎙️ Listen Japanese",
                        onPrimaryClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                                }
                                japaneseSpeechLauncher.launch(intent)
                            }.onFailure {
                                val spoken = if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
                                speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                            }
                        },
                        secondaryText = if (isEs) "🔊 Oír en Español" else "🔊 Hear in English",
                        onSecondaryClick = {
                            val spoken = if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
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
                            Text(
                                text = if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (isEs) "Simular respuestas habituales en Japón:" else "Simulate common Japanese replies:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfflineTranslatorEngine.incomingJapaneseSamples.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { sample ->
                                    AppOptionButton(
                                        text = "🔊 ${sample.contextLabel}",
                                        selected = japaneseHeardInput == sample.japaneseHeardText,
                                        onClick = {
                                            japaneseHeardInput = sample.japaneseHeardText
                                            val spoken = if (isEs) sample.spanishSpokenText else sample.englishSpokenText
                                            speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
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
        } else {
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
                        primaryText = if (isEs) "📷 Tomar Foto" else "📷 Take Photo",
                        onPrimaryClick = {
                            runCatching {
                                takePhotoLauncher.launch(null)
                            }
                        },
                        secondaryText = if (isEs) "🖼️ Elegir Imagen" else "🖼️ Pick Image",
                        onSecondaryClick = {
                            runCatching {
                                pickImageLauncher.launch("image/*")
                            }
                        }
                    )

                    if (capturedBitmap != null) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Foto capturada",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                                AppOptionButton(
                                    text = if (isEs) "🗑️ Quitar foto" else "🗑️ Clear photo",
                                    selected = false,
                                    onClick = { capturedBitmap = null },
                                    modifier = Modifier.fillMaxWidth(0.5f)
                                )
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

                    if (ocrResult.lines.isNotEmpty()) {
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

                        Spacer(Modifier.height(14.dp))

                        AppButtonRow(
                            primaryText = if (isEs) "🔊 Oír en Español" else "🔊 Hear in English",
                            onPrimaryClick = {
                                speakOutLoud(
                                    ocrResult.lines.joinToString(". ") { it.translatedText },
                                    if (isEs) Locale.forLanguageTag("es-ES") else Locale.US
                                )
                            },
                            secondaryText = if (isEs) "🔊 Oír en Japonés" else "🔊 Hear Japanese",
                            onSecondaryClick = {
                                speakOutLoud(
                                    ocrResult.lines.joinToString(" ") { it.japaneseText },
                                    Locale.JAPAN
                                )
                            }
                        )
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
                                            capturedBitmap = null
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
                            "Para realidad aumentada con superposición en tiempo real, puedes abrir directamente la cámara de Google Traductor o Google Lens:"
                        } else {
                            "For live augmented reality overlay translation, you can directly launch Google Translate or Lens:"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    AppPrimaryButton(
                        text = if (isEs) "🌐 Abrir Cámara Google Traductor" else "🌐 Open Google Translate Camera",
                        onClick = {
                            val gTranslateIntent = Intent(Intent.ACTION_VIEW).apply {
                                setPackage("com.google.android.apps.translate")
                                data = Uri.parse("googletranslate://camera")
                            }
                            runCatching {
                                context.startActivity(gTranslateIntent)
                            }.onFailure {
                                runCatching {
                                    val webIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://translate.google.com/?sl=ja&tl=es&op=images")
                                    )
                                    context.startActivity(webIntent)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
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
    onNavigateToPlanner: () -> Unit
) {
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var geminiPrompt by rememberSaveable { mutableStateOf("") }
    var assistantAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantSource by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantTime by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var actionStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showApiKeyConfig by rememberSaveable { mutableStateOf(false) }
    var apiKeyInput by rememberSaveable { mutableStateOf(prefs.geminiApiKey ?: "") }

    fun performQuery(promptToQuery: String) {
        val clean = promptToQuery.trim()
        if (clean.isBlank() || isLoading) return
        isLoading = true
        actionStatusMessage = null
        coroutineScope.launch {
            val result = GeminiAssistantEngine.queryAssistant(
                prompt = clean,
                apiKey = prefs.geminiApiKey,
                eurToJpyRate = 164.80,
                currentPlan = portablePlan
            )
            assistantAnswer = result.answer
            assistantSource = result.source.name
            assistantTime = result.timestampFormatted
            isLoading = false
        }
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
        modifier = Modifier.fillMaxSize(),
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

                // Engine mode & API Key banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasKey = !prefs.geminiApiKey.isNullOrBlank()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (hasKey) {
                                if (isEs) "✨ Gemini 2.0 Flash (Cloud)" else "✨ Gemini 2.0 Flash (Cloud)"
                            } else {
                                if (isEs) "⚡ Motor Experto (100% Offline)" else "⚡ Expert Engine (Offline)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(onClick = { showApiKeyConfig = !showApiKeyConfig }) {
                        Text(
                            text = if (showApiKeyConfig) {
                                if (isEs) "Ocultar Ajustes" else "Hide Key"
                            } else {
                                if (isEs) "⚙️ Gemini API Key" else "⚙️ Gemini API Key"
                            },
                            fontSize = 13.sp
                        )
                    }
                }

                if (showApiKeyConfig) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isEs) "Clave de API de Google Gemini (Opcional):" else "Google Gemini API Key (Optional):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                placeholder = { Text("AIzaSy...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        prefsRepository.setGeminiApiKey(apiKeyInput.trim())
                                        showApiKeyConfig = false
                                        actionStatusMessage = if (isEs) "✅ Clave Gemini guardada" else "✅ Gemini key saved"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEs) "Guardar Clave" else "Save Key", fontSize = 13.sp)
                                }
                                if (!prefs.geminiApiKey.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            apiKeyInput = ""
                                            prefsRepository.setGeminiApiKey(null)
                                            actionStatusMessage = if (isEs) "🗑️ Clave eliminada (Modo offline activo)" else "🗑️ Key removed (Offline mode active)"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isEs) "Borrar" else "Clear", fontSize = 13.sp)
                                    }
                                }
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { performQuery(geminiPrompt) },
                        enabled = !isLoading && geminiPrompt.isNotBlank(),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isEs) "Consultando..." else "Consulting...", fontSize = 14.sp)
                        } else {
                            Text(if (isEs) "✨ Consultar IA" else "✨ Ask AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (geminiPrompt.isNotBlank()) {
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
                        enabled = !isLoading && geminiPrompt.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "🗓️ Aplicar" else "🗓️ Apply", fontSize = 14.sp)
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
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Column {
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

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = currentAnswer,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
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

    // Vault is unlocked if biometric lock is turned off by user or device doesn't support lock
    var vaultUnlocked by rememberSaveable {
        mutableStateOf(!prefs.biometricLockEnabled && !biometricAvailable)
    }
    var authErrorText by remember { mutableStateOf<String?>(null) }
    var refreshPhotoCounter by remember { mutableIntStateOf(0) }

    fun launchRealBiometricPrompt() {
        if (activity == null) {
            vaultUnlocked = true
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    vaultUnlocked = true
                    authErrorText = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authErrorText = errString.toString()
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

    LaunchedEffect(biometricAvailable, prefs.biometricLockEnabled) {
        if (biometricAvailable && !vaultUnlocked) {
            launchRealBiometricPrompt()
        }
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
                        text = if (isEs) "Pasaporte, DNI y Documentos" else "Passport, ID & Documents",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (biometricAvailable) {
                        Switch(
                            checked = prefs.biometricLockEnabled,
                            onCheckedChange = { enabled ->
                                prefsRepository.setBiometricLockEnabled(enabled)
                                if (enabled) {
                                    vaultUnlocked = false
                                    launchRealBiometricPrompt()
                                } else {
                                    vaultUnlocked = true
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (biometricAvailable) {
                    Text(
                        text = if (isEs) {
                            "🔒 Protegido con Huella / Rostro / PIN del Pixel"
                        } else {
                            "🔒 Protected with Pixel Fingerprint / Face / PIN"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!vaultUnlocked && biometricAvailable) {
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
                                    "Por seguridad, tus fotos de Pasaporte y DNI están encriptadas offline. Autentícate para verlas."
                                } else {
                                    "Your Passport and ID photos are encrypted offline. Authenticate to view."
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
                    Spacer(Modifier.height(14.dp))

                    // 1. PASSPORT SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🛂 Pasaporte (Página de Datos)" else "🛂 Passport (Data Page)",
                        subtitle = if (isEs) "Foto lista para mostrar en hoteles y compras Tax-Free" else "Ready for hotels and Tax-Free shops",
                        fileName = "passport_data_page.jpg",
                        refreshTrigger = refreshPhotoCounter,
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )

                    Spacer(Modifier.height(14.dp))

                    // 2. DNI FRONT SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🪪 DNI / ID (Anverso)" else "🪪 National ID (Front)",
                        subtitle = if (isEs) "Copia frontal de tu documento nacional" else "Front copy of your national identity card",
                        fileName = "dni_front.jpg",
                        refreshTrigger = refreshPhotoCounter,
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )

                    Spacer(Modifier.height(14.dp))

                    // 3. DNI BACK SECTION
                    IdDocumentPhotoSection(
                        isEs = isEs,
                        title = if (isEs) "🪪 DNI / ID (Reverso)" else "🪪 National ID (Back)",
                        subtitle = if (isEs) "Reverso con dirección y datos de expedición" else "Back side with address and expiration",
                        fileName = "dni_back.jpg",
                        refreshTrigger = refreshPhotoCounter,
                        onPhotoChanged = { refreshPhotoCounter++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdDocumentPhotoSection(
    isEs: Boolean,
    title: String,
    subtitle: String,
    fileName: String,
    refreshTrigger: Int,
    onPhotoChanged: () -> Unit
) {
    val context = LocalContext.current
    val photoFile = remember(fileName, refreshTrigger) {
        SharedDocumentsAndGuidesStore.getIdPhotoFile(context, fileName)
    }
    val photoBitmap = remember(photoFile) {
        photoFile?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }.getOrNull() }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            SharedDocumentsAndGuidesStore.saveIdPhoto(context, uri, fileName)
            onPhotoChanged()
        }
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

            if (photoBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(12.dp))
                AppButtonRow(
                    primaryText = if (isEs) "🔄 Cambiar Foto" else "🔄 Change Photo",
                    onPrimaryClick = { photoPickerLauncher.launch("image/*") },
                    secondaryText = if (isEs) "🗑️ Quitar Foto" else "🗑️ Delete Photo",
                    onSecondaryClick = {
                        SharedDocumentsAndGuidesStore.deleteIdPhoto(context, fileName)
                        onPhotoChanged()
                    }
                )
            } else {
                AppPrimaryButton(
                    text = if (isEs) "📷 Seleccionar Foto (Galería / Drive)" else "📷 Select Photo (Gallery / Drive)",
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

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
                        AppSecondaryButton(
                            text = if (isEs) "📄 Abrir Documento / PDF" else "📄 Open Attached Document / PDF",
                            onClick = { openDocumentFile(context, attachedPath) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    AppButtonRow(
                        primaryText = if (isEs) "✏️ Editar" else "✏️ Edit",
                        onPrimaryClick = {
                            isCreatingDoc = false
                            editingDoc = doc
                        },
                        secondaryText = if (isEs) "🗑️ Eliminar" else "🗑️ Delete",
                        onSecondaryClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                itemName = doc.title,
                                itemTypeEs = "documento",
                                itemTypeEn = "document",
                                isEs = isEs,
                                onConfirm = { SharedDocumentsAndGuidesStore.deleteDocument(doc.id) }
                            )
                        }
                    )
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
    var selectedCategory by rememberSaveable { mutableStateOf(initialDoc.category) }
    var isPinned by rememberSaveable { mutableStateOf(initialDoc.isPinned) }
    var attachedFilePath by rememberSaveable { mutableStateOf(initialDoc.fileUri ?: initialDoc.internalUriPath) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = SharedDocumentsAndGuidesStore.saveDocumentAttachment(context, uri, uri.lastPathSegment)
            if (saved != null) {
                attachedFilePath = saved
            }
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
                .navigationBarsPadding(),
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
                label = { Text(if (isEs) "Localizador, hora de entrada o notas" else "Booking ref, entry time, notes", fontSize = 15.sp) },
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
                AppSecondaryButton(
                    text = if (isEs) "📎 Adjuntar PDF o Foto" else "📎 Attach PDF or Photo",
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppButtonRow(
                primaryText = if (isEs) "💾 Guardar" else "💾 Save",
                onPrimaryClick = {
                    val updated = initialDoc.copy(
                        title = titleText.ifBlank { if (isEs) "Documento" else "Document" },
                        referenceOrNotes = notesText,
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
    val filteredGuides = remember(guides, searchQuery) {
        DocumentsFilterUseCase.filterGuides(guides, null, searchQuery)
    }

    var editingGuide by remember { mutableStateOf<TravelGuideArticle?>(null) }
    var isCreatingGuide by remember { mutableStateOf(false) }
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }

    AppConfirmationDialog(
        state = confirmationDialogState,
        onDismiss = { confirmationDialogState = null }
    )

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
                            text = guide.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        InfoTagPill(text = guide.cityRegion)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = guide.markdownSummary,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    AppButtonRow(
                        primaryText = if (isEs) "✏️ Editar" else "✏️ Edit",
                        onPrimaryClick = {
                            isCreatingGuide = false
                            editingGuide = guide
                        },
                        secondaryText = if (isEs) "🗑️ Eliminar" else "🗑️ Delete",
                        onSecondaryClick = {
                            confirmationDialogState = ConfirmationDialogBuilder.buildDeleteConfirmation(
                                itemName = guide.title,
                                itemTypeEs = "guía de viaje",
                                itemTypeEn = "travel guide",
                                isEs = isEs,
                                onConfirm = { SharedDocumentsAndGuidesStore.deleteGuide(guide.id) }
                            )
                        }
                    )
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
                .navigationBarsPadding(),
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
    var emailInput by rememberSaveable { mutableStateOf(prefs.googleAccountEmail ?: "carlos.travel.japan@gmail.com") }

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
            KotlinConfCard {
                Text(
                    text = if (isEs) "Sincronización con Cuenta Google" else "Google Account Sync",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account Email", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                AppPrimaryButton(
                    text = if (isEs) "🔄 Sincronizar Dispositivos" else "🔄 Sync Devices",
                    onClick = {
                        prefsRepository.setGoogleAccountSync(
                            email = emailInput,
                            lastSyncedMillis = System.currentTimeMillis()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Asistente IA (Google Gemini API Key)" else "AI Assistant (Google Gemini API Key)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isEs) {
                        "Opcional: Si configuras tu API Key de Google AI Studio, las consultas complejas usarán Gemini 2.0 Flash cuando tengas conexión. Si no configuras ninguna clave, la app utilizará siempre el motor experto local 100% offline."
                    } else {
                        "Optional: Enter your Google AI Studio API key to enable Gemini 2.0 Flash cloud answers when online. Without a key, the app relies on the offline expert engine."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                var settingsGeminiKey by rememberSaveable { mutableStateOf(prefs.geminiApiKey ?: "") }
                OutlinedTextField(
                    value = settingsGeminiKey,
                    onValueChange = { settingsGeminiKey = it },
                    label = { Text("Gemini API Key (AIzaSy...)", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { prefsRepository.setGeminiApiKey(settingsGeminiKey.trim()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEs) "💾 Guardar Clave" else "💾 Save Key", fontSize = 14.sp)
                    }
                    if (!prefs.geminiApiKey.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                settingsGeminiKey = ""
                                prefsRepository.setGeminiApiKey(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEs) "🗑️ Borrar Clave" else "🗑️ Clear Key", fontSize = 14.sp)
                        }
                    }
                }
            }
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
                .padding(horizontal = 18.dp, vertical = 10.dp)
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
                        confirmationDialogState = ConfirmationDialogState(
                            title = if (isEs) "¿Vaciar itinerario?" else "Clear itinerary?",
                            message = if (isEs) {
                                "Se eliminarán todas las paradas y tramos de transporte del plan actual. Esta acción no se puede deshacer."
                            } else {
                                "All stops and transport segments in the current plan will be deleted. This action cannot be undone."
                            },
                            confirmButtonText = if (isEs) "🗑️ Vaciar" else "🗑️ Clear",
                            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
                            isDestructive = true,
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
