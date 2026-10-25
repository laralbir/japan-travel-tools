package com.laralbir.japantraveltools.feature.ai

data class ApiKeySetupStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

object GeminiApiKeyHelper {
    const val AI_STUDIO_API_KEY_URL = "https://aistudio.google.com/app/apikey"

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
                    description = "Vuelve a Japan Travel Tools, pulsa «Pegar desde Portapapeles» y luego «Guardar Clave». ¡Listo para usar Gemini 2.0 Flash!"
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
                    description = "Return to Japan Travel Tools, tap «Paste from Clipboard» and «Save Key». Ready to use Gemini 2.0 Flash!"
                )
            )
        }
    }
}
