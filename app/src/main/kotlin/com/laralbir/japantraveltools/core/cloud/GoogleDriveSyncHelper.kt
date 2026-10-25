package com.laralbir.japantraveltools.core.cloud

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import com.laralbir.japantraveltools.core.portability.TravelPlanJsonCodec
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.weather.WeatherCityItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JapanTravelCloudSyncBundle(
    val schemaVersion: Int = 1,
    val backupId: String,
    val accountEmail: String,
    val timestampMillis: Long,
    val deviceModel: String,
    val travelPlans: List<PortableTravelPlanBundle>,
    val travelGuides: List<TravelGuideArticle>,
    val travelDocuments: List<TravelDocumentItem>,
    val weatherCities: List<WeatherCityItem>,
    val appLanguage: String = "SYSTEM",
    val themeMode: String = "SYSTEM",
    val originZoneId: String = "Europe/Madrid"
)

object GoogleAccountSelectorHelper {

    fun createAccountChooserIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            false,
            null,
            null,
            null,
            null
        )
    }

    fun extractAccountEmail(intentData: Intent?): String? {
        if (intentData == null) return null
        return intentData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun isValidGoogleEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.length < 5 || !trimmed.contains('@') || !trimmed.contains('.')) return false
        val atIdx = trimmed.indexOf('@')
        val dotIdx = trimmed.lastIndexOf('.')
        return atIdx > 0 && dotIdx > atIdx + 1 && dotIdx < trimmed.length - 1
    }

    fun maskAccountEmail(email: String): String {
        val trimmed = email.trim()
        if (!isValidGoogleEmail(trimmed)) return trimmed
        val parts = trimmed.split('@')
        val user = parts[0]
        val domain = parts[1]
        if (user.length <= 2) {
            return "${user.first()}***@$domain"
        }
        return "${user.first()}***${user.last()}@$domain"
    }
}

object GoogleDriveSyncHelper {

    private const val BACKUP_DIR_NAME = "cloud_sync"
    private const val DEFAULT_BACKUP_FILE_PREFIX = "japan-travel-backup"

    fun buildSyncBundle(
        accountEmail: String,
        deviceModel: String = "Android Device",
        plans: List<PortableTravelPlanBundle>,
        guides: List<TravelGuideArticle>,
        documents: List<TravelDocumentItem>,
        cities: List<WeatherCityItem>,
        prefs: UserPreferences,
        timestampMillis: Long = System.currentTimeMillis()
    ): JapanTravelCloudSyncBundle {
        return JapanTravelCloudSyncBundle(
            schemaVersion = 1,
            backupId = "backup-$timestampMillis",
            accountEmail = accountEmail.trim(),
            timestampMillis = timestampMillis,
            deviceModel = deviceModel,
            travelPlans = plans,
            travelGuides = guides,
            travelDocuments = documents,
            weatherCities = cities,
            appLanguage = prefs.language.name,
            themeMode = prefs.themeMode.name,
            originZoneId = prefs.originZoneId
        )
    }

    fun getCloudBackupFileName(accountEmail: String): String {
        val sanitized = accountEmail.trim().lowercase(Locale.ROOT)
            .substringBefore('@')
            .replace(Regex("[^a-z0-9._-]"), "_")
        return if (sanitized.isNotBlank()) {
            "$DEFAULT_BACKUP_FILE_PREFIX-$sanitized.json"
        } else {
            "$DEFAULT_BACKUP_FILE_PREFIX.json"
        }
    }

    fun formatSyncTime(epochMillis: Long?, isEs: Boolean): String {
        if (epochMillis == null || epochMillis <= 0L) {
            return if (isEs) "Sin sincronizar" else "Not synced yet"
        }
        val date = Date(epochMillis)
        val pattern = if (isEs) "d MMM yyyy, HH:mm" else "MMM d, yyyy, HH:mm"
        val locale = if (isEs) Locale.forLanguageTag("es-ES") else Locale.ENGLISH
        val formatted = SimpleDateFormat(pattern, locale).format(date)
        return if (isEs) "Última sincronización: $formatted" else "Last synced: $formatted"
    }

    fun mergePlans(
        existingPlans: List<PortableTravelPlanBundle>,
        incomingPlans: List<PortableTravelPlanBundle>
    ): List<PortableTravelPlanBundle> {
        val result = existingPlans.toMutableList()
        for (incoming in incomingPlans) {
            val idx = result.indexOfFirst { it.id == incoming.id }
            if (idx >= 0) {
                val existing = result[idx]
                val mergedStops = (existing.itineraryStops + incoming.itineraryStops).distinctBy { it.id }
                val mergedSegments = (existing.transportSegments + incoming.transportSegments).distinctBy { it.id }
                result[idx] = incoming.copy(
                    itineraryStops = mergedStops,
                    transportSegments = mergedSegments
                )
            } else {
                result.add(incoming)
            }
        }
        return result
    }

