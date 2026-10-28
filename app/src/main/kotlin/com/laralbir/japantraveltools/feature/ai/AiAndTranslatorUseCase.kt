package com.laralbir.japantraveltools.feature.ai

import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import java.util.Locale

enum class AssistantSource {
    GEMINI_CLOUD,
    OFFLINE_EXPERT
}

data class AiTravelAssistantResult(
    val answer: String,
    val source: AssistantSource,
    val timestampFormatted: String,
    val cloudErrorDetail: String? = null
)

object GeminiAssistantEngine {

    /**
     * Executes an AI query via Gemini Cloud API if an API key is available and connectivity exists,
     * otherwise falls back seamlessly to the comprehensive offline expert engine, providing
     * transparent error diagnostics if the cloud call failed.
     */
    suspend fun queryAssistant(
        prompt: String,
        apiKey: String?,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle? = null,
        modelId: String? = null,
        isEs: Boolean = true
    ): AiTravelAssistantResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        val nowFormatted = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        var cloudError: String? = null
        if (!apiKey.isNullOrBlank()) {
            val (cloudAnswer, err) = queryGeminiCloudApi(cleanPrompt, apiKey, eurToJpyRate, currentPlan, modelId, isEs)
            if (cloudAnswer != null && cloudAnswer.isNotBlank()) {
                return@withContext AiTravelAssistantResult(
                    answer = cloudAnswer,
                    source = AssistantSource.GEMINI_CLOUD,
                    timestampFormatted = nowFormatted
                )
            }
            cloudError = err
        }

