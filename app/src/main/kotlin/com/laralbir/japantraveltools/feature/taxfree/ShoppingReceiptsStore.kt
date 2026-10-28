package com.laralbir.japantraveltools.feature.taxfree

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShoppingReceiptsStore(
    private val storageDir: File
) {

    private val receiptsFile = File(storageDir, "shopping_receipts.json")
    private val mutex = Mutex()

    private val _receiptsFlow = MutableStateFlow<List<ShoppingReceipt>>(emptyList())
    val receiptsFlow: StateFlow<List<ShoppingReceipt>> = _receiptsFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!receiptsFile.exists()) {
            _receiptsFlow.value = emptyList()
            return
        }
        try {
            val content = receiptsFile.readText()
            if (content.isNotBlank()) {
                val list = ShoppingReceiptsJsonCodec.importReceiptList(content)
                _receiptsFlow.value = list.sortedByDescending { it.purchaseDateIso }
            } else {
                _receiptsFlow.value = emptyList()
            }
        } catch (_: Exception) {
            _receiptsFlow.value = emptyList()
        }
    }

    private fun saveToDisk(list: List<ShoppingReceipt>) {
        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val content = ShoppingReceiptsJsonCodec.exportReceiptList(list)
            receiptsFile.writeText(content)
        } catch (_: Exception) {
            // Log / ignore
        }
    }

    suspend fun getAllReceipts(): List<ShoppingReceipt> = mutex.withLock {
        _receiptsFlow.value
    }

    suspend fun getReceiptById(id: String): ShoppingReceipt? = mutex.withLock {
        _receiptsFlow.value.firstOrNull { it.id == id }
    }

    suspend fun saveReceipt(receipt: ShoppingReceipt) = mutex.withLock {
        val current = _receiptsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == receipt.id }
        if (index >= 0) {
            current[index] = receipt.copy(updatedAtMillis = System.currentTimeMillis())
        } else {
            current.add(receipt.copy(updatedAtMillis = System.currentTimeMillis()))
        }
        val sorted = current.sortedByDescending { it.purchaseDateIso }
        saveToDisk(sorted)
        _receiptsFlow.value = sorted
    }

    suspend fun deleteReceipt(receiptId: String): Boolean = mutex.withLock {
        val current = _receiptsFlow.value.toMutableList()
        val removedReceipt = current.firstOrNull { it.id == receiptId } ?: return false
        current.remove(removedReceipt)

        // Si tenía foto local, eliminar el archivo físico
        removedReceipt.photoFilePath?.let { path ->
            val photoFile = File(path)
            if (photoFile.exists() && photoFile.parentFile?.absolutePath == storageDir.absolutePath) {
                photoFile.delete()
            }
        }

        saveToDisk(current)
        _receiptsFlow.value = current
        true
    }

    suspend fun exportToJson(): String = mutex.withLock {
        ShoppingReceiptsJsonCodec.exportReceiptList(_receiptsFlow.value)
    }

    suspend fun importFromJson(jsonContent: String): Int = mutex.withLock {
        try {
            val imported = ShoppingReceiptsJsonCodec.importReceiptList(jsonContent)
            val currentMap = _receiptsFlow.value.associateBy { it.id }.toMutableMap()
            for (rec in imported) {
                currentMap[rec.id] = rec
            }
            val combined = currentMap.values.sortedByDescending { it.purchaseDateIso }
            saveToDisk(combined)
            _receiptsFlow.value = combined
            imported.size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun generateTaxFreeReport(eurToJpyRate: Double): TaxFreeReport = mutex.withLock {
        TaxFreeReportGenerator.generateReport(_receiptsFlow.value, eurToJpyRate)
    }

    suspend fun calculateBudgetSummary(eurToJpyRate: Double): TravelBudgetSummary = mutex.withLock {
        TaxFreeCalculator.calculateBudgetSummary(_receiptsFlow.value, eurToJpyRate)
    }
}

object SharedShoppingReceiptsStore {
    private var instance: ShoppingReceiptsStore? = null
    private val _receiptsState = MutableStateFlow<List<ShoppingReceipt>>(emptyList())
    val receiptsState: StateFlow<List<ShoppingReceipt>> = _receiptsState.asStateFlow()

    fun init(context: android.content.Context) {
        if (instance != null) return
        val dir = File(context.applicationContext.filesDir, "taxfree_receipts")
        if (!dir.exists()) dir.mkdirs()
        initForTesting(dir)
    }

    fun initForTesting(dir: File) {
        val store = ShoppingReceiptsStore(dir)
        instance = store
        _receiptsState.value = store.receiptsFlow.value
    }

    fun getStore(): ShoppingReceiptsStore? = instance

    suspend fun saveReceipt(receipt: ShoppingReceipt) {
        val currentStore = instance ?: return
        currentStore.saveReceipt(receipt)
        _receiptsState.value = currentStore.receiptsFlow.value
    }

    suspend fun deleteReceipt(receiptId: String): Boolean {
        val currentStore = instance ?: return false
        val result = currentStore.deleteReceipt(receiptId)
        _receiptsState.value = currentStore.receiptsFlow.value
        return result
    }

    suspend fun exportToJson(): String {
        return instance?.exportToJson() ?: "[]"
    }

    suspend fun importFromJson(json: String): Int {
        val currentStore = instance ?: return 0
        val count = currentStore.importFromJson(json)
        _receiptsState.value = currentStore.receiptsFlow.value
        return count
    }

    suspend fun generateTaxFreeReport(eurToJpyRate: Double): TaxFreeReport {
        return instance?.generateTaxFreeReport(eurToJpyRate)
            ?: TaxFreeReportGenerator.generateReport(emptyList(), eurToJpyRate)
    }

    suspend fun calculateBudgetSummary(eurToJpyRate: Double): TravelBudgetSummary {
        return instance?.calculateBudgetSummary(eurToJpyRate)
            ?: TaxFreeCalculator.calculateBudgetSummary(emptyList(), eurToJpyRate)
    }
}

