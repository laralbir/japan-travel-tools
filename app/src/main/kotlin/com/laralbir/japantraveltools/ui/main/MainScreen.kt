package com.laralbir.japantraveltools.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.laralbir.japantraveltools.ui.theme.JapanTravelToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

enum class JapanToolTab(
    val shortTitleEn: String,
    val shortTitleEs: String,
    val symbol: String
) {
    HOME_CLOCK_CURRENCY("Clocks & ¥", "Reloj y ¥", "¥/🕒"),
    PLANNER("Planner", "Planificador", "⛩️"),
    TRANSPORT("Transport", "Transporte", "🚅"),
    VAULT_GUIDES("ID & Guides", "ID y Guías", "🛂"),
    AI_TRANSLATE("Gemini & 訳", "IA y Trad.", "訳"),
    SETTINGS("Settings", "Ajustes", "⚙️")
}

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

    JapanTravelToolsTheme(darkTheme = isDark) {
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
    val isTabletWide = configuration.screenWidthDp >= 600 // TCL NXTPAPER 11 Plus (11.5" 3:2) vs Pixel 9/10 Pro XL

    val effectiveLang = LanguageResolver.resolveEffectiveLanguage(prefs.language)
    val isEs = effectiveLang == EffectiveLanguage.SPANISH

    var selectedTab by rememberSaveable { mutableStateOf(JapanToolTab.HOME_CLOCK_CURRENCY) }
    var showConnectivitySheet by rememberSaveable { mutableStateOf(false) }
    var showQuickTranslateModal by rememberSaveable { mutableStateOf(false) }

    // Live ticker for clocks & connectivity
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KotlinConfStyleHeaderBar(
                isEs = isEs,
                isTabletWide = isTabletWide,
                connectivityKind = connectivitySnapshot.kind,
                onConnectivityClick = { showConnectivitySheet = true },
                onQuickLiveTranslateClick = { showQuickTranslateModal = true }
            )
        },
        bottomBar = {
            if (!isTabletWide) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    JapanToolTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Text(
                                    text = tab.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            },
                            label = {
                                Text(
                                    text = if (isEs) tab.shortTitleEs else tab.shortTitleEn,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTabletWide) {
                // TCL NXTPAPER 11 Plus Adaptive NavigationRail
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(12.dp))
                    JapanToolTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Text(
                                    text = tab.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            label = {
                                Text(
                                    text = if (isEs) tab.shortTitleEs else tab.shortTitleEn,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (selectedTab) {
                    JapanToolTab.HOME_CLOCK_CURRENCY -> ClocksAndCurrencyDashboardScreen(
                        isEs = isEs,
                        isTabletWide = isTabletWide,
                        currentInstant = currentInstant,
                        prefs = prefs,
                        prefsRepository = prefsRepository
                    )
                    JapanToolTab.PLANNER -> TripPlannerAdaptiveScreen(
                        isEs = isEs,
                        isTabletWide = isTabletWide
                    )
                    JapanToolTab.TRANSPORT -> TransportMultiModalScreen(
                        isEs = isEs
                    )
                    JapanToolTab.VAULT_GUIDES -> VaultAndGuidesAdaptiveScreen(
                        isEs = isEs,
                        isTabletWide = isTabletWide,
                        biometricLockEnabled = prefs.biometricLockEnabled,
                        onToggleBiometric = { prefsRepository.setBiometricLockEnabled(it) }
                    )
                    JapanToolTab.AI_TRANSLATE -> GeminiAndTranslatorScreen(
                        isEs = isEs,
                        isTabletWide = isTabletWide
                    )
                    JapanToolTab.SETTINGS -> SettingsAndAboutScreen(
                        isEs = isEs,
                        prefs = prefs,
                        prefsRepository = prefsRepository
                    )
                }
            }
        }
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
                onOpenInternetPanel = { ConnectivityStatusHelper.openAndroidInternetPanel(context) },
                onOpenSimSettings = { ConnectivityStatusHelper.openSimAndEsimSettings(context) }
            )
        }
    }

    if (showQuickTranslateModal) {
        ModalBottomSheet(
            onDismissRequest = { showQuickTranslateModal = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QuickSimultaneousTranslationSheet(isEs = isEs)
        }
    }
}

