package com.laralbir.japantraveltools.feature.ai

sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val text: String, val level: Int = 0) : MarkdownBlock
    data class NumberedItem(val number: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

data class InlineSpan(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isCode: Boolean = false
)

object MarkdownTextParser {

    fun parseBlocks(rawMarkdown: String): List<MarkdownBlock> {
        val lines = rawMarkdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var inCodeBlock = false
        var codeBlockLang: String? = null
        val codeBlockLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            if (inCodeBlock) {
                if (trimmed.startsWith("```")) {
                    blocks.add(
                        MarkdownBlock.CodeBlock(
                            code = codeBlockLines.joinToString("\n"),
                            language = codeBlockLang
                        )
                    )
                    inCodeBlock = false
                    codeBlockLang = null
                    codeBlockLines.clear()
                } else {
                    codeBlockLines.add(line)
                }
                continue
            }

            if (trimmed.startsWith("```")) {
                inCodeBlock = true
                codeBlockLang = trimmed.removePrefix("```").trim().ifEmpty { null }
                codeBlockLines.clear()
                continue
            }

            if (trimmed.isEmpty()) {
                continue
            }

            // Headers
            if (trimmed.startsWith("#")) {
                val match = """^(#{1,6})\s+(.*)$""".toRegex().find(trimmed)
                if (match != null) {
                    val level = match.groupValues[1].length
                    val text = match.groupValues[2]
                    blocks.add(MarkdownBlock.Header(level = level, text = text))
                    continue
                }
            }

            // Bullet list items (*, -, •)
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                val bulletText = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.BulletItem(text = bulletText))
                continue
            }

            // Numbered list items (1. 2. etc.)
            val numberedMatch = """^(\d+)\.\s+(.*)$""".toRegex().find(trimmed)
            if (numberedMatch != null) {
                val number = numberedMatch.groupValues[1].toIntOrNull() ?: 1
                val text = numberedMatch.groupValues[2]
                blocks.add(MarkdownBlock.NumberedItem(number = number, text = text))
                continue
            }

            // Standard Paragraph
            blocks.add(MarkdownBlock.Paragraph(text = trimmed))
        }

        if (inCodeBlock) {
            blocks.add(
                MarkdownBlock.CodeBlock(
                    code = codeBlockLines.joinToString("\n"),
                    language = codeBlockLang
                )
            )
        }

        return blocks
    }

    /**
     * Parse inline markdown tokens (**bold**, *italic*, `code`, _italic_) into a list of InlineSpan.
     */
    fun parseInline(text: String): List<InlineSpan> {
        if (text.isEmpty()) return emptyList()

        val spans = mutableListOf<InlineSpan>()
        val pattern = """(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|_[^_]+_)""".toRegex()
        var lastIndex = 0

        pattern.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                spans.add(InlineSpan(text = text.substring(lastIndex, start)))
            }

            val token = match.value
            when {
                token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                    spans.add(InlineSpan(text = token.substring(1, token.length - 1), isCode = true))
                }
                token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                    val inner = token.substring(2, token.length - 2)
                    spans.add(InlineSpan(text = inner, isBold = true))
                }
                token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    spans.add(InlineSpan(text = inner, isItalic = true))
                }
                token.startsWith("_") && token.endsWith("_") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    spans.add(InlineSpan(text = inner, isItalic = true))
                }
                else -> {
                    spans.add(InlineSpan(text = token))
                }
            }
            lastIndex = end
        }

        if (lastIndex < text.length) {
            spans.add(InlineSpan(text = text.substring(lastIndex)))
        }

        return spans
    }
}
