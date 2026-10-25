package com.laralbir.japantraveltools.ui.main

import android.app.Activity
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.laralbir.japantraveltools.feature.ai.GeminiAssistantEngine
import com.laralbir.japantraveltools.feature.ai.OfflineTranslatorEngine
import com.laralbir.japantraveltools.feature.clock.CalculateClockComparisonUseCase
import com.laralbir.japantraveltools.feature.clock.OfflineTimeZoneResolver
import com.laralbir.japantraveltools.feature.currency.ConvertCurrencyUseCase
import com.laralbir.japantraveltools.feature.currency.FrankfurterJsonParser
import com.laralbir.japantraveltools.feature.currency.SeedExchangeRates
import com.laralbir.japantraveltools.feature.documents.DefaultTravelGuidesCatalog
import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.DocumentsFilterUseCase
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.settings.OpenSourceLibrariesCatalog
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import com.laralbir.japantraveltools.feature.weather.JapanWeatherCity
import com.laralbir.japantraveltools.feature.weather.OpenMeteoWeatherUseCase
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
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
// 3. WEATHER TOOL SCREEN (OPEN-METEO)
// ============================================================================
@Composable
private fun WeatherToolScreen(isEs: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val allForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    var selectedCity by rememberSaveable { mutableStateOf(JapanWeatherCity.TOKYO) }
    var isSyncingWeather by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val cityDays = remember(allForecasts, selectedCity) {
        allForecasts.filter { it.cityKey == selectedCity.key }.ifEmpty {
            listOf(OpenMeteoWeatherUseCase.defaultSeedForecasts().first())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Previsión Open-Meteo Japón" else "Open-Meteo Japan Forecast",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    JapanWeatherCity.entries.chunked(2).forEach { cityPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cityPair.forEach { city ->
                                AppOptionButton(
                                    text = if (isEs) city.nameEs else city.nameEn,
                                    selected = selectedCity == city,
                                    onClick = { selectedCity = city },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                cityDays.take(4).forEach { day ->
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
                        coroutineScope.launch {
                            OpenMeteoWeatherUseCase.fetchCityForecastOnline(selectedCity)
                                .onSuccess { liveDays ->
                                    SharedWeatherForecastStore.updateCityForecasts(selectedCity.key, liveDays)
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
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// ============================================================================
// 4. TRIP PLANNER SCREEN
// ============================================================================
@Composable
private fun TripPlannerTabScreen(
    isEs: Boolean,
    onOpenJsonPortability: () -> Unit
) {
    val context = LocalContext.current
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

    val dayStops = remember(allStops, selectedDay) {
        TripPlannerUseCase.sortAndDetectConflicts(
            allStops.filter { it.dayNumber == selectedDay }
        )
    }
    val dayWeather = remember(weatherForecasts, selectedDay) {
        SharedWeatherForecastStore.forecastForDayNumber(selectedDay)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableDays.forEach { dayNum ->
                    AppOptionButton(
                        text = if (isEs) "Día $dayNum" else "Day $dayNum",
                        selected = selectedDay == dayNum,
                        onClick = { selectedDay = dayNum },
                        modifier = Modifier.width(96.dp)
                    )
                }
            }

            AppSecondaryButton(
                text = if (isEs) "⚙️ Opciones Plan" else "⚙️ Plan Options",
                onClick = onOpenJsonPortability
            )
        }

        if (dayWeather != null) {
            Spacer(Modifier.height(10.dp))
            InfoTagPill(
                text = "${dayWeather.condition.emoji} ${dayWeather.tempMinC.toInt()}°/${dayWeather.tempMaxC.toInt()}°C · ☔ ${dayWeather.precipitationProbabilityMax}% · 🗻 Fuji ${dayWeather.fujiVisibilityPercent}%"
            )
        }

        Spacer(Modifier.height(14.dp))

        if (dayStops.isEmpty()) {
            KotlinConfCard {
                Text(
                    text = if (isEs) "Tu planificador está vacío" else "Your planner is empty",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isEs) {
                        "Pulsa «⚙️ Opciones Plan» para importar tu JSON, añadir rutas con IA o restaurar el itinerario de demostración."
                    } else {
                        "Tap «⚙️ Plan Options» to import JSON, generate a route with AI, or restore the demo itinerary."
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AppPrimaryButton(
                    text = if (isEs) "⚙️ Abrir Opciones del Plan" else "⚙️ Open Plan Options",
                    onClick = onOpenJsonPortability,
                    modifier = Modifier.fillMaxWidth()
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
                        onOpenGoogleMaps = { stop ->
                            val geoUri = Uri.parse(
                                "geo:${stop.latitude},${stop.longitude}?q=${Uri.encode(stop.placeNameJapanese)}"
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
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stop.title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stop.placeNameJapanese,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )

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
            primaryText = if (isEs) "🗺️ Abrir Mapa" else "🗺️ Open Map",
            onPrimaryClick = { onOpenGoogleMaps(stop) },
            secondaryText = if (isEs) "📅 Al Calendario" else "📅 To Calendar",
            onSecondaryClick = { onExportCalendar(stop) }
        )
    }
}

// ============================================================================
// 5. TRANSPORT & TRAINS SCREEN
// ============================================================================
@Composable
private fun TransportTabScreen(
    isEs: Boolean,
    onOpenJsonPortability: () -> Unit
) {
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val segments = portablePlan.transportSegments
    var selectedFilter by rememberSaveable { mutableStateOf<TransportMode?>(null) }
    val filteredSegments = remember(segments, selectedFilter) {
        if (selectedFilter == null) segments else segments.filter { it.type == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

                AppSecondaryButton(
                    text = if (isEs) "⚙️ Importar / Exportar JSON o Demo" else "⚙️ Import / Export JSON or Demo",
                    onClick = onOpenJsonPortability,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filteredSegments.indices.toList()) { idx ->
            val seg = filteredSegments[idx]
            val durationMins = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg)
            KotlinConfCard {
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
                Text(
                    text = "📍 ${seg.departureTerminalOrPlatform} · 💺 ${seg.seatOrCarInfo}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// 6. VOICE TRANSLATOR SCREEN (BIDIRECTIONAL STT + AUTO-TTS)
// ============================================================================
@Composable
private fun VoiceTranslatorScreen(isEs: Boolean) {
    val context = LocalContext.current
    var phraseInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    var japaneseHeardInput by rememberSaveable {
        mutableStateOf("いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？")
    }
    var rotateJapaneseCard180 by rememberSaveable { mutableStateOf(false) }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine = instance
            }
        }
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    fun speakOutLoud(text: String, locale: Locale) {
        val engine = ttsEngine ?: return
        engine.language = locale
        engine.setSpeechRate(0.95f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
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
                phraseInput = spokenText
                val translated = OfflineTranslatorEngine.translateToJapanese(spokenText)
                speakOutLoud(translated.japaneseText, Locale.JAPAN)
            }
        }
    }

    val japaneseSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heardJa = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heardJa.isNullOrBlank()) {
                japaneseHeardInput = heardJa
                val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech(heardJa)
                val targetSpeech = if (isEs) reply.spanishSpokenText else reply.englishSpokenText
                speakOutLoud(targetSpeech, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // CARD 1: YOU SPEAK -> PHONE SPEAKS JAPANESE
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

        item {
            // CARD 2: JAPANESE PERSON SPEAKS -> PHONE SPEAKS SPANISH TO YOU
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
    }
}

// ============================================================================
// 7. AI TRAVEL ASSISTANT SCREEN (SMART ANSWERS + ROUTE BUILDER)
// ============================================================================
@Composable
private fun AiAssistantScreen(
    isEs: Boolean,
    onNavigateToPlanner: () -> Unit
) {
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    var geminiPrompt by rememberSaveable { mutableStateOf("¿Cómo funciona la Suica y el metro?") }
    var geminiResponse by rememberSaveable {
        mutableStateOf(
            GeminiAssistantEngine.generateSmartTravelAnswer(
                prompt = "¿Cómo funciona la Suica y el metro?",
                eurToJpyRate = 164.80,
                currentPlan = null
            )
        )
    }

    val quickTopics = listOf(
        "💳 Tarjeta Suica" to "¿Cómo funciona la Suica y el metro?",
        "🚄 Shinkansen" to "Consejos de Shinkansen y maletas",
        "🍜 Carta Ramen ¥" to "Traducir carta de Ramen con precios en ¥",
        "🦌 Ruta Nara" to "Añadir excursión a Nara y ciervos",
        "🗻 Ruta Hakone" to "Añadir excursión a Hakone y Monte Fuji",
        "🐙 Ruta Osaka" to "Añadir día en Osaka y Dotonbori",
        "🏧 Cajeros 7-Eleven" to "¿Dónde sacar efectivo en cajeros 7-Eleven?",
        "🧳 Envío Maletas" to "¿Cómo enviar maletas con Takkyubin Yamato?"
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
                        "Selecciona un tema rápido, escribe importes en yenes o añade nuevas excursiones directamente a tu Planificador."
                    } else {
                        "Pick a quick topic, type Yen amounts, or add new day trips directly to your Planner."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

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
                                        geminiResponse = GeminiAssistantEngine.generateSmartTravelAnswer(
                                            prompt = fullQuery,
                                            eurToJpyRate = 164.80,
                                            currentPlan = portablePlan
                                        )
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
                            text = if (isEs) "Escribe tu consulta o ciudad a añadir..." else "Type your question or city to add...",
                            fontSize = 15.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                AppButtonRow(
                    primaryText = if (isEs) "✨ Consultar IA" else "✨ Ask AI",
                    onPrimaryClick = {
                        geminiResponse = GeminiAssistantEngine.generateSmartTravelAnswer(
                            prompt = geminiPrompt,
                            eurToJpyRate = 164.80,
                            currentPlan = portablePlan
                        )
                    },
                    secondaryText = if (isEs) "🗓️ Aplicar en Ruta" else "🗓️ Apply to Route",
                    onSecondaryClick = {
                        val updated = AiPlanBuilderEngine.improveAndExpandPlan(
                            currentPlan = portablePlan,
                            userInstruction = geminiPrompt
                        )
                        SharedTravelPlanStore.updatePlan(updated)
                        geminiResponse = updated.aiOptimizationSummary
                    }
                )

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = geminiResponse,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
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
// 8. PASSPORT & CUSTOMS SCREEN
// ============================================================================
@Composable
private fun PassportAndCustomsScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    var vaultUnlocked by rememberSaveable { mutableStateOf(!prefs.biometricLockEnabled) }

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
                        text = if (isEs) "Pasaporte, DNI y QR Aduanas" else "Passport, ID & Customs QR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = prefs.biometricLockEnabled,
                        onCheckedChange = { enabled ->
                            prefsRepository.setBiometricLockEnabled(enabled)
                            if (!enabled) vaultUnlocked = true
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (prefs.biometricLockEnabled && !vaultUnlocked) {
                    AppPrimaryButton(
                        text = if (isEs) "🔓 Desbloquear Documentación" else "🔓 Unlock ID Vault",
                        onClick = { vaultUnlocked = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🛂 PASAPORTE + QR VISIT JAPAN WEB",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isEs) {
                                    "Guardado en almacenamiento privado offline para mostrar en hoteles y tiendas Tax-Free."
                                } else {
                                    "Stored offline in private storage for hotels & Tax-Free."
                                },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🪪 DNI / DOCUMENTO DE IDENTIDAD",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isEs) {
                                    "Anverso y reverso disponibles 100% sin conexión (Context.filesDir)."
                                } else {
                                    "Front and back available 100% offline (Context.filesDir)."
                                },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. BOOKINGS VAULT SCREEN
// ============================================================================
@Composable
private fun BookingsVaultScreen(isEs: Boolean) {
    val allDocuments = remember { DefaultTravelGuidesCatalog.initialDocuments() }
    var selectedCategory by rememberSaveable { mutableStateOf<DocumentCategory?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredDocs = remember(allDocuments, selectedCategory, searchQuery) {
        DocumentsFilterUseCase.filterDocuments(allDocuments, selectedCategory, searchQuery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOptionButton(
                        text = if (isEs) "Todos" else "All",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        modifier = Modifier.weight(1f)
                    )
                    AppOptionButton(
                        text = if (isEs) "✈️ Vuelos" else "✈️ Flights",
                        selected = selectedCategory == DocumentCategory.FLIGHT,
                        onClick = { selectedCategory = DocumentCategory.FLIGHT },
                        modifier = Modifier.weight(1f)
                    )
                    AppOptionButton(
                        text = if (isEs) "🏨 Hoteles" else "🏨 Hotels",
                        selected = selectedCategory == DocumentCategory.HOTEL,
                        onClick = { selectedCategory = DocumentCategory.HOTEL },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (isEs) "🔍 Buscar vuelos, hoteles, JR Pass..." else "🔍 Search bookings...", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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
            }
        }
    }
}

// ============================================================================
// 10. TRAVEL GUIDES SCREEN
// ============================================================================
@Composable
private fun TravelGuidesScreen(isEs: Boolean) {
    val guides = remember { DefaultTravelGuidesCatalog.initialGuides() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(guides, key = { it.id }) { guide ->
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
            }
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
                        SharedTravelPlanStore.updatePlan(
                            PortableTravelPlanBundle(
                                schemaVersion = 1,
                                tripTitle = "Mi Viaje a Japón",
                                itineraryStops = emptyList(),
                                transportSegments = emptyList()
                            )
                        )
                        jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                        statusMessage = if (isEs) "✅ Datos demo eliminados." else "✅ Demo data cleared."
                    } else {
                        SharedTravelPlanStore.resetToDefaults()
                        jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                        statusMessage = if (isEs) "✅ Datos demo restaurados." else "✅ Demo data restored."
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            AppButtonRow(
                primaryText = if (isEs) "📥 Importar JSON" else "📥 Import JSON",
                onPrimaryClick = {
                    SharedTravelPlanStore.importJson(jsonEditorText, mergeWithExisting = false)
                        .onSuccess {
                            statusMessage = if (isEs) "✅ Plan JSON importado." else "✅ JSON plan imported."
                        }
                        .onFailure {
                            statusMessage = if (isEs) "⚠️ JSON no válido." else "⚠️ Invalid JSON."
                        }
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
