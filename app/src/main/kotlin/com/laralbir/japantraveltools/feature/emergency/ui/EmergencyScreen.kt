package com.laralbir.japantraveltools.feature.emergency.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.ui.components.ConsularMissionCard
import com.laralbir.japantraveltools.feature.emergency.ui.components.EmergencyCallCard
import com.laralbir.japantraveltools.feature.emergency.ui.components.EmergencyPhraseAudioCard
import com.laralbir.japantraveltools.feature.emergency.ui.components.MedicalProfileSection
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit = {},
    viewModel: EmergencyViewModel = viewModel(
        factory = EmergencyViewModel.provideFactory(LocalContext.current)
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Offline Japanese Text-To-Speech engine
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

    fun speakInJapanese(text: String) {
        val engine = ttsEngine
        if (engine == null) {
            Toast.makeText(
                context,
                context.getString(R.string.emergency_tts_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val res = engine.setLanguage(Locale.JAPANESE)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.JAPAN)
        }
        engine.setSpeechRate(0.85f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "emergency_${System.currentTimeMillis()}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.emergency_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% Offline-First",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row Navigation
            val tabs = listOf(
                EmergencyTab.NUMBERS to stringResource(R.string.emergency_tab_numbers),
                EmergencyTab.CONSULATES to stringResource(R.string.emergency_tab_consulates),
                EmergencyTab.MEDICAL to stringResource(R.string.emergency_tab_medical),
                EmergencyTab.PHRASES to stringResource(R.string.emergency_tab_phrases)
            )

            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = uiState.selectedTab == pair.first,
                        onClick = { viewModel.selectTab(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                maxLines = 1,
                                fontWeight = if (uiState.selectedTab == pair.first) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (uiState.selectedTab) {
                EmergencyTab.NUMBERS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.emergency_numbers_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(uiState.emergencyNumbers, key = { it.id }) { contact ->
                            EmergencyCallCard(contact = contact)
                        }
                    }
                }

                EmergencyTab.CONSULATES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Country selector chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableCountries.forEach { (code, name) ->
                                val flag = when (code.uppercase()) {
                                    "ES" -> "🇪🇸 "
                                    "MX" -> "🇲🇽 "
                                    "CO" -> "🇨🇴 "
                                    "AR" -> "🇦🇷 "
                                    "CL" -> "🇨🇱 "
                                    "PE" -> "🇵🇪 "
                                    else -> "🌐 "
                                }
                                FilterChip(
                                    selected = uiState.selectedCountryCode.equals(code, ignoreCase = true),
                                    onClick = { viewModel.selectCountry(code) },
                                    label = { Text(flag + name, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.consularMissions, key = { it.id }) { mission ->
                                ConsularMissionCard(mission = mission)
                            }
                        }
                    }
                }

                EmergencyTab.MEDICAL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_medical_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        MedicalProfileSection(
                            profile = uiState.medicalProfile,
                            isSaving = uiState.isSavingProfile,
                            saveSuccess = uiState.profileSaveSuccess,
                            onSaveProfile = { viewModel.saveMedicalProfile(it) },
                            onResetSaveSuccess = { viewModel.resetProfileSaveSuccess() }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                EmergencyTab.PHRASES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Phrase Category Selector Chips
                        val categories = listOf(
                            null to stringResource(R.string.emergency_filter_all),
                            EmergencyCategory.MEDICAL to ("🩺 " + stringResource(R.string.emergency_filter_medical)),
                            EmergencyCategory.POLICE to ("👮 " + stringResource(R.string.emergency_filter_police)),
                            EmergencyCategory.DISASTER to ("⚠️ " + stringResource(R.string.emergency_filter_disaster)),
                            EmergencyCategory.ALLERGY to ("🥜 " + stringResource(R.string.emergency_filter_allergy))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { (cat, label) ->
                                FilterChip(
                                    selected = uiState.selectedPhraseCategory == cat,
                                    onClick = { viewModel.selectPhraseCategory(cat) },
                                    label = { Text(label, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.emergencyPhrases, key = { it.id }) { phrase ->
                                EmergencyPhraseAudioCard(
                                    phrase = phrase,
                                    onSpeak = { speakInJapanese(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
