package com.laralbir.japantraveltools.feature.updater.data

import com.laralbir.japantraveltools.core.preferences.KeyValueStore
import com.laralbir.japantraveltools.feature.updater.domain.UpdateNetworkPolicy
import com.laralbir.japantraveltools.feature.updater.domain.UpdatePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpdatePreferencesDataStore(
    private val store: KeyValueStore,
    private val defaultIncludePreReleases: Boolean = false
) {
    private val _preferencesFlow = MutableStateFlow(loadFromStore())
    val preferencesFlow: StateFlow<UpdatePreferences> = _preferencesFlow.asStateFlow()

    val current: UpdatePreferences
        get() = _preferencesFlow.value

    private fun loadFromStore(): UpdatePreferences {
        val autoCheck = store.getBoolean(KEY_AUTO_CHECK_ENABLED, true)
        val policyRaw = store.getString(KEY_NETWORK_POLICY, UpdateNetworkPolicy.WIFI_ONLY.name)
            ?: UpdateNetworkPolicy.WIFI_ONLY.name
        val policy = runCatching { UpdateNetworkPolicy.valueOf(policyRaw) }
            .getOrDefault(UpdateNetworkPolicy.WIFI_ONLY)
        val includePreReleases = store.getBoolean(KEY_INCLUDE_PRERELEASES, defaultIncludePreReleases)
        val lastCheckMillis = store.getLong(KEY_LAST_CHECK_MILLIS, 0L).coerceAtLeast(0L)
        val dismissedTag = store.getString(KEY_DISMISSED_VERSION_TAG, null)?.takeIf { it.isNotBlank() }
        val lastEtag = store.getString(KEY_LAST_ETAG, null)?.takeIf { it.isNotBlank() }
        val cachedJson = store.getString(KEY_CACHED_RELEASE_JSON, null)?.takeIf { it.isNotBlank() }
        val pendingApkPath = store.getString(KEY_PENDING_VERIFIED_APK_PATH, null)?.takeIf { it.isNotBlank() }
        val lastKnownVersionCode = store.getLong(KEY_LAST_INSTALLED_VERSION_CODE, 0L).coerceAtLeast(0L)

        return UpdatePreferences(
            autoCheckEnabled = autoCheck,
            networkPolicy = policy,
            includePreReleases = includePreReleases,
            lastUpdateCheckEpochMillis = lastCheckMillis,
            dismissedVersionTag = dismissedTag,
            lastEtag = lastEtag,
            cachedReleaseJson = cachedJson,
            pendingVerifiedApkPath = pendingApkPath,
            lastKnownInstalledVersionCode = lastKnownVersionCode
        )
    }

    private fun refresh() {
        _preferencesFlow.value = loadFromStore()
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        store.putBoolean(KEY_AUTO_CHECK_ENABLED, enabled)
        refresh()
    }

    fun setNetworkPolicy(policy: UpdateNetworkPolicy) {
        store.putString(KEY_NETWORK_POLICY, policy.name)
        refresh()
    }

    fun setIncludePreReleases(include: Boolean) {
        val previous = current.includePreReleases
        store.putBoolean(KEY_INCLUDE_PRERELEASES, include)
        if (previous != include) {
            store.putString(KEY_LAST_ETAG, null)
            store.putString(KEY_CACHED_RELEASE_JSON, null)
        }
        refresh()
    }

    fun setDismissedVersionTag(tag: String?) {
        store.putString(KEY_DISMISSED_VERSION_TAG, tag?.trim()?.takeIf { it.isNotEmpty() })
        refresh()
    }

    fun recordSuccessfulCheck(
        epochMillis: Long,
        etag: String?,
        cachedJson: String?
    ) {
        store.putLong(KEY_LAST_CHECK_MILLIS, epochMillis.coerceAtLeast(0L))
        store.putString(KEY_LAST_ETAG, etag?.trim()?.takeIf { it.isNotEmpty() })
        store.putString(KEY_CACHED_RELEASE_JSON, cachedJson?.takeIf { it.isNotBlank() })
        refresh()
    }

    fun recordCheckTimestampOnly(epochMillis: Long) {
        store.putLong(KEY_LAST_CHECK_MILLIS, epochMillis.coerceAtLeast(0L))
        refresh()
    }

    fun setPendingVerifiedApkPath(path: String?) {
        store.putString(KEY_PENDING_VERIFIED_APK_PATH, path?.trim()?.takeIf { it.isNotEmpty() })
        refresh()
    }

    fun setLastKnownInstalledVersionCode(versionCode: Long) {
        store.putLong(KEY_LAST_INSTALLED_VERSION_CODE, versionCode.coerceAtLeast(0L))
        refresh()
    }

    companion object {
        const val KEY_AUTO_CHECK_ENABLED = "pref_update_auto_check_enabled"
        const val KEY_NETWORK_POLICY = "pref_update_network_policy"
        const val KEY_INCLUDE_PRERELEASES = "pref_update_include_prereleases"
        const val KEY_LAST_CHECK_MILLIS = "pref_update_last_check_millis"
        const val KEY_DISMISSED_VERSION_TAG = "pref_update_dismissed_version_tag"
        const val KEY_LAST_ETAG = "pref_update_last_etag"
        const val KEY_CACHED_RELEASE_JSON = "pref_update_cached_release_json"
        const val KEY_PENDING_VERIFIED_APK_PATH = "pref_update_pending_verified_apk_path"
        const val KEY_LAST_INSTALLED_VERSION_CODE = "pref_update_last_installed_version_code"
    }
}
