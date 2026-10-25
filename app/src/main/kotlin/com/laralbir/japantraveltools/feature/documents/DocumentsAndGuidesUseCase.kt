package com.laralbir.japantraveltools.feature.documents

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class DocumentCategory(val labelEn: String, val labelEs: String, val badgeCode: String) {
    PASSPORT("Passport", "Pasaporte", "ID-PASS"),
    NATIONAL_ID("National ID (DNI)", "DNI / Identidad", "ID-DNI"),
    FLIGHT("Flights & Boarding", "Vuelos y Embarque", "FLIGHT"),
    HOTEL("Hotels & Ryokan", "Hoteles y Ryokan", "HOTEL"),
    TICKET("Tickets & Museums", "Entradas y Museos", "TICKET"),
    TRANSPORT("JR Pass & Trains", "JR Pass y Trenes", "TRAIN"),
    INSURANCE("Travel Insurance", "Seguro de Viaje", "INSUR"),
    GUIDE("PDF / Map Guides", "Guías PDF / Mapas", "GUIDE")
}

data class TravelDocumentItem(
    val id: String,
    val title: String,
    val category: DocumentCategory,
    val referenceOrNotes: String,
    val isPinned: Boolean = false,
    val internalUriPath: String? = null,
    val lastReadPage: Int = 1,
    val fileUri: String? = internalUriPath
)

data class TravelGuideArticle(
    val id: String,
    val title: String,
    val cityRegion: String,
    val tags: List<String>,
    val markdownSummary: String,
    val photoCount: Int = 0,
    val isPinned: Boolean = false
)

object DefaultTravelGuidesCatalog {
    fun initialDocuments(): List<TravelDocumentItem> = listOf(
        TravelDocumentItem(
            id = "doc-passport",
            title = "Pasaporte Biométrico + QR Visit Japan Web",
            category = DocumentCategory.PASSPORT,
            referenceOrNotes = "Tax-Free & Inmigración · Almacenamiento Interno Privado",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-dni",
            title = "DNI (Anverso y Reverso)",
            category = DocumentCategory.NATIONAL_ID,
            referenceOrNotes = "Documento Nacional de Identidad · Protegido Offline",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-ticket-ghibli",
            title = "Entradas Museo Ghibli (Mitaka, Tokio)",
            category = DocumentCategory.TICKET,
            referenceOrNotes = "Entrada 10:00 AM · Entrada por Lawson Ticket · QR descargado",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-ticket-teamlab",
            title = "teamLab Planets Tokio (Toyosu)",
            category = DocumentCategory.TICKET,
            referenceOrNotes = "Horario 16:30 · Llevar pantalones remangables para el agua",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-ticket-usj",
            title = "Universal Studios Japan (USJ Osaka) + Nintendo World",
            category = DocumentCategory.TICKET,
            referenceOrNotes = "Pase Area Timed Entry Nintendo World 11:00 AM",
            isPinned = false
        ),
        TravelDocumentItem(
            id = "doc-flight-1",
            title = "Billete e-Ticket Madrid (MAD) ➔ Tokio Haneda (HND)",
            category = DocumentCategory.FLIGHT,
            referenceOrNotes = "PNR: JPN26X · Terminal 4S ➔ Doha ➔ Haneda T3",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-hotel-tokyo",
            title = "Reserva Hotel Shinjuku Granbell (Tokio)",
            category = DocumentCategory.HOTEL,
            referenceOrNotes = "歌舞伎町 2-14-5 · Check-in 15:00 · Pagado",
            isPinned = false
        ),
        TravelDocumentItem(
            id = "doc-jr-pass",
            title = "Voucher JR Pass / SmartEX Shinkansen QR",
            category = DocumentCategory.TRANSPORT,
            referenceOrNotes = "Canje en JR East Travel Service Center · Asientos 12D/12E",
            isPinned = true
        ),
        TravelDocumentItem(
            id = "doc-insurance",
            title = "Póliza Seguro Médico Internacional 24h",
            category = DocumentCategory.INSURANCE,
            referenceOrNotes = "Asistencia en español 24/7 · Cobertura médica ilimitada",
            isPinned = false
        )
    )

