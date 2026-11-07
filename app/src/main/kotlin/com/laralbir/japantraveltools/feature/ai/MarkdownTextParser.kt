package com.laralbir.japantraveltools.feature.ai

sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class BulletItem(
        val text: String,
        val level: Int = 0,
        val checked: Boolean? = null
    ) : MarkdownBlock
    data class NumberedItem(
        val number: Int,
        val text: String,
        val level: Int = 0
    ) : MarkdownBlock
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

data class InlineSpan(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isCode: Boolean = false,
    val linkUrl: String? = null
)

object MarkdownTextParser {

    private val HEADER_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
    private val HORIZONTAL_RULE_REGEX = Regex("""^(?:-\s*){3,}$|^(?:\*\s*){3,}$|^(?:_\s*){3,}$""")
    private val TABLE_SEPARATOR_REGEX = Regex("""^\|[\s:-]+(\|[\s:-]+)*\|?$""")
    private val CHECKBOX_BODY_REGEX = Regex("""^\[([ xX])\](?:\s+(.*)|$)""")
    private val NUMBERED_ITEM_REGEX = Regex("""^(\d+)\.\s+(.*)$""")
    private val LINK_REGEX = Regex("""\[([^\[\]\n]+)\]\(((?:[^()\s]|\([^()\s]*\))+)\)""")

    private val INLINE_TOKEN_REGEX = Regex(
        listOf(
            """`([^`\n]+)`""",
            """\[([^\[\]\n]+)\]\(((?:[^()\s]|\([^()\s]*\))+)\)""",
            """\*\*\*([^\s*](?:[^\n]*?[^\s*])?)\*\*\*""",
            """(?<![\p{L}\p{N}])___([^\s_](?:[^\n]*?[^\s_])?)___(?![\p{L}\p{N}])""",
            """\*\*([^\s](?:[^\n]*?[^\s])?)\*\*""",
            """(?<![\p{L}\p{N}])__([^\s_](?:[^\n]*?[^\s_])?)__(?![\p{L}\p{N}])""",
            """\*([^\s*](?:[^*\n]*[^\s*])?)\*""",
            """(?<![\p{L}\p{N}])_([^\s_](?:[^_\n]*[^\s_])?)_(?![\p{L}\p{N}])"""
        ).joinToString("|")
    )

    fun parseBlocks(rawMarkdown: String): List<MarkdownBlock> {
        val lines = rawMarkdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var inCodeBlock = false
        var codeBlockLang: String? = null
        val codeBlockLines = mutableListOf<String>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val leadingSpaces = line
                .takeWhile { it == ' ' || it == '\t' }
                .sumOf { if (it == '\t') 2 else 1 }
            val indentLevel = leadingSpaces / 2
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
                i++
                continue
            }

