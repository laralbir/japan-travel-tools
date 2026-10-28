package com.laralbir.japantraveltools.feature.taxfree

import java.time.Instant
import java.util.Locale

object TaxFreeReportGenerator {

    fun generateReport(
        receipts: List<ShoppingReceipt>,
        eurToJpyRate: Double
    ): TaxFreeReport {
        val rate = if (eurToJpyRate > 0.0) eurToJpyRate else 160.0
        val eligibleReceipts = receipts.filter { it.isTaxFreeEligible }

        // Agrupación por tienda y fecha
        val storeGroups = eligibleReceipts.groupBy {
            Pair(it.storeName, it.purchaseDateIso.take(10))
        }.map { (pair, groupReceipts) ->
            val store = pair.first
            val date = pair.second
            val subtotal = groupReceipts.sumOf { it.subtotalWithoutTaxYen }
            val tax = groupReceipts.sumOf { it.totalTaxYen }
            val total = groupReceipts.sumOf { it.totalWithTaxYen }

            val has8 = groupReceipts.any { it.goodsType == GoodsType.CONSUMABLES || it.goodsType == GoodsType.MIXED }
            val has10 = groupReceipts.any { it.goodsType == GoodsType.GENERAL_GOODS || it.goodsType == GoodsType.MIXED }
            val groupGoodsType = when {
                has8 && has10 -> GoodsType.MIXED
                has8 -> GoodsType.CONSUMABLES
                else -> GoodsType.GENERAL_GOODS
            }

            val groupStatus = if (groupReceipts.all { it.taxFreeStatus == groupReceipts.first().taxFreeStatus }) {
                groupReceipts.first().taxFreeStatus
            } else {
                TaxFreeClaimStatus.PENDING
            }

            TaxFreeStoreGroup(
                storeName = store,
                dateIso = date,
                receiptCount = groupReceipts.size,
                subtotalWithoutTaxYen = subtotal,
                totalTaxYen = tax,
                totalWithTaxYen = total,
                goodsType = groupGoodsType,
                status = groupStatus,
                receipts = groupReceipts
            )
        }.sortedByDescending { it.totalTaxYen }

        val totalReceiptsCount = eligibleReceipts.size
        val totalSubtotalWithoutTaxYen = eligibleReceipts.sumOf { it.subtotalWithoutTaxYen }
        val totalRecoverableTaxYen = eligibleReceipts.sumOf { it.totalTaxYen }
        val totalRecoverableTaxEur = totalRecoverableTaxYen / rate

        val statusBreakdown = eligibleReceipts.groupingBy { it.taxFreeStatus }.eachCount()

        val textReport = buildTextReport(
            storeGroups = storeGroups,
            totalCount = totalReceiptsCount,
            subtotalYen = totalSubtotalWithoutTaxYen,
            taxYen = totalRecoverableTaxYen,
            taxEur = totalRecoverableTaxEur,
            rate = rate
        )

        val markdownReport = buildMarkdownReport(
            storeGroups = storeGroups,
            totalCount = totalReceiptsCount,
            subtotalYen = totalSubtotalWithoutTaxYen,
            taxYen = totalRecoverableTaxYen,
            taxEur = totalRecoverableTaxEur,
            statusBreakdown = statusBreakdown,
            rate = rate
        )

        val jsonExport = buildJsonExport(
            storeGroups = storeGroups,
            totalCount = totalReceiptsCount,
            subtotalYen = totalSubtotalWithoutTaxYen,
            taxYen = totalRecoverableTaxYen,
            taxEur = totalRecoverableTaxEur,
            rate = rate
        )

        return TaxFreeReport(
            generatedAtIso = Instant.now().toString(),
            storeGroups = storeGroups,
            totalReceiptsCount = totalReceiptsCount,
            totalSubtotalWithoutTaxYen = totalSubtotalWithoutTaxYen,
            totalRecoverableTaxYen = totalRecoverableTaxYen,
            totalRecoverableTaxEur = totalRecoverableTaxEur,
            statusBreakdown = statusBreakdown,
            exchangeRateUsed = rate,
            textReport = textReport,
            markdownReport = markdownReport,
            jsonExport = jsonExport
        )
    }

    private fun buildTextReport(
        storeGroups: List<TaxFreeStoreGroup>,
        totalCount: Int,
        subtotalYen: Double,
        taxYen: Double,
        taxEur: Double,
        rate: Double
    ): String {
        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("      INFORME TAX-FREE JAPÓN            ")
        sb.appendLine("========================================")
        sb.appendLine("Total Comprobantes Elegibles: $totalCount")
        sb.appendLine("Base Imponible Total: ${subtotalYen.toLong()} ¥")
        sb.appendLine("Impuesto Recuperable Total: ${taxYen.toLong()} ¥ (${String.format(Locale.US, "%.2f", taxEur)} EUR)")
        sb.appendLine("Tipo de Cambio: 1 EUR = $rate JPY")
        sb.appendLine("----------------------------------------")
        sb.appendLine("DESGLOSE POR COMERCIO Y FECHA:")
        for (g in storeGroups) {
            sb.appendLine("• ${g.storeName} (${g.dateIso}):")
            sb.appendLine("  Base: ${g.subtotalWithoutTaxYen.toLong()} ¥ | Impuesto: ${g.totalTaxYen.toLong()} ¥")
            sb.appendLine("  Tipo: ${g.goodsType} | Estado: ${g.status}")
        }
        sb.appendLine("========================================")
        return sb.toString()
    }

