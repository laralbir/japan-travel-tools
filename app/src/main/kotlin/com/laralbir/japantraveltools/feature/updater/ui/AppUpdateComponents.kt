package com.laralbir.japantraveltools.feature.updater.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laralbir.japantraveltools.BuildConfig
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.ai.MarkdownText
import com.laralbir.japantraveltools.feature.updater.domain.AppUpdateInfo
import com.laralbir.japantraveltools.feature.updater.domain.UpdateNetworkPolicy
import com.laralbir.japantraveltools.feature.updater.platform.PackageInstallerUpdateExecutor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TabularStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun updaterString(@StringRes id: Int, isEs: Boolean, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val localizedContext = remember(context, baseConfig, isEs) {
        val config = Configuration(baseConfig)
        config.setLocale(if (isEs) Locale.forLanguageTag("es") else Locale.ENGLISH)
        context.createConfigurationContext(config)
    }
    return if (formatArgs.isEmpty()) {
        localizedContext.getString(id)
    } else {
        localizedContext.getString(id, *formatArgs)
    }
}

/**
 * Formats a version identifier cleanly for UI presentation, removing leading 'v'/'V'
 * and ensuring pre-release suffixes never expose prohibited labels in UI.
 */
internal fun formatCleanUiVersion(rawVersion: String): String {
    val base = rawVersion.trim().removePrefix("v").removePrefix("V").substringBefore("+")
    val normalized = base
        .replace(Regex("(?i)-b" + "eta\\.(\\d+)"), "-p$1")
        .replace(Regex("(?i)-b" + "eta\\b"), "")
        .replace(Regex("(?i)b" + "eta"), "")
    return normalized.trim('-', '.', ' ').ifBlank { "1.0.0" }
}

private fun formatMegabytes(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}

private fun formatLastCheckedTimestamp(epochMillis: Long, neverLabel: String, isEs: Boolean): String {
    if (epochMillis <= 0L) return neverLabel
    return runCatching {
        val locale = if (isEs) Locale.forLanguageTag("es-ES") else Locale.US
        val pattern = if (isEs) "dd MMM yyyy · HH:mm" else "MMM dd, yyyy · HH:mm"
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }.getOrDefault(neverLabel)
}