            if (trimmed.startsWith("```")) {
                inCodeBlock = true
                codeBlockLang = trimmed.removePrefix("```").trim().ifEmpty { null }
                codeBlockLines.clear()
                i++
                continue
            }

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Horizontal rules (checked BEFORE bullet items or ***bold***)
            if (HORIZONTAL_RULE_REGEX.matches(trimmed)) {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // Headers (# .. ######)
            if (trimmed.startsWith("#")) {
                val match = HEADER_REGEX.find(trimmed)
                if (match != null) {
                    val level = match.groupValues[1].length
                    val text = match.groupValues[2]
                    blocks.add(MarkdownBlock.Header(level = level, text = text))
                    i++
                    continue
                }
            }

            // Blockquotes (consecutive '>' lines joined cleanly)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size) {
                    val candidateTrimmed = lines[i].trim()
                    if (!candidateTrimmed.startsWith(">")) break
                    val stripped = candidateTrimmed
                        .removePrefix(">")
                        .let { if (it.startsWith(" ")) it.substring(1) else it }
                        .trimEnd()
                    quoteLines.add(stripped)
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(text = quoteLines.joinToString("\n")))
                continue
            }

            // Markdown Tables (header row with '|' followed by separator row)
            if (trimmed.startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1].trim())) {
                val headers = parseTableRow(trimmed)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowTrimmed = lines[i].trim()
                    if (!rowTrimmed.startsWith("|")) break
                    if (!isTableSeparator(rowTrimmed)) {
                        rows.add(parseTableRow(rowTrimmed))
                    }
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers = headers, rows = rows))
                continue
            }

            // Bullet list items (*, -, •) including task list checkboxes (- [ ] / - [x])
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                val bulletBody = trimmed.substring(2).trim()
                val checkboxMatch = CHECKBOX_BODY_REGEX.find(bulletBody)
                if (checkboxMatch != null) {
                    val isChecked = checkboxMatch.groupValues[1].equals("x", ignoreCase = true)
                    val itemText = checkboxMatch.groupValues[2].trim()
                    blocks.add(
                        MarkdownBlock.BulletItem(
                            text = itemText,
                            level = indentLevel,
                            checked = isChecked
                        )
                    )
                } else {
                    blocks.add(
                        MarkdownBlock.BulletItem(
                            text = bulletBody,
                            level = indentLevel,
                            checked = null
                        )
                    )
                }
                i++
                continue
            }

            // Numbered list items (1. 2. etc.) with indentation support
            val numberedMatch = NUMBERED_ITEM_REGEX.find(trimmed)
            if (numberedMatch != null) {
                val number = numberedMatch.groupValues[1].toIntOrNull() ?: 1
                val text = numberedMatch.groupValues[2]
                blocks.add(
                    MarkdownBlock.NumberedItem(
                        number = number,
                        text = text,
                        level = indentLevel
                    )
                )
                i++
                continue
            }

            // Standard Paragraph
            blocks.add(MarkdownBlock.Paragraph(text = trimmed))
            i++
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
     * Parse inline markdown tokens (`code`, [label](url), ***bold italic***, **bold**, *italic*, _italic_)
     * into a list of [InlineSpan].
     */
    fun parseInline(text: String): List<InlineSpan> {
        if (text.isEmpty()) return emptyList()

        val spans = mutableListOf<InlineSpan>()
        var lastIndex = 0

        INLINE_TOKEN_REGEX.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                spans.add(InlineSpan(text = text.substring(lastIndex, start)))
            }

            val groups = match.groups
            val codeInner = groups[1]?.value
            val linkLabel = groups[2]?.value
            val linkUrl = groups[3]?.value
            val boldItalicInner = groups[4]?.value ?: groups[5]?.value
            val boldInner = groups[6]?.value ?: groups[7]?.value
            val italicInner = groups[8]?.value ?: groups[9]?.value

            when {
                codeInner != null -> {
                    spans.add(InlineSpan(text = codeInner, isCode = true))
                }
                linkLabel != null && linkUrl != null -> {
                    val innerSpans = parseInline(linkLabel)
                    if (innerSpans.isEmpty()) {
                        spans.add(InlineSpan(text = linkLabel, linkUrl = linkUrl))
                    } else {
                        innerSpans.forEach { inner ->
                            spans.add(inner.copy(linkUrl = linkUrl))
                        }
                    }
                }
                boldItalicInner != null -> {
                    val innerSpans = parseInline(boldItalicInner)
                    if (innerSpans.isEmpty()) {
                        spans.add(InlineSpan(text = boldItalicInner, isBold = true, isItalic = true))
                    } else {
                        innerSpans.forEach { span ->
                            spans.add(span.copy(isBold = true, isItalic = true))
                        }
                    }
                }
                boldInner != null && boldInner.any { it != '*' } -> {
                    val innerSpans = parseInline(boldInner)
                    if (innerSpans.isEmpty()) {
                        spans.add(InlineSpan(text = boldInner, isBold = true))
                    } else {
                        innerSpans.forEach { span ->
                            spans.add(span.copy(isBold = true))
                        }
                    }
                }
                italicInner != null -> {
                    val innerSpans = parseInline(italicInner)
                    if (innerSpans.isEmpty()) {
                        spans.add(InlineSpan(text = italicInner, isItalic = true))
                    } else {
                        innerSpans.forEach { span ->
                            spans.add(span.copy(isItalic = true))
                        }
                    }
                }
                else -> {
                    spans.add(InlineSpan(text = match.value))
                }
            }
            lastIndex = end
        }

        if (lastIndex < text.length) {
            spans.add(InlineSpan(text = text.substring(lastIndex)))
        }

        return spans
    }

    /**
     * Extracts a clean plain-text preview from [rawMarkdown] suitable for compact summary cards,
     * stripping structural and inline Markdown tokens and truncating cleanly at [maxChars].
     */
    fun extractPlainTextPreview(rawMarkdown: String, maxChars: Int = 220): String {
        if (rawMarkdown.isBlank() || maxChars <= 0) return ""

        val cleanedHeaderLines = mutableListOf<String>()
        val cleanedBodyLines = mutableListOf<String>()

        for (rawLine in rawMarkdown.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("```")) continue
            if (HORIZONTAL_RULE_REGEX.matches(trimmed)) continue
            if (isTableSeparator(trimmed)) continue

            val headerMatch = HEADER_REGEX.find(trimmed)
            if (headerMatch != null) {
                val cleanedHeader = cleanInlineAndBlockSyntax(headerMatch.groupValues[2])
                if (cleanedHeader.isNotEmpty()) {
                    cleanedHeaderLines.add(cleanedHeader)
                }
            } else {
                val cleanedBody = cleanInlineAndBlockSyntax(trimmed)
                if (cleanedBody.isNotEmpty()) {
                    cleanedBodyLines.add(cleanedBody)
                }
            }
        }

        val chosenLines = if (cleanedBodyLines.isNotEmpty()) cleanedBodyLines else cleanedHeaderLines
        val joined = chosenLines.joinToString(" ").replace(Regex("""\s+"""), " ").trim()
        if (joined.isEmpty()) return ""
        if (maxChars == 1) return if (joined.length <= 1) joined else "…"
        if (joined.length <= maxChars) return joined

        val truncated = joined.take(maxChars - 1).trimEnd()
        return "${truncated}…"
    }

    private fun isTableSeparator(trimmedLine: String): Boolean {
        return trimmedLine.contains('-') && TABLE_SEPARATOR_REGEX.matches(trimmedLine)
    }

    private fun parseTableRow(trimmedLine: String): List<String> {
        val stripped = trimmedLine.removePrefix("|").removeSuffix("|")
        return stripped.split("|").map { it.trim() }
    }

    private fun cleanInlineAndBlockSyntax(line: String): String {
        var current = line.trim()

        // Strip leading '>' blockquote markers
        while (current.startsWith(">")) {
            current = current.removePrefix(">").trimStart()
        }

        // Strip any nested '#' header prefix (e.g. "> ### Aviso")
        current = current.replace(Regex("""^#{1,6}\s+"""), "")

        // Strip leading task list, bullet, or numbered markers
        current = current
            .replace(Regex("""^(?:[-*•])\s+\[[ xX]\]\s*"""), "")
            .replace(Regex("""^(?:[-*•])\s+"""), "")
            .replace(Regex("""^\d+\.\s+"""), "")

        // Strip table pipes '|'
        if (current.contains('|')) {
            current = current
                .removePrefix("|")
                .removeSuffix("|")
                .replace("|", " ")
        }

        // Convert [label](url) -> label before stripping inline emphasis
        current = LINK_REGEX.replace(current, "$1")

        // Strip inline code backticks
        current = current.replace(Regex("""`([^`]+)`"""), "$1").replace("`", "")

        // Strip bold/italic delimiters (***, **, *, ___, __, _)
        current = current
            .replace(Regex("""\*\*\*([^\s*](?:[^\n]*?[^\s*])?)\*\*\*"""), "$1")
            .replace(Regex("""(?<![\p{L}\p{N}])___([^\s_](?:[^\n]*?[^\s_])?)___(?![\p{L}\p{N}])"""), "$1")
            .replace(Regex("""\*\*([^\s](?:[^\n]*?[^\s])?)\*\*"""), "$1")
            .replace(Regex("""(?<![\p{L}\p{N}])__([^\s_](?:[^\n]*?[^\s_])?)__(?![\p{L}\p{N}])"""), "$1")
            .replace(Regex("""\*([^\s*](?:[^*\n]*[^\s*])?)\*"""), "$1")
            .replace(Regex("""(?<![\p{L}\p{N}])_([^\s_](?:[^_\n]*[^\s_])?)_(?![\p{L}\p{N}])"""), "$1")
            .replace("**", "")

        return current.replace(Regex("""\s+"""), " ").trim()
    }
}
