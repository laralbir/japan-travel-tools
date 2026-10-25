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

    @Test
    fun `builds sensitive delete confirmation requiring double check in Spanish`() {
        var deleted = false
        val state = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
            itemName = "Mi Viaje a Japón 2026",
            itemTypeEs = "plan de viaje completo",
            itemTypeEn = "complete travel plan",
            isEs = true,
            onConfirm = { deleted = true }
        )

        assertTrue(state.title.contains("plan de viaje completo"))
        assertTrue(state.message.contains("Mi Viaje a Japón 2026"))
        assertTrue(state.message.contains("doble confirmación"))
        assertTrue(state.isDestructive)
        assertTrue(state.requiresDoubleCheck)
        assertTrue(state.doubleCheckPrompt != null && state.doubleCheckPrompt.contains("DOBLE COMPROBACIÓN"))
        assertEquals("🗑️ Eliminar Definitivamente", state.confirmButtonText)

        assertFalse(deleted)
        state.onConfirm()
        assertTrue(deleted)
    }

    @Test
    fun `builds sensitive delete confirmation requiring double check in English`() {
        var deleted = false
        val state = ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation(
            itemName = "Passport Front Page",
            itemTypeEs = "foto de pasaporte",
            itemTypeEn = "passport photo",
            isEs = false,
            onConfirm = { deleted = true }
        )

        assertTrue(state.title.contains("passport photo"))
        assertTrue(state.message.contains("Passport Front Page"))
        assertTrue(state.message.contains("double check"))
        assertTrue(state.isDestructive)
        assertTrue(state.requiresDoubleCheck)
        assertTrue(state.doubleCheckPrompt != null && state.doubleCheckPrompt.contains("DOUBLE CHECK"))
        assertEquals("🗑️ Delete Permanently", state.confirmButtonText)

        assertFalse(deleted)
        state.onConfirm()
        assertTrue(deleted)
    }

    @Test
    fun `standard delete confirmation has requiresDoubleCheck as false`() {
        val standard = ConfirmationDialogBuilder.buildDeleteConfirmation(
            itemName = "Día 1",
            itemTypeEs = "parada",
            itemTypeEn = "stop",
            isEs = true,
            onConfirm = {}
        )
        assertFalse(standard.requiresDoubleCheck)
    }
}

