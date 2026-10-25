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
    const val DEFAULT_MODEL_ID = "gemini-2.0-flash"

    fun getAvailableModels(): List<GeminiModelOption> = listOf(
        GeminiModelOption(
            id = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            descriptionEs = "Recomendado. Rápido, multimodal y gratuito",
            descriptionEn = "Recommended. Fast, multimodal and free",
            isRecommended = true
        ),
        GeminiModelOption(
            id = "gemini-2.0-flash-lite",
            displayName = "Gemini 2.0 Flash Lite",
            descriptionEs = "Máxima velocidad y menor latencia",
            descriptionEn = "Maximum speed and lower latency"
        ),
        GeminiModelOption(
            id = "gemini-1.5-pro",
            displayName = "Gemini 1.5 Pro",
            descriptionEs = "Razonamiento profundo para planes extensos",
            descriptionEn = "Deep reasoning for complex plans"
        ),
        GeminiModelOption(
            id = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            descriptionEs = "Generación previa estable",
            descriptionEn = "Stable previous generation"
        ),
        GeminiModelOption(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            descriptionEs = "Última versión 2.5 preview de Google",
            descriptionEn = "Latest 2.5 Flash preview from Google"
        )
    )

    fun resolveModelName(modelId: String?): String {
        val targetId = if (modelId.isNullOrBlank()) DEFAULT_MODEL_ID else modelId.trim()
        return getAvailableModels().firstOrNull { it.id == targetId }?.displayName ?: targetId
    }

    fun buildGenerateContentUrl(apiKey: String, modelId: String? = null): String {
        val resolved = if (modelId.isNullOrBlank()) DEFAULT_MODEL_ID else modelId.trim()
        return "https://generativelanguage.googleapis.com/v1beta/models/$resolved:generateContent?key=${apiKey.trim()}"
    }

    fun evaluateHttpError(httpCode: Int, responseBody: String?, keyPrefix: String?, isEs: Boolean): String {
        val prefix = keyPrefix ?: ""
        return when (httpCode) {
            403 -> {
                if (prefix.isNotBlank() && !prefix.startsWith("AIzaSy")) {
                    if (isEs) {
                        "Error 403 (Permiso Denegado): La clave proporcionada no es una clave de Google AI Studio válida. Las claves de Google empiezan siempre por «AIzaSy...» (la introducida empieza por «$prefix»). Por favor, crea y copia la clave correcta en aistudio.google.com/app/apikey"
                    } else {
                        "Error 403 (Permission Denied): The provided key is not a valid Google AI Studio key. Google keys always start with «AIzaSy...» (the entered one starts with «$prefix»). Please copy the correct key at aistudio.google.com/app/apikey"
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
                    "Error 404 (Modelo no Encontrado): El modelo de Gemini seleccionado no existe o no está habilitado para esta clave."
                } else {
                    "Error 404 (Not Found): Selected Gemini model does not exist or is not enabled for this key."
                }
            }
            429 -> {
                if (isEs) {
                    "Error 429 (Cuota Agotada): Se ha alcanzado temporalmente el límite de solicitudes gratuitas por minuto en Google AI Studio. Espera un momento antes de reintentar."
                } else {
                    "Error 429 (Quota Exceeded): Free tier rate limit exceeded temporarily in Google AI Studio. Please wait a moment before retrying."
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
                    connectTimeout = 6000
                    readTimeout = 8000
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
                val isFormatErr = responseCode == 403 && !cleanKey.startsWith("AIzaSy")
                val explanation = evaluateHttpError(responseCode, responseBody, prefix, isEs)
                GeminiConnectionResult.Error(
                    httpCode = responseCode,
                    message = explanation,
                    isFormatIssue = isFormatErr
                )
            }
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
        return trimmed.startsWith("AIzaSy")
    }

    fun maskApiKey(key: String?): String {
        if (key.isNullOrBlank()) return ""
        val trimmed = key.trim()
        if (trimmed.length <= 8) return "••••••••"
        val prefix = trimmed.take(6)
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
