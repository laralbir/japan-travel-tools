package com.laralbir.japantraveltools.core.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Universal date, time and timezone formatter and ISO UTC converter for international travel.
 */
object TravelDateTimeFormatter {

    /**
     * Formats an ISO date string ("YYYY-MM-DD") into the full localized date string
     * using the system regional format (e.g. "Lunes, 15 de marzo de 2027" or "Monday, March 15, 2027").
     */
    fun formatFullLocalDate(dateIso: String, locale: Locale = Locale.getDefault()): String {
        return runCatching {
            val localDate = LocalDate.parse(dateIso)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
            val formatted = localDate.format(formatter)
            // Capitalize first letter if lowercase (e.g. Spanish "lunes" -> "Lunes")
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }.getOrDefault(dateIso)
    }

    /**
     * Formats local time with location name and explicit UTC offset (e.g. "12:25 (Múnich · UTC+1 · CET)").
     */
    fun formatLocalTimeWithZone(
        timeHhMm: String,
        dateIso: String,
        zoneId: String,
        locationName: String? = null,
        isEs: Boolean = true
    ): String {
        return runCatching {
            val zone = ZoneId.of(zoneId)
            val localDate = runCatching { LocalDate.parse(dateIso) }.getOrDefault(LocalDate.now())
            val timeParts = timeHhMm.split(":")
            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val localTime = LocalTime.of(h, m)
            val zonedDateTime = ZonedDateTime.of(localDate, localTime, zone)

            val offsetId = zonedDateTime.offset.id
            val utcOffsetStr = formatUtcOffsetString(offsetId)
            val shortZoneName = zone.getDisplayName(TextStyle.SHORT, if (isEs) Locale.forLanguageTag("es-ES") else Locale.US)

            val locPart = locationName?.takeIf { it.isNotBlank() }?.let { "$it · " } ?: ""
            val zoneInfo = if (shortZoneName.isNotBlank() && shortZoneName != offsetId && !shortZoneName.startsWith("GMT") && !shortZoneName.startsWith("UTC")) {
                "$locPart$utcOffsetStr · $shortZoneName"
            } else {
                "$locPart$utcOffsetStr"
            }

            val formattedTime = String.format(Locale.US, "%02d:%02d", h, m)
            "$formattedTime ($zoneInfo)"
        }.getOrDefault(timeHhMm)
    }

    /**
     * Converts a raw offset string (e.g. "+01:00", "+09:00", "Z") to readable UTC offset (e.g. "UTC+1", "UTC+9").
     */
    fun formatUtcOffsetString(offsetId: String): String {
        if (offsetId == "Z" || offsetId == "+00:00" || offsetId == "-00:00") return "UTC"
        val sign = if (offsetId.startsWith("-")) "-" else "+"
        val clean = offsetId.removePrefix("+").removePrefix("-")
        val parts = clean.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val mins = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return if (mins > 0) {
            String.format(Locale.US, "UTC%s%d:%02d", sign, hours, mins)
        } else {
            String.format(Locale.US, "UTC%s%d", sign, hours)
        }
    }

    /**
     * Converts a local date, time and zoneId to an ISO UTC string (e.g. "2027-03-15T11:25:00Z").
     */
    fun localToUtcIso(dateIso: String, timeHhMm: String, zoneId: String): String {
        return runCatching {
            val zone = ZoneId.of(zoneId)
            val localDate = LocalDate.parse(dateIso)
            val timeParts = timeHhMm.split(":")
            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val localTime = LocalTime.of(h, m)
            val zonedDateTime = ZonedDateTime.of(localDate, localTime, zone)
            zonedDateTime.toInstant().toString()
        }.getOrDefault("${dateIso}T${timeHhMm}:00Z")
    }

    /**
     * Converts an ISO UTC string back to local date ("YYYY-MM-DD") and local time ("HH:mm") in target zone.
     */
    fun utcIsoToLocal(utcIso: String, targetZoneId: String): Pair<String, String> {
        return runCatching {
            val instant = Instant.parse(utcIso)
            val zone = ZoneId.of(targetZoneId)
            val zonedDateTime = instant.atZone(zone)
            val dateStr = zonedDateTime.toLocalDate().toString()
            val timeStr = String.format(Locale.US, "%02d:%02d", zonedDateTime.hour, zonedDateTime.minute)
            Pair(dateStr, timeStr)
        }.getOrDefault(Pair(utcIso.substringBefore('T'), utcIso.substringAfter('T').take(5)))
    }

    /**
     * Infers the ZoneId based on coordinates (lat, lng) or location name keywords.
     */
    fun inferZoneIdFromCoordinatesOrLocation(
        lat: Double,
        lng: Double,
        locationText: String? = null
    ): String {
        // 1. Geographic bounding boxes
        if (lat in 24.0..46.0 && lng in 122.0..154.0) {
            return "Asia/Tokyo"
        }
        if (lat in 35.0..44.0 && lng in -10.0..5.0) {
            return "Europe/Madrid"
        }
        if (lat in 47.0..55.0 && lng in 5.0..16.0) {
            return "Europe/Berlin"
        }

        // 2. Keyword heuristic
        val lower = locationText?.lowercase(Locale.ROOT) ?: ""
        if (lower.contains("madrid") || lower.contains("barajas") || lower.contains("barcelona") || lower.contains("valencia") || lower.contains("españa") || lower.contains("spain")) {
            return "Europe/Madrid"
        }
        if (lower.contains("múnich") || lower.contains("munich") || lower.contains("frankfurt") || lower.contains("berlin") || lower.contains("berlín") || lower.contains("germany") || lower.contains("alemania")) {
            return "Europe/Berlin"
        }
        if (lower.contains("doha") || lower.contains("qatar")) {
            return "Asia/Qatar"
        }
        if (lower.contains("dubai") || lower.contains("dubái") || lower.contains("emirates")) {
            return "Asia/Dubai"
        }
        if (lower.contains("tokyo") || lower.contains("tokio") || lower.contains("osaka") || lower.contains("kyoto") || lower.contains("kioto") || lower.contains("japan") || lower.contains("japón") || lower.contains("kix") || lower.contains("hnd") || lower.contains("nrt") || lower.contains("takayama") || lower.contains("hakone") || lower.contains("nara") || lower.contains("hiroshima") || lower.contains("shinkansen")) {
            return "Asia/Tokyo"
        }

        return "Asia/Tokyo"
    }
}
