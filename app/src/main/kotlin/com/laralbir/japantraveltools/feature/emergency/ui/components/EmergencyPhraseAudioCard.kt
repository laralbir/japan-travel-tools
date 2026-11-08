package com.laralbir.japantraveltools.feature.emergency.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyPhraseItem
import java.util.Locale

@Composable
fun EmergencyPhraseAudioCard(
    phrase: EmergencyPhraseItem,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEs = Locale.getDefault().language.lowercase().startsWith("es")
    val translation = if (isEs) phrase.spanishText else phrase.englishText

    val categoryBadge = when (phrase.category) {
        EmergencyCategory.MEDICAL -> "🩺 " + stringResource(R.string.emergency_filter_medical)
        EmergencyCategory.POLICE -> "👮 " + stringResource(R.string.emergency_filter_police)
        EmergencyCategory.DISASTER -> "⚠️ " + stringResource(R.string.emergency_filter_disaster)
        EmergencyCategory.ALLERGY -> "🥜 " + stringResource(R.string.emergency_filter_allergy)
    }

    val categoryColor = when (phrase.category) {
        EmergencyCategory.MEDICAL -> MaterialTheme.colorScheme.errorContainer
        EmergencyCategory.POLICE -> MaterialTheme.colorScheme.primaryContainer
        EmergencyCategory.DISASTER -> MaterialTheme.colorScheme.tertiaryContainer
        EmergencyCategory.ALLERGY -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = categoryBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = categoryColor
                    ),
                    border = null
                )

                Button(
                    onClick = { onSpeak(phrase.japaneseKanji) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.emergency_action_listen),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent high contrast display card for showing to locals
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = phrase.japaneseKanji,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = phrase.romaji,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Localized Translation
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isEs) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phrase.spanishText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
