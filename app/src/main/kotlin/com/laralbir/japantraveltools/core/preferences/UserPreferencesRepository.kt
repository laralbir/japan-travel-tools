package com.laralbir.japantraveltools.core.preferences

import android.content.Context
import com.laralbir.japantraveltools.core.i18n.AppLanguagePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguagePreference = AppLanguagePreference.SYSTEM,
    val originZoneId: String = "Europe/Madrid",
    val use24HourFormat: Boolean = true,
    val baseCurrency: String = "EUR",
    val targetCurrency: String = "JPY",
    val customExchangeRate: Double? = null,
    val biometricLockEnabled: Boolean = false,
    val forceOfflineMode: Boolean = false,
    val syncOnlyOnWifi: Boolean = true,
    val googleAccountEmail: String? = null,
    val lastCloudSyncEpochMillis: Long? = null,
    val geminiApiKey: String? = null,
    val geminiModelId: String = "gemini-2.0-flash"
)

interface KeyValueStore {
    fun getString(key: String, default: String?): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, Any?>()

    override fun getString(key: String, default: String?): String? =
        if (map.containsKey(key)) map[key] as? String else default

    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        (map[key] as? Boolean) ?: default

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    override fun getLong(key: String, default: Long): Long =
        (map[key] as? Long) ?: default

    override fun putLong(key: String, value: Long) {
        map[key] = value
    }
}

class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(
        "japan_travel_tools_prefs",
        Context.MODE_PRIVATE
    )

    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
}

class DefaultUserPreferencesRepository(
    private val store: KeyValueStore
) {
    private val _preferencesFlow = MutableStateFlow(loadFromStore())
    val preferencesFlow: Flow<UserPreferences> = _preferencesFlow.asStateFlow()

    val current: UserPreferences
        get() = _preferencesFlow.value

    private fun loadFromStore(): UserPreferences {
        val themeStr = store.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val langStr = store.getString(KEY_LANGUAGE, AppLanguagePreference.SYSTEM.name) ?: AppLanguagePreference.SYSTEM.name
        val originZone = store.getString(KEY_ORIGIN_ZONE, "Europe/Madrid") ?: "Europe/Madrid"
        val use24h = store.getBoolean(KEY_USE_24H, true)
        val baseCurr = store.getString(KEY_BASE_CURRENCY, "EUR") ?: "EUR"
        val targetCurr = store.getString(KEY_TARGET_CURRENCY, "JPY") ?: "JPY"
        val customRateStr = store.getString(KEY_CUSTOM_RATE, null)
        val biometric = store.getBoolean(KEY_BIOMETRIC, false)
        val forceOffline = store.getBoolean(KEY_FORCE_OFFLINE, false)
        val syncWifi = store.getBoolean(KEY_SYNC_WIFI_ONLY, true)
        val email = store.getString(KEY_GOOGLE_EMAIL, null)
        val syncTime = store.getLong(KEY_LAST_SYNC, -1L).takeIf { it > 0L }
        val apiKey = store.getString(KEY_GEMINI_API_KEY, null)
        val modelId = store.getString(KEY_GEMINI_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash"

        return UserPreferences(
            themeMode = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.SYSTEM),
            language = runCatching { AppLanguagePreference.valueOf(langStr) }.getOrDefault(AppLanguagePreference.SYSTEM),
            originZoneId = originZone,
            use24HourFormat = use24h,
            baseCurrency = baseCurr,
            targetCurrency = targetCurr,
            customExchangeRate = customRateStr?.toDoubleOrNull(),
            biometricLockEnabled = biometric,
            forceOfflineMode = forceOffline,
            syncOnlyOnWifi = syncWifi,
            googleAccountEmail = email,
            lastCloudSyncEpochMillis = syncTime,
            geminiApiKey = apiKey,
            geminiModelId = modelId
        )
    }

    private fun refresh() {
        _preferencesFlow.value = loadFromStore()
    }

    fun setThemeMode(mode: AppThemeMode) {
        store.putString(KEY_THEME, mode.name)
        refresh()
    }

    fun setLanguage(language: AppLanguagePreference) {
        store.putString(KEY_LANGUAGE, language.name)
        refresh()
    }

    fun setOriginZoneId(zoneId: String) {
        store.putString(KEY_ORIGIN_ZONE, zoneId)
        refresh()
    }

    fun setCurrencyPair(base: String, target: String) {
        store.putString(KEY_BASE_CURRENCY, base)
        store.putString(KEY_TARGET_CURRENCY, target)
        refresh()
    }

    fun setCustomExchangeRate(rate: Double?) {
        store.putString(KEY_CUSTOM_RATE, rate?.toString())
        refresh()
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        store.putBoolean(KEY_BIOMETRIC, enabled)
        refresh()
    }

    fun setForceOfflineMode(enabled: Boolean) {
        store.putBoolean(KEY_FORCE_OFFLINE, enabled)
        refresh()
    }

    fun setSyncOnlyOnWifi(enabled: Boolean) {
        store.putBoolean(KEY_SYNC_WIFI_ONLY, enabled)
        refresh()
    }

    fun setGoogleAccountSync(email: String?, lastSyncedMillis: Long?) {
        store.putString(KEY_GOOGLE_EMAIL, email)
        store.putLong(KEY_LAST_SYNC, lastSyncedMillis ?: -1L)
        refresh()
    }

    fun setGeminiApiKey(apiKey: String?) {
        store.putString(KEY_GEMINI_API_KEY, apiKey?.takeIf { it.isNotBlank() })
        refresh()
    }

    fun setGeminiModelId(modelId: String) {
        val safeModel = modelId.trim().ifBlank { "gemini-2.0-flash" }
        store.putString(KEY_GEMINI_MODEL, safeModel)
        refresh()
    }

    companion object {
        private const val KEY_THEME = "pref_theme_mode"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_ORIGIN_ZONE = "pref_origin_zone_id"
        private const val KEY_USE_24H = "pref_use_24h"
        private const val KEY_BASE_CURRENCY = "pref_base_currency"
        private const val KEY_TARGET_CURRENCY = "pref_target_currency"
        private const val KEY_CUSTOM_RATE = "pref_custom_exchange_rate"
        private const val KEY_BIOMETRIC = "pref_biometric_lock"
        private const val KEY_FORCE_OFFLINE = "pref_force_offline"
        private const val KEY_SYNC_WIFI_ONLY = "pref_sync_wifi_only"
        private const val KEY_GOOGLE_EMAIL = "pref_google_account_email"
        private const val KEY_LAST_SYNC = "pref_last_cloud_sync_millis"
        private const val KEY_GEMINI_API_KEY = "pref_gemini_api_key"
        private const val KEY_GEMINI_MODEL = "pref_gemini_model_id"
    }
}

