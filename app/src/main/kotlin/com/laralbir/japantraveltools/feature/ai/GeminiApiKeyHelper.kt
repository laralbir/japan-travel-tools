package com.laralbir.japantraveltools.feature.ai

data class ApiKeySetupStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val descriptionEs: String,
    val descriptionEn: String,
    val isRecommended: Boolean = false
)

sealed class GeminiConnectionResult {
    data class Success(val modelUsed: String, val message: String) : GeminiConnectionResult()
    data class Error(val httpCode: Int?, val message: String, val isFormatIssue: Boolean) : GeminiConnectionResult()
}

object GeminiApiKeyHelper {
    const val AI_STUDIO_API_KEY_URL = "https://aistudio.google.com/app/apikey"
    const val DEFAULT_MODEL_ID = "gemini-flash-lite-latest"
    const val FLASH_DEFAULT_MODEL_ID = "gemini-flash-lite-latest"
    const val PRO_DEFAULT_MODEL_ID = "gemini-pro-latest"

    fun isProModel(modelId: String?): Boolean =
        !modelId.isNullOrBlank() && modelId.contains("pro", ignoreCase = true)

    fun getAvailableModels(): List<GeminiModelOption> = listOf(
        GeminiModelOption(
            id = "gemini-flash-lite-latest",
            displayName = "Gemini Flash-Lite",
            descriptionEs = "Recomendado. Respuesta ultra-rápida (<1s), gratuito y sin límites de cuota",
            descriptionEn = "Recommended. Ultra-fast response (<1s), free and without rate limits",
            isRecommended = true
        ),
        GeminiModelOption(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            descriptionEs = "Excelente equilibrio entre velocidad y calidad (gratuito)",
            descriptionEn = "Great balance between speed and quality (free)"
        ),
        GeminiModelOption(
            id = "gemini-flash-latest",
            displayName = "Gemini Flash (Latest)",
            descriptionEs = "Última versión de Flash (alta demanda en servidores de Google)",
            descriptionEn = "Latest Flash version (high demand on Google servers)"
        ),
        GeminiModelOption(
            id = "gemini-pro-latest",
            displayName = "Gemini Pro (Latest)",
            descriptionEs = "Razonamiento avanzado (requiere facturación en Google Cloud; en plan gratis da 429)",
            descriptionEn = "Advanced reasoning (requires Google Cloud billing; returns 429 on free tier)"
        ),
        GeminiModelOption(
            id = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro Preview",
            descriptionEs = "Última preview de Pro (requiere facturación en Google Cloud)",
            descriptionEn = "Latest Pro preview (requires Google Cloud billing)"
        )
    )

    fun getProModels(): List<GeminiModelOption> =
        getAvailableModels().filter { isProModel(it.id) }

    fun getFlashModels(): List<GeminiModelOption> =
        getAvailableModels().filter { !isProModel(it.id) }

    fun resolveModelName(modelId: String?): String {
        val targetId = if (modelId.isNullOrBlank()) DEFAULT_MODEL_ID else modelId.trim()
        return getAvailableModels().firstOrNull { it.id == targetId }?.displayName ?: targetId
    }

    fun buildGenerateContentUrl(apiKey: String, modelId: String? = null): String {
        val resolved = if (modelId.isNullOrBlank()) DEFAULT_MODEL_ID else modelId.trim()
        return "https://generativelanguage.googleapis.com/v1beta/models/$resolved:generateContent?key=${apiKey.trim()}"
    }

