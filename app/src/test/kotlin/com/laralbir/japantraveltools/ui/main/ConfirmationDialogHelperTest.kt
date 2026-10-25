package com.laralbir.japantraveltools.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmationDialogHelperTest {

    @Test
    fun `builds destructive delete confirmation with item name and proper texts`() {
        var deleted = false
        val state = ConfirmationDialogBuilder.buildDeleteConfirmation(
            itemName = "Vuelo QR150",
            itemTypeEs = "transporte",
            itemTypeEn = "transport",
            isEs = true,
            onConfirm = { deleted = true }
        )

        assertEquals("¿Eliminar transporte?", state.title)
        assertTrue(state.message.contains("Vuelo QR150"))
        assertTrue(state.message.contains("no se puede deshacer"))
        assertEquals("🗑️ Eliminar", state.confirmButtonText)
        assertEquals("Cancelar", state.dismissButtonText)
        assertTrue(state.isDestructive)

        // Execute action
        assertFalse(deleted)
        state.onConfirm()
        assertTrue(deleted)
    }

    @Test
    fun `builds destructive delete confirmation in English`() {
        var deleted = false
        val state = ConfirmationDialogBuilder.buildDeleteConfirmation(
            itemName = "Tokyo Station",
            itemTypeEs = "ciudad",
            itemTypeEn = "city",
            isEs = false,
            onConfirm = { deleted = true }
        )

        assertEquals("Delete city?", state.title)
        assertTrue(state.message.contains("Tokyo Station"))
        assertEquals("🗑️ Delete", state.confirmButtonText)
        assertEquals("Cancel", state.dismissButtonText)
        assertTrue(state.isDestructive)

        assertFalse(deleted)
        state.onConfirm()
        assertTrue(deleted)
    }

    @Test
    fun `builds edit confirmation for modifying existing items`() {
        var saved = false
        val state = ConfirmationDialogBuilder.buildEditConfirmation(
            itemName = "Ruta Kioto Día 2",
            isEs = true,
            onConfirm = { saved = true }
        )

        assertEquals("¿Guardar cambios?", state.title)
        assertTrue(state.message.contains("Ruta Kioto Día 2"))
        assertEquals("💾 Guardar", state.confirmButtonText)
        assertEquals("Cancelar", state.dismissButtonText)
        assertFalse(state.isDestructive)

        assertFalse(saved)
        state.onConfirm()
        assertTrue(saved)
    }
}

