package com.laralbir.japantraveltools.core.portability

import com.laralbir.japantraveltools.feature.documents.DocumentsAndGuidesJsonCodec

enum class JsonContentType(val labelEs: String, val labelEn: String) {
    TRAVEL_PLAN("Plan de Viaje", "Travel Plan"),
    TRAVEL_GUIDES("Guías de Viaje", "Travel Guides"),
    TRAVEL_DOCUMENTS("Billetes y Reservas", "Tickets & Reservations"),
    FULL_BACKUP("Copia de Seguridad Completa", "Full Backup Bundle"),
    UNKNOWN("Archivo no reconocido", "Unrecognized File")
}

data class DetectedJsonContent(
    val type: JsonContentType,
    val title: String,
    val summaryEs: String,
    val summaryEn: String,
    val stopCount: Int = 0,
    val transportCount: Int = 0,
    val guideCount: Int = 0,
    val documentCount: Int = 0,
    val rawJson: String = ""
)

object JsonContentDetector {

    fun inspect(json: String): DetectedJsonContent {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return DetectedJsonContent(
                type = JsonContentType.UNKNOWN,
                title = "JSON Inválido",
                summaryEs = "El archivo no contiene un objeto JSON válido.",
                summaryEn = "The file does not contain a valid JSON object.",
                rawJson = json
            )
        }

        // 1. Full Backup Bundle Check
        if (trimmed.contains("\"cloudBackupVersion\"") || (trimmed.contains("\"syncData\"") && trimmed.contains("\"plans\""))) {
            return DetectedJsonContent(
                type = JsonContentType.FULL_BACKUP,
                title = "Copia de Seguridad Completa",
                summaryEs = "Copia de seguridad completa (planes, guías, billetes, ciudades y preferencias).",
                summaryEn = "Full backup package (plans, guides, tickets, cities and preferences).",
                rawJson = json
            )
        }

        // 2. Travel Plan Check
        if (trimmed.contains("\"itineraryStops\"") || trimmed.contains("\"transportSegments\"") || (trimmed.contains("\"plans\"") && trimmed.contains("\"activePlanId\""))) {
            return runCatching {
                val bundle = TravelPlanJsonCodec.importFromJson(json)
                val title = bundle.tripTitle.ifBlank { "Plan de Viaje" }
                val stops = bundle.itineraryStops.size
                val transport = bundle.transportSegments.size
                DetectedJsonContent(
                    type = JsonContentType.TRAVEL_PLAN,
                    title = title,
                    summaryEs = "Plan de viaje con $stops paradas de itinerario y $transport tramos de transporte.",
                    summaryEn = "Travel plan with $stops itinerary stops and $transport transport segments.",
                    stopCount = stops,
                    transportCount = transport,
                    rawJson = json
                )
            }.getOrElse {
                DetectedJsonContent(
                    type = JsonContentType.UNKNOWN,
                    title = "Error al analizar plan",
                    summaryEs = "Estructura de plan de viaje no válida.",
                    summaryEn = "Invalid travel plan structure.",
                    rawJson = json
                )
            }
        }

        // 3. Travel Guides Check
        if (trimmed.contains("\"guides\"")) {
            return runCatching {
                val guides = DocumentsAndGuidesJsonCodec.importGuidesJson(json)
                if (guides.isNotEmpty()) {
                    DetectedJsonContent(
                        type = JsonContentType.TRAVEL_GUIDES,
                        title = "Guías de Viaje (${guides.size})",
                        summaryEs = "Colección con ${guides.size} guías de viaje.",
                        summaryEn = "Collection with ${guides.size} travel guides.",
                        guideCount = guides.size,
                        rawJson = json
                    )
                } else {
                    DetectedJsonContent(
                        type = JsonContentType.UNKNOWN,
                        title = "Guías vacías",
                        summaryEs = "El archivo no contiene guías válidas.",
                        summaryEn = "The file contains no valid guides.",
                        rawJson = json
                    )
                }
            }.getOrElse {
                DetectedJsonContent(
                    type = JsonContentType.UNKNOWN,
                    title = "Error al analizar guías",
                    summaryEs = "Estructura de guías no válida.",
                    summaryEn = "Invalid guides structure.",
                    rawJson = json
                )
            }
        }

        // 4. Travel Documents Check
        if (trimmed.contains("\"documents\"")) {
            return runCatching {
                val docs = DocumentsAndGuidesJsonCodec.importDocumentsJson(json)
                if (docs.isNotEmpty()) {
                    DetectedJsonContent(
                        type = JsonContentType.TRAVEL_DOCUMENTS,
                        title = "Billetes y Reservas (${docs.size})",
                        summaryEs = "Colección con ${docs.size} billetes o reservas de viaje.",
                        summaryEn = "Collection with ${docs.size} travel tickets or reservations.",
                        documentCount = docs.size,
                        rawJson = json
                    )
                } else {
                    DetectedJsonContent(
                        type = JsonContentType.UNKNOWN,
                        title = "Documentos vacíos",
                        summaryEs = "El archivo no contiene documentos válidos.",
                        summaryEn = "The file contains no valid documents.",
                        rawJson = json
                    )
                }
            }.getOrElse {
                DetectedJsonContent(
                    type = JsonContentType.UNKNOWN,
                    title = "Error al analizar documentos",
                    summaryEs = "Estructura de documentos no válida.",
                    summaryEn = "Invalid documents structure.",
                    rawJson = json
                )
            }
        }

        return DetectedJsonContent(
            type = JsonContentType.UNKNOWN,
            title = "Contenido No Reconocido",
            summaryEs = "El archivo JSON no corresponde a ningún plan, guía ni reserva conocido.",
            summaryEn = "The JSON file does not correspond to any known plan, guide or reservation.",
            rawJson = json
        )
    }
}