@Composable
private fun KotlinConfStyleHeaderBar(
    isEs: Boolean,
    isTabletWide: Boolean,
    connectivityKind: ActiveConnectionKind,
    onConnectivityClick: () -> Unit,
    onQuickLiveTranslateClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isEs) "Japan Travel Tools" else "Japan Travel Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(6.dp))
                    BetaBadgePill(text = "v0.0.1-beta.1")
                }

                // Direct Quick Action for Simultaneous Translation (SPEC-06)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onQuickLiveTranslateClick() }
                ) {
                    Text(
                        text = if (isEs) "🎙️ Traducción 180°" else "🎙️ Live Translate",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live Connectivity Status Pill & Switcher (RF-00.9)
                val connColor = when (connectivityKind) {
                    ActiveConnectionKind.CELLULAR_ROAMING -> MaterialTheme.colorScheme.primary
                    ActiveConnectionKind.OFFLINE -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = connColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, connColor),
                    modifier = Modifier.clickable { onConnectivityClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) {
                                "📡 ${connectivityKind.badgeEs} · Cambiar (Wi-Fi/SIM/eSIM)"
                            } else {
                                "📡 ${connectivityKind.badgeEn} · Switch (Wi-Fi/SIM/eSIM)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isTabletWide) {
                    Text(
                        text = "TCL NXTPAPER 11 Plus · 3:2 Multi-Pane",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BetaBadgePill(text: String = "BETA") {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ============================================================================
// TAB 1: WORLD CLOCKS (SPEC-01) & OFFLINE CURRENCY CONVERTER (SPEC-02)
// ============================================================================
@Composable
private fun ClocksAndCurrencyDashboardScreen(
    isEs: Boolean,
    isTabletWide: Boolean,
    currentInstant: Instant,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    var gpsDetectedZone by rememberSaveable { mutableStateOf(TimeZone.getDefault().id) }
    var simulatedInTokyo by rememberSaveable { mutableStateOf(false) }

    val effectiveLocalZone = remember(gpsDetectedZone, simulatedInTokyo) {
        if (simulatedInTokyo) {
            OfflineTimeZoneResolver.resolveZoneFromCoordinates(35.6762, 139.6503)
        } else {
            runCatching { ZoneId.of(gpsDetectedZone) }.getOrDefault(ZoneId.systemDefault())
        }
    }
    val originZone = remember(prefs.originZoneId) {
        runCatching { ZoneId.of(prefs.originZoneId) }.getOrDefault(ZoneId.of("Europe/Madrid"))
    }

    val clockComparison = remember(currentInstant, effectiveLocalZone, originZone) {
        CalculateClockComparisonUseCase.calculate(
            instant = currentInstant,
            localZone = effectiveLocalZone,
            originZone = originZone
        )
    }

    // Currency Converter State (SPEC-02)
    val coroutineScope = rememberCoroutineScope()
    var rateSnapshot by remember { mutableStateOf(SeedExchangeRates.defaultSnapshot()) }
    var isSyncingRates by remember { mutableStateOf(false) }
    var syncStatusMessage by remember {
        mutableStateOf(
            if (isEs) "Modo Offline Listo · Semilla BCE 1 EUR = 164.80 JPY"
            else "Offline Ready · ECB Seed 1 EUR = 164.80 JPY"
        )
    }
    var inputAmountText by rememberSaveable { mutableStateOf("1000") }
    var isJpyToBaseDirection by rememberSaveable { mutableStateOf(true) } // Default quick check: ¥ -> €

    val numericInput = inputAmountText.toDoubleOrNull() ?: 0.0
    val fromCurrency = if (isJpyToBaseDirection) prefs.targetCurrency else prefs.baseCurrency
    val toCurrency = if (isJpyToBaseDirection) prefs.baseCurrency else prefs.targetCurrency

    val convertedValue = ConvertCurrencyUseCase.convert(
        amount = numericInput,
        fromCurrency = fromCurrency,
        toCurrency = toCurrency,
        snapshot = rateSnapshot,
        customDirectRate = prefs.customExchangeRate
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Dual / Triple World Clocks Card (SPEC-01)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "🕒 Relojes de Viaje y Ubicación" else "🕒 Travel Clocks & Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill()
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Japan JST Clock
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🇯🇵 TOKYO (JST) ${clockComparison.japanDayPeriod.iconSymbol}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = clockComparison.japanTimeFormatted,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = clockComparison.japanDateFormatted,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Origin Country Clock
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🏠 ${clockComparison.originZoneId.substringAfterLast('/')} ${clockComparison.originDayPeriod.iconSymbol}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = clockComparison.originTimeFormatted,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            val sign = if (clockComparison.hoursDifferenceTokyoVsOrigin >= 0) "+" else ""
                            Text(
                                text = "${clockComparison.originDateFormatted} (${sign}${clockComparison.hoursDifferenceTokyoVsOrigin}h vs JP)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Location & Origin Selector Chips
                Text(
                    text = if (isEs) "País de Origen (Persistido Offline):" else "Origin Country (Persisted Offline):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OfflineTimeZoneResolver.supportedOrigins.forEach { origin ->
                        FilterChip(
                            selected = prefs.originZoneId == origin.zoneId,
                            onClick = { prefsRepository.setOriginZoneId(origin.zoneId) },
                            label = {
                                Text(
                                    text = "${origin.flagEmoji} ${if (isEs) origin.nameEs else origin.nameEn}",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) {
                            "📍 Zona Local GPS: ${effectiveLocalZone.id} (${clockComparison.localTimeFormatted})"
                        } else {
                            "📍 Local GPS Zone: ${effectiveLocalZone.id} (${clockComparison.localTimeFormatted})"
                        },
                        fontSize = 12.sp
                    )
                    OutlinedButton(
                        onClick = { simulatedInTokyo = !simulatedInTokyo },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (simulatedInTokyo) {
                                if (isEs) "GPS: En Tokio" else "GPS: In Tokyo"
                            } else {
                                if (isEs) "Simular GPS Tokio" else "Simulate Tokyo GPS"
                            },
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            // Offline-First Currency Converter Card (SPEC-02: EUR <-> JPY + Frankfurter API)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "💴 Conversor Offline (${prefs.baseCurrency} ⇄ ${prefs.targetCurrency})" else "💴 Offline Converter (${prefs.baseCurrency} ⇄ ${prefs.targetCurrency})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill()
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "📊 Frankfurter Open API (BCE) · Fecha: ${rateSnapshot.rateDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = syncStatusMessage,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputAmountText,
                        onValueChange = { newValue ->
                            inputAmountText = newValue.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("$fromCurrency") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { isJpyToBaseDirection = !isJpyToBaseDirection },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("⇅", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = toCurrency, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${ConvertCurrencyUseCase.formatAmount(convertedValue, toCurrency)} $toCurrency",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Quick Japanese Coin / Banknote Buttons (Ergonomic thumb zone for Pixel 9/10 Pro XL)
                Text(
                    text = if (isEs) "Billetes y Monedas Rápidas de Japón:" else "Quick Japan Coins & Banknotes:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConvertCurrencyUseCase.japanQuickAmountsJpy.forEach { yenBill ->
                        FilterChip(
                            selected = isJpyToBaseDirection && inputAmountText == yenBill.toString(),
                            onClick = {
                                isJpyToBaseDirection = true
                                inputAmountText = yenBill.toString()
                            },
                            label = { Text("¥%,d".format(yenBill), fontFamily = FontFamily.Monospace) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Base currency selector
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EUR", "USD", "GBP", "MXN").forEach { curr ->
                            FilterChip(
                                selected = prefs.baseCurrency == curr,
                                onClick = { prefsRepository.setCurrencyPair(curr, "JPY") },
                                label = { Text(curr, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        enabled = !isSyncingRates,
                        onClick = {
                            isSyncingRates = true
                            coroutineScope.launch {
                                val result = FrankfurterJsonParser.fetchFromFrankfurterApi()
                                result.onSuccess { fresh ->
                                    rateSnapshot = fresh
                                    val jpyRate = fresh.rates["JPY"] ?: 164.80
                                    syncStatusMessage = if (isEs) {
                                        "✅ Actualizado de Frankfurter API: 1 EUR = $jpyRate JPY (${fresh.rateDate})"
                                    } else {
                                        "✅ Synced from Frankfurter API: 1 EUR = $jpyRate JPY (${fresh.rateDate})"
                                    }
                                }.onFailure {
                                    syncStatusMessage = if (isEs) {
                                        "📡 Sin red: usando tasa cacheada local (${rateSnapshot.rateDate})"
                                    } else {
                                        "📡 Offline: using locally cached rate (${rateSnapshot.rateDate})"
                                    }
                                }
                                isSyncingRates = false
                            }
                        }
                    ) {
                        Text(if (isEs) "🔄 Actualizar BCE" else "🔄 Sync ECB")
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: TRIP PLANNER WITH PHOTOS, GOOGLE MAPS DUAL & CALENDAR (SPEC-04)
// ============================================================================
@Composable
private fun TripPlannerAdaptiveScreen(
    isEs: Boolean,
    isTabletWide: Boolean
) {
    val context = LocalContext.current
    var selectedDay by rememberSaveable { mutableIntStateOf(1) }
    var stops by remember { mutableStateOf(TripPlannerUseCase.defaultSampleStops()) }
    var selectedStopForMap by remember { mutableStateOf<ItineraryStopItem?>(null) }

    val dayStops = remember(stops, selectedDay) {
        TripPlannerUseCase.sortAndDetectConflicts(stops.filter { it.dayNumber == selectedDay })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // KotlinConf-style Day Schedule Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "⛩️ Planificador de Viaje, Fotos y Google Maps" else "⛩️ Trip Planner, Photos & Google Maps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            BetaBadgePill()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDay == 1,
                onClick = { selectedDay = 1 },
                label = { Text(if (isEs) "Día 1 · 11 Nov · Tokio (Asakusa/Shinjuku)" else "Day 1 · Nov 11 · Tokyo") }
            )
            FilterChip(
                selected = selectedDay == 2,
                onClick = { selectedDay = 2 },
                label = { Text(if (isEs) "Día 2 · 12 Nov · Kioto (Inari/Gion)" else "Day 2 · Nov 12 · Kyoto") }
            )
        }

        if (isTabletWide) {
            // TCL NXTPAPER 11 Plus 50/50 Dual Pane (Timeline Left + Embedded Map Right)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DayTimelineList(
                        isEs = isEs,
                        analyzedStops = dayStops,
                        onSelectStop = { selectedStopForMap = it },
                        onOpenGoogleMaps = { stop -> launchGoogleMapsForStop(context, stop) },
                        onExportCalendar = { stop -> exportStopToSystemCalendar(context, stop) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    EmbeddedRouteMapCard(
                        isEs = isEs,
                        dayNumber = selectedDay,
                        analyzedStops = dayStops,
                        highlightedStop = selectedStopForMap ?: dayStops.firstOrNull()?.item,
                        onLaunchFullMap = { stop -> launchGoogleMapsForStop(context, stop) }
                    )
                }
            }
        } else {
            // Pixel 10 Pro XL / Pixel 9 Pro XL: Embedded Map Summary Top + Ergonomic Timeline
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    EmbeddedRouteMapCard(
                        isEs = isEs,
                        dayNumber = selectedDay,
                        analyzedStops = dayStops,
                        highlightedStop = selectedStopForMap ?: dayStops.firstOrNull()?.item,
                        onLaunchFullMap = { stop -> launchGoogleMapsForStop(context, stop) }
                    )
                }
                items(dayStops) { analyzed ->
                    TimelineStopCard(
                        isEs = isEs,
                        analyzed = analyzed,
                        onClick = { selectedStopForMap = analyzed.item },
                        onOpenGoogleMaps = { launchGoogleMapsForStop(context, analyzed.item) },
                        onExportCalendar = { exportStopToSystemCalendar(context, analyzed.item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTimelineList(
    isEs: Boolean,
    analyzedStops: List<com.laralbir.japantraveltools.feature.planner.AnalyzedItineraryStop>,
    onSelectStop: (ItineraryStopItem) -> Unit,
    onOpenGoogleMaps: (ItineraryStopItem) -> Unit,
    onExportCalendar: (ItineraryStopItem) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(analyzedStops) { analyzed ->
            TimelineStopCard(
                isEs = isEs,
                analyzed = analyzed,
                onClick = { onSelectStop(analyzed.item) },
                onOpenGoogleMaps = { onOpenGoogleMaps(analyzed.item) },
                onExportCalendar = { onExportCalendar(analyzed.item) }
            )
        }
    }
}

@Composable
private fun TimelineStopCard(
    isEs: Boolean,
    analyzed: com.laralbir.japantraveltools.feature.planner.AnalyzedItineraryStop,
    onClick: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onExportCalendar: () -> Unit
) {
    val stop = analyzed.item
    KotlinConfCard(
        modifier = Modifier.clickable { onClick() },
        accentBorderColor = if (analyzed.hasTimeConflict) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "#${analyzed.orderNumber}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stop.startTime} - ${stop.endTime} JST",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            BetaBadgePill(text = if (isEs) stop.category.labelEs else stop.category.labelEn)
        }

        Spacer(Modifier.height(6.dp))
        Text(text = stop.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(
            text = "🇯🇵 ${stop.placeNameJapanese} · (${stop.latitude}, ${stop.longitude})",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )
        if (stop.notes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = stop.notes, fontSize = 12.sp)
        }

        // Offline Reference Photos Chips (SPEC-04 RF-04.2)
        if (stop.photoLabels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                stop.photoLabels.forEach { photo ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "📷 $photo (Offline)",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenGoogleMaps,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(if (isEs) "🗺️ Abrir Google Maps" else "🗺️ Open Google Maps", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onExportCalendar,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(if (isEs) "📅 Añadir a Calendar" else "📅 Add to Calendar", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmbeddedRouteMapCard(
    isEs: Boolean,
    dayNumber: Int,
    analyzedStops: List<com.laralbir.japantraveltools.feature.planner.AnalyzedItineraryStop>,
    highlightedStop: ItineraryStopItem?,
    onLaunchFullMap: (ItineraryStopItem) -> Unit
) {
    KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.secondary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "🗺️ Mapa Interactivo de Ruta (Día $dayNumber)" else "🗺️ Interactive Route Map (Day $dayNumber)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Offline Vector + Google Maps",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(Modifier.height(8.dp))

        // Interactive numbered stop nodes connected sequentially
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isEs) "Secuencia de Marcadores Numerados del Día:" else "Day Numbered Route Markers:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                analyzedStops.forEach { node ->
                    val isSelected = highlightedStop?.id == node.item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[#${node.orderNumber}] ${node.item.startTime} · ${node.item.title}",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${node.item.latitude}, ${node.item.longitude}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                highlightedStop?.let { stop ->
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onLaunchFullMap(stop) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isEs) "🧭 Cómo llegar a ${stop.title} en Google Maps" else "🧭 Navigate to ${stop.title} in Google Maps",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun launchGoogleMapsForStop(context: android.content.Context, stop: ItineraryStopItem) {
    runCatching {
        val uri = Uri.parse("geo:${stop.latitude},${stop.longitude}?q=${Uri.encode("${stop.title} ${stop.placeNameJapanese}")}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun exportStopToSystemCalendar(context: android.content.Context, stop: ItineraryStopItem) {
    runCatching {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "[JP] ${stop.title}")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "${stop.placeNameJapanese} (${stop.latitude}, ${stop.longitude})")
            putExtra(CalendarContract.Events.DESCRIPTION, stop.notes)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Tokyo")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

// ============================================================================
// TAB 3: MULTIMODAL TRANSPORT PLANNER (FLIGHTS, SHINKANSEN, METRO, BUS - SPEC-05)
// ============================================================================
@Composable
private fun TransportMultiModalScreen(isEs: Boolean) {
    var selectedFilter by rememberSaveable { mutableStateOf<TransportMode?>(null) }
    val segments = remember { TransportPlannerUseCase.defaultSampleSegments() }
    val filtered = remember(segments, selectedFilter) {
        if (selectedFilter == null) segments else segments.filter { it.type == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "🚅 Planificador de Vuelos, Trenes, Metros y Autobuses" else "🚅 Flights, Shinkansen, Subway & Bus Planner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                BetaBadgePill()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text(if (isEs) "Todos (Multimodal)" else "All (Multimodal)") }
                )
                TransportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedFilter == mode,
                        onClick = { selectedFilter = mode },
                        label = { Text(if (isEs) mode.labelEs else mode.labelEn) }
                    )
                }
            }
        }

        items(filtered.indices.toList()) { idx ->
            val seg = filtered[idx]
            val durationMins = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg)

            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${seg.operatorName} · ${seg.lineOrNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    BetaBadgePill(text = seg.passOrPaymentBadge)
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🛫 ${seg.departureCode} (${seg.departureName}) ➔ 🛬 ${seg.arrivalCode} (${seg.arrivalName})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "🕒 Salida: ${seg.departureTimeIso.replace('T', ' ')} (${seg.departureZoneId}) | Llegada: ${seg.arrivalTimeIso.replace('T', ' ')} (${seg.arrivalZoneId}) · ⏱️ ${TransportPlannerUseCase.formatDurationHoursMinutes(durationMins)}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (seg.checkInTime != null || seg.boardingTime != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            seg.checkInTime?.let { append("🧾 Facturación/Andén: ${it.substringAfter('T')}   ") }
                            seg.boardingTime?.let { append("🛂 Embarque: ${it.substringAfter('T')}") }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "📍 ${seg.departureTerminalOrPlatform} · 💺 ${seg.seatOrCarInfo}",
                    fontSize = 12.sp
                )
                if (seg.stationExitOrBelt.isNotBlank()) {
                    Text(
                        text = "🚪 Salida / Equipaje: ${seg.stationExitOrBelt}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Show Layover / Transfer Card between consecutive segments (SPEC-05 RF-05.3)
            if (idx < filtered.lastIndex) {
                val nextSeg = filtered[idx + 1]
                if (seg.arrivalCode == nextSeg.departureCode) {
                    val conn = TransportPlannerUseCase.calculateConnection(seg, nextSeg)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (conn.isTightConnection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isEs) {
                                    "🔄 Escala / Transbordo en ${conn.fromCode}: ${TransportPlannerUseCase.formatDurationHoursMinutes(conn.totalConnectionMinutes)} totales (${TransportPlannerUseCase.formatDurationHoursMinutes(conn.minutesUntilNextBoardingOrDeparture)} hasta siguiente embarque)"
                                } else {
                                    "🔄 Layover / Transfer at ${conn.fromCode}: ${TransportPlannerUseCase.formatDurationHoursMinutes(conn.totalConnectionMinutes)} total (${TransportPlannerUseCase.formatDurationHoursMinutes(conn.minutesUntilNextBoardingOrDeparture)} until next boarding)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = conn.transferSummary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 4: PASSPORT/DNI VAULT, DOCUMENTS & TRAVEL GUIDES (SPEC-03)
// ============================================================================
@Composable
private fun VaultAndGuidesAdaptiveScreen(
    isEs: Boolean,
    isTabletWide: Boolean,
    biometricLockEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit
) {
    var vaultUnlocked by rememberSaveable { mutableStateOf(!biometricLockEnabled) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDocCategory by rememberSaveable { mutableStateOf<DocumentCategory?>(null) }

    val docs = remember { DefaultTravelGuidesCatalog.initialDocuments() }
    val guides = remember { DefaultTravelGuidesCatalog.initialGuides() }

    val filteredDocs = remember(docs, selectedDocCategory, searchQuery) {
        DocumentsFilterUseCase.filterDocuments(docs, selectedDocCategory, searchQuery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Passport & DNI Quick Vault Card (RF-03.1 & RF-03.2)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "🛂 Bóveda Rápida: Pasaporte y DNI (Tax-Free)" else "🛂 Quick ID Vault: Passport & National ID",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill()
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "🔒 Bloqueo Biométrico / PIN de Identificación" else "🔒 Biometric / PIN Lock for ID Vault",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = {
                            onToggleBiometric(it)
                            if (it) vaultUnlocked = false else vaultUnlocked = true
                        }
                    )
                }

                if (biometricLockEnabled && !vaultUnlocked) {
                    Button(
                        onClick = { vaultUnlocked = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEs) "🔓 Desbloquear Pasaporte y DNI con Huella/PIN" else "🔓 Unlock Passport & ID with Biometrics")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🛂 PASSPORT", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text(
                                    text = if (isEs) "Foto HD + QR Visit Japan Web (Almacenamiento Privado)" else "HD Photo + Visit Japan QR (Private Storage)",
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🪪 DNI / ID CARD", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text(
                                    text = if (isEs) "Anverso + Reverso Offline con Zoom" else "Front & Back Offline with Pinch-Zoom",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Travel Guides Section (RF-03.4)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.secondary) {
                Text(
                    text = if (isEs) "📖 Guías de Viaje Offline (Tokio, Kioto, Osaka)" else "📖 Offline Travel Guides (Tokyo, Kyoto, Osaka)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                guides.forEach { guide ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(guide.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                BetaBadgePill(text = "${guide.cityRegion} · 📷${guide.photoCount}")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(guide.markdownSummary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            // Document Repository Filter & Search (RF-03.3)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isEs) "🔍 Buscar billetes, hoteles, JR Pass, seguros..." else "🔍 Search flights, hotels, JR Pass...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDocCategory == null,
                    onClick = { selectedDocCategory = null },
                    label = { Text(if (isEs) "Todos" else "All") }
                )
                DocumentCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedDocCategory == cat,
                        onClick = { selectedDocCategory = cat },
                        label = { Text(if (isEs) cat.labelEs else cat.labelEn, fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filteredDocs) { doc ->
            KotlinConfCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (doc.isPinned) "📌 ${doc.title}" else doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    BetaBadgePill(text = doc.category.badgeCode)
                }
                Spacer(Modifier.height(4.dp))
                Text(text = doc.referenceOrNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============================================================================
// TAB 5: GEMINI AI ASSISTANT & SIMULTANEOUS TRANSLATION (SPEC-06)
// ============================================================================
@Composable
private fun GeminiAndTranslatorScreen(
    isEs: Boolean,
    isTabletWide: Boolean
) {
    var phraseInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    var rotateJapaneseCard180 by rememberSaveable { mutableStateOf(false) }
    var geminiPrompt by rememberSaveable { mutableStateOf("Traducir carta de Ramen y convertir precios ¥ a EUR") }
    var geminiResponse by rememberSaveable {
        mutableStateOf(GeminiAssistantEngine.generateSmartTravelAnswer("ramen", 164.80))
    }

    val translation = remember(phraseInput) {
        OfflineTranslatorEngine.translateToJapanese(phraseInput)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Simultaneous Face-to-Face Translator Card (180° Flippable)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "🎙️ Traducción Simultánea ES/EN ⇄ 日本語 (Offline)" else "🎙️ Simultaneous Live Translate (Offline)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { rotateJapaneseCard180 = !rotateJapaneseCard180 }) {
                        Text(if (rotateJapaneseCard180) "🔄 Normal" else "🔄 Girar 180° Mesa", fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Japanese Counterpart Display (rotatable 180 degrees to show across a counter/table)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .rotate(if (rotateJapaneseCard180) 180f else 0f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = translation.japaneseText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "🗣️ Romaji: ${translation.romaji}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = phraseInput,
                    onValueChange = { phraseInput = it },
                    label = { Text(if (isEs) "Habla o escribe en Español / Inglés" else "Speak or type in English / Spanish") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OfflineTranslatorEngine.essentialPhrases.forEach { preset ->
                        FilterChip(
                            selected = phraseInput == preset.sourceText,
                            onClick = { phraseInput = preset.sourceText },
                            label = { Text(preset.categoryBadge, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            // Gemini AI Assistant (Gemini Nano On-Device Pixel 10/9 Pro XL + Gemini Cloud)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "✨ Asistente IA con Google Gemini (Menús + Rutas)" else "✨ Google Gemini AI Travel Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill(text = "GEMINI AI")
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = geminiPrompt,
                    onValueChange = { geminiPrompt = it },
                    label = { Text(if (isEs) "Pregunta a Gemini o analiza carta en ¥..." else "Ask Gemini or analyze ¥ menu...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            geminiResponse = GeminiAssistantEngine.generateSmartTravelAnswer(geminiPrompt, 164.80)
                        }
                    ) {
                        Text(if (isEs) "✨ Consultar a Gemini" else "✨ Ask Gemini")
                    }
                    OutlinedButton(
                        onClick = {
                            geminiPrompt = "Plan para tarde de lluvia en Kioto"
                            geminiResponse = GeminiAssistantEngine.generateSmartTravelAnswer("kioto lluvia", 164.80)
                        }
                    ) {
                        Text(if (isEs) "🌧️ Ejemplo Kioto" else "🌧️ Kyoto Sample")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = geminiResponse,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 6: SETTINGS, GOOGLE ACCOUNT SYNC & ABOUT / OSS LIBRARIES (SPEC-00)
// ============================================================================
@Composable
private fun SettingsAndAboutScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    var emailInput by rememberSaveable { mutableStateOf(prefs.googleAccountEmail ?: "carlos.travel.japan@gmail.com") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Language & Theme Configuration (RF-00.1, RF-00.2, RF-00.3)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.primary) {
                Text(
                    text = if (isEs) "⚙️ Idioma y Tema Visual (Persistente)" else "⚙️ Language & Visual Theme (Persistent)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isEs) "Idioma (Por defecto Sistema con fallback a Inglés):" else "Language (System default with English fallback):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.language == AppLanguagePreference.SYSTEM,
                        onClick = { prefsRepository.setLanguage(AppLanguagePreference.SYSTEM) },
                        label = { Text(if (isEs) "Sistema" else "System") }
                    )
                    FilterChip(
                        selected = prefs.language == AppLanguagePreference.SPANISH,
                        onClick = { prefsRepository.setLanguage(AppLanguagePreference.SPANISH) },
                        label = { Text("🇪🇸 Español") }
                    )
                    FilterChip(
                        selected = prefs.language == AppLanguagePreference.ENGLISH,
                        onClick = { prefsRepository.setLanguage(AppLanguagePreference.ENGLISH) },
                        label = { Text("🇬🇧 English") }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isEs) "Tema de Color Japonés (Washi Claro / Sumi OLED Oscuro):" else "Japanese Color Theme (Washi Light / Sumi Dark):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.SYSTEM,
                        onClick = { prefsRepository.setThemeMode(AppThemeMode.SYSTEM) },
                        label = { Text(if (isEs) "Sistema" else "System") }
                    )
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.LIGHT,
                        onClick = { prefsRepository.setThemeMode(AppThemeMode.LIGHT) },
                        label = { Text(if (isEs) "☀️ Claro (Washi)" else "☀️ Light (Washi)") }
                    )
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.DARK,
                        onClick = { prefsRepository.setThemeMode(AppThemeMode.DARK) },
                        label = { Text(if (isEs) "🌙 Oscuro (Sumi)" else "🌙 Dark (Sumi)") }
                    )
                }
            }
        }

        item {
            // Google Account Multi-Device Synchronization (RF-00.11)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "☁️ Sincronización Cuenta de Google (Pixel ⇄ Tablet TCL)" else "☁️ Google Account Sync (Pixel ⇄ TCL Tablet)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill()
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEs) {
                        "Sincroniza itinerarios, transportes, guías y documentos entre tu Pixel 10/9 Pro XL y tu TCL NXTPAPER 11 Plus manteniendo copia 100% offline."
                    } else {
                        "Syncs itineraries, transport, guides and documents across your Pixel 10/9 Pro XL and TCL NXTPAPER 11 Plus keeping 100% offline copies."
                    },
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (prefs.googleAccountEmail != null) {
                            "✅ Vinculado: ${prefs.googleAccountEmail}"
                        } else {
                            if (isEs) "No vinculado aún" else "Not linked yet"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            prefsRepository.setGoogleAccountSync(
                                email = emailInput,
                                lastSyncedMillis = System.currentTimeMillis()
                            )
                        }
                    ) {
                        Text(if (isEs) "🔄 Sincronizar Dispositivos" else "🔄 Sync Devices")
                    }
                }
            }
        }

        item {
            // About & Third-Party Open Source Libraries Section (RF-00.6)
            KotlinConfCard(accentBorderColor = MaterialTheme.colorScheme.tertiary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "ℹ️ Acerca de y Librerías Open Source" else "ℹ️ About & Open Source Libraries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BetaBadgePill(text = "v${OpenSourceLibrariesCatalog.APP_VERSION_NAME}")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Japan Travel Tools · Version ${OpenSourceLibrariesCatalog.APP_VERSION_NAME} (Code ${OpenSourceLibrariesCatalog.APP_VERSION_CODE})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (isEs) {
                        "Optimizado para Google Pixel 10 Pro XL, Google Pixel 9 Pro XL y Tablet TCL NXTPAPER 11 Plus."
                    } else {
                        "Optimized for Google Pixel 10 Pro XL, Google Pixel 9 Pro XL & TCL NXTPAPER 11 Plus Tablet."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isEs) "Librerías Open Source de Terceros Utilizadas:" else "Third-Party Open Source Libraries Used:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                OpenSourceLibrariesCatalog.thirdPartyLibraries.forEach { lib ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(lib.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(lib.license, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "${lib.artifactId} (${lib.version}) · ${lib.author}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isEs) lib.purposeEs else lib.purposeEn,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEs) "📡 Estado de Red y Cambio Rápido (Wi-Fi / SIM / eSIM)" else "📡 Live Network & Quick Switcher (Wi-Fi / SIM / eSIM)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = if (isEs) "Conexión activa detectada: ${kind.badgeEs}" else "Active connection detected: ${kind.badgeEn}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = onOpenInternetPanel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEs) "📶 Abrir Panel de Internet (Wi-Fi / Datos Móviles)" else "📶 Open Android Internet Panel (Wi-Fi / Cellular)")
        }

        OutlinedButton(
            onClick = onOpenSimSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEs) "📲 Cambiar entre SIM Física y eSIM de Japón" else "📲 Switch between Physical SIM & Japan eSIM")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "Forzar Modo Solo Offline en la app (Ahorro eSIM)" else "Force App Offline-Only Mode (Save eSIM Data)",
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = forceOffline, onCheckedChange = onToggleForceOffline)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun QuickSimultaneousTranslationSheet(isEs: Boolean) {
    var quickInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    val translated = remember(quickInput) { OfflineTranslatorEngine.translateToJapanese(quickInput) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isEs) "🎙️ Acceso Directo: Traducción Simultánea" else "🎙️ Quick Simultaneous Translation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(translated.japaneseText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(translated.romaji, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        OutlinedTextField(
            value = quickInput,
            onValueChange = { quickInput = it },
            label = { Text(if (isEs) "Frase a traducir al Japonés" else "Phrase to translate to Japanese") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}
