package com.laralbir.japantraveltools.feature.clock

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DayPeriod(val iconSymbol: String) {
    NIGHT("🌙"),
    MORNING("🌅"),
    AFTERNOON("☀️"),
    EVENING("🌆")
}

data class ClockComparisonResult(
    val localZoneId: String,
    val localTimeFormatted: String,
    val localDateFormatted: String,
    val localDayPeriod: DayPeriod,
    val japanTimeFormatted: String,
    val japanDateFormatted: String,
    val japanDayPeriod: DayPeriod,
    val originZoneId: String,
    val originTimeFormatted: String,
    val originDateFormatted: String,
    val originDayPeriod: DayPeriod,
    val hoursDifferenceTokyoVsOrigin: Int
)

data class SupportedOriginCountry(
    val zoneId: String,
    val nameEn: String,
    val nameEs: String,
    val flagEmoji: String
)

object OfflineTimeZoneResolver {

    val supportedOrigins = listOf(
        SupportedOriginCountry("Europe/Madrid", "Spain (Madrid)", "España (Madrid)", "🇪🇸"),
        SupportedOriginCountry("Atlantic/Canary", "Spain (Canary Islands)", "España (Islas Canarias)", "🇮🇨"),
        SupportedOriginCountry("Europe/London", "United Kingdom (London)", "Reino Unido (Londres)", "🇬🇧"),
        SupportedOriginCountry("Europe/Paris", "France (Paris)", "Francia (París)", "🇫🇷"),
        SupportedOriginCountry("Europe/Berlin", "Germany (Berlin)", "Alemania (Berlín)", "🇩🇪"),
        SupportedOriginCountry("America/Mexico_City", "Mexico (Mexico City)", "México (CDMX)", "🇲🇽"),
        SupportedOriginCountry("America/Argentina/Buenos_Aires", "Argentina (Buenos Aires)", "Argentina (Buenos Aires)", "🇦🇷"),
        SupportedOriginCountry("America/Bogota", "Colombia (Bogotá)", "Colombia (Bogotá)", "🇨🇴"),
        SupportedOriginCountry("America/Santiago", "Chile (Santiago)", "Chile (Santiago)", "🇨🇱"),
        SupportedOriginCountry("America/New_York", "USA (New York EST)", "EE. UU. (Nueva York)", "🇺🇸"),
        SupportedOriginCountry("America/Los_Angeles", "USA (Los Angeles PST)", "EE. UU. (Los Ángeles)", "🇺🇸"),
        SupportedOriginCountry("Asia/Tokyo", "Japan (Tokyo JST)", "Japón (Tokio JST)", "🇯🇵")
    )

    /**
     * Resolves geographic coordinates to a ZoneId 100% offline without requiring network calls.
     */
    fun resolveZoneFromCoordinates(latitude: Double, longitude: Double): ZoneId {
        return when {
            // Japan bounding box (Hokkaido to Okinawa)
            latitude in 24.0..46.0 && longitude in 122.0..146.5 -> ZoneId.of("Asia/Tokyo")
            // Iberian Peninsula (Spain)
            latitude in 35.5..44.0 && longitude in -9.5..3.5 -> ZoneId.of("Europe/Madrid")
            // Canary Islands
            latitude in 27.5..29.5 && longitude in -18.5..-13.0 -> ZoneId.of("Atlantic/Canary")
            // UK / Ireland
            latitude in 49.5..60.0 && longitude in -10.5..1.8 -> ZoneId.of("Europe/London")
            // Central Europe
            latitude in 41.0..55.0 && longitude in 2.0..18.0 -> ZoneId.of("Europe/Paris")
            // Gulf Hub (Doha / Dubai layovers)
            latitude in 22.0..27.0 && longitude in 50.0..56.5 -> ZoneId.of("Asia/Qatar")
            // Mexico
            latitude in 14.5..32.5 && longitude in -117.0..-86.5 -> ZoneId.of("America/Mexico_City")
            // US East
            latitude in 25.0..48.0 && longitude in -85.0..-67.0 -> ZoneId.of("America/New_York")
            // US West
            latitude in 32.0..49.0 && longitude in -125.0..-114.0 -> ZoneId.of("America/Los_Angeles")
            else -> ZoneId.systemDefault()
        }
    }
}

object CalculateClockComparisonUseCase {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.US)
    private val tokyoZone = ZoneId.of("Asia/Tokyo")

    fun calculate(
        instant: Instant,
        localZone: ZoneId,
        originZone: ZoneId
    ): ClockComparisonResult {
        val localZdt = ZonedDateTime.ofInstant(instant, localZone)
        val japanZdt = ZonedDateTime.ofInstant(instant, tokyoZone)
        val originZdt = ZonedDateTime.ofInstant(instant, originZone)

        val tokyoOffsetSeconds = japanZdt.offset.totalSeconds
        val originOffsetSeconds = originZdt.offset.totalSeconds
        val diffHours = (tokyoOffsetSeconds - originOffsetSeconds) / 3600

        return ClockComparisonResult(
            localZoneId = localZone.id,
            localTimeFormatted = localZdt.format(timeFormatter),
            localDateFormatted = localZdt.format(dateFormatter),
            localDayPeriod = periodOf(localZdt.hour),
            japanTimeFormatted = japanZdt.format(timeFormatter),
            japanDateFormatted = japanZdt.format(dateFormatter),
            japanDayPeriod = periodOf(japanZdt.hour),
            originZoneId = originZone.id,
            originTimeFormatted = originZdt.format(timeFormatter),
            originDateFormatted = originZdt.format(dateFormatter),
            originDayPeriod = periodOf(originZdt.hour),
            hoursDifferenceTokyoVsOrigin = diffHours
        )
    }

    private fun periodOf(hour: Int): DayPeriod = when (hour) {
        in 0..5 -> DayPeriod.NIGHT
        in 6..11 -> DayPeriod.MORNING
        in 12..17 -> DayPeriod.AFTERNOON
        else -> DayPeriod.EVENING
    }
}

