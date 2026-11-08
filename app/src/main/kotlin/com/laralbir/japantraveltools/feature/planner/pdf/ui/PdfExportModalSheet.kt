package com.laralbir.japantraveltools.feature.planner.pdf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfDocumentLayoutCalculator
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportOptions
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportResult
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportScope
import kotlinx.coroutines.launch

private val ColorAkabeni = Color(PdfDocumentLayoutCalculator.COLOR_AKABENI)
private val ColorAiIro = Color(PdfDocumentLayoutCalculator.COLOR_AI_IRO)

/**
 * Material 3 ModalBottomSheet to configure and export itinerary PDF documents 100% offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportModalSheet(
    planTitle: String,
    currentDayIndex: Int,
    totalDays: Int,
    onExportPdf: suspend (PdfExportOptions) -> PdfExportResult?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedScope by remember { mutableStateOf(PdfExportScope.SINGLE_DAY) }
    var includeJapaneseNames by remember { mutableStateOf(true) }
    var includeTransportSegments by remember { mutableStateOf(true) }
    var includeDayMap by remember { mutableStateOf(true) }
    var includeEmergencyContacts by remember { mutableStateOf(true) }

    var isExporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pdf_export_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.pdf_export_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(onClick = onDismiss, enabled = !isExporting, modifier = Modifier.size(32.dp)) {
                        Text(
                            text = "✕",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scope Selection Section
            Text(
                text = stringResource(R.string.pdf_export_scope_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ColorAiIro
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Option 1: Single Day
            ScopeSelectionCard(
                title = stringResource(R.string.pdf_export_scope_day, currentDayIndex),
                subtitle = planTitle,
                isSelected = selectedScope == PdfExportScope.SINGLE_DAY,
                onClick = { selectedScope = PdfExportScope.SINGLE_DAY }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Option 2: Full Trip
            ScopeSelectionCard(
                title = stringResource(R.string.pdf_export_scope_full, totalDays),
                subtitle = "Portada + Todos los días + Transportes + Emergencias",
                isSelected = selectedScope == PdfExportScope.FULL_TRIP,
                onClick = { selectedScope = PdfExportScope.FULL_TRIP }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Options Section
            Text(
                text = stringResource(R.string.pdf_export_options_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ColorAiIro
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Checkbox: Japanese Names
            OptionCheckboxRow(
                text = stringResource(R.string.pdf_export_opt_japanese),
                checked = includeJapaneseNames,
                onCheckedChange = { includeJapaneseNames = it }
            )

            // Checkbox: Transport Segments
            OptionCheckboxRow(
                text = stringResource(R.string.pdf_export_opt_transport),
                checked = includeTransportSegments,
                onCheckedChange = { includeTransportSegments = it }
            )

            // Checkbox: Daily Route Map
            OptionCheckboxRow(
                text = stringResource(R.string.pdf_export_opt_map),
                checked = includeDayMap,
                onCheckedChange = { includeDayMap = it }
            )

            // Checkbox: Emergency Contacts
            OptionCheckboxRow(
                text = stringResource(R.string.pdf_export_opt_emergency),
                checked = includeEmergencyContacts,
                onCheckedChange = { includeEmergencyContacts = it }
            )

            // Error notice if generation failed
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isExporting = true
                        errorMessage = null
                        try {
                            val options = PdfExportOptions(
                                scope = selectedScope,
                                targetDayIndex = if (selectedScope == PdfExportScope.SINGLE_DAY) currentDayIndex else null,
                                includeJapaneseNames = includeJapaneseNames,
                                includeTransportSegments = includeTransportSegments,
                                includeEmergencyContacts = includeEmergencyContacts,
                                includeDayMap = includeDayMap
                            )
                            val result = onExportPdf(options)
                            if (result != null && result.exists) {
                                PdfShareHelper.sharePdf(context, result)
                                onDismiss()
                            } else {
                                errorMessage = context.getString(R.string.pdf_export_error)
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: context.getString(R.string.pdf_export_error)
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAkabeni,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.pdf_export_generating),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.pdf_export_action_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ColorAkabeni else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) ColorAkabeni.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = ColorAkabeni)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OptionCheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = ColorAkabeni)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
