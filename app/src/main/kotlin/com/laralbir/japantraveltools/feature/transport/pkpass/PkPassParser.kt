package com.laralbir.japantraveltools.feature.transport.pkpass

import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedPkPass(
    val organizationName: String,
    val description: String,
    val transitType: String?,
    val originName: String,
    val destinationName: String,
    val departureDateIso: String,
    val departureTime: String,
    val arrivalDateIso: String,
    val arrivalTime: String,
    val operatorName: String,
    val serviceNumber: String,
    val carriage: String?,
    val seat: String?,
    val boardingTime: String? = null,
    val passengerName: String?,
    val bookingCode: String?,
    val ticketNumber: String?,
    val barcodeMessage: String?,
    val barcodeFormat: String?,
    val barcodeAltText: String?,
    val rawJson: String
)

object PkPassParser {

    fun parsePkPass(inputStream: InputStream): Result<ParsedPkPass> {
        return runCatching {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            var passJsonContent: String? = null
            while (entry != null) {
                if (entry.name.equals("pass.json", ignoreCase = true)) {
                    passJsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                    break
                }
                entry = zis.nextEntry
            }
            if (passJsonContent == null) {
                error("No se encontró pass.json dentro del archivo .pkpass")
            }
            parsePassJson(passJsonContent).getOrThrow()
        }
    }

    fun parsePassJson(jsonString: String): Result<ParsedPkPass> {
        return runCatching {
            val root = JSONObject(jsonString)
            val orgName = root.optString("organizationName", "").trim()
            val description = root.optString("description", "").trim()
            val relevantDate = root.optString("relevantDate", "").trim()

            var fallbackDate = ""
            var relevantTime = ""
            if (relevantDate.isNotBlank()) {
                runCatching {
                    val parsedOdt = OffsetDateTime.parse(relevantDate)
                    fallbackDate = parsedOdt.toLocalDate().toString()
                    relevantTime = String.format(Locale.US, "%02d:%02d", parsedOdt.hour, parsedOdt.minute)
                }
            }

            val boardingPass = root.optJSONObject("boardingPass")
                ?: root.optJSONObject("generic")
                ?: root.optJSONObject("eventTicket")
                ?: root.optJSONObject("coupon")
                ?: JSONObject()

            val transitType = boardingPass.optString("transitType", "").takeIf { it.isNotBlank() }

            val primaryFields = boardingPass.optJSONArray("primaryFields").toList()
            val secondaryFields = boardingPass.optJSONArray("secondaryFields").toList()
            val auxiliaryFields = boardingPass.optJSONArray("auxiliaryFields").toList()
            val headerFields = boardingPass.optJSONArray("headerFields").toList()
            val backFields = boardingPass.optJSONArray("backFields").toList()

            val allFields = primaryFields + secondaryFields + auxiliaryFields + headerFields + backFields

            // 1. Extraer Fecha
            var departureDateIso = fallbackDate
            val dateField = allFields.firstOrNull { it.optString("key").contains("date", ignoreCase = true) }
            if (dateField != null) {
                val rawDateVal = dateField.optString("value")
                val parsed = parseAnyDateToIso(rawDateVal)
                if (parsed != null) {
                    departureDateIso = parsed
                }
            }
            if (departureDateIso.isBlank()) {
                departureDateIso = LocalDate.now().toString()
            }

            // 2. Extraer Origen y Destino + Horas
            var originName = ""
            var destinationName = ""
            var departureTime = relevantTime
            var arrivalTime = ""

            val departField = primaryFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("depart") || k.contains("origin") || k.contains("from")
            }
            val arriveField = primaryFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("arrive") || k.contains("dest") || k.contains("to")
            }

            if (departField != null) {
                val (loc, time) = extractLocationAndTime(departField)
                originName = loc
                if (time.isNotBlank()) departureTime = time
            }

            if (arriveField != null) {
                val (loc, time) = extractLocationAndTime(arriveField)
                destinationName = loc
                if (time.isNotBlank()) arrivalTime = time
            }

            // Si no se encontraron por key, probar primer y segundo elemento de primaryFields
            if (originName.isBlank() && primaryFields.isNotEmpty()) {
                val (loc, time) = extractLocationAndTime(primaryFields[0])
                originName = loc
                if (time.isNotBlank() && departureTime.isBlank()) departureTime = time
            }
            if (destinationName.isBlank() && primaryFields.size > 1) {
                val (loc, time) = extractLocationAndTime(primaryFields[1])
                destinationName = loc
                if (time.isNotBlank()) arrivalTime = time
            }

            if (departureTime.isBlank()) departureTime = "10:00"
            if (arrivalTime.isBlank()) {
                runCatching {
                    val parts = departureTime.split(":")
                    val h = (parts[0].toInt() + 2) % 24
                    arrivalTime = String.format(Locale.US, "%02d:%s", h, parts.getOrElse(1) { "00" })
                }.onFailure {
                    arrivalTime = "12:00"
                }
            }