    fun initialGuides(): List<TravelGuideArticle> = listOf(
        TravelGuideArticle(
            id = "guide-tokyo-1",
            title = "Guía de Supervivencia en Estaciones de Tokio y Tarjetas Suica",
            cityRegion = "Tokyo",
            tags = listOf("Transporte", "Suica", "Shinjuku", "Salidas"),
            markdownSummary = "• Usa siempre el número de Salida (Exit A4, Hachiko, etc.) antes de cruzar los tornos.\n• En escaleras mecánicas: en Tokio colócate a la izquierda; en Osaka a la derecha.\n• Recarga Suica/Pasmo en efectivo en máquinas de estación.",
            photoCount = 3,
            isPinned = true
        ),
        TravelGuideArticle(
            id = "guide-kyoto-1",
            title = "Ruta Temprana por Fushimi Inari, Kiyomizu-dera y Gion",
            cityRegion = "Kyoto",
            tags = listOf("Templos", "Madrugar", "Fotografía", "Gion"),
            markdownSummary = "• Llega a Fushimi Inari-taisha antes de las 07:15 (JR Nara Line desde Kyoto Station, 5 min).\n• Sube hasta la intersección Yotsutsuji para vistas de Kioto sin multitudes.\n• Continúa en Keihan Main Line hacia Gion-Shijo.",
            photoCount = 4,
            isPinned = true
        ),
        TravelGuideArticle(
            id = "guide-osaka-1",
            title = "Dónde Comer en Dotonbori y Shinsekai (Takoyaki, Okonomiyaki y Kushikatsu)",
            cityRegion = "Osaka",
            tags = listOf("Gastronomía", "Dotonbori", "Tax-Free"),
            markdownSummary = "• No mojes dos veces el Kushikatsu en la salsa compartida.\n• Lleva efectivo (billetes de ¥1,000) para puestos callejeros y máquinas de tickets (Shok券機).",
            photoCount = 2,
            isPinned = false
        )
    )
}

object SharedDocumentsAndGuidesStore {
    private var storageContext: Context? = null
    private const val DOCS_FILE_NAME = "saved_documents.json"
    private const val GUIDES_FILE_NAME = "saved_guides.json"

    private val _documentsState = MutableStateFlow(DefaultTravelGuidesCatalog.initialDocuments())
    val documentsState: StateFlow<List<TravelDocumentItem>> = _documentsState.asStateFlow()

    private val _guidesState = MutableStateFlow(DefaultTravelGuidesCatalog.initialGuides())
    val guidesState: StateFlow<List<TravelGuideArticle>> = _guidesState.asStateFlow()

    fun init(context: Context) {
        if (storageContext != null) return
        storageContext = context.applicationContext
        loadFromDisk()
    }

    private fun saveToDisk() {
        val ctx = storageContext ?: return
        runCatching {
            val docsFile = File(ctx.filesDir, DOCS_FILE_NAME)
            docsFile.writeText(DocumentsAndGuidesJsonCodec.exportDocumentsJson(_documentsState.value))
            val guidesFile = File(ctx.filesDir, GUIDES_FILE_NAME)
            guidesFile.writeText(DocumentsAndGuidesJsonCodec.exportGuidesJson(_guidesState.value))
        }
    }

    private fun loadFromDisk() {
        val ctx = storageContext ?: return
        runCatching {
            val docsFile = File(ctx.filesDir, DOCS_FILE_NAME)
            if (docsFile.exists() && docsFile.length() > 0) {
                val loaded = DocumentsAndGuidesJsonCodec.importDocumentsJson(docsFile.readText())
                if (loaded.isNotEmpty()) {
                    _documentsState.value = loaded
                }
            }
        }
        runCatching {
            val guidesFile = File(ctx.filesDir, GUIDES_FILE_NAME)
            if (guidesFile.exists() && guidesFile.length() > 0) {
                val loaded = DocumentsAndGuidesJsonCodec.importGuidesJson(guidesFile.readText())
                if (loaded.isNotEmpty()) {
                    _guidesState.value = loaded
                }
            }
        }
    }

    fun upsertDocument(document: TravelDocumentItem) {
        val current = _documentsState.value.toMutableList()
        val idx = current.indexOfFirst { it.id == document.id }
        if (idx >= 0) {
            current[idx] = document
        } else {
            current.add(0, document)
        }
        _documentsState.value = current
        saveToDisk()
    }

