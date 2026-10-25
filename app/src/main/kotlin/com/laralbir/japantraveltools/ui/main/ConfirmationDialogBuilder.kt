package com.laralbir.japantraveltools.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class ConfirmationDialogState(
    val title: String,
    val message: String,
    val confirmButtonText: String,
    val dismissButtonText: String,
    val isDestructive: Boolean = false,
    val onConfirm: () -> Unit
)

@Composable
fun AppConfirmationDialog(
    state: ConfirmationDialogState?,
    onDismiss: () -> Unit
) {
    if (state == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = state.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(text = state.message, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm()
                    onDismiss()
                },
                colors = if (state.isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
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
            onConfirm = onConfirm
        )
    }
}
