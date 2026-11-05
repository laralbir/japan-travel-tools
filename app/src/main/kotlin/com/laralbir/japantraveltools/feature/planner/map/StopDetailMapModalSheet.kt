package com.laralbir.japantraveltools.feature.planner.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailMapModalSheet(
    stop: ItineraryStopItem,
    stopIndex: Int,
    isEs: Boolean,
    allGuides: List<TravelGuideArticle>,
    allDocuments: List<TravelDocumentItem>,
    allSegments: List<TransportSegmentItem>,
    onDismiss: () -> Unit,
    onOpenGuide: (TravelGuideArticle) -> Unit,
    onViewTicket: (TravelDocumentItem?, TransportSegmentItem?) -> Unit,
    onOpenInGoogleMaps: (ItineraryStopItem) -> Unit,
    onEditStop: (ItineraryStopItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    val linkedResources = remember(stop, allGuides, allDocuments, allSegments) {
        ItineraryLinkedResourcesResolver.resolveAll(
            stop = stop,
            allGuides = allGuides,
            allDocuments = allDocuments,
            allSegments = allSegments
        )
    }

    val categoryColor = when (stop.category) {
        StopCategory.SIGHTSEEING -> Color(0xFF6750A4)
        StopCategory.TRANSPORT -> Color(0xFF0061A4)
        StopCategory.FOOD -> Color(0xFFC0392B)
        StopCategory.HOTEL -> Color(0xFF006C4C)
        StopCategory.ACTIVITY -> Color(0xFFD35400)
        StopCategory.SHOPPING -> Color(0xFF984061)
    }

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
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header Row: Badge #, Title, Category Pill, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Badge Number
                    Surface(
                        shape = CircleShape,
                        color = categoryColor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$stopIndex",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stop.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = categoryColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isEs) stop.category.labelEs else stop.category.labelEn,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = categoryColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (stop.startTime.isNotBlank()) {
                                Text(
                                    text = "⏰ ${stop.startTime}${if (stop.endTime.isNotBlank()) " – ${stop.endTime}" else ""}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Close Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Japanese Place Name with Fast Copy
            if (stop.placeNameJapanese.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEs) "Nombre en japonés (para taxi o locales):"
                                else "Japanese Name (for taxi or locals):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Copy button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Japanese Name", stop.placeNameJapanese)
                                    clipboard?.setPrimaryClip(clip)
                                    copiedToClipboard = true
                                    Toast.makeText(
                                        context,
                                        if (isEs) "¡Copiado al portapapeles!" else "Copied to clipboard!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (copiedToClipboard) "✓ ${if (isEs) "Copiado" else "Copied"}"
                                    else "📋 ${if (isEs) "Copiar" else "Copy"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stop.placeNameJapanese,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Notes Section
            if (stop.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isEs) "Notas y consejos:" else "Notes & Tips:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stop.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // LINKED TRAVEL GUIDES
            if (linkedResources.relatedGuides.isNotEmpty()) {
                val guide = linkedResources.relatedGuides.first()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F8E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📖", fontSize = 16.sp)
                            Text(
                                text = if (isEs) "Guía de Viaje Relacionada" else "Related Travel Guide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                            if (guide.cityRegion.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFC8E6C9)
                                ) {
                                    Text(
                                        text = guide.cityRegion,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = guide.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF1B5E20)
                        )
                        if (guide.markdownSummary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            val summarySnippet = guide.markdownSummary
                                .lines()
                                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                                ?: guide.markdownSummary.take(120)
                            Text(
                                text = summarySnippet,
                                fontSize = 12.5.sp,
                                color = Color(0xFF33691E),
                                maxLines = 2
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenGuide(guide)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isEs) "📖 Leer Guía Completa" else "📖 Read Full Guide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // LINKED TICKETS / TRANSPORT / RESERVATIONS
            if (linkedResources.relatedDocuments.isNotEmpty() || linkedResources.relatedSegments.isNotEmpty()) {
                val matchedDoc = linkedResources.relatedDocuments.firstOrNull()
                val matchedSeg = linkedResources.relatedSegments.firstOrNull()

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE3F2FD),
                    border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🎫", fontSize = 16.sp)
                            Text(
                                text = if (isEs) "Billete / Reserva Asociada" else "Associated Ticket / Booking",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1565C0)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        val ticketTitle = matchedSeg?.let { "${it.operatorName} · ${it.lineOrNumber}" }
                            ?: matchedDoc?.title
                            ?: ""
                        Text(
                            text = ticketTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF0D47A1)
                        )

                        val detailsText = matchedSeg?.let {
                            "${it.departureName} ➔ ${it.arrivalName} · 💺 ${it.seatOrCarInfo}"
                        } ?: matchedDoc?.let {
                            it.locatorCode?.let { loc -> "Localizador: $loc" } ?: it.referenceOrNotes.lines().firstOrNull()
                        } ?: ""

                        if (detailsText.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = detailsText,
                                fontSize = 12.5.sp,
                                color = Color(0xFF1976D2)
                            )
                        }

                        // Button to view QR / Ticket
                        if (linkedResources.hasAnyBarcode) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onDismiss()
                                    onViewTicket(matchedDoc, matchedSeg)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0D47A1),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("📱", fontSize = 14.sp)
                                    Text(
                                        text = if (isEs) "Ver Billete / Código QR (Pantalla Completa)"
                                        else "View Ticket / QR Code (Full Screen)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))

            // Action Buttons: Open in Google Maps & Edit Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenInGoogleMaps(stop)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isEs) "🗺️ Google Maps" else "🗺️ Google Maps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEditStop(stop)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isEs) "✏️ Editar Parada" else "✏️ Edit Stop",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