    fun mergeGuides(
        existing: List<TravelGuideArticle>,
        incoming: List<TravelGuideArticle>
    ): List<TravelGuideArticle> {
        val map = existing.associateBy { it.id }.toMutableMap()
        for (guide in incoming) {
            map[guide.id] = guide
        }
        return map.values.toList()
    }

    fun mergeDocuments(
        existing: List<TravelDocumentItem>,
        incoming: List<TravelDocumentItem>
    ): List<TravelDocumentItem> {
        val map = existing.associateBy { it.id }.toMutableMap()
        for (doc in incoming) {
            map[doc.id] = doc
        }
        return map.values.toList()
    }

    fun mergeWeatherCities(
        existing: List<WeatherCityItem>,
        incoming: List<WeatherCityItem>
    ): List<WeatherCityItem> {
        val result = existing.toMutableList()
        for (city in incoming) {
            val idx = result.indexOfFirst { it.id == city.id }
            if (idx >= 0) {
                result[idx] = city
            } else {
                val exists = result.any { it.latitude == city.latitude && it.longitude == city.longitude }
                if (!exists) {
                    result.add(city)
                }
            }
        }
        return result
    }

    fun getLocalBackupFile(context: Context, accountEmail: String): File {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val name = getCloudBackupFileName(accountEmail)
        return File(dir, name)
    }

    fun saveToLocalCloudCache(context: Context, bundle: JapanTravelCloudSyncBundle): File {
        val file = getLocalBackupFile(context, bundle.accountEmail)
        file.writeText(exportCloudBackupJson(bundle))
        return file
    }

