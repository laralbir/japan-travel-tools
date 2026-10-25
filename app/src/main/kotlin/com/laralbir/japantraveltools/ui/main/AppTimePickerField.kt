package com.laralbir.japantraveltools.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import java.util.Locale

object TimeSelectorHelper {

    fun parseHourAndMinute(
        timeStr: String,
        defaultHour: Int = 9,
        defaultMinute: Int = 0
    ): Pair<Int, Int> {
        val trimmed = timeStr.trim()
        if (trimmed.isBlank() || !trimmed.contains(':')) {
            return Pair(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
        }
        val parts = trimmed.split(':')
        if (parts.size != 2) {
            return Pair(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
        }
        val hour = parts[0].trim().toIntOrNull()
        val minute = parts[1].trim().toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return Pair(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
        }
        return Pair(hour, minute)
    }

    fun formatTime(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return String.format(Locale.ROOT, "%02d:%02d", h, m)
    }

    fun isValidTime(timeStr: String): Boolean {
        val trimmed = timeStr.trim()
        if (trimmed.isBlank() || !trimmed.contains(':')) return false
        val parts = trimmed.split(':')
        if (parts.size != 2) return false
        val hour = parts[0].trim().toIntOrNull() ?: return false
        val minute = parts[1].trim().toIntOrNull() ?: return false
        return hour in 0..23 && minute in 0..59
    }

    fun formatDisplayTime(timeStr: String, isEs: Boolean): String {
        val trimmed = timeStr.trim()
        if (trimmed.isBlank()) return ""
        if (!isValidTime(trimmed)) return trimmed
        val (h, m) = parseHourAndMinute(trimmed)
        return formatTime(h, m)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 9,
    initialMinute: Int = 0,
    isEs: Boolean = true,
    isOptional: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                    onDismissRequest()
                }
            ) {
                Text(if (isEs) "Aceptar" else "OK")
            }
        },
        dismissButton = {
            Row {
                if (isOptional && onClear != null) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismissRequest()
                        }
                    ) {
                        Text(if (isEs) "Borrar" else "Clear")
                    }
                }
                TextButton(onClick = onDismissRequest) {
                    Text(if (isEs) "Cancelar" else "Cancel")
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@Composable
fun AppTimePickerField(
    label: String,
    timeText: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEs: Boolean = true,
    isOptional: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    val (currentHour, currentMinute) = remember(timeText) {
        TimeSelectorHelper.parseHourAndMinute(timeText, defaultHour = 9, defaultMinute = 0)
    }

    if (showDialog) {
        AppTimePickerDialog(
            onDismissRequest = { showDialog = false },
            onConfirm = { h, m ->
                val formatted = TimeSelectorHelper.formatTime(h, m)
                onTimeSelected(formatted)
            },
            initialHour = currentHour,
            initialMinute = currentMinute,
            isEs = isEs,
            isOptional = isOptional,
            onClear = onClear ?: { onTimeSelected("") }
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = timeText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 14.sp) },
            placeholder = { Text(if (isEs) "Elegir hora 🕒" else "Pick time 🕒") },
            trailingIcon = {
                if (isOptional && timeText.isNotBlank()) {
                    IconButton(onClick = { (onClear ?: { onTimeSelected("") }).invoke() }) {
                        Text("✕", fontSize = 16.sp)
                    }
                } else {
                    IconButton(onClick = { showDialog = true }) {
                        Text("🕒", fontSize = 16.sp)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }
}
