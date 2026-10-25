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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.laralbir.japantraveltools.feature.weather.PlanningSuitability
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
import com.laralbir.japantraveltools.ui.theme.JapanTravelToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

enum class JapanToolTab(
    val shortTitleEn: String,
    val shortTitleEs: String,
    val symbol: String
) {
    HOME_CLOCK_CURRENCY("Home", "Inicio", "💴"),
    PLANNER("Route", "Ruta", "⛩️"),
    TRANSPORT("Transit", "Tren/Vuelo", "🚅"),
    VAULT_GUIDES("Docs", "Docs", "🛂"),
    AI_TRANSLATE("Voice", "Voz / IA", "🎙️"),
    SETTINGS("Config", "Ajustes", "⚙️")
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
    val isTabletWide = configuration.screenWidthDp >= 600

    val effectiveLang = LanguageResolver.resolveEffectiveLanguage(prefs.language)
    val isEs = effectiveLang == EffectiveLanguage.SPANISH

    var selectedTab by rememberSaveable { mutableStateOf(JapanToolTab.HOME_CLOCK_CURRENCY) }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CleanMinimalHeaderBar(
                isEs = isEs,
                connectivityKind = connectivitySnapshot.kind,
                onConnectivityClick = { showConnectivitySheet = true },
                onQuickVoiceClick = { selectedTab = JapanToolTab.AI_TRANSLATE }
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
                                    fontSize = 18.sp
                                )
                            },
                            label = {
                                Text(
                                    text = if (isEs) tab.shortTitleEs else tab.shortTitleEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
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
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(12.dp))
                    JapanToolTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Text(text = tab.symbol, fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = if (isEs) tab.shortTitleEs else tab.shortTitleEn,
                                    fontSize = 12.sp
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
                        biometricLockEnabled = prefs.biometricLockEnabled,
                        onToggleBiometric = { prefsRepository.setBiometricLockEnabled(it) }
                    )
                    JapanToolTab.AI_TRANSLATE -> GeminiAndTranslatorScreen(
                        isEs = isEs
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
}

@Composable
private fun CleanMinimalHeaderBar(
    isEs: Boolean,
    connectivityKind: ActiveConnectionKind,
    onConnectivityClick: () -> Unit,
    onQuickVoiceClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🇯🇵 Japan Travel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onConnectivityClick() }
                    ) {
                        Text(
                            text = "📡 ${if (isEs) connectivityKind.badgeEs.substringBefore(" ") else connectivityKind.badgeEn.substringBefore(" ")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onQuickVoiceClick() }
                    ) {
                        Text(
                            text = if (isEs) "🎙️ Voz" else "🎙️ Voice",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun InfoTagPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

// ============================================================================
// TAB 1: CLEAN SEGMENTED HOME (CONVERTER | CLOCKS | WEATHER)
// ============================================================================
@Composable
private fun ClocksAndCurrencyDashboardScreen(
    isEs: Boolean,
    currentInstant: Instant,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0: Divisa, 1: Relojes, 2: Tiempo

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

    val coroutineScope = rememberCoroutineScope()
    var rateSnapshot by remember { mutableStateOf(SeedExchangeRates.defaultSnapshot()) }
    var isSyncingRates by remember { mutableStateOf(false) }
    var syncStatusMessage by remember {
        mutableStateOf(
            if (isEs) "1 EUR = 164.80 JPY · Offline listo"
            else "1 EUR = 164.80 JPY · Offline ready"
        )
    }
    var inputAmountText by rememberSaveable { mutableStateOf("1000") }
    var isJpyToBaseDirection by rememberSaveable { mutableStateOf(true) }

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Sub-view selector: only 1 focused tool on screen at a time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = {
                        Text(
                            text = if (isEs) "💴 Conversor" else "💴 Currency",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = {
                        Text(
                            text = if (isEs) "🕒 Relojes" else "🕒 Clocks",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 2,
                    onClick = { activeSection = 2 },
                    label = {
                        Text(
                            text = if (isEs) "🌦️ Tiempo" else "🌦️ Weather",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        when (activeSection) {
            0 -> item {
                // Focused Currency Converter Card + Compact Tokyo Time header
                KotlinConfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "Conversor de Yenes" else "Yen Converter",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        InfoTagPill(text = "🇯🇵 ${clockComparison.japanTimeFormatted}")
                    }

                    Spacer(Modifier.height(16.dp))

                    // Large Converted Result Display
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$inputAmountText $fromCurrency =",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${ConvertCurrencyUseCase.formatAmount(convertedValue, toCurrency)} $toCurrency",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputAmountText,
                            onValueChange = { newValue ->
                                inputAmountText = newValue.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text(if (isEs) "Cantidad en $fromCurrency" else "Amount in $fromCurrency", fontSize = 14.sp) },
                            textStyle = MaterialTheme.typography.titleLarge,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { isJpyToBaseDirection = !isJpyToBaseDirection },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("⇅ $toCurrency", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isEs) "Importes rápidos en Japón:" else "Quick amounts in Japan:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
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
                                label = {
                                    Text(
                                        text = "¥%,d".format(yenBill),
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncStatusMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            enabled = !isSyncingRates,
                            onClick = {
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
                            }
                        ) {
                            Text(if (isEs) "🔄 Actualizar" else "🔄 Update", fontSize = 14.sp)
                        }
                    }
                }
            }

            1 -> item {
                // Focused World Clocks Card
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Hora en Japón y en Casa" else "Japan & Home Time",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🇯🇵 Tokio (JST)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = clockComparison.japanDateFormatted,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = clockComparison.japanTimeFormatted,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sign = if (clockComparison.hoursDifferenceTokyoVsOrigin >= 0) "+" else ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🏠 ${clockComparison.originZoneId.substringAfterLast('/')}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${clockComparison.originDateFormatted} (${sign}${clockComparison.hoursDifferenceTokyoVsOrigin}h)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = clockComparison.originTimeFormatted,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isEs) "Cambiar país de origen:" else "Change home country:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
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
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            else -> item {
                OpenMeteoWeatherPlannerCard(isEs = isEs)
            }
        }
    }
}

@Composable
private fun OpenMeteoWeatherPlannerCard(isEs: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val allForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    var selectedCity by rememberSaveable { mutableStateOf(JapanWeatherCity.TOKYO) }
    var isSyncingWeather by remember { mutableStateOf(false) }

    val cityDays = remember(allForecasts, selectedCity) {
        allForecasts.filter { it.cityKey == selectedCity.key }.ifEmpty {
            listOf(OpenMeteoWeatherUseCase.defaultSeedForecasts().first())
        }
    }

    KotlinConfCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEs) "Tiempo en Japón" else "Japan Weather",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                enabled = !isSyncingWeather,
                onClick = {
                    isSyncingWeather = true
                    coroutineScope.launch {
                        OpenMeteoWeatherUseCase.fetchCityForecastOnline(selectedCity)
                            .onSuccess { liveDays ->
                                SharedWeatherForecastStore.updateCityForecasts(selectedCity.key, liveDays)
                            }
                        isSyncingWeather = false
                    }
                }
            ) {
                Text(if (isEs) "🔄 Actualizar" else "🔄 Update", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JapanWeatherCity.entries.forEach { city ->
                FilterChip(
                    selected = selectedCity == city,
                    onClick = { selectedCity = city },
                    label = {
                        Text(
                            text = if (isEs) city.nameEs else city.nameEn,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        cityDays.take(3).forEach { day ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${day.condition.emoji} ${day.dateIso}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${day.tempMaxC.toInt()}° / ${day.tempMinC.toInt()}°C · ☔ ${day.precipitationProbabilityMax}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) day.userPlanningAdviceEs else day.userPlanningAdviceEn,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: TRIP PLANNER (CLEAN CARDS, LARGE FONTS, OPTIONS SHEET FOR DEMO/AI/JSON)
// ============================================================================
@Composable
private fun TripPlannerAdaptiveScreen(
    isEs: Boolean,
    isTabletWide: Boolean
) {
    val context = LocalContext.current
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val weatherForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    val stops = portablePlan.itineraryStops

    var selectedDay by rememberSaveable { mutableIntStateOf(1) }
    var showJsonSheet by rememberSaveable { mutableStateOf(false) }

    val availableDays = remember(stops) {
        stops.map { it.dayNumber }.distinct().sorted().ifEmpty { listOf(1) }
    }

    val selectedDayWeather = remember(weatherForecasts, selectedDay) {
        SharedWeatherForecastStore.forecastForDayNumber(selectedDay)
    }

    val dayStops = remember(stops, selectedDay) {
        TripPlannerUseCase.sortAndDetectConflicts(stops.filter { it.dayNumber == selectedDay })
    }

    if (showJsonSheet) {
        JsonPortabilityModalSheet(
            isEs = isEs,
            onDismiss = { showJsonSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "Itinerario del Viaje" else "Trip Itinerary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { showJsonSheet = true }) {
                    Text(if (isEs) "⚙️ IA / JSON / Demo" else "⚙️ AI / JSON / Demo", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableDays.forEach { dayNum ->
                    val dayLabel = when (dayNum) {
                        1 -> if (isEs) "Día 1 · Tokio" else "Day 1 · Tokyo"
                        2 -> if (isEs) "Día 2 · Kioto" else "Day 2 · Kyoto"
                        3 -> if (isEs) "Día 3 · Osaka" else "Day 3 · Osaka"
                        else -> if (isEs) "Día $dayNum" else "Day $dayNum"
                    }
                    FilterChip(
                        selected = selectedDay == dayNum,
                        onClick = { selectedDay = dayNum },
                        label = {
                            Text(
                                text = dayLabel,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Clean 1-line Weather Summary for the selected day
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedDayWeather.condition.emoji} ${selectedDayWeather.tempMaxC.toInt()}° / ${selectedDayWeather.tempMinC.toInt()}°C · ☔ ${selectedDayWeather.precipitationProbabilityMax}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDayWeather.suitability == PlanningSuitability.INDOOR_RECOMMENDED) {
                            if (isEs) "☔ Plan Cubierto" else "☔ Indoor Day"
                        } else {
                            if (isEs) "☀️ Ideal Exterior" else "☀️ Great Outdoors"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (dayStops.isEmpty()) {
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Itinerario vacío" else "Empty itinerary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) {
                            "Pulsa en '⚙️ IA / JSON / Demo' para crear tu ruta con IA, importar tu JSON o cargar los datos de ejemplo."
                        } else {
                            "Tap '⚙️ AI / JSON / Demo' to create a route with AI, import your JSON, or load sample data."
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(dayStops) { analyzed ->
                TimelineStopCard(
                    isEs = isEs,
                    analyzed = analyzed,
                    onOpenGoogleMaps = { launchGoogleMapsForStop(context, analyzed.item) },
                    onExportCalendar = { exportStopToSystemCalendar(context, analyzed.item) }
                )
            }
        }
    }
}

@Composable
private fun TimelineStopCard(
    isEs: Boolean,
    analyzed: com.laralbir.japantraveltools.feature.planner.AnalyzedItineraryStop,
    onOpenGoogleMaps: () -> Unit,
    onExportCalendar: () -> Unit
) {
    val stop = analyzed.item
    KotlinConfCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stop.startTime} – ${stop.endTime}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            InfoTagPill(text = if (isEs) stop.category.labelEs else stop.category.labelEn)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stop.title,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "🇯🇵 ${stop.placeNameJapanese}",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )

        if (stop.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stop.notes,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenGoogleMaps,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEs) "🗺️ Google Maps" else "🗺️ Google Maps", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onExportCalendar,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEs) "📅 Calendario" else "📅 Calendar", fontSize = 14.sp)
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
            putExtra(CalendarContract.Events.EVENT_LOCATION, stop.placeNameJapanese)
            putExtra(CalendarContract.Events.DESCRIPTION, stop.notes)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Tokyo")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonPortabilityModalSheet(
    isEs: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val weatherForecasts by SharedWeatherForecastStore.forecastsState.collectAsState()
    var jsonEditorText by remember { mutableStateOf(SharedTravelPlanStore.exportCurrentJson()) }
    var statusMessage by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (isEs) "⚙️ Gestionar Plan (IA, JSON y Datos Demo)" else "⚙️ Manage Plan (AI, JSON & Demo Data)",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Row 1: AI & Demo Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updated = AiPlanBuilderEngine.improveAndExpandPlan(
                            currentPlan = portablePlan,
                            userInstruction = "Optimizar ruta y añadir Día 3 en Osaka"
                        )
                        val withWeather = AiPlanBuilderEngine.optimizePlanWithWeather(updated, weatherForecasts)
                        SharedTravelPlanStore.updatePlan(withWeather)
                        jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                        statusMessage = if (isEs) "✅ Plan optimizado con IA + Clima Open-Meteo." else "✅ Plan optimized with AI + Weather."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEs) "✨ Mejorar con IA" else "✨ AI Optimize", fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = {
                        SharedTravelPlanStore.updatePlan(
                            PortableTravelPlanBundle(
                                schemaVersion = 1,
                                tripTitle = "Mi Viaje a Japón",
                                itineraryStops = emptyList(),
                                transportSegments = emptyList()
                            )
                        )
                        jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                        statusMessage = if (isEs) "🗑️ Datos de demostración vaciados." else "🗑️ Demo data cleared."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEs) "🗑️ Vaciar Demo" else "🗑️ Clear Demo", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 2: JSON Export / Import / Restore Demo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val exported = SharedTravelPlanStore.exportCurrentJson()
                        jsonEditorText = exported
                        runCatching {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "Japan Travel Plan JSON")
                                putExtra(Intent.EXTRA_TEXT, exported)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "JSON").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                ) {
                    Text(if (isEs) "📤 Exportar JSON" else "📤 Export JSON", fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = {
                        val res = SharedTravelPlanStore.importJson(jsonEditorText, mergeWithExisting = false)
                        statusMessage = if (res.isSuccess) {
                            if (isEs) "✅ JSON importado correctamente." else "✅ JSON imported."
                        } else {
                            if (isEs) "⚠️ Error en el formato JSON." else "⚠️ Invalid JSON."
                        }
                    }
                ) {
                    Text(if (isEs) "📥 Importar JSON" else "📥 Import JSON", fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = {
                        SharedTravelPlanStore.resetToDefaults()
                        jsonEditorText = SharedTravelPlanStore.exportCurrentJson()
                        statusMessage = if (isEs) "🔄 Demo restaurada." else "🔄 Demo restored."
                    }
                ) {
                    Text(if (isEs) "🔄 Cargar Demo" else "🔄 Load Demo", fontSize = 14.sp)
                }
            }

            if (statusMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = jsonEditorText,
                onValueChange = { jsonEditorText = it },
                label = { Text("JSON (Itinerario + Vuelos y Trenes)", fontSize = 14.sp) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ============================================================================
// TAB 3: MULTIMODAL TRANSPORT (CLEAN CARDS & LARGE FONTS)
// ============================================================================
@Composable
private fun TransportMultiModalScreen(isEs: Boolean) {
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    val segments = portablePlan.transportSegments
    var selectedFilter by rememberSaveable { mutableStateOf<TransportMode?>(null) }
    var showJsonSheet by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(segments, selectedFilter) {
        if (selectedFilter == null) segments else segments.filter { it.type == selectedFilter }
    }

    if (showJsonSheet) {
        JsonPortabilityModalSheet(
            isEs = isEs,
            onDismiss = { showJsonSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "Vuelos, Trenes y Metro" else "Flights, Trains & Metro",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { showJsonSheet = true }) {
                    Text("⚙️ JSON / Demo", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = {
                        Text(
                            text = if (isEs) "Todos" else "All",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                TransportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedFilter == mode,
                        onClick = { selectedFilter = mode },
                        label = {
                            Text(
                                text = if (isEs) mode.labelEs else mode.labelEn,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    )
                }
            }
        }

        items(filtered.indices.toList()) { idx ->
            val seg = filtered[idx]
            val durationMins = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg)

            KotlinConfCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${seg.operatorName} · ${seg.lineOrNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTagPill(text = seg.passOrPaymentBadge)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${seg.departureCode} (${seg.departureName}) ➔ ${seg.arrivalCode} (${seg.arrivalName})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🕒 ${seg.departureTimeIso.substringAfter('T')} ➔ ${seg.arrivalTimeIso.substringAfter('T')} (${TransportPlannerUseCase.formatDurationHoursMinutes(durationMins)})",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "📍 ${seg.departureTerminalOrPlatform} · 💺 ${seg.seatOrCarInfo}",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (seg.stationExitOrBelt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🚪 ${seg.stationExitOrBelt}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (idx < filtered.lastIndex) {
                val nextSeg = filtered[idx + 1]
                if (seg.arrivalCode == nextSeg.departureCode) {
                    val conn = TransportPlannerUseCase.calculateConnection(seg, nextSeg)
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🔄 Escala en ${conn.fromCode}: ${TransportPlannerUseCase.formatDurationHoursMinutes(conn.totalConnectionMinutes)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = conn.transferSummary,
                                fontSize = 14.sp,
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
// TAB 4: PASSPORT/DNI, RESERVATIONS & GUIDES (SEGMENTED SO IT DOESN'T CLUTTER)
// ============================================================================
@Composable
private fun VaultAndGuidesAdaptiveScreen(
    isEs: Boolean,
    biometricLockEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit
) {
    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0: Pasaporte, 1: Reservas, 2: Guías
    var vaultUnlocked by rememberSaveable { mutableStateOf(!biometricLockEnabled) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val docs = remember { DefaultTravelGuidesCatalog.initialDocuments() }
    val guides = remember { DefaultTravelGuidesCatalog.initialGuides() }
    val filteredDocs = remember(docs, searchQuery) {
        DocumentsFilterUseCase.filterDocuments(docs, null, searchQuery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = {
                        Text(
                            text = if (isEs) "🛂 Pasaporte" else "🛂 Passport",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = {
                        Text(
                            text = if (isEs) "📄 Reservas" else "📄 Bookings",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 2,
                    onClick = { activeSection = 2 },
                    label = {
                        Text(
                            text = if (isEs) "📖 Guías" else "📖 Guides",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        when (activeSection) {
            0 -> item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Pasaporte y DNI (Tax-Free)" else "Passport & ID Vault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEs) "🔒 Proteger con Huella / PIN" else "🔒 Biometric / PIN Lock",
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = {
                                onToggleBiometric(it)
                                vaultUnlocked = !it
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    if (biometricLockEnabled && !vaultUnlocked) {
                        Button(
                            onClick = { vaultUnlocked = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isEs) "🔓 Desbloquear Identificación" else "🔓 Unlock ID", fontSize = 16.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("🛂 PASAPORTE + QR VISIT JAPAN WEB", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isEs) "Guardado en almacenamiento privado offline para mostrar en hoteles y tiendas Tax-Free." else "Stored offline in private storage for hotels & Tax-Free.",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("🪪 DNI / DOCUMENTO DE IDENTIDAD", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isEs) "Anverso y reverso disponibles sin conexión." else "Front and back available offline.",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(if (isEs) "🔍 Buscar vuelos, hoteles, JR Pass..." else "🔍 Search bookings...", fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                fontSize = 17.sp,
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

            else -> {
                items(guides) { guide ->
                    KotlinConfCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = guide.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
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
    }
}

// ============================================================================
// TAB 5: VOICE TRANSLATOR & GEMINI AI (SEGMENTED & LARGE READABLE CONTROLS)
// ============================================================================
@Composable
private fun GeminiAndTranslatorScreen(isEs: Boolean) {
    val context = LocalContext.current
    val portablePlan by SharedTravelPlanStore.planState.collectAsState()
    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0: Traductor de Voz, 1: Asistente IA Gemini

    var phraseInput by rememberSaveable { mutableStateOf("¿Dónde está el andén del Shinkansen?") }
    var japaneseHeardInput by rememberSaveable {
        mutableStateOf("いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？")
    }
    var rotateJapaneseCard180 by rememberSaveable { mutableStateOf(false) }
    var geminiPrompt by rememberSaveable { mutableStateOf("Traducir carta de Ramen y convertir precios ¥ a EUR") }
    var geminiResponse by rememberSaveable {
        mutableStateOf(GeminiAssistantEngine.generateSmartTravelAnswer("ramen", 164.80))
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = {
                        Text(
                            text = if (isEs) "🎙️ Traductor de Voz" else "🎙️ Voice Translator",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = {
                        Text(
                            text = if (isEs) "✨ Asistente Gemini IA" else "✨ Gemini AI",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        if (activeSection == 0) {
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
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { rotateJapaneseCard180 = !rotateJapaneseCard180 }) {
                            Text(if (rotateJapaneseCard180) "Normal" else "🔄 180°", fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
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
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(if (isEs) "🎙️ Hablar por Micro" else "🎙️ Speak Mic", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { speakOutLoud(translation.japaneseText, Locale.JAPAN) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(if (isEs) "🔊 Sonar en Japonés" else "🔊 Play Japanese", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .rotate(if (rotateJapaneseCard180) 180f else 0f)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = phraseInput,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = translation.japaneseText,
                                fontSize = 24.sp,
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

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfflineTranslatorEngine.essentialPhrases.forEach { preset ->
                            FilterChip(
                                selected = phraseInput == preset.sourceText,
                                onClick = {
                                    phraseInput = preset.sourceText
                                    speakOutLoud(preset.japaneseText, Locale.JAPAN)
                                },
                                label = {
                                    Text(
                                        text = "🔊 ${preset.categoryBadge}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            )
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

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(if (isEs) "🎙️ Escuchar Japonés" else "🎙️ Listen Japanese", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val spoken = if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText
                                speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(if (isEs) "🔊 Oír en Español" else "🔊 Hear in English", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = if (isEs) japaneseReplyTranslation.spanishSpokenText else japaneseReplyTranslation.englishSpokenText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfflineTranslatorEngine.incomingJapaneseSamples.forEach { sample ->
                            FilterChip(
                                selected = japaneseHeardInput == sample.japaneseHeardText,
                                onClick = {
                                    japaneseHeardInput = sample.japaneseHeardText
                                    val spoken = if (isEs) sample.spanishSpokenText else sample.englishSpokenText
                                    speakOutLoud(spoken, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)
                                },
                                label = {
                                    Text(
                                        text = "🔊 ${sample.contextLabel}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                // Gemini AI Assistant & Plan Optimizer
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Asistente Gemini IA" else "Gemini AI Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = geminiPrompt,
                        onValueChange = { geminiPrompt = it },
                        label = { Text(if (isEs) "Pregunta o traduce un menú en ¥..." else "Ask Gemini or translate ¥ menu...", fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                geminiResponse = GeminiAssistantEngine.generateSmartTravelAnswer(geminiPrompt, 164.80)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEs) "✨ Consultar" else "✨ Ask Gemini", fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val updated = AiPlanBuilderEngine.improveAndExpandPlan(
                                    currentPlan = portablePlan,
                                    userInstruction = "Mejorar plan y añadir Osaka"
                                )
                                val withWeather = AiPlanBuilderEngine.optimizePlanWithWeather(
                                    currentPlan = updated,
                                    forecasts = SharedWeatherForecastStore.forecastsState.value
                                )
                                SharedTravelPlanStore.updatePlan(withWeather)
                                geminiResponse = withWeather.aiOptimizationSummary
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEs) "🗺️ Mejorar mi Ruta" else "🗺️ Improve Route", fontSize = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = geminiResponse,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 6: SETTINGS & ABOUT (SEGMENTED & CLEAN)
// ============================================================================
@Composable
private fun SettingsAndAboutScreen(
    isEs: Boolean,
    prefs: UserPreferences,
    prefsRepository: DefaultUserPreferencesRepository
) {
    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0: Ajustes, 1: Acerca de
    var emailInput by rememberSaveable { mutableStateOf(prefs.googleAccountEmail ?: "carlos.travel.japan@gmail.com") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = {
                        Text(
                            text = if (isEs) "⚙️ Preferencias y Sync" else "⚙️ Preferences & Sync",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = {
                        Text(
                            text = if (isEs) "ℹ️ Acerca de" else "ℹ️ About",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        if (activeSection == 0) {
            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Idioma y Tema Visual" else "Language & Theme",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = if (isEs) "Idioma:" else "Language:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.language == AppLanguagePreference.SYSTEM,
                            onClick = { prefsRepository.setLanguage(AppLanguagePreference.SYSTEM) },
                            label = { Text(if (isEs) "Sistema" else "System", fontSize = 15.sp) }
                        )
                        FilterChip(
                            selected = prefs.language == AppLanguagePreference.SPANISH,
                            onClick = { prefsRepository.setLanguage(AppLanguagePreference.SPANISH) },
                            label = { Text("🇪🇸 Español", fontSize = 15.sp) }
                        )
                        FilterChip(
                            selected = prefs.language == AppLanguagePreference.ENGLISH,
                            onClick = { prefsRepository.setLanguage(AppLanguagePreference.ENGLISH) },
                            label = { Text("🇬🇧 English", fontSize = 15.sp) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isEs) "Tema:" else "Theme:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.SYSTEM,
                            onClick = { prefsRepository.setThemeMode(AppThemeMode.SYSTEM) },
                            label = { Text(if (isEs) "Sistema" else "System", fontSize = 15.sp) }
                        )
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.LIGHT,
                            onClick = { prefsRepository.setThemeMode(AppThemeMode.LIGHT) },
                            label = { Text(if (isEs) "☀️ Claro" else "☀️ Light", fontSize = 15.sp) }
                        )
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.DARK,
                            onClick = { prefsRepository.setThemeMode(AppThemeMode.DARK) },
                            label = { Text(if (isEs) "🌙 Oscuro" else "🌙 Dark", fontSize = 15.sp) }
                        )
                    }
                }
            }

            item {
                KotlinConfCard {
                    Text(
                        text = if (isEs) "Sincronización con Google" else "Google Account Sync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account", fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            prefsRepository.setGoogleAccountSync(
                                email = emailInput,
                                lastSyncedMillis = System.currentTimeMillis()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEs) "🔄 Sincronizar Dispositivos" else "🔄 Sync Devices", fontSize = 15.sp)
                    }
                }
            }
        } else {
            item {
                KotlinConfCard {
                    Text(
                        text = "Japan Travel Tools · v${OpenSourceLibrariesCatalog.APP_VERSION_NAME}",
                        fontSize = 20.sp,
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
                                Text(lib.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = "${lib.version} · ${lib.license}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isEs) lib.purposeEs else lib.purposeEn,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isEs) "📡 Conexión Activa: ${kind.badgeEs}" else "📡 Active Connection: ${kind.badgeEn}",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onOpenInternetPanel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEs) "📶 Abrir Panel Wi-Fi / Datos" else "📶 Open Wi-Fi / Cellular Panel", fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onOpenSimSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEs) "📲 Cambiar entre SIM y eSIM" else "📲 Switch SIM / eSIM", fontSize = 16.sp)
        }

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