    fun exportCloudBackupJson(bundle: JapanTravelCloudSyncBundle): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"schemaVersion\": ${bundle.schemaVersion},")
        sb.appendLine("  \"backupId\": \"${escape(bundle.backupId)}\",")
        sb.appendLine("  \"accountEmail\": \"${escape(bundle.accountEmail)}\",")
        sb.appendLine("  \"timestampMillis\": ${bundle.timestampMillis},")
        sb.appendLine("  \"deviceModel\": \"${escape(bundle.deviceModel)}\",")
        sb.appendLine("  \"appLanguage\": \"${escape(bundle.appLanguage)}\",")
        sb.appendLine("  \"themeMode\": \"${escape(bundle.themeMode)}\",")
        sb.appendLine("  \"originZoneId\": \"${escape(bundle.originZoneId)}\",")

        // Plans
        sb.appendLine("  \"travelPlans\": [")
        bundle.travelPlans.forEachIndexed { idx, plan ->
            val comma = if (idx < bundle.travelPlans.lastIndex) "," else ""
            sb.appendLine(TravelPlanJsonCodec.exportToPrettyJson(plan).prependIndent("    ") + comma)
        }
        sb.appendLine("  ],")

        // Guides
        sb.appendLine("  \"travelGuides\": [")
        bundle.travelGuides.forEachIndexed { idx, g ->
            val comma = if (idx < bundle.travelGuides.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(g.id)}\",")
            sb.appendLine("      \"title\": \"${escape(g.title)}\",")
            sb.appendLine("      \"cityRegion\": \"${escape(g.cityRegion)}\",")
            val tagsStr = g.tags.joinToString(", ") { "\"${escape(it)}\"" }
            sb.appendLine("      \"tags\": [$tagsStr],")
            sb.appendLine("      \"markdownSummary\": \"${escape(g.markdownSummary)}\",")
            sb.appendLine("      \"photoCount\": ${g.photoCount},")
            sb.appendLine("      \"isPinned\": ${g.isPinned}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ],")

        // Documents
        sb.appendLine("  \"travelDocuments\": [")
        bundle.travelDocuments.forEachIndexed { idx, doc ->
            val comma = if (idx < bundle.travelDocuments.lastIndex) "," else ""
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
        sb.appendLine("  ],")

        // Cities
        sb.appendLine("  \"weatherCities\": [")
        bundle.weatherCities.forEachIndexed { idx, c ->
            val comma = if (idx < bundle.weatherCities.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(c.id)}\",")
            sb.appendLine("      \"nameEs\": \"${escape(c.nameEs)}\",")
            sb.appendLine("      \"nameEn\": \"${escape(c.nameEn)}\",")
            sb.appendLine("      \"latitude\": ${c.latitude},")
            sb.appendLine("      \"longitude\": ${c.longitude},")
            sb.appendLine("      \"isDefault\": ${c.isDefault}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    fun importCloudBackupJson(jsonString: String): JapanTravelCloudSyncBundle? {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank() || !trimmed.startsWith('{') || !trimmed.endsWith('}')) return null
        if (trimmed.contains("\"schemaVersion\"") && extractInt(trimmed, "schemaVersion") == null) return null
        if (!trimmed.contains("\"backupId\"") || !trimmed.contains("\"accountEmail\"")) return null
        return runCatching {
            val schemaVersion = extractInt(trimmed, "schemaVersion") ?: 1
            val backupId = extractString(trimmed, "backupId") ?: return null
            val accountEmail = extractString(trimmed, "accountEmail") ?: return null
            val timestampMillis = extractLong(trimmed, "timestampMillis") ?: System.currentTimeMillis()
            val deviceModel = extractString(trimmed, "deviceModel") ?: "Android Device"
            val appLanguage = extractString(trimmed, "appLanguage") ?: "SYSTEM"
            val themeMode = extractString(trimmed, "themeMode") ?: "SYSTEM"
            val originZoneId = extractString(trimmed, "originZoneId") ?: "Europe/Madrid"

            val plansBlock = extractArrayBlock(trimmed, "travelPlans")
            val planObjects = splitJsonObjects(plansBlock)
            val plans = planObjects.mapNotNull { pJson ->
                runCatching { TravelPlanJsonCodec.importFromJson(pJson) }.getOrNull()
            }

            val guidesBlock = extractArrayBlock(trimmed, "travelGuides")
            val guideObjects = splitJsonObjects(guidesBlock)
            val guides = guideObjects.mapNotNull { gJson ->
                val id = extractString(gJson, "id") ?: return@mapNotNull null
                val title = extractString(gJson, "title") ?: ""
                val city = extractString(gJson, "cityRegion") ?: ""
                val summary = extractString(gJson, "markdownSummary") ?: ""
                val photoCount = extractInt(gJson, "photoCount") ?: 0
                val isPinned = extractBoolean(gJson, "isPinned") ?: false
                TravelGuideArticle(
                    id = id,
                    title = title,
                    cityRegion = city,
                    tags = emptyList(),
                    markdownSummary = summary,
                    photoCount = photoCount,
                    isPinned = isPinned
                )
            }

            val docsBlock = extractArrayBlock(trimmed, "travelDocuments")
            val docObjects = splitJsonObjects(docsBlock)
            val docs = docObjects.mapNotNull { dJson ->
                val id = extractString(dJson, "id") ?: return@mapNotNull null
                val title = extractString(dJson, "title") ?: ""
                val catName = extractString(dJson, "category") ?: "TICKET"
                val cat = runCatching { DocumentCategory.valueOf(catName) }
                    .getOrDefault(DocumentCategory.TICKET)
                val notes = extractString(dJson, "referenceOrNotes") ?: ""
                val isPinned = extractBoolean(dJson, "isPinned") ?: false
                TravelDocumentItem(
                    id = id,
                    title = title,
                    category = cat,
                    referenceOrNotes = notes,
                    isPinned = isPinned
                )
            }

            val citiesBlock = extractArrayBlock(trimmed, "weatherCities")
            val cityObjects = splitJsonObjects(citiesBlock)
            val cities = cityObjects.mapNotNull { cJson ->
                val id = extractString(cJson, "id") ?: return@mapNotNull null
                val nameEs = extractString(cJson, "nameEs") ?: ""
                val nameEn = extractString(cJson, "nameEn") ?: nameEs
                val lat = extractDouble(cJson, "latitude") ?: 35.6762
                val lon = extractDouble(cJson, "longitude") ?: 139.6503
                val isDefault = extractBoolean(cJson, "isDefault") ?: false
                WeatherCityItem(
                    id = id,
                    nameEs = nameEs,
                    nameEn = nameEn,
                    latitude = lat,
                    longitude = lon,
                    isDefault = isDefault
                )
            }

            JapanTravelCloudSyncBundle(
                schemaVersion = schemaVersion,
                backupId = backupId,
                accountEmail = accountEmail,
                timestampMillis = timestampMillis,
                deviceModel = deviceModel,
                travelPlans = plans,
                travelGuides = guides,
                travelDocuments = docs,
                weatherCities = cities,
                appLanguage = appLanguage,
                themeMode = themeMode,
                originZoneId = originZoneId
            )
        }.getOrNull()
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun extractString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = Regex("\"$key\"\\s*:\\s*(true|false)")
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractArrayBlock(json: String, key: String): String {
        val marker = "\"$key\""
        val keyIdx = json.indexOf(marker)
        if (keyIdx < 0) return ""
        val startBracket = json.indexOf('[', keyIdx)
        if (startBracket < 0) return ""
        var depth = 0
        var inQuotes = false
        var isEscaped = false
        for (i in startBracket until json.length) {
            val c = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startBracket + 1, i)
                    }
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayBlock: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var startIdx = -1
        var inQuotes = false
        var isEscaped = false
        for (i in arrayBlock.indices) {
            val c = arrayBlock[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) startIdx = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIdx >= 0) {
                        list.add(arrayBlock.substring(startIdx, i + 1))
                        startIdx = -1
                    }
                }
            }
        }
        return list
    }
}
