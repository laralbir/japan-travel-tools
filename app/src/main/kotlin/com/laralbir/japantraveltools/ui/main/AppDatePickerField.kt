package com.laralbir.japantraveltools.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateSelectorHelper {

    fun parseIsoDateToUtcMillis(dateIso: String): Long {
        return runCatching {
            val localDate = LocalDate.parse(dateIso.trim())
            localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull() ?: LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun formatUtcMillisToIsoDate(millis: Long): String {
        return runCatching {
            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
        }.getOrNull() ?: LocalDate.now(ZoneOffset.UTC).toString()
    }

    fun formatDisplayDate(dateIso: String, isEs: Boolean): String {
        return runCatching {
            val localDate = LocalDate.parse(dateIso.trim())
            val locale = if (isEs) Locale.forLanguageTag("es-ES") else Locale.ENGLISH
            val pattern = if (isEs) "d 'de' MMM 'de' yyyy" else "MMM d, yyyy"
            val formatted = localDate.format(DateTimeFormatter.ofPattern(pattern, locale))
            "$formatted ($dateIso)"
        }.getOrNull() ?: dateIso.ifBlank { if (isEs) "Seleccionar fecha" else "Select date" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    label: String,
    dateIso: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEs: Boolean = true
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val displayValue = remember(dateIso, isEs) {
        if (dateIso.isBlank()) {
            ""
        } else {
            DateSelectorHelper.formatDisplayDate(dateIso, isEs)
        }
    }

    val initialMillis = remember(dateIso) {
        DateSelectorHelper.parseIsoDateToUtcMillis(dateIso)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            val isoDate = DateSelectorHelper.formatUtcMillisToIsoDate(selected)
                            onDateSelected(isoDate)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(if (isEs) "Aceptar" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(if (isEs) "Cancelar" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 14.sp) },
            placeholder = { Text(if (isEs) "Elegir fecha 📅" else "Pick date 📅") },
            trailingIcon = {
                IconButton(onClick = { showDatePickerDialog = true }) {
                    Text("📅", fontSize = 18.sp)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePickerDialog = true }
        )
    }
}