    private fun buildMarkdownReport(
        storeGroups: List<TaxFreeStoreGroup>,
        totalCount: Int,
        subtotalYen: Double,
        taxYen: Double,
        taxEur: Double,
        statusBreakdown: Map<TaxFreeClaimStatus, Int>,
        rate: Double
    ): String {
        val formatYen = String.format(Locale.GERMAN, "%,d ¥", taxYen.toLong())
        val formatEur = String.format(Locale.GERMAN, "%.2f €", taxEur)
        val formatBase = String.format(Locale.GERMAN, "%,d ¥", subtotalYen.toLong())

        val sb = StringBuilder()
        sb.appendLine("# Informe de Devolución Tax-Free (Japón)")
        sb.appendLine()
        sb.appendLine("> Documento de control fiscal y aduanero para compras realizadas en Japón bajo el régimen de exención fiscal para turistas no residentes.")
        sb.appendLine()
        sb.appendLine("### 📊 Resumen Consolidado")
        sb.appendLine("- **Total Comprobantes Elegibles**: $totalCount")
        sb.appendLine("- **Base Imponible Total**: $formatBase")
        sb.appendLine("- **Impuesto Recuperable Total**: **$formatYen** (**$formatEur**)")
        sb.appendLine("- **Tipo de Cambio Aplicado**: 1 EUR = $rate JPY")
        sb.appendLine("- **Estado de Solicitudes**: ${statusBreakdown.entries.joinToString(", ") { "${it.key}: ${it.value}" }}")
        sb.appendLine()
        sb.appendLine("### 🏬 Desglose por Establecimiento")
        sb.appendLine("| Comercio | Fecha | Tipo de Bienes | Base (¥) | Impuesto Recuperable (¥) | Equiv. (€) | Estado |")
        sb.appendLine("| :--- | :---: | :---: | :---: | :---: | :---: | :---: |")

        for (g in storeGroups) {
            val groupTaxYen = String.format(Locale.GERMAN, "%,d ¥", g.totalTaxYen.toLong())
            val groupTaxEur = String.format(Locale.GERMAN, "%.2f €", g.totalTaxYen / rate)
            val groupBaseYen = String.format(Locale.GERMAN, "%,d ¥", g.subtotalWithoutTaxYen.toLong())
            sb.appendLine("| **${g.storeName}** | ${g.dateIso} | ${g.goodsType} | $groupBaseYen | $groupTaxYen | $groupTaxEur | ${g.status} |")
        }

        sb.appendLine()
        sb.appendLine("### 🛂 Normas Aduaneras Importantes en Japón")
        sb.appendLine("1. **Consumibles (`CONSUMABLES`)**: No deben abrirse en Japón y deben transportarse en la bolsa sellada precintada.")
        sb.appendLine("2. **Bienes Generales (`GENERAL_GOODS`)**: Deben salir de Japón en un plazo máximo de 6 meses desde la entrada al país.")
        sb.appendLine("3. **Aduana en Aeropuerto (Haneda, Narita, Kansai)**: Escanear el pasaporte en los quioscos automáticos de aduana antes de facturar o pasar el control de seguridad.")
        return sb.toString()
    }

    private fun buildJsonExport(
        storeGroups: List<TaxFreeStoreGroup>,
        totalCount: Int,
        subtotalYen: Double,
        taxYen: Double,
        taxEur: Double,
        rate: Double
    ): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"generatedAtIso\": \"${Instant.now()}\",")
        sb.appendLine("  \"totalReceiptsCount\": $totalCount,")
        sb.appendLine("  \"totalSubtotalWithoutTaxYen\": ${subtotalYen.toLong()},")
        sb.appendLine("  \"totalRecoverableTaxYen\": ${taxYen.toLong()},")
        sb.appendLine("  \"totalRecoverableTaxEur\": ${String.format(Locale.US, "%.2f", taxEur)},")
        sb.appendLine("  \"exchangeRateUsed\": $rate,")
        sb.appendLine("  \"storeGroups\": [")
        storeGroups.forEachIndexed { idx, g ->
            val comma = if (idx < storeGroups.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"storeName\": \"${g.storeName.replace("\"", "\\\"")}\",")
            sb.appendLine("      \"dateIso\": \"${g.dateIso}\",")
            sb.appendLine("      \"receiptCount\": ${g.receiptCount},")
            sb.appendLine("      \"subtotalWithoutTaxYen\": ${g.subtotalWithoutTaxYen},")
            sb.appendLine("      \"totalTaxYen\": ${g.totalTaxYen},")
            sb.appendLine("      \"totalWithTaxYen\": ${g.totalWithTaxYen},")
            sb.appendLine("      \"goodsType\": \"${g.goodsType.name}\",")
            sb.appendLine("      \"status\": \"${g.status.name}\"")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }
}
