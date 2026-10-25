package com.laralbir.japantraveltools.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object NumericInputHelper {

    fun sanitizeInteger(raw: String, min: Int, max: Int, default: Int): Int {
        val trimmed = raw.trim()
        val parsed = trimmed.toIntOrNull() ?: return default.coerceIn(min, max)
        return parsed.coerceIn(min, max)
    }

    fun increment(current: Int, step: Int = 1, max: Int = Int.MAX_VALUE): Int {
        return (current + step).coerceAtMost(max)
    }

    fun decrement(current: Int, step: Int = 1, min: Int = 1): Int {
        return (current - step).coerceAtLeast(min)
    }

    fun filterNumeric(input: String): String {
        return input.filter { it.isDigit() }
    }

    fun filterDecimal(input: String): String {
        val builder = StringBuilder()
        var hasDot = false
        var isFirstChar = true
        for (ch in input.trim()) {
            if (ch == '-' && isFirstChar) {
                builder.append(ch)
            } else if (ch.isDigit()) {
                builder.append(ch)
            } else if (ch == '.' && !hasDot) {
                builder.append(ch)
                hasDot = true
            }
            isFirstChar = false
        }
        return builder.toString()
    }
}

@Composable
fun AppNumberStepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    maxValue: Int = 999,
    step: Int = 1
) {
    val currentInt = value.toIntOrNull() ?: minValue

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = NumericInputHelper.filterNumeric(input)
            onValueChange(filtered)
        },
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        leadingIcon = {
            IconButton(
                onClick = {
                    val next = NumericInputHelper.decrement(currentInt, step, minValue)
                    onValueChange(next.toString())
                },
                enabled = currentInt > minValue
            ) {
                Text(
                    text = "－",
                    fontSize = 18.sp,
                    color = if (currentInt > minValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    val next = NumericInputHelper.increment(currentInt, step, maxValue)
                    onValueChange(next.toString())
                },
                enabled = currentInt < maxValue
            ) {
                Text(
                    text = "＋",
                    fontSize = 18.sp,
                    color = if (currentInt < maxValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
