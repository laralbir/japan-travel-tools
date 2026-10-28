package com.laralbir.japantraveltools.ui.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.feature.currency.SharedCurrencyRateStore
import com.laralbir.japantraveltools.feature.taxfree.AiReceiptScannerEngine
import com.laralbir.japantraveltools.feature.taxfree.CategoryExpenseBreakdown
import com.laralbir.japantraveltools.feature.taxfree.ExpenseCategory
import com.laralbir.japantraveltools.feature.taxfree.GoodsType
import com.laralbir.japantraveltools.feature.taxfree.ReceiptProductItem
import com.laralbir.japantraveltools.feature.taxfree.ReceiptScanResult
import com.laralbir.japantraveltools.feature.taxfree.ScannerSource
import com.laralbir.japantraveltools.feature.taxfree.SharedShoppingReceiptsStore
import com.laralbir.japantraveltools.feature.taxfree.ShoppingReceipt
import com.laralbir.japantraveltools.feature.taxfree.TaxFreeCalculator
import com.laralbir.japantraveltools.feature.taxfree.TaxFreeClaimStatus
import com.laralbir.japantraveltools.feature.taxfree.TaxFreeReport
import com.laralbir.japantraveltools.feature.taxfree.TaxFreeReportGenerator
import com.laralbir.japantraveltools.feature.taxfree.TravelBudgetSummary
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val TOKYO_ZONE_ID = ZoneId.of("Asia/Tokyo")
private val TOKYO_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
fun ShoppingReceiptsScreen(
    isEs: Boolean,
    prefs: UserPreferences? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SharedShoppingReceiptsStore.init(context)
    }

    val receipts by SharedShoppingReceiptsStore.receiptsState.collectAsState()
    val rateSnapshot by SharedCurrencyRateStore.rateState.collectAsState()

    // Tasa EUR -> JPY (por defecto 160.0 si no hay red)
    val eurToJpyRate = remember(rateSnapshot) {
        rateSnapshot.rates["JPY"] ?: 160.0
    }

    // Presupuesto y cálculos consolidados
    var budgetSummary by remember {
        mutableStateOf(
            TaxFreeCalculator.calculateBudgetSummary(receipts, eurToJpyRate)
        )
    }

    LaunchedEffect(receipts, eurToJpyRate) {
        budgetSummary = TaxFreeCalculator.calculateBudgetSummary(receipts, eurToJpyRate)
    }

    // Estados de UI y modales
    var isEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingReceipt by remember { mutableStateOf<ShoppingReceipt?>(null) }
    var confirmationState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var showScanSourceDialog by rememberSaveable { mutableStateOf(false) }
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var scanResultNotice by remember { mutableStateOf<String?>(null) }

    // Filtros de listado
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) }
    // 0: Todos, 1: Tax-Free Aplicable, 2: Pendientes Aduana, 3: Sellados, 4: Compras, 5: Comida

    val filteredReceipts = remember(receipts, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> receipts.filter { it.isTaxFreeEligible }
            2 -> receipts.filter { it.taxFreeStatus == TaxFreeClaimStatus.PENDING && it.isTaxFreeEligible }
            3 -> receipts.filter { it.taxFreeStatus == TaxFreeClaimStatus.CUSTOMS_STAMPED || it.taxFreeStatus == TaxFreeClaimStatus.REFUNDED }
            4 -> receipts.filter { it.category == ExpenseCategory.SHOPPING }
            5 -> receipts.filter { it.category == ExpenseCategory.FOOD }
            else -> receipts
        }
    }

    // Manejador de escaneo (Cámara / Galería / Archivo / Drive con soporte de Imágenes y PDFs)
    fun processScannedImage(bitmap: Bitmap) {
        isScanning = true
        scanResultNotice = null
        coroutineScope.launch {
            try {
                val apiKey = prefs?.geminiApiKey?.takeIf { it.isNotBlank() }
                val imageBytes = withContext(Dispatchers.IO) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }

                // OCR con ML Kit (reconocimiento trilingüe robusto)
                val rawOcrText = suspendCancellableCoroutine<String> { continuation ->
                    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            continuation.resume(visionText.text)
                        }
                        .addOnFailureListener {
                            continuation.resume("")
                        }
                }

                val scanResult = withContext(Dispatchers.Default) {
                    AiReceiptScannerEngine.scanWithFallback(
                        rawOcrText = rawOcrText,
                        geminiApiKey = apiKey,
                        imageBytes = imageBytes
                    )
                }

                val notice = when (scanResult.source) {
                    ScannerSource.GEMINI_CLOUD_VISION -> {
                        if (isEs) "✨ Ticket procesado con IA Gemini Vision" else "✨ Receipt processed with Gemini Vision AI"
                    }
                    ScannerSource.LOCAL_MLKIT_OCR -> {
                        if (isEs) "⚡ Ticket procesado con OCR Local Offline" else "⚡ Receipt processed with Local Offline OCR"
                    }
                }
                scanResultNotice = notice

                // Abrir editor con los datos detectados
                editingReceipt = scanResult.receipt
                isEditorOpen = true
            } catch (e: Exception) {
                scanResultNotice = if (isEs) {
                    "No se pudo extraer el ticket automáticamente. Puedes introducirlo a mano."
                } else {
                    "Could not extract receipt automatically. You can enter details manually."
                }
            } finally {
                isScanning = false
            }
        }
    }

    // Lanzadores de Cámara, Almacenamiento Local y Google Drive
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                val bmp = decodeBitmapOrPdfFromUri(context, uri)
                if (bmp != null) processScannedImage(bmp)
            }
        }
    }

    val pickLocalImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = decodeBitmapOrPdfFromUri(context, uri)
            if (bmp != null) processScannedImage(bmp)
        }
    }

    val openDriveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val bmp = decodeBitmapOrPdfFromUri(context, uri)
            if (bmp != null) {
                processScannedImage(bmp)
            } else {
                Toast.makeText(
                    context,
                    if (isEs) "No se pudo leer el archivo o imagen" else "Could not read file or image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                val file = File(context.cacheDir, "receipt_scan_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(
                context,
                if (isEs) "Permiso de cámara denegado" else "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchCameraScan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            runCatching {
                val file = File(context.cacheDir, "receipt_scan_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Cabecera de la sección
        Text(
            text = if (isEs) "🧾 Tickets de Compra y Tax-Free" else "🧾 Shopping Receipts & Tax-Free",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isEs) {
                "Control de gastos del viaje, detección de 5.000 ¥ para Tax-Free e informe de aduanas."
            } else {
                "Travel expense tracking, 5,000 ¥ Tax-Free threshold detection and customs report."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        // Tarjeta de Resumen Presupuestario y Tax-Free
        BudgetSummaryDashboardCard(
            isEs = isEs,
            summary = budgetSummary,
            eurToJpyRate = eurToJpyRate
        )

        Spacer(Modifier.height(10.dp))

        // Filas de Acciones: Alturas estandarizadas (48.dp) y formato uniforme
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila 1: Creación manual y Escáner con IA (con selector Cámara / Local / Drive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingReceipt = null
                        isEditorOpen = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isEs) "➕ Ticket Manual" else "➕ Manual Receipt",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { showScanSourceDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isEs) "✨ Escanear con IA" else "✨ Scan with AI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Fila 2: Acceso directo a Archivos (Local / Drive) e Informe Oficial de Aduanas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openDriveDocumentLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isEs) "📁 Local / ☁️ Drive" else "📁 Local / ☁️ Drive",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isEs) "📄 Informe Aduanas" else "📄 Customs Report",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Aviso de procesamiento de escaneo
        AnimatedVisibility(visible = isScanning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = if (isEs) {
                            "Escaneando ticket con IA (japonés, inglés o español)..."
                        } else {
                            "Scanning receipt with AI (Japanese, English, or Spanish)..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        scanResultNotice?.let { notice ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { scanResultNotice = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Fila de Chips de Filtro
        val filterTitles = remember(isEs, receipts.size) {
            if (isEs) {
                listOf("Todos (${receipts.size})", "Tax-Free ✅", "Pendientes ⏳", "Sellados 🛂", "Compras 🛍️", "Comida 🍜")
            } else {
                listOf("All (${receipts.size})", "Tax-Free ✅", "Pending ⏳", "Stamped 🛂", "Shopping 🛍️", "Food 🍜")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterTitles.forEachIndexed { index, title ->
                FilterChip(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    label = { Text(title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Listado de tickets
        if (filteredReceipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧾", fontSize = 42.sp)
                    Text(
                        text = if (isEs) "No hay tickets en esta sección" else "No receipts in this section",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isEs) {
                            "Añade tus compras a mano o escanea fotos de tickets (cámara, galería o Google Drive) para controlar tus gastos y preparar el reembolso Tax-Free de aduanas.\n\nSoporta comprobantes en japonés (日本語), inglés y español."
                        } else {
                            "Add purchases manually or scan receipt photos (camera, gallery or Google Drive) to track expenses and claim airport Tax-Free refunds.\n\nSupports receipts in Japanese (日本語), English, and Spanish."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredReceipts, key = { it.id }) { receipt ->
                    ShoppingReceiptCard(
                        isEs = isEs,
                        receipt = receipt,
                        eurToJpyRate = eurToJpyRate,
                        onEdit = {
                            editingReceipt = receipt
                            isEditorOpen = true
                        },
                        onDelete = {
                            confirmationState = ConfirmationDialogState(
                                title = if (isEs) "Eliminar Ticket" else "Delete Receipt",
                                message = if (isEs) {
                                    "¿Estás seguro de que deseas eliminar el ticket de '${receipt.storeName}' por importe de ¥${receipt.totalWithTaxYen.toInt()}?"
                                } else {
                                    "Are you sure you want to delete receipt from '${receipt.storeName}' with total ¥${receipt.totalWithTaxYen.toInt()}?"
                                },
                                confirmButtonText = if (isEs) "Eliminar" else "Delete",
                                dismissButtonText = if (isEs) "Cancelar" else "Cancel",
                                isDestructive = true,
                                onConfirm = {
                                    coroutineScope.launch {
                                        SharedShoppingReceiptsStore.deleteReceipt(receipt.id)
                                        confirmationState = null
                                    }
                                }
                            )
                        },
                        onStatusChange = { newStatus ->
                            coroutineScope.launch {
                                SharedShoppingReceiptsStore.saveReceipt(receipt.copy(taxFreeStatus = newStatus))
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet de Selección de Origen de Escaneo (Cámara, Local o Google Drive)
    if (showScanSourceDialog) {
        ScanSourceSelectorDialog(
            isEs = isEs,
            onDismiss = { showScanSourceDialog = false },
            onSelectCamera = { launchCameraScan() },
            onSelectLocal = { pickLocalImageLauncher.launch("image/*") },
            onSelectDrive = { openDriveDocumentLauncher.launch(arrayOf("image/*", "application/pdf")) }
        )
    }

    // Modal Sheet de Edición / Creación de Ticket
    if (isEditorOpen) {
        ShoppingReceiptEditorModalSheet(
            isEs = isEs,
            initialReceipt = editingReceipt,
            onDismiss = {
                isEditorOpen = false
                editingReceipt = null
            },
            onSave = { savedReceipt ->
                coroutineScope.launch {
                    SharedShoppingReceiptsStore.saveReceipt(savedReceipt)
                    isEditorOpen = false
                    editingReceipt = null
                }
            }
        )
    }

    // Diálogo de Confirmación de Borrado
    AppConfirmationDialog(
        state = confirmationState,
        onDismiss = { confirmationState = null }
    )

    // Diálogo de Informe Consolidado para Aduanas
    if (showReportDialog) {
        TaxFreeCustomsReportDialog(
            isEs = isEs,
            receipts = receipts,
            eurToJpyRate = eurToJpyRate,
            onDismiss = { showReportDialog = false }
        )
    }
}

@Composable
private fun ScanSourceSelectorDialog(
    isEs: Boolean,
    onDismiss: () -> Unit,
    onSelectCamera: () -> Unit,
    onSelectLocal: () -> Unit,
    onSelectDrive: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "📸 Escanear Ticket con IA" else "📸 AI Receipt Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", fontSize = 16.sp)
                    }
                }

                Text(
                    text = if (isEs) {
                        "Elige de dónde tomar el comprobante para extraer fecha, tienda, productos, importes e IVA:"
                    } else {
                        "Choose where to get the receipt from to extract date, merchant, items, totals and tax:"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Opción 1: Cámara de fotos
                ScanSourceItemCard(
                    icon = "📷",
                    title = if (isEs) "Cámara de Fotos" else "Photo Camera",
                    subtitle = if (isEs) "Hacer foto instantánea al ticket físico" else "Take a live photo of physical receipt",
                    onClick = {
                        onDismiss()
                        onSelectCamera()
                    }
                )

                // Opción 2: Almacenamiento Local / Galería
                ScanSourceItemCard(
                    icon = "📁",
                    title = if (isEs) "Almacenamiento Local / Galería" else "Local Storage / Gallery",
                    subtitle = if (isEs) "Elegir foto (JPG, PNG) o PDF del móvil" else "Select photo (JPG, PNG) or PDF from phone",
                    onClick = {
                        onDismiss()
                        onSelectLocal()
                    }
                )

                // Opción 3: Google Drive
                ScanSourceItemCard(
                    icon = "☁️",
                    title = if (isEs) "Google Drive" else "Google Drive",
                    subtitle = if (isEs) "Seleccionar ticket o factura en PDF/imagen desde Drive" else "Select receipt or invoice PDF/image from Drive",
                    onClick = {
                        onDismiss()
                        onSelectDrive()
                    }
                )

                // Banner explicativo trilingüe
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEs) {
                            "ℹ️ Compatible con tickets en japonés (日本語), inglés (English) y español."
                        } else {
                            "ℹ️ Compatible with receipts in Japanese (日本語), English, and Spanish."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanSourceItemCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 18.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BudgetSummaryDashboardCard(
    isEs: Boolean,
    summary: TravelBudgetSummary,
    eurToJpyRate: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEs) "GASTO TOTAL" else "TOTAL EXPENSES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥%,d".format(Locale.US, summary.totalSpentYen.toInt()),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "≈ %.2f € (1 € = %.1f ¥)".format(summary.totalSpentEur, eurToJpyRate),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (isEs) "TAX-FREE RECUPERABLE" else "RECOVERABLE TAX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "+¥%,d".format(Locale.US, summary.totalTaxFreeRecoverableYen.toInt()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32) // Verde Torii de recuperación
                        )
                        Text(
                            text = "≈ +%.2f €".format(summary.totalTaxFreeRecoverableEur),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (summary.byCategory.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isEs) "Desglose de Gastos por Categoría:" else "Expenses Breakdown by Category:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.byCategory.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(cat.category.icon)
                                Text(
                                    text = if (isEs) cat.category.displayNameEs else cat.category.displayNameEn,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "¥%,d (%.0f%%)".format(Locale.US, cat.totalYen.toInt(), cat.percentageOfTotal),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
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
private fun ShoppingReceiptCard(
    isEs: Boolean,
    receipt: ShoppingReceipt,
    eurToJpyRate: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (TaxFreeClaimStatus) -> Unit
) {
    var expandedItems by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(
            width = if (receipt.isTaxFreeEligible) 1.5.dp else 1.dp,
            color = if (receipt.isTaxFreeEligible) {
                Color(0xFF4CAF50).copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecera de la tarjeta: Tienda, Categoría y Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(receipt.category.icon, fontSize = 18.sp)
                        }
                    }

                    Column {
                        Text(
                            text = receipt.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${receipt.location}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatIsoToTokyo(receipt.purchaseDateIso),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Importe Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥%,d".format(Locale.US, receipt.totalWithTaxYen.toInt()),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val approxEur = if (eurToJpyRate > 0) receipt.totalWithTaxYen / eurToJpyRate else 0.0
                    Text(
                        text = "≈ %.2f €".format(approxEur),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Fila de Tax-Free y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (receipt.isTaxFreeEligible) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✅", fontSize = 11.sp)
                            Text(
                                text = if (isEs) {
                                    "Tax-Free (+¥%,d IVA)".format(Locale.US, receipt.totalTaxYen.toInt())
                                } else {
                                    "Tax-Free (+¥%,d Tax)".format(Locale.US, receipt.totalTaxYen.toInt())
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = if (isEs) "Sin Tax-Free (<5.000 ¥)" else "No Tax-Free (<5,000 ¥)",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Selector de Estado Tax-Free
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (receipt.taxFreeStatus) {
                        TaxFreeClaimStatus.NOT_APPLICABLE -> if (isEs) "N/A" else "N/A"
                        TaxFreeClaimStatus.PENDING -> if (isEs) "⏳ Pendiente" else "⏳ Pending"
                        TaxFreeClaimStatus.CUSTOMS_STAMPED -> if (isEs) "🛂 Sellado" else "🛂 Stamped"
                        TaxFreeClaimStatus.REFUNDED -> if (isEs) "💰 Reembolsado" else "💰 Refunded"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (receipt.taxFreeStatus) {
                            TaxFreeClaimStatus.PENDING -> Color(0xFFFFF3E0)
                            TaxFreeClaimStatus.CUSTOMS_STAMPED -> Color(0xFFE8F5E9)
                            TaxFreeClaimStatus.REFUNDED -> Color(0xFFE3F2FD)
                            TaxFreeClaimStatus.NOT_APPLICABLE -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.clickable {
                            val nextStatus = when (receipt.taxFreeStatus) {
                                TaxFreeClaimStatus.PENDING -> TaxFreeClaimStatus.CUSTOMS_STAMPED
                                TaxFreeClaimStatus.CUSTOMS_STAMPED -> TaxFreeClaimStatus.REFUNDED
                                TaxFreeClaimStatus.REFUNDED -> TaxFreeClaimStatus.PENDING
                                TaxFreeClaimStatus.NOT_APPLICABLE -> TaxFreeClaimStatus.PENDING
                            }
                            onStatusChange(nextStatus)
                        }
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (receipt.taxFreeStatus) {
                                TaxFreeClaimStatus.PENDING -> Color(0xFFE65100)
                                TaxFreeClaimStatus.CUSTOMS_STAMPED -> Color(0xFF2E7D32)
                                TaxFreeClaimStatus.REFUNDED -> Color(0xFF1565C0)
                                TaxFreeClaimStatus.NOT_APPLICABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Desplegable de productos
            if (receipt.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedItems = !expandedItems },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEs) {
                            "📦 ${receipt.items.size} producto(s) en este ticket ${if (expandedItems) "▲" else "▼"}"
                        } else {
                            "📦 ${receipt.items.size} item(s) in this receipt ${if (expandedItems) "▲" else "▼"}"
                        },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Subtotal: ¥%,d".format(Locale.US, receipt.subtotalWithoutTaxYen.toInt()),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expandedItems) {
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        receipt.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.name}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "¥%,d (%s%%)".format(
                                        Locale.US,
                                        item.totalWithTaxYen.toInt(),
                                        if (item.taxRatePercent == 8.0) "8" else "10"
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(4.dp))

            // Botones de acción: Editar / Eliminar con alturas estandarizadas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(if (isEs) "✏️ Editar" else "✏️ Edit", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.height(38.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEs) "🗑️ Eliminar" else "🗑️ Delete", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingReceiptEditorModalSheet(
    isEs: Boolean,
    initialReceipt: ShoppingReceipt?,
    onDismiss: () -> Unit,
    onSave: (ShoppingReceipt) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var storeName by rememberSaveable { mutableStateOf(initialReceipt?.storeName ?: "") }
    var location by rememberSaveable { mutableStateOf(initialReceipt?.location ?: "Tokyo") }
    var category by remember { mutableStateOf(initialReceipt?.category ?: ExpenseCategory.SHOPPING) }
    var taxFreeStatus by remember { mutableStateOf(initialReceipt?.taxFreeStatus ?: TaxFreeClaimStatus.PENDING) }

    // Fecha y hora
    val initialInstant = remember {
        initialReceipt?.purchaseDateIso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now()
    }
    val initialZoned = remember(initialInstant) { initialInstant.atZone(TOKYO_ZONE_ID) }
    var selectedDateIso by rememberSaveable { mutableStateOf(initialZoned.toLocalDate().toString()) }
    var selectedTimeText by rememberSaveable {
        val h = initialZoned.hour
        val m = initialZoned.minute
        mutableStateOf(String.format(Locale.ROOT, "%02d:%02d", h, m))
    }

    // Lista de productos
    val items = remember {
        mutableStateListOf<ReceiptProductItem>().apply {
            if (initialReceipt != null && initialReceipt.items.isNotEmpty()) {
                addAll(initialReceipt.items)
            } else {
                add(
                    ReceiptProductItem(
                        id = UUID.randomUUID().toString(),
                        name = "General Goods",
                        quantity = 1,
                        unitPriceYen = 5500.0,
                        taxRatePercent = 10.0
                    )
                )
            }
        }
    }

    // Cálculos en vivo
    val liveSubtotal = items.sumOf { it.subtotalWithoutTaxYen }
    val liveTax = items.sumOf { it.taxYen }
    val liveTotal = items.sumOf { it.totalWithTaxYen }
    val isThresholdMet = liveSubtotal >= 5000.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (initialReceipt == null) {
                    if (isEs) "➕ Registrar Ticket de Compra" else "➕ Add Shopping Receipt"
                } else {
                    if (isEs) "✏️ Editar Ticket de Compra" else "✏️ Edit Shopping Receipt"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Nombre de la tienda
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text(if (isEs) "Tienda / Comercio" else "Store / Merchant") },
                placeholder = { Text("Ej: Don Quijote, Bic Camera, Uniqlo, Lawson...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Sugerencias rápidas de tiendas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Don Quijote", "Bic Camera", "Yodobashi", "Uniqlo", "7-Eleven", "Lawson", "FamilyMart", "Muji").forEach { quickStore ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { storeName = quickStore }
                    ) {
                        Text(
                            text = quickStore,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Ciudad / Lugar
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(if (isEs) "Lugar / Ciudad" else "Location / City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Chips rápidos de ciudades
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tokyo", "Kyoto", "Osaka", "Nara", "Hiroshima", "Hakone", "Sapporo").forEach { city ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { location = city }
                    ) {
                        Text(
                            text = city,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selectores de Fecha y Hora (utilizando helpers estándar del proyecto)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppDatePickerField(
                        label = if (isEs) "Fecha de compra" else "Purchase Date",
                        dateIso = selectedDateIso,
                        onDateSelected = { selectedDateIso = it },
                        modifier = Modifier.fillMaxWidth(),
                        isEs = isEs
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AppTimePickerField(
                        label = if (isEs) "Hora (Tokio)" else "Time (Tokyo)",
                        timeText = selectedTimeText,
                        onTimeSelected = { selectedTimeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        isEs = isEs
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Categoría de Gasto
            Text(
                text = if (isEs) "Categoría de Gasto:" else "Expense Category:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.icon} ${if (isEs) cat.displayNameEs else cat.displayNameEn}", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            // Sección de Productos Desglosados
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "📦 Productos Comprados (${items.size}):" else "📦 Purchased Items (${items.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Button(
                    onClick = {
                        items.add(
                            ReceiptProductItem(
                                id = UUID.randomUUID().toString(),
                                name = if (isEs) "Artículo ${items.size + 1}" else "Item ${items.size + 1}",
                                quantity = 1,
                                unitPriceYen = 1000.0,
                                taxRatePercent = 10.0
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEs) "+ Añadir Ítem" else "+ Add Item", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (items.size > 1) {
                                TextButton(
                                    onClick = { items.removeAt(index) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(if (isEs) "Quitar" else "Remove", fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { newName ->
                                items[index] = item.copy(
                                    name = newName,
                                    subtotalWithoutTaxYen = item.quantity * item.unitPriceYen,
                                    taxYen = Math.round((item.quantity * item.unitPriceYen) * (item.taxRatePercent / 100.0)).toDouble(),
                                    totalWithTaxYen = (item.quantity * item.unitPriceYen) + Math.round((item.quantity * item.unitPriceYen) * (item.taxRatePercent / 100.0)).toDouble()
                                )
                            },
                            label = { Text(if (isEs) "Nombre del producto" else "Product name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { qtyStr ->
                                    val qty = qtyStr.toIntOrNull() ?: 1
                                    val safeQty = qty.coerceAtLeast(1)
                                    val sub = safeQty * item.unitPriceYen
                                    val tax = Math.round(sub * (item.taxRatePercent / 100.0)).toDouble()
                                    items[index] = item.copy(
                                        quantity = safeQty,
                                        subtotalWithoutTaxYen = sub,
                                        taxYen = tax,
                                        totalWithTaxYen = sub + tax
                                    )
                                },
                                label = { Text(if (isEs) "Cant." else "Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.unitPriceYen.toInt().toString(),
                                onValueChange = { priceStr ->
                                    val price = priceStr.toDoubleOrNull() ?: 0.0
                                    val sub = item.quantity * price
                                    val tax = Math.round(sub * (item.taxRatePercent / 100.0)).toDouble()
                                    items[index] = item.copy(
                                        unitPriceYen = price,
                                        subtotalWithoutTaxYen = sub,
                                        taxYen = tax,
                                        totalWithTaxYen = sub + tax
                                    )
                                },
                                label = { Text(if (isEs) "Precio sin tasas (¥)" else "Price w/o tax (¥)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.4f),
                                singleLine = true
                            )

                            // Chip IVA 8% vs 10%
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isEs) "IVA" else "Tax",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    FilterChip(
                                        selected = item.taxRatePercent == 8.0,
                                        onClick = {
                                            val sub = item.subtotalWithoutTaxYen
                                            val tax = Math.round(sub * (8.0 / 100.0)).toDouble()
                                            items[index] = item.copy(
                                                taxRatePercent = 8.0,
                                                taxYen = tax,
                                                totalWithTaxYen = sub + tax
                                            )
                                        },
                                        label = { Text("8%", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = item.taxRatePercent == 10.0,
                                        onClick = {
                                            val sub = item.subtotalWithoutTaxYen
                                            val tax = Math.round(sub * (10.0 / 100.0)).toDouble()
                                            items[index] = item.copy(
                                                taxRatePercent = 10.0,
                                                taxYen = tax,
                                                totalWithTaxYen = sub + tax
                                            )
                                        },
                                        label = { Text("10%", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Resumen de cálculos en vivo y umbral Tax-Free
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isThresholdMet) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEs) "Subtotal sin tasas:" else "Subtotal w/o tax:", fontSize = 12.sp)
                        Text("¥%,d".format(Locale.US, liveSubtotal.toInt()), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEs) "Total impuestos (IVA):" else "Total tax amount:", fontSize = 12.sp)
                        Text("+¥%,d".format(Locale.US, liveTax.toInt()), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEs) "TOTAL CON TASAS:" else "TOTAL WITH TAX:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("¥%,d".format(Locale.US, liveTotal.toInt()), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isThresholdMet) {
                            if (isEs) "✅ Supera los 5.000 ¥: ¡Aplica para devolución Tax-Free!" else "✅ Exceeds 5,000 ¥: Eligible for Tax-Free refund!"
                        } else {
                            if (isEs) "ℹ️ No alcanza los 5.000 ¥ (faltan ¥%,d)".format(Locale.US, (5000.0 - liveSubtotal).toInt().coerceAtLeast(0))
                            else "ℹ️ Below 5,000 ¥ threshold (¥%,d remaining)".format(Locale.US, (5000.0 - liveSubtotal).toInt().coerceAtLeast(0))
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThresholdMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botones Cancelar / Guardar con altura estandarizada (48.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEs) "Cancelar" else "Cancel")
                }

                Button(
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(selectedDateIso) }.getOrNull() ?: LocalDate.now()
                        val parts = selectedTimeText.split(':')
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val parsedTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                        val finalDateTime = parsedDate.atTime(parsedTime).atZone(TOKYO_ZONE_ID).toInstant()

                        val receipt = TaxFreeCalculator.createReceipt(
                            id = initialReceipt?.id ?: UUID.randomUUID().toString(),
                            storeName = storeName.ifBlank { "Comercio Japón" },
                            location = location.ifBlank { "Tokyo" },
                            purchaseDateIso = finalDateTime.toString(),
                            items = items.toList(),
                            category = category,
                            taxFreeStatus = taxFreeStatus,
                            photoFilePath = initialReceipt?.photoFilePath,
                            notes = initialReceipt?.notes ?: ""
                        )
                        onSave(receipt)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEs) "Guardar Ticket" else "Save Receipt")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TaxFreeCustomsReportDialog(
    isEs: Boolean,
    receipts: List<ShoppingReceipt>,
    eurToJpyRate: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val report = remember(receipts, eurToJpyRate) {
        TaxFreeReportGenerator.generateReport(receipts, eurToJpyRate)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEs) "📄 Informe Aduanas Tax-Free" else "📄 Customs Tax-Free Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isEs) {
                        "Documento consolidado para presentar a los inspectores de aduanas en el aeropuerto de salida (Haneda, Narita, Kansai):"
                    } else {
                        "Consolidated document to present to customs officers at departure airport (Haneda, Narita, Kansai):"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // Resumen rápido
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEs) "Total Compras Tax-Free" else "Total Tax-Free Purchases",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "¥%,d".format(Locale.US, report.totalSubtotalWithoutTaxYen.toInt()),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isEs) "Impuesto a Reclamar" else "Tax Refund",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "+¥%,d".format(Locale.US, report.totalRecoverableTaxYen.toInt()),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "≈ +%.2f €".format(report.totalRecoverableTaxEur),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Previsualización de texto del informe
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = report.textReport,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Botones de acción con altura estandarizada (48.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("TaxFree Report", report.textReport))
                            Toast.makeText(
                                context,
                                if (isEs) "Informe copiado al portapapeles" else "Report copied to clipboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEs) "📋 Copiar Texto" else "📋 Copy Text", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEs) "Entendido" else "Got it", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPERS AUXILIARES DE IMAGEN, PDF Y FECHA
// ============================================================================

private fun formatIsoToTokyo(iso: String): String {
    return runCatching {
        val instant = Instant.parse(iso)
        val zonedDateTime = instant.atZone(TOKYO_ZONE_ID)
        zonedDateTime.format(TOKYO_DISPLAY_FORMATTER)
    }.getOrElse { iso }
}

private fun renderPdfPageToBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            val scale = (1400f / page.width.toFloat()).coerceAtLeast(1f)
            val w = (page.width * scale).toInt()
            val h = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            bitmap
        }
    }.getOrNull()
}

private fun decodeBitmapOrPdfFromUri(context: Context, uri: Uri): Bitmap? {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val isPdf = mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)
    return if (isPdf) {
        renderPdfPageToBitmap(context, uri) ?: decodeBitmapFromUri(context, uri)
    } else {
        decodeBitmapFromUri(context, uri) ?: renderPdfPageToBitmap(context, uri)
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            // Detectar orientación EXIF
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }

            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (degrees != 0f) {
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        }
    }.getOrNull()
}
