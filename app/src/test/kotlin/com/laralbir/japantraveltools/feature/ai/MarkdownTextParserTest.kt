package com.laralbir.japantraveltools.feature.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