    fun evaluateHttpError(
        httpCode: Int,
        responseBody: String?,
        keyPrefix: String?,
        isEs: Boolean,
        modelId: String? = null
    ): String {
        val prefix = keyPrefix ?: ""
        return when (httpCode) {
            403 -> {
                if (prefix.isNotBlank() && !prefix.startsWith("AIzaSy") && !prefix.startsWith("AQ.")) {
                    if (isEs) {
                        "Error 403 (Permiso Denegado): La clave proporcionada no parece ser una clave de Google AI Studio válida. Las claves de Google empiezan por «AIzaSy...» o «AQ...» (la introducida empieza por «$prefix»). Por favor, crea y copia la clave correcta en aistudio.google.com/app/apikey"
                    } else {
                        "Error 403 (Permission Denied): The provided key is not a valid Google AI Studio key. Google keys start with «AIzaSy...» or «AQ...» (the entered one starts with «$prefix»). Please copy the correct key at aistudio.google.com/app/apikey"
                    }
                } else {
                    if (isEs) {
                        "Error 403 (Permiso Denegado): La clave no tiene permisos habilitados en Google AI Studio o ha sido restringida. Verifica tu proyecto en aistudio.google.com."
                    } else {
                        "Error 403 (Permission Denied): The key does not have permissions enabled in Google AI Studio or is restricted. Check your project at aistudio.google.com."
                    }
                }
            }
            400 -> {
                if (isEs) {
                    "Error 400 (Petición Incorrecta): Los parámetros de consulta o el formato de solicitud no fueron aceptados por Google."
                } else {
                    "Error 400 (Bad Request): Query parameters or request format were not accepted by Google."
                }
            }
            404 -> {
                if (isEs) {
                    "Error 404 (Modelo no Disponible): Google ha retirado este modelo de su API. Por favor, selecciona «Gemini Flash-Lite» o «Gemini 3.5 Flash» en Ajustes."
                } else {
                    "Error 404 (Model Unavailable): Google has deprecated this model. Please select «Gemini Flash-Lite» or «Gemini 3.5 Flash» in Settings."
                }
            }
            429 -> {
                val isPro = isProModel(modelId)
                if (isEs) {
                    if (isPro) {
                        "Error 429 (Límite en Plan Gratuito): Google AI Studio restringe los modelos PRO a 0 peticiones/min en cuentas gratuitas (requiere activar facturación en Google Cloud). Para uso gratuito e ilimitado, selecciona «Gemini Flash-Lite» o «Gemini 3.5 Flash»."
                    } else {
                        "Error 429 (Cuota Agotada): Se ha alcanzado temporalmente el límite de solicitudes por minuto en Google AI Studio. Espera un momento antes de reintentar o selecciona «Gemini Flash-Lite»."
                    }
                } else {
                    if (isPro) {
                        "Error 429 (Free Tier Limit): Google AI Studio restricts PRO models to 0 req/min on the free tier (requires Google Cloud billing). For 100% free usage without rate limits, select «Gemini Flash-Lite» or «Gemini 3.5 Flash»."
                    } else {
                        "Error 429 (Quota Exceeded): Rate limit exceeded in Google AI Studio. Please wait a moment before retrying or select «Gemini Flash-Lite»."
                    }
                }
            }
            503 -> {
                if (isEs) {
                    "Error 503 (Servidores de Google Saturados): El modelo de Google seleccionado está experimentando alta demanda. Se recomienda cambiar a «Gemini Flash-Lite», que responde en menos de 1 segundo."
                } else {
                    "Error 503 (Google Overloaded): The selected model is experiencing temporary high demand. Switching to «Gemini Flash-Lite» is recommended for sub-second responses."
                }
            }
            else -> {
                val detail = responseBody?.take(120)?.replace("\n", " ") ?: "Respuesta no satisfactoria"
                if (isEs) "Error HTTP $httpCode: $detail" else "HTTP Error $httpCode: $detail"
            }
        }
    }