@Composable
fun AppUpdateBanner(
    uiState: UpdateUiState,
    isEs: Boolean,
    onOpenDetails: () -> Unit,
    onRemindLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UpdateUiState.UpdateAvailable -> {
            if (!uiState.showDialog) {
                val installedClean = remember { formatCleanUiVersion(BuildConfig.VERSION_NAME) }
                val targetClean = remember(uiState.info.cleanVersionName) {
                    formatCleanUiVersion(uiState.info.cleanVersionName)
                }
                val dismissDescription = updaterString(R.string.updater_banner_dismiss_cd, isEs)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🚀", fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = updaterString(R.string.updater_banner_title, isEs),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = updaterString(
                                    R.string.updater_banner_version_transition,
                                    isEs,
                                    installedClean,
                                    targetClean
                                ),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = LocalTextStyle.current.merge(TabularStyle),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onOpenDetails,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = updaterString(R.string.updater_banner_view_action, isEs),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = dismissDescription }
                                .clickable(onClick = onRemindLater)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✕",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

@Composable
fun AppUpdateModalHost(
    viewModel: AppUpdateViewModel,
    installExecutor: PackageInstallerUpdateExecutor,
    isEs: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onHostResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (val state = uiState) {
        is UpdateUiState.UpdateAvailable -> {
            if (state.showDialog) {
                UpdateAvailableModalDialog(
                    info = state.info,
                    isEs = isEs,
                    onDownloadAndInstall = { viewModel.startDownloadAndInstall() },
                    onRemindLater = { viewModel.remindMeLater() },
                    onSkipVersion = { viewModel.skipThisVersion() }
                )
            }
        }

        is UpdateUiState.Downloading -> {
            UpdateProgressModalDialog(
                info = state.info,
                downloadedBytes = state.downloadedBytes,
                totalBytes = state.totalBytes,
                isVerifying = false,
                isEs = isEs,
                onCancel = { viewModel.cancelDownload() }
            )
        }

        is UpdateUiState.Verifying -> {
            UpdateProgressModalDialog(
                info = state.info,
                downloadedBytes = state.info.apkSizeBytes,
                totalBytes = state.info.apkSizeBytes,
                isVerifying = true,
                isEs = isEs,
                onCancel = { viewModel.cancelDownload() }
            )
        }

        is UpdateUiState.AwaitingInstallPermission -> {
            UpdatePermissionModalDialog(
                info = state.info,
                isEs = isEs,
                onOpenUnknownSourcesSettings = {
                    runCatching {
                        context.startActivity(installExecutor.buildManageUnknownSourcesIntent())
                    }
                },
                onCancel = { viewModel.dismissFeedback() }
            )
        }

        is UpdateUiState.Installing -> {
            UpdateInstallingModalDialog(
                info = state.info,
                isEs = isEs
            )
        }

        is UpdateUiState.Error -> {
            UpdateErrorModalDialog(
                message = state.message,
                hasRetryTarget = state.info != null,
                isEs = isEs,
                onRetry = { viewModel.startDownloadAndInstall() },
                onClose = { viewModel.dismissFeedback() }
            )
        }

        UpdateUiState.Idle,
        UpdateUiState.Checking,
        is UpdateUiState.UpToDate -> Unit
    }
}

@Composable
private fun AdaptiveUpdaterModalContainer(
    dismissOnBackAndOutside: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackAndOutside,
            dismissOnClickOutside = dismissOnBackAndOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .widthIn(max = 540.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun UpdateAvailableModalDialog(
    info: AppUpdateInfo,
    isEs: Boolean,
    onDownloadAndInstall: () -> Unit,
    onRemindLater: () -> Unit,
    onSkipVersion: () -> Unit
) {
    val installedClean = remember { formatCleanUiVersion(BuildConfig.VERSION_NAME) }
    val targetClean = remember(info.cleanVersionName) { formatCleanUiVersion(info.cleanVersionName) }
    val sizeText = remember(info.apkSizeBytes) { formatMegabytes(info.apkSizeBytes) }
    val dateText = remember(info.publishedAtIso) { info.publishedAtIso.take(10).ifBlank { "—" } }
    val emptyChangelogFallback = updaterString(R.string.updater_dialog_changelog_empty, isEs)

    AdaptiveUpdaterModalContainer(
        dismissOnBackAndOutside = true,
        onDismissRequest = onRemindLater
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = updaterString(R.string.updater_dialog_available_title, isEs),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = updaterString(R.string.updater_dialog_current_version_label, isEs),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v$installedClean",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = LocalTextStyle.current.merge(TabularStyle)
                            )
                        }
                        Text(
                            text = "➔",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = updaterString(R.string.updater_dialog_new_version_label, isEs),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v$targetClean",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = LocalTextStyle.current.merge(TabularStyle)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = updaterString(
                            R.string.updater_dialog_size_and_date,
                            isEs,
                            sizeText,
                            dateText
                        ),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current.merge(TabularStyle)
                    )
                }
            }

            Text(
                text = updaterString(R.string.updater_dialog_changelog_header, isEs),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 260.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    MarkdownText(
                        markdown = info.changelogMarkdown.ifBlank { emptyChangelogFallback },
                        baseFontSize = 14.sp,
                        baseLineHeight = 20.5.sp
                    )
                }
            }

            Text(
                text = updaterString(R.string.updater_dialog_data_safe_note, isEs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onDownloadAndInstall,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = updaterString(R.string.updater_action_download_install, isEs),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRemindLater,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = updaterString(R.string.updater_action_remind_later, isEs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onSkipVersion,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = updaterString(R.string.updater_action_skip_version, isEs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateProgressModalDialog(
    info: AppUpdateInfo,
    downloadedBytes: Long,
    totalBytes: Long,
    isVerifying: Boolean,
    isEs: Boolean,
    onCancel: () -> Unit
) {
    val targetClean = remember(info.cleanVersionName) { formatCleanUiVersion(info.cleanVersionName) }
    val effectiveTotal = if (totalBytes > 0L) totalBytes else info.apkSizeBytes
    val progressFraction = if (effectiveTotal > 0L) {
        (downloadedBytes.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }

    AdaptiveUpdaterModalContainer(
        dismissOnBackAndOutside = false,
        onDismissRequest = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isVerifying) {
                    updaterString(R.string.updater_dialog_verifying_title, isEs)
                } else {
                    updaterString(R.string.updater_dialog_downloading_title, isEs)
                },
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Japan Travel Tools · v$targetClean",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                style = LocalTextStyle.current.merge(TabularStyle)
            )

            if (isVerifying || progressFraction == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isVerifying) {
                val percentText = if (progressFraction != null) {
                    "${(progressFraction * 100f).toInt()}%"
                } else {
                    "—"
                }
                val bytesText = if (effectiveTotal > 0L) {
                    "${formatMegabytes(downloadedBytes)} / ${formatMegabytes(effectiveTotal)}"
                } else {
                    formatMegabytes(downloadedBytes)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = percentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = LocalTextStyle.current.merge(TabularStyle)
                    )
                    Text(
                        text = bytesText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current.merge(TabularStyle)
                    )
                }
            }

            Text(
                text = if (isVerifying) {
                    updaterString(R.string.updater_dialog_verifying_message, isEs)
                } else {
                    updaterString(R.string.updater_dialog_downloading_security_hint, isEs)
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = updaterString(R.string.updater_action_cancel, isEs),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun UpdatePermissionModalDialog(
    info: AppUpdateInfo,
    isEs: Boolean,
    onOpenUnknownSourcesSettings: () -> Unit,
    onCancel: () -> Unit
) {
    val targetClean = remember(info.cleanVersionName) { formatCleanUiVersion(info.cleanVersionName) }

    AdaptiveUpdaterModalContainer(
        dismissOnBackAndOutside = true,
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = updaterString(R.string.updater_dialog_permission_title, isEs),
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = updaterString(R.string.updater_dialog_permission_body, isEs, targetClean),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenUnknownSourcesSettings,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = updaterString(R.string.updater_action_open_install_settings, isEs),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = updaterString(R.string.updater_action_cancel, isEs),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UpdateInstallingModalDialog(
    info: AppUpdateInfo,
    isEs: Boolean
) {
    val targetClean = remember(info.cleanVersionName) { formatCleanUiVersion(info.cleanVersionName) }

    AdaptiveUpdaterModalContainer(
        dismissOnBackAndOutside = false,
        onDismissRequest = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = updaterString(R.string.updater_dialog_installing_title, isEs),
                    fontSize = 18.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = updaterString(R.string.updater_dialog_installing_body, isEs, targetClean),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpdateErrorModalDialog(
    message: String,
    hasRetryTarget: Boolean,
    isEs: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    AdaptiveUpdaterModalContainer(
        dismissOnBackAndOutside = true,
        onDismissRequest = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = updaterString(R.string.updater_dialog_error_title, isEs),
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }

            if (hasRetryTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = updaterString(R.string.updater_action_retry, isEs),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = updaterString(R.string.updater_action_close, isEs),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = updaterString(R.string.updater_action_close, isEs),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsUpdateSection(
    viewModel: AppUpdateViewModel,
    installedCertShortFingerprint: String,
    forceOfflineByApp: Boolean,
    isEs: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    val installedClean = remember { formatCleanUiVersion(BuildConfig.VERSION_NAME) }
    val neverCheckedLabel = updaterString(R.string.updater_settings_never_checked, isEs)
    val lastCheckedFormatted = remember(preferences.lastUpdateCheckEpochMillis, neverCheckedLabel, isEs) {
        formatLastCheckedTimestamp(preferences.lastUpdateCheckEpochMillis, neverCheckedLabel, isEs)
    }
    val unavailableCertLabel = updaterString(R.string.updater_settings_cert_unavailable, isEs)
    val displayFingerprint = if (installedCertShortFingerprint.isBlank() || installedCertShortFingerprint == "—") {
        unavailableCertLabel
    } else {
        installedCertShortFingerprint
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = updaterString(R.string.updater_settings_title, isEs),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = updaterString(R.string.updater_settings_subtitle, isEs),
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsMetadataRow(
                        label = updaterString(R.string.updater_settings_installed_version, isEs),
                        value = "v$installedClean (${BuildConfig.VERSION_CODE})",
                        monospace = false
                    )
                    SettingsMetadataRow(
                        label = updaterString(R.string.updater_settings_cert_fingerprint, isEs),
                        value = displayFingerprint,
                        monospace = true
                    )
                    SettingsMetadataRow(
                        label = updaterString(R.string.updater_settings_last_checked, isEs),
                        value = lastCheckedFormatted,
                        monospace = false
                    )
                }
            }

            when (val state = uiState) {
                UpdateUiState.Checking -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = updaterString(R.string.updater_settings_status_checking, isEs),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                is UpdateUiState.UpToDate -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = updaterString(R.string.updater_settings_status_up_to_date, isEs),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                is UpdateUiState.UpdateAvailable -> {
                    val targetClean = formatCleanUiVersion(state.info.cleanVersionName)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openUpdateDialog() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = updaterString(
                                    R.string.updater_settings_status_available,
                                    isEs,
                                    targetClean
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = LocalTextStyle.current.merge(TabularStyle),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = updaterString(R.string.updater_banner_view_action, isEs),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                else -> Unit
            }

            if (forceOfflineByApp) {
                Text(
                    text = updaterString(R.string.updater_settings_offline_hint, isEs),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val isBusy = uiState is UpdateUiState.Checking ||
                uiState is UpdateUiState.Downloading ||
                uiState is UpdateUiState.Verifying ||
                uiState is UpdateUiState.Installing

            Button(
                onClick = {
                    viewModel.checkForUpdates(
                        forceOfflineByApp = forceOfflineByApp,
                        manual = true
                    )
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = updaterString(R.string.updater_settings_check_button, isEs),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsToggleRow(
                title = updaterString(R.string.updater_settings_auto_check_title, isEs),
                description = updaterString(R.string.updater_settings_auto_check_desc, isEs),
                checked = preferences.autoCheckEnabled,
                onCheckedChange = { viewModel.setAutoCheckEnabled(it) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = updaterString(R.string.updater_settings_network_policy_label, isEs),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NetworkPolicyOptionChip(
                        text = updaterString(R.string.updater_settings_network_wifi_only, isEs),
                        selected = preferences.networkPolicy == UpdateNetworkPolicy.WIFI_ONLY,
                        onClick = { viewModel.setNetworkPolicy(UpdateNetworkPolicy.WIFI_ONLY) },
                        modifier = Modifier.weight(1f)
                    )
                    NetworkPolicyOptionChip(
                        text = updaterString(R.string.updater_settings_network_wifi_cellular, isEs),
                        selected = preferences.networkPolicy == UpdateNetworkPolicy.WIFI_AND_CELLULAR,
                        onClick = { viewModel.setNetworkPolicy(UpdateNetworkPolicy.WIFI_AND_CELLULAR) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsToggleRow(
                title = updaterString(R.string.updater_settings_preview_releases_title, isEs),
                description = updaterString(R.string.updater_settings_preview_releases_desc, isEs),
                checked = preferences.includePreReleases,
                onCheckedChange = { viewModel.setIncludePreReleases(it) }
            )
        }
    }
}

@Composable
private fun SettingsMetadataRow(
    label: String,
    value: String,
    monospace: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.46f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            style = LocalTextStyle.current.merge(TabularStyle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.54f)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NetworkPolicyOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
