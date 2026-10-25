package com.laralbir.japantraveltools.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConfirmationDialogState(
    val title: String,
    val message: String,
    val confirmButtonText: String,
    val dismissButtonText: String,
    val isDestructive: Boolean = false,
    val requiresDoubleCheck: Boolean = false,
    val doubleCheckPrompt: String? = null,
    val onConfirm: () -> Unit
)

@Composable
fun AppConfirmationDialog(
    state: ConfirmationDialogState?,
    onDismiss: () -> Unit
) {
    if (state == null) return

    var isDoubleChecked by remember(state) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.requiresDoubleCheck) {
                    Text("🚨", fontSize = 20.sp)
                }
                Text(
                    text = state.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = state.message, fontSize = 14.sp)
                if (state.requiresDoubleCheck) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDoubleChecked = !isDoubleChecked }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Switch(
                                checked = isDoubleChecked,
                                onCheckedChange = { isDoubleChecked = it }
                            )
                            Text(
                                text = state.doubleCheckPrompt ?: "Confirmo la eliminación definitiva.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm()
                    onDismiss()
                },
                enabled = !state.requiresDoubleCheck || isDoubleChecked,
                colors = if (state.isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.4f)
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(state.confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(state.dismissButtonText)
            }
        }
    )
}

object ConfirmationDialogBuilder {

    fun buildDeleteConfirmation(
        itemName: String,
        itemTypeEs: String,
        itemTypeEn: String,
        isEs: Boolean,
        onConfirm: () -> Unit
    ): ConfirmationDialogState {
        val title = if (isEs) "¿Eliminar $itemTypeEs?" else "Delete $itemTypeEn?"
        val message = if (isEs) {
            "Esta acción no se puede deshacer. ¿Seguro que deseas eliminar «$itemName»?"
        } else {
            "This action cannot be undone. Are you sure you want to delete \"$itemName\"?"
        }
        return ConfirmationDialogState(
            title = title,
            message = message,
            confirmButtonText = if (isEs) "🗑️ Eliminar" else "🗑️ Delete",
            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
            isDestructive = true,
            requiresDoubleCheck = false,
            onConfirm = onConfirm
        )
    }

    fun buildSensitiveDeleteConfirmation(
        itemName: String,
        itemTypeEs: String,
        itemTypeEn: String,
        isEs: Boolean,
        onConfirm: () -> Unit
    ): ConfirmationDialogState {
        val title = if (isEs) "🚨 ¿Eliminar $itemTypeEs?" else "🚨 Delete $itemTypeEn?"
        val message = if (isEs) {
            "Estás a punto de eliminar «$itemName». Al ser un elemento sensible, se requiere una doble confirmación de seguridad para evitar accidentes irreversibles."
        } else {
            "You are about to delete \"$itemName\". As this is a sensitive item, a safety double check is required to prevent accidental permanent deletion."
        }
        val doubleCheckPrompt = if (isEs) {
            "🚨 DOBLE COMPROBACIÓN: Confirmo que deseo eliminar «$itemName» y entiendo que esta acción es 100% irreversible."
        } else {
            "🚨 DOUBLE CHECK: I confirm that I want to delete \"$itemName\" and understand this action is 100% irreversible."
        }
        return ConfirmationDialogState(
            title = title,
            message = message,
            confirmButtonText = if (isEs) "🗑️ Eliminar Definitivamente" else "🗑️ Delete Permanently",
            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
            isDestructive = true,
            requiresDoubleCheck = true,
            doubleCheckPrompt = doubleCheckPrompt,
            onConfirm = onConfirm
        )
    }

    fun buildEditConfirmation(
        itemName: String,
        isEs: Boolean,
        onConfirm: () -> Unit
    ): ConfirmationDialogState {
        val title = if (isEs) "¿Guardar cambios?" else "Save changes?"
        val message = if (isEs) {
            "Se actualizará la información de «$itemName». ¿Deseas aplicar estos cambios?"
        } else {
            "Information for \"$itemName\" will be updated. Do you want to apply these changes?"
        }
        return ConfirmationDialogState(
            title = title,
            message = message,
            confirmButtonText = if (isEs) "💾 Guardar" else "💾 Save",
            dismissButtonText = if (isEs) "Cancelar" else "Cancel",
            isDestructive = false,
            requiresDoubleCheck = false,
            onConfirm = onConfirm
        )
    }
}
