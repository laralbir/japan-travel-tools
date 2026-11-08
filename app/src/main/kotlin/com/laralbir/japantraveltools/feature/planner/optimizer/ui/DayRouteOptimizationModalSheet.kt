package com.laralbir.japantraveltools.feature.planner.optimizer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.optimizer.model.RouteOptimizationResult
import java.util.Locale

// Traditional Japanese Palette Accents (SPEC-00 / SPEC-13)
private val ColorAkabeniTorii = Color(0xFFC93A40) // Torii vermilion red (赤紅)
private val ColorAiiroIndigo = Color(0xFF1E3A5F)  // Japanese deep indigo (藍色)
private val ColorMatchaGreen = Color(0xFF2E7D32)  // Japanese matcha tea green (抹茶)

/**
 * Material 3 Modal Bottom Sheet for reviewing and applying geodesic route optimization (SPEC-13 / ANALYSIS-15).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayRouteOptimizationModalSheet(
    result: RouteOptimizationResult,
    onApplyOptimization: (List<ItineraryStopItem>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEs = LocalConfiguration.current.locales[0].language.startsWith("es")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row: Title, Subtitle, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.route_optimizer_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.route_optimizer_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Comparative Card with Akabeni / Ai-iro traditional Japanese styling
            OptimizationComparisonCard(result = result)

            Spacer(modifier = Modifier.height(16.dp))

            // Suggested Sequence Header
            Text(
                text = stringResource(
                    R.string.route_optimizer_suggested_sequence,
                    result.optimizedStops.size
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable List of Suggested Stops
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                result.optimizedStops.forEach { stop ->
                    OptimizedStopRow(stop = stop, isEs = isEs)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Buttons
            Button(
                onClick = { onApplyOptimization(result.optimizedStops) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAkabeniTorii,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.route_optimizer_apply_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = stringResource(R.string.route_optimizer_discard_button),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Prominent comparison card contrasting current distance vs optimized distance with savings badges.
 */
@Composable
private fun OptimizationComparisonCard(
    result: RouteOptimizationResult,
    modifier: Modifier = Modifier
) {
    val origKm = result.originalDistanceMeters / 1000.0
    val optKm = result.optimizedDistanceMeters / 1000.0
    val savedKm = result.distanceSavedMeters / 1000.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.5.dp,
            if (result.isBeneficial) ColorAkabeniTorii.copy(alpha = 0.35f) else ColorAiiroIndigo.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Distance Metric Row: Current -> Optimized
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Distance
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.route_optimizer_current_distance),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", origKm),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (result.isBeneficial) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                // Arrow
                Text(
                    text = "➔",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isBeneficial) ColorAkabeniTorii else ColorAiiroIndigo
                )

                // Optimized Distance
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.route_optimizer_optimized_distance),
                        fontSize = 12.sp,
                        color = if (result.isBeneficial) ColorAkabeniTorii else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", optKm),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (result.isBeneficial) ColorAkabeniTorii else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row
            if (result.isBeneficial) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Savings in Km & %
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ColorAkabeniTorii.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.route_optimizer_savings_badge,
                                savedKm,
                                result.percentageSaved.toInt()
                            ),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorAkabeniTorii,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // Estimated Transit Time Saved
                    if (result.estimatedMinutesSaved > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ColorMatchaGreen.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.route_optimizer_time_saved,
                                    result.estimatedMinutesSaved
                                ),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorMatchaGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorAiiroIndigo.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.route_optimizer_already_optimal),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorAiiroIndigo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Single stop row presenting chronological order badge, suggested recalculated time, title and status pills.
 */
@Composable
private fun OptimizedStopRow(
    stop: ItineraryStopItem,
    isEs: Boolean,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (stop.category) {
        StopCategory.SIGHTSEEING -> Color(0xFF6750A4)
        StopCategory.TRANSPORT -> Color(0xFF0061A4)
        StopCategory.FOOD -> Color(0xFFC0392B)
        StopCategory.HOTEL -> Color(0xFF006C4C)
        StopCategory.ACTIVITY -> Color(0xFFD35400)
        StopCategory.SHOPPING -> Color(0xFF984061)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (stop.isTimeFixed) ColorAkabeniTorii.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sequential Order Badge (1..N)
            Surface(
                shape = CircleShape,
                color = if (stop.isTimeFixed) ColorAkabeniTorii else ColorAiiroIndigo,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${stop.order}",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // Stop Info Column
            Column(modifier = Modifier.weight(1f)) {
                // Suggested Timing & Category Pill Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Suggested Time
                    if (stop.startTime.isNotBlank()) {
                        Text(
                            text = "⏰ ${stop.startTime}${if (stop.endTime.isNotBlank()) " – ${stop.endTime}" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isEs) stop.category.labelEs else stop.category.labelEn,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = stop.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Japanese Place Name (if present)
                if (stop.placeNameJapanese.isNotBlank()) {
                    Text(
                        text = stop.placeNameJapanese,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Fixed Time or Missing Coordinates Badges
                if (stop.isTimeFixed) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ColorAkabeniTorii.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(R.string.route_optimizer_fixed_badge),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorAkabeniTorii,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else if (stop.latitude == null || stop.longitude == null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF57C00).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.route_optimizer_no_coords_badge),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD84315),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