    fun deleteDocument(id: String) {
        val current = _documentsState.value
        val docToDelete = current.firstOrNull { it.id == id }
        if (docToDelete?.fileUri != null) {
            deleteDocumentAttachment(docToDelete.fileUri)
        }
        _documentsState.value = current.filterNot { it.id == id }
        saveToDisk()
    }

    fun upsertGuide(guide: TravelGuideArticle) {
        val current = _guidesState.value.toMutableList()
        val idx = current.indexOfFirst { it.id == guide.id }
        if (idx >= 0) {
            current[idx] = guide
        } else {
            current.add(0, guide)
        }
        _guidesState.value = current
        saveToDisk()
    }

    fun deleteGuide(id: String) {
        _guidesState.value = _guidesState.value.filterNot { it.id == id }
        saveToDisk()
    }

    fun resetToDefaults() {
        _documentsState.value = DefaultTravelGuidesCatalog.initialDocuments()
        _guidesState.value = DefaultTravelGuidesCatalog.initialGuides()
        saveToDisk()
    }

    // Attachments for documents (PDFs, Ticket photos, QR codes)
    fun getDocumentsDir(context: Context): File {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveDocumentAttachment(context: Context, sourceUri: Uri, originalName: String? = null): String? {
        return runCatching {
            val ext = originalName?.substringAfterLast('.', "")?.let { if (it.isNotBlank()) ".$it" else "" } ?: ""
            val fileName = "doc_${System.currentTimeMillis()}$ext"
            val targetFile = File(getDocumentsDir(context), fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    fun deleteDocumentAttachment(filePath: String): Boolean {
        return runCatching {
            val file = File(filePath)
            if (file.exists()) file.delete() else true
        }.getOrDefault(false)
    }

    // Photo vault in Context.filesDir/id_vault/
    fun getIdVaultDir(context: Context): File {
        val dir = File(context.filesDir, "id_vault")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveIdPhoto(context: Context, sourceUri: Uri, targetFileName: String): Boolean {
        return runCatching {
            val targetFile = File(getIdVaultDir(context), targetFileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        }.getOrDefault(false)
    }

    fun deleteIdPhoto(context: Context, fileName: String): Boolean {
        return runCatching {
            val file = File(getIdVaultDir(context), fileName)
            if (file.exists()) file.delete() else true
        }.getOrDefault(false)
    }

    fun getIdPhotoFile(context: Context, fileName: String): File? {
        val file = File(getIdVaultDir(context), fileName)
        return if (file.exists() && file.length() > 0) file else null
    }
}

object DocumentsAndGuidesJsonCodec {
    fun exportDocumentsJson(docs: List<TravelDocumentItem>): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"documents\": [")
        docs.forEachIndexed { index, doc ->
            val comma = if (index < docs.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(doc.id)}\",")
            sb.appendLine("      \"title\": \"${escape(doc.title)}\",")
            sb.appendLine("      \"category\": \"${doc.category.name}\",")
            sb.appendLine("      \"referenceOrNotes\": \"${escape(doc.referenceOrNotes)}\",")
            val fileUriStr = if (doc.fileUri != null) "\"${escape(doc.fileUri)}\"" else "null"
            sb.appendLine("      \"fileUri\": $fileUriStr,")
            sb.appendLine("      \"isPinned\": ${doc.isPinned}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    fun importDocumentsJson(json: String): List<TravelDocumentItem> {
        val block = extractArrayBlock(json, "documents")
        val objs = splitJsonObjects(block)
        return objs.mapNotNull { obj ->
            val id = extractString(obj, "id") ?: return@mapNotNull null
            val title = extractString(obj, "title") ?: ""
            val catName = extractString(obj, "category") ?: DocumentCategory.TICKET.name
            val cat = runCatching { DocumentCategory.valueOf(catName) }.getOrDefault(DocumentCategory.TICKET)
            val notes = extractString(obj, "referenceOrNotes") ?: ""
            val fileUri = extractString(obj, "fileUri")
            val isPinned = extractBoolean(obj, "isPinned") ?: false
            TravelDocumentItem(
                id = id,
                title = title,
                category = cat,
                referenceOrNotes = notes,
                fileUri = fileUri,
                isPinned = isPinned
            )
        }
    }

    fun exportGuidesJson(guides: List<TravelGuideArticle>): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"guides\": [")
        guides.forEachIndexed { index, g ->
            val comma = if (index < guides.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(g.id)}\",")
            sb.appendLine("      \"title\": \"${escape(g.title)}\",")
            sb.appendLine("      \"cityRegion\": \"${escape(g.cityRegion)}\",")
            val tagsStr = g.tags.joinToString(", ") { "\"${escape(it)}\"" }
            sb.appendLine("      \"tags\": [$tagsStr],")
            sb.appendLine("      \"markdownSummary\": \"${escape(g.markdownSummary)}\"")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    fun importGuidesJson(json: String): List<TravelGuideArticle> {
        val block = extractArrayBlock(json, "guides")
        val objs = splitJsonObjects(block)
        return objs.mapNotNull { obj ->
            val id = extractString(obj, "id") ?: return@mapNotNull null
            val title = extractString(obj, "title") ?: ""
            val cityRegion = extractString(obj, "cityRegion") ?: "Tokyo"
            val notes = extractString(obj, "markdownSummary") ?: ""
            val tagsBlock = extractArrayBlock(obj, "tags")
            val tags = """\"([^\"]*)\"""".toRegex().findAll(tagsBlock).map { it.groupValues[1] }.toList()
            TravelGuideArticle(
                id = id,
                title = title,
                cityRegion = cityRegion,
                tags = tags.ifEmpty { listOf("Tips") },
                markdownSummary = notes
            )
        }
    }

    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun unescape(raw: String): String =
        raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")

    private fun extractString(source: String, key: String): String? {
        val regex = """"$key"\s*:\s*(?:"((?:\\.|[^"\\])*)"|(null))""".toRegex()
        val match = regex.find(source) ?: return null
        val quoted = match.groupValues[1]
        val isNull = match.groupValues[2] == "null"
        return if (isNull) null else unescape(quoted)
    }

    private fun extractBoolean(source: String, key: String): Boolean? {
        val regex = """"$key"\s*:\s*(true|false)""".toRegex()
        return regex.find(source)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractArrayBlock(source: String, key: String): String {
        val keyIdx = source.indexOf("\"$key\"")
        if (keyIdx == -1) return ""
        val openBracket = source.indexOf('[', keyIdx)
        if (openBracket == -1) return ""
        var depth = 0
        for (i in openBracket until source.length) {
            when (source[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return source.substring(openBracket + 1, i)
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var objStart = -1
        for (i in arrayContent.indices) {
            when (arrayContent[i]) {
                '{' -> {
                    if (depth == 0) objStart = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        result.add(arrayContent.substring(objStart, i + 1))
                        objStart = -1
                    }
                }
            }
        }
        return result
    }
}

object DocumentsFilterUseCase {
    fun filterDocuments(
        documents: List<TravelDocumentItem>,
        selectedCategory: DocumentCategory?,
        searchQuery: String
    ): List<TravelDocumentItem> {
        val q = searchQuery.trim().lowercase()
        return documents
            .filter { doc ->
                val matchesCategory = selectedCategory == null || doc.category == selectedCategory
                val matchesQuery = q.isEmpty() ||
                    doc.title.lowercase().contains(q) ||
                    doc.referenceOrNotes.lowercase().contains(q)
                matchesCategory && matchesQuery
            }
            .sortedWith(compareByDescending<TravelDocumentItem> { it.isPinned }.thenBy { it.title })
    }

    fun filterGuides(
        guides: List<TravelGuideArticle>,
        cityFilter: String?,
        searchQuery: String
    ): List<TravelGuideArticle> {
        val q = searchQuery.trim().lowercase()
        return guides
            .filter { guide ->
                val matchesCity = cityFilter.isNullOrBlank() ||
                    cityFilter == "All" ||
                    guide.cityRegion.equals(cityFilter, ignoreCase = true)
                val matchesQuery = q.isEmpty() ||
                    guide.title.lowercase().contains(q) ||
                    guide.markdownSummary.lowercase().contains(q) ||
                    guide.tags.any { it.lowercase().contains(q) }
                matchesCity && matchesQuery
            }
            .sortedByDescending { it.isPinned }
    }
}