            // 3. Extraer Servicio / Número de tren o vuelo
            val serviceField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("service") || k.contains("flight") || k.contains("train") || k.contains("tren")
            }
            var serviceNumber = ""
            if (serviceField != null) {
                val label = serviceField.optString("label").trim()
                val value = serviceField.optString("value").trim()
                val isGenericLabel = label.equals("FLIGHT", ignoreCase = true) ||
                    label.equals("VUELO", ignoreCase = true) ||
                    label.equals("TRAIN", ignoreCase = true) ||
                    label.equals("TREN", ignoreCase = true) ||
                    label.equals("SERVICE", ignoreCase = true) ||
                    label.equals("SERVICIO", ignoreCase = true)
                serviceNumber = if (!isGenericLabel && label.isNotBlank() && !value.contains(label, ignoreCase = true)) {
                    val cleanLabel = label.removePrefix("TREN ").trim()
                    if (cleanLabel.isNotBlank() && !cleanLabel.equals("FLIGHT", ignoreCase = true) && !cleanLabel.equals("TRAIN", ignoreCase = true)) {
                        "$cleanLabel $value"
                    } else {
                        value
                    }
                } else if (label.contains("AVE", ignoreCase = true) && !value.contains("AVE", ignoreCase = true)) {
                    "AVE $value"
                } else {
                    value
                }
            }
            if (serviceNumber.isBlank()) {
                serviceNumber = description.ifBlank { "Transporte" }
            }

            // 4. Extraer Coche y Asiento
            val carriageField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("carriage") || k.contains("coche") || k.contains("car") || k.contains("wagon")
            }
            val seatField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("seat") || k.contains("asiento")
            }
            val carriage = carriageField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }
            val seat = seatField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }

            // 5. Extraer Hora de Embarque
            val boardingField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("boarding") || k.contains("embarque")
            }
            val boardingTime = boardingField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }

            // 6. Pasajero, Localizador y Número de Billete
            val passengerField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("passenger") || k.contains("pasajero") || k.contains("name")
            }
            val passengerName = passengerField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }

            val bookingField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("booking") || k.contains("localizador") || k.contains("pnr") || k.contains("record")
            }
            val bookingCode = bookingField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }

            val ticketField = allFields.firstOrNull {
                val k = it.optString("key").lowercase(Locale.ROOT)
                k.contains("ticket") || k.contains("billete") || k.contains("e-ticket")
            }
            val ticketNumber = ticketField?.optString("value")?.trim()?.takeIf { it.isNotBlank() }
                ?: root.optString("serialNumber", "").takeIf { it.isNotBlank() }

            // 7. Código de barras / Aztec / QR
            val barcodeObj = root.optJSONObject("barcode")
                ?: root.optJSONArray("barcodes")?.optJSONObject(0)
            val barcodeMessage = barcodeObj?.optString("message", "")?.takeIf { it.isNotBlank() }
            val barcodeFormat = barcodeObj?.optString("format", "")?.takeIf { it.isNotBlank() }
            val barcodeAltText = barcodeObj?.optString("altText", "")?.takeIf { it.isNotBlank() }
                ?: ticketNumber

            ParsedPkPass(
                organizationName = orgName,
                description = description,
                transitType = transitType,
                originName = originName.ifBlank { "Origen" },
                destinationName = destinationName.ifBlank { "Destino" },
                departureDateIso = departureDateIso,
                departureTime = departureTime,
                arrivalDateIso = departureDateIso,
                arrivalTime = arrivalTime,
                operatorName = orgName.ifBlank { "Operador" },
                serviceNumber = serviceNumber,
                carriage = carriage,
                seat = seat,
                boardingTime = boardingTime,
                passengerName = passengerName,
                bookingCode = bookingCode,
                ticketNumber = ticketNumber,
                barcodeMessage = barcodeMessage,
                barcodeFormat = barcodeFormat,
                barcodeAltText = barcodeAltText,
                rawJson = jsonString
            )
        }
    }

    private fun extractLocationAndTime(obj: JSONObject): Pair<String, String> {
        val label = obj.optString("label").trim()
        val value = obj.optString("value").trim()

        val isValueTime = value.matches(Regex("""\d{1,2}:\d{2}"""))
        val isLabelTime = label.matches(Regex("""\d{1,2}:\d{2}"""))

        return when {
            isValueTime && label.isNotBlank() -> label to value
            isLabelTime && value.isNotBlank() -> value to label
            label.isNotBlank() && value.isNotBlank() -> {
                // ej. label: "Madrid Barajas (MAD)", value: "MAD"
                if (label.length >= value.length) label to "" else value to ""
            }
            label.isNotBlank() -> label to ""
            else -> value to ""
        }
    }

    private fun parseAnyDateToIso(dateStr: String): String? {
        val trimmed = dateStr.trim()
        if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return trimmed

        val slashDmy = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""").find(trimmed)
        if (slashDmy != null) {
            val d = slashDmy.groupValues[1].toInt()
            val m = slashDmy.groupValues[2].toInt()
            val y = slashDmy.groupValues[3].toInt()
            return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
        }
        return null
    }

    private fun JSONArray?.toList(): List<JSONObject> {
        if (this == null) return emptyList()
        val list = mutableListOf<JSONObject>()
        for (i in 0 until length()) {
            optJSONObject(i)?.let { list.add(it) }
        }
        return list
    }

    fun toTransportSegment(
        pass: ParsedPkPass,
        order: Int,
        id: String = "seg-pkpass-${System.currentTimeMillis()}"
    ): TransportSegmentItem {
        val mode = when (pass.transitType) {
            "PKTransitTypeAir" -> TransportMode.FLIGHT
            "PKTransitTypeBus" -> TransportMode.BUS
            else -> TransportMode.TRAIN_SHINKANSEN
        }

        val seatParts = mutableListOf<String>()
        if (!pass.carriage.isNullOrBlank()) seatParts.add("Coche ${pass.carriage}")
        if (!pass.seat.isNullOrBlank()) seatParts.add("Asiento ${pass.seat}")
        val seatOrCarInfo = if (seatParts.isNotEmpty()) seatParts.joinToString(" · ") else "Asiento asignado"

        val extraNotes = buildString {
            if (!pass.bookingCode.isNullOrBlank()) append("Localizador: ${pass.bookingCode}")
            if (!pass.ticketNumber.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Billete: ${pass.ticketNumber}")
            }
        }

        val zoneId = if (pass.organizationName.contains("Renfe", ignoreCase = true) ||
            pass.originName.contains("Madrid", ignoreCase = true) ||
            pass.originName.contains("València", ignoreCase = true)
        ) {
            "Europe/Madrid"
        } else {
            "Asia/Tokyo"
        }

        val departureTimeIso = "${pass.departureDateIso}T${pass.departureTime}"
        val arrivalTimeIso = "${pass.arrivalDateIso}T${pass.arrivalTime}"

        val calculatedBoardingTime = if (!pass.boardingTime.isNullOrBlank()) {
            "${pass.departureDateIso}T${pass.boardingTime}"
        } else {
            // En trenes el acceso cierra 2-5 min antes, sugerimos 20 min antes
            runCatching {
                val parts = pass.departureTime.split(":")
                var h = parts[0].toInt()
                var m = parts[1].toInt() - 20
                if (m < 0) {
                    m += 60
                    h = (h - 1).coerceAtLeast(0)
                }
                String.format(Locale.US, "%sT%02d:%02d", pass.departureDateIso, h, m)
            }.getOrNull()
        }

        return TransportSegmentItem(
            id = id,
            order = order,
            type = mode,
            operatorName = pass.operatorName,
            lineOrNumber = pass.serviceNumber,
            departureCode = pass.originName.take(3).uppercase(Locale.ROOT),
            departureName = pass.originName,
            departureZoneId = zoneId,
            departureTerminalOrPlatform = "Estación / Terminal",
            boardingTime = calculatedBoardingTime,
            departureTimeIso = departureTimeIso,
            arrivalCode = pass.destinationName.take(3).uppercase(Locale.ROOT),
            arrivalName = pass.destinationName,
            arrivalZoneId = zoneId,
            arrivalTerminalOrPlatform = "Estación / Terminal",
            arrivalTimeIso = arrivalTimeIso,
            seatOrCarInfo = seatOrCarInfo,
            stationExitOrBelt = extraNotes,
            passOrPaymentBadge = "Apple Wallet (.pkpass)"
        )
    }

    fun toTravelDocument(
        pass: ParsedPkPass,
        id: String = "doc-pkpass-${System.currentTimeMillis()}",
        fileRelativePath: String? = null
    ): TravelDocumentItem {
        val category = if (pass.transitType == "PKTransitTypeAir") {
            DocumentCategory.FLIGHT
        } else {
            DocumentCategory.TRANSPORT
        }

        val title = "${pass.operatorName} ${pass.serviceNumber}: ${pass.originName} ➔ ${pass.destinationName}"
        val notes = buildString {
            if (!pass.passengerName.isNullOrBlank()) append("Pasajero: ${pass.passengerName}\n")
            if (!pass.bookingCode.isNullOrBlank()) append("Localizador: ${pass.bookingCode}\n")
            if (!pass.ticketNumber.isNullOrBlank()) append("Nº Billete: ${pass.ticketNumber}\n")
            if (!pass.carriage.isNullOrBlank()) append("Coche: ${pass.carriage} · ")
            if (!pass.seat.isNullOrBlank()) append("Asiento: ${pass.seat}\n")
            append("Salida: ${pass.departureDateIso} ${pass.departureTime}\n")
            append("Llegada: ${pass.arrivalDateIso} ${pass.arrivalTime}")
        }

        return TravelDocumentItem(
            id = id,
            title = title,
            category = category,
            referenceOrNotes = notes,
            internalUriPath = fileRelativePath,
            locatorCode = pass.bookingCode
        )
    }
}
