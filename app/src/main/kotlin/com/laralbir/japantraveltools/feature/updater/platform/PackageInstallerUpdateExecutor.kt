package com.laralbir.japantraveltools.feature.updater.platform

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface InstallExecutionResult {
    data object SessionCommitted : InstallExecutionResult
    data object PermissionRequired : InstallExecutionResult
    data class Failed(val reason: String) : InstallExecutionResult
}

sealed interface InstallStatusEvent {
    data class Succeeded(val sessionId: Int) : InstallStatusEvent
    data class Cancelled(val sessionId: Int) : InstallStatusEvent
    data class Failed(val sessionId: Int, val status: Int, val message: String) : InstallStatusEvent
}

object InstallStatusEventBus {
    private val _events = MutableSharedFlow<InstallStatusEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<InstallStatusEvent> = _events.asSharedFlow()

    fun emit(event: InstallStatusEvent) {
        _events.tryEmit(event)
    }
}

interface ApkInstallExecutor {
    fun canInstallPackages(): Boolean
    fun buildManageUnknownSourcesIntent(): Intent
    fun installVerifiedApk(apkFile: File): InstallExecutionResult
}

class PackageInstallerUpdateExecutor(
    private val context: Context
) : ApkInstallExecutor {

    override fun canInstallPackages(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    override fun buildManageUnknownSourcesIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun installVerifiedApk(apkFile: File): InstallExecutionResult {
        if (!canInstallPackages()) {
            return InstallExecutionResult.PermissionRequired
        }
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) {
            return InstallExecutionResult.Failed("Verified APK file is missing or empty.")
        }

        val packageInstaller = context.packageManager.packageInstaller
        var createdSessionId: Int? = null

        return try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(context.packageName)
                setSize(apkFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setRequestUpdateOwnership(true)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            createdSessionId = sessionId

            packageInstaller.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite(SESSION_SPLIT_NAME, 0L, apkFile.length()).use { output ->
                        input.copyTo(output, bufferSize = STREAM_BUFFER_SIZE_BYTES)
                        session.fsync(output)
                    }
                }

                val statusIntent = Intent(context, UpdateInstallStatusReceiver::class.java).apply {
                    action = UpdateInstallStatusReceiver.ACTION_INSTALL_STATUS
                    setPackage(context.packageName)
                }
                val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    statusIntent,
                    pendingFlags
                )
                session.commit(pendingIntent.intentSender)
            }
            InstallExecutionResult.SessionCommitted
        } catch (e: Exception) {
            createdSessionId?.let { id ->
                runCatching { packageInstaller.abandonSession(id) }
            }
            InstallExecutionResult.Failed(e.message ?: e::class.java.simpleName)
        }
    }

    private companion object {
        const val SESSION_SPLIT_NAME = "jtt_update_base.apk"
        const val STREAM_BUFFER_SIZE_BYTES = 64 * 1024
    }
}

class UpdateInstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.action != ACTION_INSTALL_STATUS) {
            return
        }

        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extractConfirmationIntent(intent)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching {
                        context.startActivity(confirmIntent)
                    }.onFailure { error ->
                        InstallStatusEventBus.emit(
                            InstallStatusEvent.Failed(
                                sessionId = sessionId,
                                status = status,
                                message = error.message ?: "Unable to launch installer confirmation prompt."
                            )
                        )
                    }
                } else {
                    InstallStatusEventBus.emit(
                        InstallStatusEvent.Failed(
                            sessionId = sessionId,
                            status = status,
                            message = "Missing confirmation intent from PackageInstaller."
                        )
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                InstallStatusEventBus.emit(InstallStatusEvent.Succeeded(sessionId = sessionId))
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                InstallStatusEventBus.emit(InstallStatusEvent.Cancelled(sessionId = sessionId))
            }

            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                InstallStatusEventBus.emit(
                    InstallStatusEvent.Failed(
                        sessionId = sessionId,
                        status = status,
                        message = buildFailureMessage(status, rawMessage)
                    )
                )
            }

            else -> {
                InstallStatusEventBus.emit(
                    InstallStatusEvent.Failed(
                        sessionId = sessionId,
                        status = status,
                        message = buildFailureMessage(status, rawMessage)
                    )
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractConfirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
        }

    private fun buildFailureMessage(status: Int, rawMessage: String?): String {
        val summary = when (status) {
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                "Installation blocked by device security policy."
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                "Conflicting package signature or existing installation detected."
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                "Update APK is incompatible with this device or Android version."
            PackageInstaller.STATUS_FAILURE_INVALID ->
                "Downloaded APK package is invalid or corrupted."
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                "Insufficient device storage space to complete the update."
            else ->
                "Package installation failed (code $status)."
        }
        return if (!rawMessage.isNullOrBlank()) {
            "$summary ($rawMessage)"
        } else {
            summary
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS =
            "com.laralbir.japantraveltools.action.UPDATE_INSTALL_STATUS"
    }
}
