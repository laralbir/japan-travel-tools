package com.laralbir.japantraveltools.feature.transport.pkpass

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem

data class BarcodeDisplayData(
    val title: String,
    val subtitle: String,
    val extraDetails: String?,
    val barcodeMessage: String,
    val barcodeFormat: String?,
    val barcodeAltText: String?
)

data class DuplicateResolutionState(
    val parsedPass: ParsedPkPass,
    val existingSegment: TransportSegmentItem
)

@Composable
fun BarcodeRendererView(
    message: String,
    format: String?,
    altText: String?,
    modifier: Modifier = Modifier
) {
    val matrix = remember(message, format) {
        BarcodeBitmapGenerator.generateMatrix(message, format, 320, 320)
    }

    val imageBitmap = remember(matrix) {
        matrix?.let { m ->
            val bmp = Bitmap.createBitmap(m.width, m.height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(m.width * m.height) { idx ->
                val x = idx % m.width
                val y = idx / m.width
                if (m.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
            bmp.setPixels(pixels, 0, m.width, 0, 0, m.width, m.height)
            bmp.asImageBitmap()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageBitmap != null && matrix != null) {
            val isLinear = matrix.width > matrix.height * 2
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                modifier = Modifier.padding(4.dp)
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Código de barras o QR",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .padding(16.dp)
                        .then(
                            if (isLinear) {
                                Modifier.size(width = 260.dp, height = 90.dp)
                            } else {
                                Modifier.size(240.dp)
                            }
                        )
                )
            }
        } else {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                modifier = Modifier.padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
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
                        text = message.ifBlank { "TICKET" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (!altText.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = altText,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun PkPassBarcodeViewerDialog(
    data: BarcodeDisplayData,
    isEs: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (data.barcodeFormat?.contains("Aztec", ignoreCase = true) == true) "AZTEC · RENFE"
                            else if (data.barcodeFormat?.contains("PDF417", ignoreCase = true) == true) "PDF417 · PASS"
                            else "QR · DIGITAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = if (isEs) "Billete Offline" else "Offline Ticket",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = data.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (data.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = data.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                if (!data.extraDetails.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = data.extraDetails,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                BarcodeRendererView(
                    message = data.barcodeMessage,
                    format = data.barcodeFormat,
                    altText = data.barcodeAltText
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEs) "💡 Sube el brillo de la pantalla para escanear en tornos o controles" else "💡 Increase screen brightness when scanning at gates",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isEs) "Cerrar Billete" else "Close Ticket", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PkPassDuplicateResolutionDialog(
    state: DuplicateResolutionState,
    isEs: Boolean,
    onReplace: () -> Unit,
    onDuplicate: () -> Unit,
    onDismiss: () -> Unit
) {
    val existing = state.existingSegment
    val pass = state.parsedPass

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEs) "⚠️ Billete ya existente" else "⚠️ Ticket already exists",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isEs) {
                        "Se ha detectado un billete similar ya registrado en tu itinerario:"
                    } else {
                        "A similar ticket is already logged in your itinerary:"
                    },
                    fontSize = 13.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEs) "📋 Actual:" else "📋 Current:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${existing.operatorName} ${existing.lineOrNumber} (${existing.departureName} ➔ ${existing.arrivalName})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📅 ${existing.departureTimeIso} · 💺 ${existing.seatOrCarInfo}",
                            fontSize = 12.sp
                        )
                        if (existing.stationExitOrBelt.isNotBlank()) {
                            Text(
                                text = "🔖 ${existing.stationExitOrBelt}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEs) "🆕 Nuevo archivo importado:" else "🆕 Newly imported:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${pass.operatorName} ${pass.serviceNumber} (${pass.originName} ➔ ${pass.destinationName})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📅 ${pass.departureDateIso}T${pass.departureTime} · 💺 Coche ${pass.carriage ?: "-"} Asiento ${pass.seat ?: "-"}",
                            fontSize = 12.sp
                        )
                        if (!pass.bookingCode.isNullOrBlank() || !pass.ticketNumber.isNullOrBlank()) {
                            Text(
                                text = "🔖 Localizador: ${pass.bookingCode ?: pass.ticketNumber}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = if (isEs) "¿Deseas sustituir el existente o duplicarlo como un tramo adicional?" else "Do you want to replace the existing one or duplicate it?",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDuplicate) {
                    Text(if (isEs) "Duplicar" else "Duplicate", fontSize = 13.sp)
                }
                Button(onClick = onReplace) {
                    Text(if (isEs) "Sustituir" else "Replace", fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEs) "Cancelar" else "Cancel", fontSize = 13.sp)
            }
        }
    )
}
