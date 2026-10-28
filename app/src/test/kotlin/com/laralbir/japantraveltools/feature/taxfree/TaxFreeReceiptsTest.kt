package com.laralbir.japantraveltools.feature.taxfree

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaxFreeReceiptsTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("taxfree_test").toFile()
    }

    // -------------------------------------------------------------------------
    // 1. Cálculos de Subtotales, Tasas (8% y 10%) y Clasificación de Bienes
    // -------------------------------------------------------------------------

    @Test
    fun `test product item calculates taxes accurately for 8 percent reduced and 10 percent standard rates`() {
        // Tasa 8% (Alimentación / Consumible)
        val foodItem = ReceiptProductItem(
            id = "item-1",
            name = "Matcha KitKat (10-pack)",
            quantity = 2,
            unitPriceYen = 400.0,
            taxRatePercent = 8.0
        )
        // Subtotal: 2 * 400 = 800 ¥
        // Tax 8%: 800 * 0.08 = 64 ¥
        // Total: 864 ¥
        assertEquals(800.0, foodItem.subtotalWithoutTaxYen, 0.01)
        assertEquals(64.0, foodItem.taxYen, 0.01)
        assertEquals(864.0, foodItem.totalWithTaxYen, 0.01)

        // Tasa 10% (Bienes Generales / Electrónica)
        val generalItem = ReceiptProductItem(
            id = "item-2",
            name = "Sony WH-1000XM5 Headphones",
            quantity = 1,
            unitPriceYen = 42000.0,
            taxRatePercent = 10.0
        )
        // Subtotal: 42000 ¥
        // Tax 10%: 42000 * 0.10 = 4200 ¥
        // Total: 46200 ¥
        assertEquals(42000.0, generalItem.subtotalWithoutTaxYen, 0.01)
        assertEquals(4200.0, generalItem.taxYen, 0.01)
        assertEquals(46200.0, generalItem.totalWithTaxYen, 0.01)
    }

    @Test
    fun `test goods type classification correctly distinguishes consumables, general goods and mixed`() {
        val consumableItem = ReceiptProductItem(
            id = "c1",
            name = "Tokyo Banana Snacks",
            quantity = 3,
            unitPriceYen = 1000.0,
            taxRatePercent = 8.0
        )
        val generalItem = ReceiptProductItem(
            id = "g1",
            name = "Kimono Scarf",
            quantity = 1,
            unitPriceYen = 3000.0,
            taxRatePercent = 10.0
        )

        // Only consumables
        val consumableReceipt = TaxFreeCalculator.createReceipt(
            storeName = "Tokyo Souvenirs",
            location = "Tokyo",
            items = listOf(consumableItem),
            category = ExpenseCategory.SHOPPING
        )
        assertEquals(GoodsType.CONSUMABLES, consumableReceipt.goodsType)

        // Only general goods
        val generalReceipt = TaxFreeCalculator.createReceipt(
            storeName = "Kyoto Crafts",
            location = "Kyoto",
            items = listOf(generalItem),
            category = ExpenseCategory.SHOPPING
        )
        assertEquals(GoodsType.GENERAL_GOODS, generalReceipt.goodsType)

        // Mixed goods
        val mixedReceipt = TaxFreeCalculator.createReceipt(
            storeName = "Don Quijote Shinjuku",
            location = "Tokyo",
            items = listOf(consumableItem, generalItem),
            category = ExpenseCategory.SHOPPING
        )
        assertEquals(GoodsType.MIXED, mixedReceipt.goodsType)
    }

    // -------------------------------------------------------------------------
    // 2. Evaluación de Reglas de Tax-Free en Japón (Umbral de 5.000 ¥)
    // -------------------------------------------------------------------------

    @Test
    fun `test tax-free threshold of 5000 yen strictly classifies eligibility`() {
        // Compra de 4.999 ¥ sin impuestos -> NO elegible
        val underThreshold = TaxFreeCalculator.createReceipt(
            storeName = "FamilyMart Shibuya",
            location = "Tokyo",
            items = listOf(
                ReceiptProductItem("1", "Snacks & Drinks", quantity = 1, unitPriceYen = 4999.0, taxRatePercent = 8.0)
            )
        )
        assertFalse(underThreshold.isTaxFreeEligible)
        assertEquals(TaxFreeClaimStatus.NOT_APPLICABLE, underThreshold.taxFreeStatus)

        // Compra exacta de 5.000 ¥ sin impuestos -> SÍ elegible
        val exactThreshold = TaxFreeCalculator.createReceipt(
            storeName = "Bic Camera Akihabara",
            location = "Tokyo",
            items = listOf(
                ReceiptProductItem("2", "Memory Card", quantity = 1, unitPriceYen = 5000.0, taxRatePercent = 10.0)
            )
        )
        assertTrue(exactThreshold.isTaxFreeEligible)
        assertEquals(TaxFreeClaimStatus.PENDING, exactThreshold.taxFreeStatus)

        // Compra superior a 5.000 ¥ -> SÍ elegible
        val overThreshold = TaxFreeCalculator.createReceipt(
            storeName = "Yodobashi Umeda",
            location = "Osaka",
            items = listOf(
                ReceiptProductItem("3", "Camera Lens", quantity = 1, unitPriceYen = 38000.0, taxRatePercent = 10.0)
            )
        )
        assertTrue(overThreshold.isTaxFreeEligible)
        assertEquals(TaxFreeClaimStatus.PENDING, overThreshold.taxFreeStatus)
    }

    // -------------------------------------------------------------------------
    // 3. Parser OCR de Recibos Japoneses (Don Quijote, Bic Camera, Uniqlo, 7-Eleven)
    // -------------------------------------------------------------------------

    @Test
    fun `test ocr parser extracts Don Quijote mixed receipt with 8 and 10 percent items`() {
        val donkiReceiptText = """
            ドン・キホーテ 道頓堀店
            TEL: 06-1234-5678
            2027/03/25 18:32
            
            キットカット 抹茶 8%         *  800
            Pocky Sakura 8%            *  400
            マグネット 大阪 10%            1,200
            Tシャツ 侍 10%               3,500
            
            小計                      5,900
            内消費税等 (8%)              96
            内消費税等 (10%)            470
            合計                      6,466
            免税対象額                5,900
        """.trimIndent()

        val parsed = JapaneseReceiptOcrParser.parse(donkiReceiptText)

        assertTrue(parsed.storeName.contains("ドン・キホーテ") || parsed.storeName.contains("Don Quijote"))
        assertTrue(parsed.subtotalWithoutTaxYen >= 5900.0)
        assertTrue(parsed.totalWithTaxYen >= 6466.0)
        assertTrue(parsed.totalTaxYen >= 566.0)
        assertTrue(parsed.isTaxFreeEligible)
        assertEquals(GoodsType.MIXED, parsed.goodsType)
    }

    @Test
    fun `test ocr parser extracts Bic Camera electronics receipt with 10 percent standard tax`() {
        val bicReceiptText = """
            ビックカメラ 有楽町店
            BIC CAMERA YURAKUCHO
            2027-04-02 14:15
            
            SDカード 128GB              4,500
            カメラバッテリー            5,500
            
            小計                     10,000
            10%対象                  10,000
            内消費税                  1,000
            合計                     11,000
            TAX FREE
        """.trimIndent()

        val parsed = JapaneseReceiptOcrParser.parse(bicReceiptText)

        assertTrue(parsed.storeName.contains("ビックカメラ") || parsed.storeName.contains("Bic Camera"))
        assertEquals(10000.0, parsed.subtotalWithoutTaxYen, 0.1)
        assertEquals(1000.0, parsed.totalTaxYen, 0.1)
        assertEquals(11000.0, parsed.totalWithTaxYen, 0.1)
        assertTrue(parsed.isTaxFreeEligible)
        assertEquals(GoodsType.GENERAL_GOODS, parsed.goodsType)
    }

    @Test
    fun `test ocr parser extracts Uniqlo clothing receipt`() {
        val uniqloReceiptText = """
            UNIQLO 銀座店
            ユニクロ 領収書
            2027/04/10 11:20
            
            エアリズム インナー           1,290
            ウルトラライトダウン         6,990
            
            小計                      8,280
            消費税等 (10%)              828
            合計                      9,108
            免税 TAX FREE
        """.trimIndent()

        val parsed = JapaneseReceiptOcrParser.parse(uniqloReceiptText)

        assertTrue(parsed.storeName.contains("UNIQLO") || parsed.storeName.contains("ユニクロ"))
        assertEquals(8280.0, parsed.subtotalWithoutTaxYen, 0.1)
        assertEquals(828.0, parsed.totalTaxYen, 0.1)
        assertEquals(9108.0, parsed.totalWithTaxYen, 0.1)
        assertTrue(parsed.isTaxFreeEligible)
        assertEquals(GoodsType.GENERAL_GOODS, parsed.goodsType)
    }

    @Test
    fun `test ocr parser extracts 7-Eleven konbini receipt under tax-free threshold`() {
        val konbiniText = """
            セブン-イレブン 新宿東口店
            2027/03/15 08:45
            
            おにぎり 鮭 軽               150
            緑茶 500ml 軽                140
            サンドイッチ 軽               320
            
            小計                         610
            外税 8%                       48
            合計                         658
        """.trimIndent()

        val parsed = JapaneseReceiptOcrParser.parse(konbiniText)

        assertTrue(parsed.storeName.contains("セブン-イレブン") || parsed.storeName.contains("7-Eleven"))
        assertEquals(610.0, parsed.subtotalWithoutTaxYen, 0.1)
        assertEquals(48.0, parsed.totalTaxYen, 0.1)
        assertEquals(658.0, parsed.totalWithTaxYen, 0.1)
        assertFalse(parsed.isTaxFreeEligible)
        assertEquals(TaxFreeClaimStatus.NOT_APPLICABLE, parsed.taxFreeStatus)
        assertEquals(GoodsType.CONSUMABLES, parsed.goodsType)
    }

    // -------------------------------------------------------------------------
    // 4. Generación de Informe Tax-Free Consolidado (Texto, Markdown y JSON)
    // -------------------------------------------------------------------------

    @Test
    fun `test tax free report generator produces consolidated breakdown by store and date with EUR conversion`() {
        val receipt1 = TaxFreeCalculator.createReceipt(
            id = "r1",
            storeName = "Don Quijote Dotonbori",
            location = "Osaka",
            purchaseDateIso = "2027-03-20T10:00:00Z",
            items = listOf(
                ReceiptProductItem("1", "Matcha Snacks", 2, 2000.0, 8.0), // 4000 + 320 = 4320
                ReceiptProductItem("2", "Souvenir Fans", 2, 1000.0, 10.0) // 2000 + 200 = 2200
            ),
            category = ExpenseCategory.SHOPPING,
            taxFreeStatus = TaxFreeClaimStatus.PENDING
        ) // Subtotal: 6000 ¥, Tax: 520 ¥, Total: 6520 ¥ -> Eligible

        val receipt2 = TaxFreeCalculator.createReceipt(
            id = "r2",
            storeName = "Bic Camera Shinjuku",
            location = "Tokyo",
            purchaseDateIso = "2027-03-22T14:30:00Z",
            items = listOf(
                ReceiptProductItem("3", "Headphones", 1, 30000.0, 10.0) // 30000 + 3000 = 33000
            ),
            category = ExpenseCategory.SHOPPING,
            taxFreeStatus = TaxFreeClaimStatus.PENDING
        ) // Subtotal: 30000 ¥, Tax: 3000 ¥, Total: 33000 ¥ -> Eligible

        val receipt3NonEligible = TaxFreeCalculator.createReceipt(
            id = "r3",
            storeName = "Lawson Shibuya",
            location = "Tokyo",
            purchaseDateIso = "2027-03-22T18:00:00Z",
            items = listOf(
                ReceiptProductItem("4", "Drinks", 1, 800.0, 8.0)
            ),
            category = ExpenseCategory.FOOD,
            taxFreeStatus = TaxFreeClaimStatus.NOT_APPLICABLE
        ) // Subtotal: 800 ¥ -> Not eligible

        val receipts = listOf(receipt1, receipt2, receipt3NonEligible)
        val eurToJpyRate = 160.0 // 1 EUR = 160 JPY

        val report = TaxFreeReportGenerator.generateReport(receipts, eurToJpyRate)

        // Only eligible receipts are included in the tax refund total
        assertEquals(2, report.totalReceiptsCount)
        assertEquals(36000.0, report.totalSubtotalWithoutTaxYen, 0.01) // 6000 + 30000
        assertEquals(3520.0, report.totalRecoverableTaxYen, 0.01)     // 520 + 3000
        // 3520 / 160.0 = 22.0 EUR
        assertEquals(22.0, report.totalRecoverableTaxEur, 0.01)

        assertEquals(2, report.storeGroups.size)
        assertEquals(2, report.statusBreakdown[TaxFreeClaimStatus.PENDING])

        // Verify Markdown report contains expected headers and store names
        assertTrue(report.markdownReport.contains("# Informe de Devolución Tax-Free"))
        assertTrue(report.markdownReport.contains("Don Quijote Dotonbori"))
        assertTrue(report.markdownReport.contains("Bic Camera Shinjuku"))
        assertTrue(report.markdownReport.contains("3.520 ¥"))
        assertTrue(report.markdownReport.contains("22,00 €"))

        // Verify Text report
        assertTrue(report.textReport.contains("INFORME TAX-FREE JAPÓN"))
        assertTrue(report.textReport.contains("3520 ¥"))

        // Verify JSON export contains parseable structure
        assertTrue(report.jsonExport.contains("\"totalRecoverableTaxYen\": 3520"))
    }

    // -------------------------------------------------------------------------
    // 5. Control de Gastos y Presupuesto del Viaje (Categorías, Días y Ciudades)
    // -------------------------------------------------------------------------

    @Test
    fun `test travel budget summary aggregates expenses accurately by category, day and city`() {
        val rShopping = TaxFreeCalculator.createReceipt(
            id = "1",
            storeName = "Uniqlo Ginza",
            location = "Tokyo",
            purchaseDateIso = "2027-03-20T11:00:00Z",
            items = listOf(ReceiptProductItem("1", "Coat", 1, 10000.0, 10.0)),
            category = ExpenseCategory.SHOPPING
        ) // Total with tax: 11000 ¥

        val rFood = TaxFreeCalculator.createReceipt(
            id = "2",
            storeName = "Ichiran Ramen",
            location = "Tokyo",
            purchaseDateIso = "2027-03-20T19:00:00Z",
            items = listOf(ReceiptProductItem("2", "Ramen Bowl", 2, 980.0, 10.0)),
            category = ExpenseCategory.FOOD
        ) // Total with tax: 2156 ¥ (1960 + 196)

        val rTransport = TaxFreeCalculator.createReceipt(
            id = "3",
            storeName = "JR West Ticket Office",
            location = "Kyoto",
            purchaseDateIso = "2027-03-21T09:00:00Z",
            items = listOf(ReceiptProductItem("3", "Haruka Express", 1, 3500.0, 10.0)),
            category = ExpenseCategory.TRANSPORT
        ) // Total with tax: 3850 ¥

        val rLeisure = TaxFreeCalculator.createReceipt(
            id = "4",
            storeName = "Kiyomizu-dera Temple",
            location = "Kyoto",
            purchaseDateIso = "2027-03-21T14:00:00Z",
            items = listOf(ReceiptProductItem("4", "Entry Ticket", 2, 400.0, 10.0)),
            category = ExpenseCategory.LEISURE
        ) // Total with tax: 880 ¥

        val receipts = listOf(rShopping, rFood, rTransport, rLeisure)
        val eurToJpyRate = 160.0

        val budget = TaxFreeCalculator.calculateBudgetSummary(receipts, eurToJpyRate)

        // Total spent with tax: 11000 + 2156 + 3850 + 880 = 17886 ¥
        assertEquals(17886.0, budget.totalSpentYen, 0.01)
        // 17886 / 160 = 111.7875 EUR
        assertEquals(111.79, budget.totalSpentEur, 0.05)

        // Categories check
        val shoppingBreakdown = budget.byCategory.firstOrNull { it.category == ExpenseCategory.SHOPPING }
        assertNotNull(shoppingBreakdown)
        assertEquals(11000.0, shoppingBreakdown!!.totalYen, 0.01)
        assertTrue(shoppingBreakdown.percentageOfTotal > 60.0)

        // Cities check
        val tokyoTotal = budget.byCity.firstOrNull { it.city.equals("Tokyo", ignoreCase = true) }
        assertNotNull(tokyoTotal)
        assertEquals(13156.0, tokyoTotal!!.totalYen, 0.01) // 11000 + 2156

        val kyotoTotal = budget.byCity.firstOrNull { it.city.equals("Kyoto", ignoreCase = true) }
        assertNotNull(kyotoTotal)
        assertEquals(4730.0, kyotoTotal!!.totalYen, 0.01) // 3850 + 880

        // Days check
        assertEquals(2, budget.byDay.size)
    }

    // -------------------------------------------------------------------------
    // 6. Motor Híbrido de Escaneo IA (Gemini Cloud Vision y Fallback a ML Kit)
    // -------------------------------------------------------------------------

    @Test
    fun `test ai scanner engine correctly parses structured gemini json response`() {
        val geminiJsonOutput = """
            {
              "storeName": "ドン・キホーテ 梅田店",
              "dateIso": "2027-03-24T15:30:00Z",
              "location": "Osaka",
              "category": "SHOPPING",
              "goodsType": "MIXED",
              "items": [
                {
                  "name": "抹茶チョコ",
                  "quantity": 3,
                  "unitPriceYen": 300.0,
                  "taxRatePercent": 8.0
                },
                {
                  "name": "ステンレスボトル",
                  "quantity": 1,
                  "unitPriceYen": 4200.0,
                  "taxRatePercent": 10.0
                }
              ]
            }
        """.trimIndent()

        val receipt = AiReceiptScannerEngine.parseGeminiJsonResponse(geminiJsonOutput)

        assertEquals("ドン・キホーテ 梅田店", receipt.storeName)
        assertEquals("Osaka", receipt.location)
        assertEquals(2, receipt.items.size)
        // Subtotal: 3 * 300 (900) + 4200 = 5100 ¥
        assertEquals(5100.0, receipt.subtotalWithoutTaxYen, 0.01)
        // Tax: (900 * 0.08 = 72) + (4200 * 0.10 = 420) = 492 ¥
        assertEquals(492.0, receipt.totalTaxYen, 0.01)
        assertEquals(5592.0, receipt.totalWithTaxYen, 0.01)
        assertTrue(receipt.isTaxFreeEligible)
        assertEquals(GoodsType.MIXED, receipt.goodsType)
    }

    @Test
    fun `test ai scanner engine falls back to local parser when gemini fails or returns error`() {
        val ocrFallbackText = """
            ビックカメラ なんば店
            2027/03/28 12:00
            小計 8000
            内消費税 800
            合計 8800
        """.trimIndent()

        // Calling scanWithFallback with empty API key should trigger local parser fallback
        val result = AiReceiptScannerEngine.scanWithFallback(
            rawOcrText = ocrFallbackText,
            geminiApiKey = null,
            imageBytes = null
        )

        assertEquals(ScannerSource.LOCAL_MLKIT_OCR, result.source)
        assertTrue(result.receipt.storeName.contains("ビックカメラ") || result.receipt.storeName.contains("Bic Camera"))
        assertEquals(8000.0, result.receipt.subtotalWithoutTaxYen, 0.01)
        assertEquals(800.0, result.receipt.totalTaxYen, 0.01)
        assertEquals(8800.0, result.receipt.totalWithTaxYen, 0.01)
        assertTrue(result.receipt.isTaxFreeEligible)
    }

    // -------------------------------------------------------------------------
    // 7. Persistencia y Operaciones CRUD en ShoppingReceiptsStore
    // -------------------------------------------------------------------------

    @Test
    fun `test shopping receipts store handles persistence and crud operations`() = runBlocking {
        val store = ShoppingReceiptsStore(storageDir = tempDir)

        assertEquals(0, store.getAllReceipts().size)

        val receipt = TaxFreeCalculator.createReceipt(
            id = "rc-101",
            storeName = "Don Quijote Roppongi",
            location = "Tokyo",
            purchaseDateIso = "2027-03-29T16:00:00Z",
            items = listOf(ReceiptProductItem("1", "Souvenirs", 1, 7500.0, 10.0)),
            category = ExpenseCategory.SHOPPING,
            notes = "Regalos para la familia"
        )

        // Save
        store.saveReceipt(receipt)
        val listAfterSave = store.getAllReceipts()
        assertEquals(1, listAfterSave.size)
        assertEquals("Don Quijote Roppongi", listAfterSave[0].storeName)
        assertEquals(7500.0, listAfterSave[0].subtotalWithoutTaxYen, 0.01)
        assertTrue(listAfterSave[0].isTaxFreeEligible)

        // Update status
        val updated = receipt.copy(taxFreeStatus = TaxFreeClaimStatus.CUSTOMS_STAMPED)
        store.saveReceipt(updated)
        val listAfterUpdate = store.getAllReceipts()
        assertEquals(1, listAfterUpdate.size)
        assertEquals(TaxFreeClaimStatus.CUSTOMS_STAMPED, listAfterUpdate[0].taxFreeStatus)

        // Export & re-import
        val jsonExport = store.exportToJson()
        assertTrue(jsonExport.contains("rc-101"))

        val newStore = ShoppingReceiptsStore(storageDir = tempDir)
        assertEquals(1, newStore.getAllReceipts().size)

        // Delete
        store.deleteReceipt("rc-101")
        assertEquals(0, store.getAllReceipts().size)
    }

    @Test
    fun `test SharedShoppingReceiptsStore singleton manages receipts and budget`() = runBlocking {
        val tempDir = Files.createTempDirectory("shared_receipts_test").toFile()
        SharedShoppingReceiptsStore.initForTesting(tempDir)

        assertEquals(0, SharedShoppingReceiptsStore.receiptsState.value.size)

        val receipt = TaxFreeCalculator.createReceipt(
            id = "shared-1",
            storeName = "Bic Camera Shinjuku",
            location = "Tokyo",
            purchaseDateIso = "2027-03-22T10:00:00Z",
            items = listOf(ReceiptProductItem("p1", "Lens", 1, 40000.0, 10.0)),
            category = ExpenseCategory.SHOPPING,
            taxFreeStatus = TaxFreeClaimStatus.PENDING
        )

        SharedShoppingReceiptsStore.saveReceipt(receipt)
        assertEquals(1, SharedShoppingReceiptsStore.receiptsState.value.size)
        assertEquals("Bic Camera Shinjuku", SharedShoppingReceiptsStore.receiptsState.value[0].storeName)

        val budget = SharedShoppingReceiptsStore.calculateBudgetSummary(eurToJpyRate = 160.0)
        assertEquals(44000.0, budget.totalSpentYen, 0.01)

        val report = SharedShoppingReceiptsStore.generateTaxFreeReport(eurToJpyRate = 160.0)
        assertEquals(4000.0, report.totalRecoverableTaxYen, 0.01)

        val deleted = SharedShoppingReceiptsStore.deleteReceipt("shared-1")
        assertTrue(deleted)
        assertEquals(0, SharedShoppingReceiptsStore.receiptsState.value.size)
    }
}