        val offlineAnswer = generateSmartTravelAnswer(cleanPrompt, eurToJpyRate, currentPlan)
        AiTravelAssistantResult(
            answer = offlineAnswer,
            source = AssistantSource.OFFLINE_EXPERT,
            timestampFormatted = nowFormatted,
            cloudErrorDetail = cloudError
        )
    }

    private fun queryGeminiCloudApi(
        prompt: String,
        apiKey: String,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle?,
        modelId: String? = null,
        isEs: Boolean = true
    ): Pair<String?, String?> {
        val resolvedModel = if (modelId.isNullOrBlank()) GeminiApiKeyHelper.DEFAULT_MODEL_ID else modelId.trim()
        val prefix = apiKey.trim().take(6)
        return try {
            val endpointUrl = GeminiApiKeyHelper.buildGenerateContentUrl(apiKey, resolvedModel)
            val url = java.net.URL(endpointUrl)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 30000
                doOutput = true
            }

            val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
            val stopsCount = currentPlan?.itineraryStops?.size ?: 0
            val systemContext = "Eres el Asistente IA de Viaje para Japón en la aplicación Japan Travel Tools. Responde con tono amable, experto y muy estructurado con viñetas y emojis. Incluye caracteres japoneses (Kanji/Kana) y fonética Romaji cuando menciones lugares, trenes o platos. Convierte precios aproximados en JPY a EUR según la tasa 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY. El usuario tiene un plan de viaje con $stopsCount paradas. Responde en español."

            val escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
            val escapedSystem = systemContext.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

            val jsonPayload = """{"system_instruction":{"parts":[{"text":"$escapedSystem"}]},"contents":[{"parts":[{"text":"$escapedPrompt"}]}],"generationConfig":{"temperature":0.4,"maxOutputTokens":1024}}"""

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = try {
                    val root = org.json.JSONObject(responseText)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    parts?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                } ?: run {
                    val textPattern = """"text"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
                    val match = textPattern.find(responseText)
                    match?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
                }
                text to null
            } else {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                val errorMsg = GeminiApiKeyHelper.evaluateHttpError(responseCode, errorBody, prefix, isEs, resolvedModel)
                null to errorMsg
            }
        } catch (e: java.net.SocketTimeoutException) {
            val msg = if (isEs) {
                "⏱️ Tiempo de espera agotado (Timeout): El modelo tardó más de 30 segundos en responder. Se recomienda seleccionar «Gemini Flash-Lite», que responde en menos de 1 segundo."
            } else {
                "⏱️ Request Timeout: Model took more than 30 seconds to respond. Select «Gemini Flash-Lite» for sub-second responses."
            }
            null to msg
        } catch (e: Exception) {
            val msg = if (isEs) {
                "📶 Error de conexión con Gemini Cloud (${e.localizedMessage ?: "timeout"})."
            } else {
                "📶 Gemini Cloud connection error (${e.localizedMessage ?: "timeout"})."
            }
            null to msg
        }
    }

    /**
     * Finds prices in Yen (e.g. ¥1,200, 1200円, 1200 yenes, 1200 yen, 1200 JPY)
     * and appends the converted EUR price using the cached offline rate.
     */
    fun enrichMenuWithConvertedPrices(text: String, eurToJpyRate: Double): String {
        val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
        val yenSymbolRegex = """¥\s?([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)""".toRegex()
        val withSymbolConverted = yenSymbolRegex.replace(text) { match ->
            val rawDigits = match.groupValues[1].replace(",", "")
            val yenValue = rawDigits.toDoubleOrNull() ?: return@replace match.value
            val eurValue = yenValue / safeRate
            val formattedEur = String.format(Locale.US, "%.2f EUR", eurValue)
            "${match.value} (~$formattedEur)"
        }
        return withSymbolConverted
    }

    fun generateSmartTravelAnswer(
        prompt: String,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle? = null
    ): String {
        val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
        val cleanPrompt = prompt.trim()
        val lower = cleanPrompt.lowercase(Locale.ROOT)

        // 1. Check if the user entered specific yen numbers (e.g., "3200 yenes", "¥1,500", "5000 jpy")
        val numberYenRegex = """(?:¥\s*)?([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{2,7})\s*(?:yenes|yen|jpy|円)?""".toRegex(RegexOption.IGNORE_CASE)
        val hasExplicitPriceContext = lower.contains("¥") || lower.contains("yen") || lower.contains("jpy") || lower.contains("円") || lower.contains("euro") || lower.contains("cuesta") || lower.contains("precio")
        if (hasExplicitPriceContext && !lower.contains("ramen") && !lower.contains("carta")) {
            val match = numberYenRegex.find(cleanPrompt)
            val rawDigits = match?.groupValues?.get(1)?.replace(",", "")?.replace(".", "")
            val yenAmount = rawDigits?.toDoubleOrNull()
            if (yenAmount != null && yenAmount > 0) {
                val eurAmount = String.format(Locale.US, "%.2f EUR", yenAmount / safeRate)
                return buildString {
                    appendLine("💴 Conversión rápida e impuestos en Japón:")
                    appendLine("• ${yenAmount.toLong()} JPY equivalen a $eurAmount (tasa actual: 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY).")
                    val withTax = (yenAmount * 1.10).toLong()
                    val withTaxEur = String.format(Locale.US, "%.2f EUR", withTax / safeRate)
                    appendLine("• Si el precio es antes de impuestos (税抜), con el 10% de IVA japonés (税込) serán ¥$withTax (~$withTaxEur).")
                    if (yenAmount >= 5000) {
                        append("• Al superar los ¥5,000 (¥5,500 con impuestos), puedes pedir devolución Tax-Free mostrando tu pasaporte físico.")
                    }
                }
            }
        }

        // 2. Topic-specific intelligent travel assistant answers
        return when {
            lower.contains("ramen") || lower.contains("menú") || lower.contains("menu") || lower.contains("carta") || lower.contains("sushi") || lower.contains("comer") -> {
                val sampleMenu = """
                    🍜 Traducción de Carta Habitual y Precios en Euros:
                    • Tonkotsu Ramen Especial (豚骨特製ラーメン): Caldo cremoso de cerdo con chashu y huevo ajitsuke — ¥1,200
                    • Gyoza a la plancha 6 uds (焼き餃子): Empanadillas crujientes de cerdo y cebollino — ¥450
                    • Katsudon (カツ丼): Cuenco de arroz con lomo de cerdo empanado y huevo — ¥980
                    • Matcha Parfait de Uji (宇治抹茶パフェ): Helado artesanal de té verde con mochi — ¥850
                    • Cerveza de barril Nama Biru (生ビール): Jarra fría — ¥600
                """.trimIndent()
                enrichMenuWithConvertedPrices(sampleMenu, safeRate)
            }

            lower.contains("tax") || lower.contains("tax-free") || lower.contains("don quijote") || lower.contains("impuesto") || lower.contains("devolución") -> {
                val minTaxEur = String.format(Locale.US, "%.2f EUR", 5000.0 / safeRate)
                """
                    🛍️ Compras Tax-Free en Japón (Don Quijote, Bic Camera, Uniqlo):
                    • Requisito de compra: Gasto superior a ¥5,000 (~$minTaxEur) antes de impuestos en un mismo día y establecimiento.
                    • Pasaporte Físico Obligatorio: Es imprescindible presentar el pasaporte original en la caja con el sello de turista (no sirven fotos ni fotocopias).
                    • Productos consumibles (cosmética, snacks, medicinas): Se entregan en una bolsa precintada que NO se debe abrir hasta salir de Japón.
                    • Ahorro directo: Te descuentan el 10% del IVA japonés en el momento de pagar, sin esperas ni sellos en aduanas.
                """.trimIndent()
            }

            lower.contains("narita") || lower.contains("haneda") || lower.contains("aeropuerto") || lower.contains("kix") || lower.contains("skyliner") || lower.contains("haruka") -> {
                val nexEur = String.format(Locale.US, "%.2f EUR", 3070.0 / safeRate)
                val skyEur = String.format(Locale.US, "%.2f EUR", 2570.0 / safeRate)
                """
                    ✈️ Traslados desde los Aeropuertos:
                    • Aeropuerto de Narita (NRT) ➔ Tokio:
                      1. JR Narita Express (N'EX): 55 min directo a Tokyo Station / Shinjuku (~¥3,070 / ~$nexEur).
                      2. Keisei Skyliner: 36 min directo a Ueno / Nippori (~¥2,570 / ~$skyEur).
                    • Aeropuerto de Haneda (HND) ➔ Tokio:
                      1. Tokyo Monorail: 13 min a Hamamatsucho Station (conecta con JR Yamanote Line).
                      2. Línea Keikyu Airport: 11 min a Shinagawa Station.
                    • Aeropuerto de Kansai (KIX) ➔ Kioto / Osaka:
                      1. Tren JR Haruka: 75 min directo a Kioto Station / 45 min a Shin-Osaka.
                      2. Nankai Rapi:t: 34 min directo a Namba (Osaka).
                """.trimIndent()
            }

            lower.contains("esim") || lower.contains("sim") || lower.contains("internet") || lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("datos") -> {
                """
                    📶 Conexión a Internet en Japón (eSIM vs Pocket Wi-Fi):
                    • eSIM (Opción más recomendada): Muy cómoda para móviles compatibles (Pixel, iPhone, Galaxy). Se instala antes del vuelo mediante código QR (proveedores como Airalo, Ubigi o Holafly). Mantienes tu WhatsApp habitual.
                    • Pocket Wi-Fi (Router portátil): Ideal si viajas en pareja o familia para conectar varios dispositivos a la vez. Se recoge en el mostrador del aeropuerto (Narita/Haneda/KIX) y se devuelve en un buzón postal con el sobre prepagado incluido.
                    • Tarjeta SIM física: Si tu teléfono no soporta eSIM, puedes comprar una SIM de datos en las máquinas automáticas o mostradores de llegada del aeropuerto.
                """.trimIndent()
            }

            lower.contains("propina") || lower.contains("tip") || lower.contains("modales") || lower.contains("educación") || lower.contains("normas") || lower.contains("costumbre") -> {
                """
                    🚫 Propinas y Modales en Japón:
                    • Cero Propinas: En Japón NUNCA se deja propina. Dejar dinero extra en la mesa o barra es considerado de mala educación o una confusión; el camarero correrá detrás de ti para devolverte el cambio.
                    • No comer caminando: Se come o bebe de pie junto a la tienda o máquina expendedora donde compraste, no andando por la calle.
                    • Silencio en transporte público: Pon el teléfono en modo silencio ("Manner Mode"). Está mal visto hablar por teléfono o con voz alta en trenes y metros.
                    • Pago en caja: Deposita el dinero o la tarjeta en la pequeña bandeja (Torii) del mostrador, no directamente en la mano del cajero.
                """.trimIndent()
            }

            lower.contains("onsen") || lower.contains("tatuaje") || lower.contains("baño termal") || lower.contains("termas") || lower.contains("kashikiri") -> {
                """
                    ♨️ Normas en los Onsen (Baños Termales) y Tatuajes:
                    • Desnudez total: Se entra completamente desnudo, sin bañador. Hombres y mujeres se bañan en zonas separadas.
                    • Ducha previa obligatoria: Hay que lavarse y enjabonarse a conciencia en las banquetas individuales con ducha antes de meterse al agua termal.
                    • Toalla pequeña: No debe tocar nunca el agua de la poza termal; déjala doblada sobre tu cabeza o en el borde.
                    • Política de Tatuajes: Muchos onsen tradicionales prohíben la entrada a personas con tatuajes. Busca baños etiquetados como "Tattoo-friendly", cúbrelos con parches adhesivos color piel o reserva un baño privado familiar (Kashikiri Onsen).
                """.trimIndent()
            }

            lower.contains("jr pass") || lower.contains("japan rail pass") || lower.contains("merece la pena") || lower.contains("rentable") -> {
                """
                    🚅 ¿Merece la pena el JR Pass?:
                    • Tras la subida de precios de octubre de 2023 (+65%), el JR Pass nacional general casi NUNCA es rentable para un itinerario estándar (Tokio - Kioto - Osaka).
                    • Alternativa económica: Es mucho más barato comprar los billetes sueltos de Shinkansen (Tokio ➔ Kioto son unos ¥14,170 en SmartEX o taquilla) y usar la tarjeta Suica/Pasmo para los metros y trenes urbanos.
                    • Pases Regionales: Si vas a moverte mucho por Kansai, Hiroshima o los Alpes japoneses, pases como el JR Kansai-Hiroshima Area Pass sí siguen saliendo muy a cuenta.
                """.trimIndent()
            }

            lower.contains("enchufe") || lower.contains("voltaje") || lower.contains("clavija") || lower.contains("adaptador") || lower.contains("electricidad") -> {
                """
                    🔌 Enchufes y Electricidad en Japón:
                    • Tipo de Clavija: Clavijas de tipo A (dos patillas planas paralelas, idéntico al estándar de EE.UU.). Necesitarás un adaptador universal sencillo.
                    • Voltaje: En Japón el voltaje es de 100 V a 50/60 Hz.
                    • Compatibilidad: Los cargadores de móviles, portátiles, tablets y cámaras son bivoltaje (100-240 V) y funcionan perfectamente solo con el adaptador físico.
                    • Precaución: Aparatos de alta potencia sin bivoltaje (secadores o planchas de pelo europeos de 220-240 V) no calentarán adecuadamente; es mejor usar los del hotel.
                """.trimIndent()
            }

            lower.contains("suica") || lower.contains("pasmo") || lower.contains("icoca") || lower.contains("metro") || lower.contains("tarjeta ic") -> {
                val depositEur = String.format(Locale.US, "%.2f EUR", 500.0 / safeRate)
                val loadEur = String.format(Locale.US, "%.2f EUR", 3000.0 / safeRate)
                """
                    💳 Tarjetas Suica / Pasmo / ICOCA:
                    • ¿Para qué sirven?: Pagan al instante metros, trenes JR urbanos, autobuses, máquinas expendedoras y tiendas Konbini (7-Eleven, Lawson, FamilyMart) acercando la tarjeta al lector.
                    • ¿Cuánto recargar?: Se recomienda empezar con ¥3,000 (~$loadEur). El depósito físico inicial es de ¥500 (~$depositEur).
                    • Nota importante: Las tarjetas IC se recargan siempre en efectivo (yenes) en las máquinas de cualquier estación de tren o metro.
                """.trimIndent()
            }

            lower.contains("shinkansen") || lower.contains("jr pass") || lower.contains("tren bala") || lower.contains("nozomi") || lower.contains("hikari") -> {
                val tokyoKyotoEur = String.format(Locale.US, "%.2f EUR", 14170.0 / safeRate)
                """
                    🚄 Guía de Shinkansen y JR Pass:
                    • Tokio ➔ Kioto: Tarda 2h 15m en Nozomi / 2h 40m en Hikari. Billete suelto: ~¥14,170 (~$tokyoKyotoEur).
                    • Regla de Maletas Grandes (>160 cm suma de alto+ancho+fondo): Es obligatorio reservar asiento "Oversized Baggage Area" (última fila del vagón) sin coste extra al sacar el billete.
                    • Vistas al Monte Fuji: Desde Tokio hacia Kioto/Osaka, elige siempre el Asiento E (ventanilla derecha en sentido de la marcha).
                """.trimIndent()
            }

            lower.contains("nara") || lower.contains("ciervo") || lower.contains("todai") -> {
                val crackersEur = String.format(Locale.US, "%.2f EUR", 200.0 / safeRate)
                """
                    🦌 Excursión Recomendada a Nara (Medio Día desde Kioto u Osaka):
                    1. 09:00 · Tren Kintetsu Nara (35 min desde Kioto / 40 min desde Osaka Namba) o JR Nara Line.
                    2. 09:45 · Parque de Nara (奈良公園): Más de 1.200 ciervos sagrados en libertad (galletas Shika-senbei: ¥200 / ~$crackersEur).
                    3. 10:30 · Gran Buda de Todai-ji (東大寺): El pabellón de madera más impresionante de Japón.
                    4. 12:00 · Santuario Kasuga Taisha (春日大社): Camino de los 3.000 faroles de piedra y bronce.
                    👉 Pulsa "Aplicar en mi Ruta" si quieres añadir este día automáticamente a tu Planificador.
                """.trimIndent()
            }

            lower.contains("hakone") || lower.contains("fuji") || lower.contains("kawaguchiko") -> {
                """
                    🗻 Excursión a Hakone y Vistas al Monte Fuji:
                    1. 08:30 · Salida desde Shinjuku (Odakyu Romancecar) o desde Tokio en Shinkansen Kodama hasta Odawara.
                    2. 10:15 · Teleférico de Hakone y valle volcánico de Owakudani (大涌谷) — prueba los huevos negros Kuro-tamago.
                    3. 12:30 · Crucero por el Lago Ashi (芦ノ湖) hasta el Torii flotante del Santuario Hakone-jinja.
                    4. 15:00 · Baño termal Onsen de día en Hakone Yuryo antes de regresar a Tokio.
                    👉 Revisa antes el índice de visibilidad del Monte Fuji en la herramienta "Tiempo (Open-Meteo)".
                """.trimIndent()
            }

            lower.contains("hiroshima") || lower.contains("miyajima") || lower.contains("itsukushima") -> {
                """
                    ⛩️ Excursión a Hiroshima y la Isla de Miyajima:
                    1. 08:00 · Shinkansen desde Kioto/Shin-Osaka hasta Hiroshima Station (~1h 25m).
                    2. 09:45 · Tren JR Sanyo Line hasta Miyajimaguchi + Ferry JR (10 min, incluido en JR Pass) hasta la isla de Miyajima.
                    3. 10:30 · Gran Torii Flotante de Itsukushima (厳島神社) y prueba las ostras a la parrilla y Momiji Manju.
                    4. 14:30 · Regreso a Hiroshima: Cúpula de la Bomba Atómica (Genbaku Dome), Parque Conmemorativo de la Paz y cena de Okonomiyaki estilo Hiroshima.
                """.trimIndent()
            }

            lower.contains("tokio") || lower.contains("tokyo") || lower.contains("shibuya") || lower.contains("shinjuku") || lower.contains("asakusa") -> {
                """
                    🗼 Ruta Imprescindible por Tokio:
                    • Mañana (Tradición): Templo Senso-ji en Asakusa (08:30, sin aglomeraciones) + Paseo por el Parque Ueno y mercado Ameyoko.
                    • Mediodía (Contraste): Santuario Meiji Jingu en el bosque de Yoyogi + calle Takeshita y Omotesando en Harajuku.
                    • Tarde/Noche (Neones): Mirador Shibuya Sky al atardecer, cruce de Shibuya y cena en Omoide Yokocho / Kabukicho (Shinjuku).
                """.trimIndent()
            }

            lower.contains("kioto") || lower.contains("kyoto") || lower.contains("lluvia") || lower.contains("gion") || lower.contains("fushimi") -> {
                """
                    ⛩️ Ruta Recomendada en Kioto (con alternativa si llueve):
                    • 08:00 · Fushimi Inari Taisha (伏見稲荷大社): Sube los mil toriis rojos a primera hora.
                    • 11:00 · Mercado Nishiki (錦市場 — Galería techada ideal si llueve) y almuerzo en Pontocho.
                    • 14:30 · Templo Kiyomizu-dera y calles históricas de madera Sannenzaka y Ninenzaka.
                    • 18:00 · Paseo al anochecer por el barrio de geishas de Gion (Hanamikoji-dori).
                """.trimIndent()
            }

            lower.contains("osaka") || lower.contains("dotonbori") || lower.contains("kuromon") -> {
                """
                    🐙 Ruta Gastronómica y Urbana en Osaka:
                    • 09:30 · Castillo de Osaka (大阪城) y jardines Nishinomaru.
                    • 12:15 · Mercado Kuromon Ichiba (黒門市場): Marisco fresco a la brasa, vieiras y carne de Wagyu.
                    • 15:30 · Barrio retro de Shinsekai y torre Tsutenkaku (prueba el Kushikatsu).
                    • 18:30 · Canal de Dotonbori, cartel de Glico Man y Takoyaki recién hecho.
                """.trimIndent()
            }

            lower.contains("efectivo") || lower.contains("cajero") || lower.contains("dinero") || lower.contains("7-eleven") || lower.contains("konbini") -> {
                val tenKEur = String.format(Locale.US, "%.2f EUR", 10000.0 / safeRate)
                """
                    🏧 Efectivo y Cajeros Automáticos en Japón:
                    • Los mejores cajeros para tarjetas extranjeras (Visa/Mastercard/Revolut/N26) son los de 7-Eleven (7-Bank ATM), disponibles 24h en todo el país.
                    • Elige siempre facturar en Yenes (JPY) en la pantalla del cajero para evitar comisiones de conversión dinámica.
                    • Lleva siempre entre ¥10,000 (~$tenKEur) y ¥20,000 en efectivo para templos, mercados callejeros y recargas de Suica.
                """.trimIndent()
            }

            lower.contains("maleta") || lower.contains("equipaje") || lower.contains("takkyubin") || lower.contains("yamato") -> {
                val bagEur = String.format(Locale.US, "%.2f EUR", 2500.0 / safeRate)
                """
                    🧳 Envío de Maletas entre Hoteles (Takkyubin / Yamato Transport):
                    • Para no cargar con maletas grandes en el Shinkansen o escaleras de metro, pide en la recepción de tu hotel el servicio "Takkyubin" (icono del gato negro de Yamato).
                    • Coste aproximado por maleta grande (Tokio ➔ Kioto): ~¥2,500 (~$bagEur), entrega al día siguiente directamente en tu próximo hotel.
                    • Maletas en Shinkansen: Si la suma de alto+ancho+profundidad supera los 160 cm, es obligatorio reservar asiento especial en la última fila con espacio para maleta sin recargo.
                """.trimIndent()
            }

            lower.contains("tarjeta") || lower.contains("crédito") || lower.contains("débito") || lower.contains("revolut") || lower.contains("pagar") -> {
                """
                    💳 Tarjetas y Pagos en Japón:
                    • Tarjetas internacionales: Visa y Mastercard (como Revolut, N26 o tarjetas bancarias con chip) se aceptan en hoteles, grandes almacenes, tiendas konbini (7-Eleven, Lawson) y cadenas de restaurantes.
                    • Necesidad de efectivo: Templos, santuarios, puestos callejeros de comida, máquinas de recarga de Suica y pequeños bares ramen solo aceptan dinero en metálico (efectivo en yenes).
                    • Recomendación: Lleva siempre entre ¥10,000 y ¥20,000 en la cartera y saca yenes en los cajeros Seven Bank (7-Eleven) eligiendo cobro en JPY.
                """.trimIndent()
            }

            lower.contains("ropa") || lower.contains("tiempo") || lower.contains("clima") || lower.contains("otoño") || lower.contains("primavera") || lower.contains("frío") -> {
                """
                    🧥 Qué Ropa Llevar y Clima por Estación:
                    • Otoño (Octubre - Noviembre): Clima templado y agradable (14°C a 20°C). Viste por capas: camisetas o camisas de manga larga, jersey fino y una chaqueta para la noche. Perfecto para ver el follaje otoñal (Momiji).
                    • Primavera (Marzo - Mayo): Fresco por la mañana (10°C) y suave al mediodía (18°C a 22°C). Lleva cazadora ligera y calzado muy cómodo para caminar por los templos y cerezos en flor (Sakura).
                    • Calzado: Imprescindible calzado fácil de poner y quitar (slip-on o sin cordones complicados), ya que te descalzarás a menudo en templos, ryokans y restaurantes tradicionales.
                """.trimIndent()
            }

            lower.contains("akihabara") || lower.contains("anime") || lower.contains("manga") || lower.contains("figura") || lower.contains("souvenir") -> {
                """
                    🎮 Akihabara, Anime y Souvenirs en Tokio:
                    • Figuras y Coleccionismo: Tiendas clave como Radio Kaikan (justo en la salida Electric Town), Mandarake Complex (8 plantas de vintage), AmiAmi y Kotobukiya.
                    • Gashapon y Recreativas: Gashapon Department Store en Ikebukuro o Akihabara con miles de máquinas de cápsulas, y salones de recreativas Taito Station y GiGO.
                    • Electrónica y Gadgets: Yodobashi Camera Multimedia Akiba (9 plantas de tecnología, fotografía y relojes con Tax-Free).
                    • Manga y Merchandising: Animate Ikebukuro (la tienda de anime más grande del mundo) o Nakano Broadway para coleccionismo retro.
                """.trimIndent()
            }

            lower.contains("emergencia") || lower.contains("terremoto") || lower.contains("hospital") || lower.contains("110") || lower.contains("119") || lower.contains("seguro") -> {
                """
                    🆘 Teléfonos de Emergencia y Terremotos:
                    • 110: Policía (asistencia de seguridad o pérdida de objetos).
                    • 119: Ambulancia y Bomberos (urgencias médicas).
                    • Línea de Asistencia al Turista JNTO (24/7 en español/inglés): 050-3816-2720.
                    • Terremotos: Japón cuenta con la arquitectura antisísmica más avanzada del mundo. Si suena la alarma J-Alert en tu teléfono, mantén la calma, protégete la cabeza bajo una mesa firme, aléjate de cristales y espera las indicaciones del personal.
                """.trimIndent()
            }

            else -> {
                val stopsCount = currentPlan?.itineraryStops?.size ?: 0
                val daysCount = currentPlan?.itineraryStops?.map { it.dayNumber }?.distinct()?.size ?: 0
                val segmentsCount = currentPlan?.transportSegments?.size ?: 0
                buildString {
                    appendLine("✨ Consulta de Viaje: «$cleanPrompt»")
                    appendLine("• Consejos destacados para tu duda:")
                    appendLine("  - Para transportes urbanos, recuerda recargar tu tarjeta IC (Suica/Pasmo) en efectivo.")
                    appendLine("  - En compras superiores a ¥5,000 en tiendas con distintivo, solicita la exención Tax-Free con tu pasaporte original.")
                    appendLine("  - Conversión de referencia: 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY (¥1,000 ≈ ${String.format(Locale.US, "%.2f EUR", 1000.0 / safeRate)}).")
                    if (stopsCount > 0 || segmentsCount > 0) {
                        appendLine("• Tu itinerario actual tiene $stopsCount paradas organizadas en $daysCount jornada(s).")
                    }
                    append("• Pregúntame sobre cualquier ciudad (Tokio, Kioto, Osaka, Nara), transportes (Shinkansen, metro), comida, etiqueta o normas Onsen.")
                }
            }
        }
    }
}