    fun testApiKeyConnection(
        apiKey: String,
        modelId: String? = null,
        isEs: Boolean = true,
        networkClient: ((String) -> Pair<Int, String?>)? = null
    ): GeminiConnectionResult {
        val cleanKey = apiKey.trim()
        val resolvedModel = if (modelId.isNullOrBlank()) DEFAULT_MODEL_ID else modelId.trim()
        val prefix = cleanKey.take(6)

        if (cleanKey.isBlank()) {
            return GeminiConnectionResult.Error(
                httpCode = null,
                message = if (isEs) "La clave de API está vacía." else "API key is empty.",
                isFormatIssue = true
            )
        }

        return try {
            val (responseCode, responseBody) = networkClient?.invoke(cleanKey) ?: run {
                val url = java.net.URL(buildGenerateContentUrl(cleanKey, resolvedModel))
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 25000
                    doOutput = true
                }
                val testPayload = """{"contents":[{"parts":[{"text":"ping"}]}]}"""
                connection.outputStream.use { os ->
                    os.write(testPayload.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                val code = connection.responseCode
                val body = if (code in 200..299) {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }
                code to body
            }

            if (responseCode in 200..299) {
                GeminiConnectionResult.Success(
                    modelUsed = resolvedModel,
                    message = if (isEs) {
                        "✅ Conexión con Gemini exitosa (Modelo: $resolvedModel)"
                    } else {
                        "✅ Gemini connection successful (Model: $resolvedModel)"
                    }
                )
            } else {
                val isFormatErr = responseCode == 403 && !cleanKey.startsWith("AIzaSy") && !cleanKey.startsWith("AQ.")
                val explanation = evaluateHttpError(responseCode, responseBody, prefix, isEs, resolvedModel)
                GeminiConnectionResult.Error(
                    httpCode = responseCode,
                    message = explanation,
                    isFormatIssue = isFormatErr
                )
            }
        } catch (e: java.net.SocketTimeoutException) {
            GeminiConnectionResult.Error(
                httpCode = 408,
                message = if (isEs) {
                    "⏱️ Tiempo de espera agotado (Timeout): El modelo $resolvedModel tardó más de 25 segundos en responder. Se recomienda seleccionar «Gemini Flash-Lite», que responde en menos de 1 segundo."
                } else {
                    "⏱️ Request Timeout: Model $resolvedModel took more than 25 seconds to respond. Select «Gemini Flash-Lite» for sub-second responses."
                },
                isFormatIssue = false
            )
        } catch (e: Exception) {
            GeminiConnectionResult.Error(
                httpCode = null,
                message = if (isEs) {
                    "📶 Error de conexión: No se pudo conectar con los servidores de Google (${e.localizedMessage ?: "timeout"})."
                } else {
                    "📶 Connection error: Could not reach Google servers (${e.localizedMessage ?: "timeout"})."
                },
                isFormatIssue = false
            )
        }
    }

    fun isValidApiKeyFormat(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val trimmed = key.trim()
        if (trimmed.length < 20) return false
        return trimmed.startsWith("AIzaSy") || trimmed.startsWith("AQ.")
    }

    fun maskApiKey(key: String?): String {
        if (key.isNullOrBlank()) return ""
        val trimmed = key.trim()
        if (trimmed.length <= 8) return "••••••••"
        val prefix = if (trimmed.startsWith("AIzaSy")) "AIzaSy" else if (trimmed.startsWith("AQ.")) "AQ." else trimmed.take(6)
        val suffix = trimmed.takeLast(4)
        return "$prefix...$suffix"
    }

    fun getSetupGuideSteps(isEs: Boolean): List<ApiKeySetupStep> {
        return if (isEs) {
            listOf(
                ApiKeySetupStep(
                    stepNumber = 1,
                    title = "Abre Google AI Studio",
                    description = "Accede a la consola oficial de Google (aistudio.google.com) usando el botón directo de abajo."
                ),
                ApiKeySetupStep(
                    stepNumber = 2,
                    title = "Inicia sesión con tu cuenta de Google",
                    description = "Usa cualquier cuenta de Gmail o Google. El acceso para uso personal es 100% gratuito y sin costes."
                ),
                ApiKeySetupStep(
                    stepNumber = 3,
                    title = "Crea y copia tu API Key",
                    description = "Pulsa en «Get API key» o «Create API key», selecciona un proyecto y copia la clave generada (empieza por AIzaSy...)."
                ),
                ApiKeySetupStep(
                    stepNumber = 4,
                    title = "Pega y guarda en la app",
                    description = "Vuelve a Japan Travel Tools, pulsa «Pegar desde Portapapeles» y luego «Guardar Clave». ¡Listo para usar Gemini!"
                )
            )
        } else {
            listOf(
                ApiKeySetupStep(
                    stepNumber = 1,
                    title = "Open Google AI Studio",
                    description = "Open Google's official developer console (aistudio.google.com) using the direct button below."
                ),
                ApiKeySetupStep(
                    stepNumber = 2,
                    title = "Sign in with your Google Account",
                    description = "Sign in with any Gmail or Google account. Personal access tier is 100% free with no charges."
                ),
                ApiKeySetupStep(
                    stepNumber = 3,
                    title = "Create and copy your API Key",
                    description = "Click «Get API key» or «Create API key», pick a project, and copy the generated key (starts with AIzaSy...)."
                ),
                ApiKeySetupStep(
                    stepNumber = 4,
                    title = "Paste and save in app",
                    description = "Return to Japan Travel Tools, tap «Paste from Clipboard» and «Save Key». Ready to use Gemini!"
                )
            )
        }
    }
}
