package com.laralbir.japantraveltools.feature.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextParserTest {

    @Test
    fun parseBlocks_extractsHeadersCorrectly() {
        val markdown = """
            # Guía de Tokio
            ## Transporte Shinkansen
            ### Reserva de Maletas
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Header)
        assertEquals(1, (blocks[0] as MarkdownBlock.Header).level)
        assertEquals("Guía de Tokio", (blocks[0] as MarkdownBlock.Header).text)

        assertTrue(blocks[1] is MarkdownBlock.Header)
        assertEquals(2, (blocks[1] as MarkdownBlock.Header).level)
        assertEquals("Transporte Shinkansen", (blocks[1] as MarkdownBlock.Header).text)

        assertTrue(blocks[2] is MarkdownBlock.Header)
        assertEquals(3, (blocks[2] as MarkdownBlock.Header).level)
        assertEquals("Reserva de Maletas", (blocks[2] as MarkdownBlock.Header).text)
    }

    @Test
    fun parseBlocks_extractsBulletAndNumberedLists() {
        val markdown = """
            Requisitos:
            * Pasaporte vigente
            - Tarjeta Suica o Pasmo
            • Seguro de viaje
            1. Llegar al aeropuerto Narita
            2. Tomar el Narita Express
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(6, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertEquals("Requisitos:", (blocks[0] as MarkdownBlock.Paragraph).text)

        assertTrue(blocks[1] is MarkdownBlock.BulletItem)
        assertEquals("Pasaporte vigente", (blocks[1] as MarkdownBlock.BulletItem).text)

        assertTrue(blocks[2] is MarkdownBlock.BulletItem)
        assertEquals("Tarjeta Suica o Pasmo", (blocks[2] as MarkdownBlock.BulletItem).text)

        assertTrue(blocks[3] is MarkdownBlock.BulletItem)
        assertEquals("Seguro de viaje", (blocks[3] as MarkdownBlock.BulletItem).text)

        assertTrue(blocks[4] is MarkdownBlock.NumberedItem)
        assertEquals(1, (blocks[4] as MarkdownBlock.NumberedItem).number)
        assertEquals("Llegar al aeropuerto Narita", (blocks[4] as MarkdownBlock.NumberedItem).text)

        assertTrue(blocks[5] is MarkdownBlock.NumberedItem)
        assertEquals(2, (blocks[5] as MarkdownBlock.NumberedItem).number)
        assertEquals("Tomar el Narita Express", (blocks[5] as MarkdownBlock.NumberedItem).text)
    }

    @Test
    fun parseBlocks_extractsCodeBlocks() {
        val markdown = """
            Aquí tienes un ejemplo en JSON:
            ```json
            {
              "city": "Tokyo",
              "temp": 18.5
            }
            ```
            Fin del ejemplo.
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)

        assertTrue(blocks[1] is MarkdownBlock.CodeBlock)
        val codeBlock = blocks[1] as MarkdownBlock.CodeBlock
        assertEquals("json", codeBlock.language)
        assertTrue(codeBlock.code.contains("\"city\": \"Tokyo\""))

        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
        assertEquals("Fin del ejemplo.", (blocks[2] as MarkdownBlock.Paragraph).text)
    }

    @Test
    fun parseInline_handlesPlainAndBoldText() {
        val raw = "Viajar a **Tokio** en tren."
        val spans = MarkdownTextParser.parseInline(raw)

        assertEquals(3, spans.size)
        assertEquals("Viajar a ", spans[0].text)
        assertFalse(spans[0].isBold)

        assertEquals("Tokio", spans[1].text)
        assertTrue(spans[1].isBold)
        assertFalse(spans[1].isItalic)
        assertFalse(spans[1].isCode)

        assertEquals(" en tren.", spans[2].text)
        assertFalse(spans[2].isBold)
    }

    @Test
    fun parseInline_handlesItalicAndInlineCode() {
        val raw = "Usa la tarjeta `Suica` para el metro y viaja *despacio*."
        val spans = MarkdownTextParser.parseInline(raw)

        assertEquals(5, spans.size)
        assertEquals("Usa la tarjeta ", spans[0].text)

        assertEquals("Suica", spans[1].text)
        assertTrue(spans[1].isCode)

        assertEquals(" para el metro y viaja ", spans[2].text)

        assertEquals("despacio", spans[3].text)
        assertTrue(spans[3].isItalic)

        assertEquals(".", spans[4].text)
    }

    @Test
    fun parseInline_handlesCombinedFormatting() {
        val raw = "**Importante:** Debes pagar en `JPY` (*yenes*)."
        val spans = MarkdownTextParser.parseInline(raw)

        assertTrue(spans.any { it.text == "Importante:" && it.isBold })
        assertTrue(spans.any { it.text == "JPY" && it.isCode })
        assertTrue(spans.any { it.text == "yenes" && it.isItalic })
    }

    @Test
    fun parseBlocks_ignoresConsecutiveEmptyLines() {
        val markdown = """
            # Título 1
            

            Primer párrafo.


            Segundo párrafo.
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Header)
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
        assertEquals("Primer párrafo.", (blocks[1] as MarkdownBlock.Paragraph).text)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
        assertEquals("Segundo párrafo.", (blocks[2] as MarkdownBlock.Paragraph).text)
    }

    @Test
    fun parseBlocks_extractsAllHeaderLevelsAndHorizontalRulesBeforeBullets() {
        val markdown = """
            ### Nivel 3
            ---
            #### Nivel 4
            ***
            ##### Nivel 5
            ___
            ###### Nivel 6
            * * *
            ***Texto en negrita y cursiva***
            * Viñeta normal
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(10, blocks.size)
        assertEquals(MarkdownBlock.Header(3, "Nivel 3"), blocks[0])
        assertEquals(MarkdownBlock.HorizontalRule, blocks[1])
        assertEquals(MarkdownBlock.Header(4, "Nivel 4"), blocks[2])
        assertEquals(MarkdownBlock.HorizontalRule, blocks[3])
        assertEquals(MarkdownBlock.Header(5, "Nivel 5"), blocks[4])
        assertEquals(MarkdownBlock.HorizontalRule, blocks[5])
        assertEquals(MarkdownBlock.Header(6, "Nivel 6"), blocks[6])
        assertEquals(MarkdownBlock.HorizontalRule, blocks[7])
        assertEquals(MarkdownBlock.Paragraph("***Texto en negrita y cursiva***"), blocks[8])
        assertEquals(MarkdownBlock.BulletItem(text = "Viñeta normal", level = 0, checked = null), blocks[9])
    }

    @Test
    fun parseBlocks_extractsIndentedListsAndCheckboxes() {
        val markdown = """
            - [ ] Comprar libro Goshuincho
            - [x] Llevar monedas de 100¥ y 500¥
              - [X] Separar para templos de Kioto
              - Subpunto de nivel 1
                * Subpunto de nivel 2
            1. Paso principal
              1. Subpaso indentado
                2. Subpaso nivel 2
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(8, blocks.size)
        assertEquals(MarkdownBlock.BulletItem("Comprar libro Goshuincho", level = 0, checked = false), blocks[0])
        assertEquals(MarkdownBlock.BulletItem("Llevar monedas de 100¥ y 500¥", level = 0, checked = true), blocks[1])
        assertEquals(MarkdownBlock.BulletItem("Separar para templos de Kioto", level = 1, checked = true), blocks[2])
        assertEquals(MarkdownBlock.BulletItem("Subpunto de nivel 1", level = 1, checked = null), blocks[3])
        assertEquals(MarkdownBlock.BulletItem("Subpunto de nivel 2", level = 2, checked = null), blocks[4])
        assertEquals(MarkdownBlock.NumberedItem(number = 1, text = "Paso principal", level = 0), blocks[5])
        assertEquals(MarkdownBlock.NumberedItem(number = 1, text = "Subpaso indentado", level = 1), blocks[6])
        assertEquals(MarkdownBlock.NumberedItem(number = 2, text = "Subpaso nivel 2", level = 2), blocks[7])
    }

    @Test
    fun parseBlocks_extractsBlockquotesAndMarkdownTables() {
        val markdown = """
            > **Consejo JR:** Reserva espacio para maletas >160 cm.
            > Sin reserva previa la tarifa a bordo es de ¥1,000.

            | Tren | Ruta | Localizador |
            | :--- | :---: | ---: |
            | **Nozomi 21** | Tokio → Kioto | `WEHI1SHC` |
            | Hikari 503 | Kioto → Shin-Osaka | `JRPASS01` |
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Blockquote)
        val quote = blocks[0] as MarkdownBlock.Blockquote
        assertEquals(
            "**Consejo JR:** Reserva espacio para maletas >160 cm.\nSin reserva previa la tarifa a bordo es de ¥1,000.",
            quote.text
        )

        assertTrue(blocks[1] is MarkdownBlock.Table)
        val table = blocks[1] as MarkdownBlock.Table
        assertEquals(listOf("Tren", "Ruta", "Localizador"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("**Nozomi 21**", "Tokio → Kioto", "`WEHI1SHC`"), table.rows[0])
        assertEquals(listOf("Hikari 503", "Kioto → Shin-Osaka", "`JRPASS01`"), table.rows[1])
    }

    @Test
    fun parseBlocks_parsesRealGoshuinAndLufthansaShinkansenGuide() {
        val markdown = """
            ### Sellos *«Goshuin»* y Vuelos Lufthansa
            ---
            #### 1. Etiqueta en Templos y Santuarios
            > Entrega tu cuaderno abierto en la página limpia antes de rezar.
            - Reglas esenciales:
              - Precio habitual: **¥300 - ¥500** en efectivo.
              - Nunca uses un cuaderno de *Eki Stamps* para *«Goshuin»*.
            1. Verificar vuelo LH1125 y localizador `WEHI1SHC`
              1. Facturación online 23h antes en [Lufthansa](https://www.lufthansa.com)
              2. Enlazar con Shinkansen en Shinagawa
        """.trimIndent()

        val blocks = MarkdownTextParser.parseBlocks(markdown)

        assertTrue(blocks.any { it is MarkdownBlock.Header && it.level == 3 && it.text.contains("*«Goshuin»*") })
        assertTrue(blocks.any { it is MarkdownBlock.HorizontalRule })
        assertTrue(blocks.any { it is MarkdownBlock.Header && it.level == 4 })
        assertTrue(blocks.any { it is MarkdownBlock.Blockquote })
        assertTrue(blocks.any { it is MarkdownBlock.BulletItem && it.level == 1 && it.text.contains("*«Goshuin»*") })
        assertTrue(blocks.any { it is MarkdownBlock.NumberedItem && it.level == 0 && it.text.contains("`WEHI1SHC`") })
        assertTrue(blocks.any { it is MarkdownBlock.NumberedItem && it.level == 1 && it.text.contains("[Lufthansa]") })
    }

    @Test
    fun parseInline_handlesBoldItalicLinksGuillemetsAndCode() {
        val raw = "Sello *«Goshuin»*, código `WEHI1SHC`, aviso ***muy urgente***, _caligrafía_ y [SmartEX](https://smart-ex.jp)."
        val spans = MarkdownTextParser.parseInline(raw)

        val goshuin = spans.first { it.text == "«Goshuin»" }
        assertTrue(goshuin.isItalic)
        assertFalse(goshuin.isBold)
        assertNull(goshuin.linkUrl)

        val code = spans.first { it.text == "WEHI1SHC" }
        assertTrue(code.isCode)

        val boldItalic = spans.first { it.text == "muy urgente" }
        assertTrue(boldItalic.isBold)
        assertTrue(boldItalic.isItalic)

        val underscoreItalic = spans.first { it.text == "caligrafía" }
        assertTrue(underscoreItalic.isItalic)

        val link = spans.first { it.text == "SmartEX" }
        assertEquals("https://smart-ex.jp", link.linkUrl)
    }

    @Test
    fun parseInline_supportsNestedItalicInsideBoldAndFormattedLinks() {
        val raw = "Reserva **asiento en *SmartEX* hoy** o visita [**Japón Oficial**](https://www.japan.travel)."
        val spans = MarkdownTextParser.parseInline(raw)

        val nestedSmartEx = spans.first { it.text == "SmartEX" }
        assertTrue(nestedSmartEx.isBold)
        assertTrue(nestedSmartEx.isItalic)

        val boldLink = spans.first { it.text == "Japón Oficial" }
        assertTrue(boldLink.isBold)
        assertEquals("https://www.japan.travel", boldLink.linkUrl)
    }

    @Test
    fun extractPlainTextPreview_stripsMarkdownSyntaxAndSkipsHeadersWhenBodyExists() {
        val markdown = """
            # Guía de Sellos Goshuin y Shinkansen
            ---
            ### 1. ¿Qué es el Goshuin?
            > El *«Goshuin»* es un sello sagrado (**¥300-¥500**).
            - [x] Código reserva `WEHI1SHC` en [Lufthansa](https://www.lufthansa.com)
            - [ ] Revisar horario en _Tokio_
            | Tren | Salida |
            |---|---|
            | Nozomi | 09:00 |
            ```text
            Andén 16
            ```
        """.trimIndent()

        val preview = MarkdownTextParser.extractPlainTextPreview(markdown, maxChars = 220)

        assertFalse("Should skip # headers when body exists: $preview", preview.contains("Guía de Sellos Goshuin"))
        assertFalse("Should skip ### headers when body exists: $preview", preview.contains("¿Qué es el Goshuin?"))
        assertFalse("Should not contain raw markdown symbols: $preview", preview.contains("**") || preview.contains("`") || preview.contains("---") || preview.contains("|") || preview.contains("[x]") || preview.contains("https://"))
        assertTrue(preview.contains("El «Goshuin» es un sello sagrado (¥300-¥500)."))
        assertTrue(preview.contains("Código reserva WEHI1SHC en Lufthansa"))
        assertTrue(preview.contains("Nozomi 09:00"))
        assertTrue(preview.length <= 220)
    }

    @Test
    fun extractPlainTextPreview_usesHeadersWhenOnlyHeadersExistAndTruncatesCleanly() {
        val headersOnly = """
            # Reserva Lufthansa `WEHI1SHC`
            ### Salida Terminal T4S
        """.trimIndent()

        val headerPreview = MarkdownTextParser.extractPlainTextPreview(headersOnly)
        assertEquals("Reserva Lufthansa WEHI1SHC Salida Terminal T4S", headerPreview)

        val longMarkdown = "Viaje en **Shinkansen Nozomi** desde Tokio hasta Kioto con reserva de equipaje sobredimensionado y tarjeta Suica cargada."
        val truncated = MarkdownTextParser.extractPlainTextPreview(longMarkdown, maxChars = 40)
        assertTrue(truncated.endsWith("…"))
        assertTrue(truncated.length <= 40)
        assertFalse(truncated.contains("**"))
    }
}
