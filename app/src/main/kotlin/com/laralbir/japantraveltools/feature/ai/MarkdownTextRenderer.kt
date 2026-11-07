package com.laralbir.japantraveltools.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    baseFontSize: TextUnit = 14.5.sp,
    baseLineHeight: TextUnit = 21.sp
) {
    val blocks = remember(markdown) { MarkdownTextParser.parseBlocks(markdown) }
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val onOpenLink: (String) -> Unit = remember(uriHandler) {
        { url -> runCatching { uriHandler.openUri(url) } }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val annotated = remember(block.text, codeBg, linkColor, onOpenLink) {
                        buildAnnotatedText(
                            spans = MarkdownTextParser.parseInline(block.text),
                            codeBgColor = codeBg,
                            linkColor = linkColor,
                            onOpenLink = onOpenLink
                        )
                    }
                    val (fontSize, fontWeight, color) = when (block.level) {
                        1 -> Triple((baseFontSize.value + 4.5f).sp, FontWeight.Bold, MaterialTheme.colorScheme.primary)
                        2 -> Triple((baseFontSize.value + 2.5f).sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface)
                        3 -> Triple((baseFontSize.value + 1f).sp, FontWeight.Bold, MaterialTheme.colorScheme.primary)
                        else -> Triple(baseFontSize, FontWeight.Bold, MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = annotated,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.32f).sp,
                        fontWeight = fontWeight,
                        color = color,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                is MarkdownBlock.Blockquote -> {
                    val annotated = remember(block.text, codeBg, linkColor, onOpenLink) {
                        buildAnnotatedText(
                            spans = MarkdownTextParser.parseInline(block.text),
                            codeBgColor = codeBg,
                            linkColor = linkColor,
                            onOpenLink = onOpenLink
                        )
                    }
                    val accentColor = MaterialTheme.colorScheme.primary
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val barWidth = 3.dp.toPx()
                                    drawRect(
                                        color = accentColor,
                                        topLeft = Offset.Zero,
                                        size = Size(width = barWidth, height = size.height)
                                    )
                                }
                                .padding(start = 13.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = annotated,
                                fontSize = baseFontSize,
                                lineHeight = baseLineHeight,
                                color = textColor
                            )
                        }
                    }
                }

                is MarkdownBlock.BulletItem -> {
                    val annotated = remember(block.text, codeBg, linkColor, onOpenLink) {
                        buildAnnotatedText(
                            spans = MarkdownTextParser.parseInline(block.text),
                            codeBgColor = codeBg,
                            linkColor = linkColor,
                            onOpenLink = onOpenLink
                        )
                    }
                    val markerGlyph = when (block.checked) {
                        true -> "☑️"
                        false -> "⬜"
                        null -> if (block.level == 0) "•" else "◦"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.level * 14).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = markerGlyph,
                            fontSize = if (block.checked != null) baseFontSize else (baseFontSize.value + 0.5f).sp,
                            lineHeight = baseLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = baseFontSize,
                            lineHeight = baseLineHeight,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    val annotated = remember(block.text, codeBg, linkColor, onOpenLink) {
                        buildAnnotatedText(
                            spans = MarkdownTextParser.parseInline(block.text),
                            codeBgColor = codeBg,
                            linkColor = linkColor,
                            onOpenLink = onOpenLink
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.level * 14).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontSize = baseFontSize,
                            lineHeight = baseLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, end = 6.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = baseFontSize,
                            lineHeight = baseLineHeight,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Table -> {
                    MarkdownTableBlock(
                        table = block,
                        textColor = textColor,
                        baseFontSize = baseFontSize,
                        baseLineHeight = baseLineHeight,
                        codeBg = codeBg,
                        linkColor = linkColor,
                        onOpenLink = onOpenLink
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!block.language.isNullOrBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = block.code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (baseFontSize.value - 2f).coerceAtLeast(11f).sp,
                                lineHeight = (baseLineHeight.value - 3f).coerceAtLeast(16f).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, codeBg, linkColor, onOpenLink) {
                        buildAnnotatedText(
                            spans = MarkdownTextParser.parseInline(block.text),
                            codeBgColor = codeBg,
                            linkColor = linkColor,
                            onOpenLink = onOpenLink
                        )
                    }
                    Text(
                        text = annotated,
                        fontSize = baseFontSize,
                        lineHeight = baseLineHeight,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    table: MarkdownBlock.Table,
    textColor: Color,
    baseFontSize: TextUnit,
    baseLineHeight: TextUnit,
    codeBg: Color,
    linkColor: Color,
    onOpenLink: (String) -> Unit
) {
    val columnCount = maxOf(
        table.headers.size,
        table.rows.maxOfOrNull { it.size } ?: 0
    )
    if (columnCount == 0) return

    val allRows = remember(table) {
        buildList {
            add(table.headers)
            addAll(table.rows)
        }
    }
    val rowCount = allRows.size
    val cellFontSize = (baseFontSize.value - 1f).coerceAtLeast(12f).sp
    val cellLineHeight = (baseLineHeight.value - 2f).coerceAtLeast(17f).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Layout(
                content = {
                    // Index 0: Header row background
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant))

                    // Indices 1 .. rowCount - 1: Row horizontal dividers
                    repeat(rowCount - 1) {
                        Box(
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                            )
                        )
                    }

                    // Indices rowCount .. end: Row-major cells
                    allRows.forEachIndexed { rowIndex, rowCells ->
                        val isHeader = rowIndex == 0
                        for (colIndex in 0 until columnCount) {
                            val rawCell = rowCells.getOrElse(colIndex) { "" }
                            val annotatedCell = remember(rawCell, codeBg, linkColor, onOpenLink) {
                                buildAnnotatedText(
                                    spans = MarkdownTextParser.parseInline(rawCell),
                                    codeBgColor = codeBg,
                                    linkColor = linkColor,
                                    onOpenLink = onOpenLink
                                )
                            }
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = annotatedCell,
                                    fontSize = cellFontSize,
                                    lineHeight = cellLineHeight,
                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHeader) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        textColor
                                    }
                                )
                            }
                        }
                    }
                }
            ) { measurables, _ ->
                val minCellWidthPx = 56.dp.roundToPx()
                val maxCellWidthPx = 260.dp.roundToPx()
                val dividerPx = 1.dp.roundToPx().coerceAtLeast(1)

                val colWidths = IntArray(columnCount)
                val rowHeights = IntArray(rowCount)
                val cellConstraints = Constraints(
                    minWidth = minCellWidthPx,
                    maxWidth = maxCellWidthPx,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity
                )

                val cellPlaceables = Array(rowCount) { rowIndex ->
                    Array(columnCount) { colIndex ->
                        val measurableIndex = rowCount + (rowIndex * columnCount) + colIndex
                        val placeable = measurables[measurableIndex].measure(cellConstraints)
                        if (placeable.width > colWidths[colIndex]) {
                            colWidths[colIndex] = placeable.width
                        }
                        if (placeable.height > rowHeights[rowIndex]) {
                            rowHeights[rowIndex] = placeable.height
                        }
                        placeable
                    }
                }

                val totalWidth = colWidths.sum()
                val totalHeight = rowHeights.sum() + ((rowCount - 1) * dividerPx)

                val headerBgPlaceable = measurables[0].measure(
                    Constraints.fixed(totalWidth, rowHeights[0])
                )
                val dividerPlaceables = (1 until rowCount).map { idx ->
                    measurables[idx].measure(Constraints.fixed(totalWidth, dividerPx))
                }

                layout(totalWidth, totalHeight) {
                    headerBgPlaceable.placeRelative(0, 0)

                    var currentY = 0
                    for (rowIndex in 0 until rowCount) {
                        var currentX = 0
                        val rowHeight = rowHeights[rowIndex]
                        for (colIndex in 0 until columnCount) {
                            val cellPlaceable = cellPlaceables[rowIndex][colIndex]
                            val centeredY = currentY + ((rowHeight - cellPlaceable.height) / 2)
                            cellPlaceable.placeRelative(currentX, centeredY)
                            currentX += colWidths[colIndex]
                        }
                        currentY += rowHeight
                        if (rowIndex < rowCount - 1) {
                            dividerPlaceables[rowIndex].placeRelative(0, currentY)
                            currentY += dividerPx
                        }
                    }
                }
            }
        }
    }
}

private fun buildAnnotatedText(
    spans: List<InlineSpan>,
    codeBgColor: Color,
    linkColor: Color,
    onOpenLink: (String) -> Unit
): AnnotatedString {
    return buildAnnotatedString {
        for (span in spans) {
            val url = span.linkUrl?.takeIf { it.isNotBlank() }
            val isLink = url != null
            val spanStyle = SpanStyle(
                color = if (isLink) linkColor else Color.Unspecified,
                fontWeight = when {
                    span.isBold -> FontWeight.Bold
                    isLink -> FontWeight.Medium
                    else -> null
                },
                fontStyle = if (span.isItalic) FontStyle.Italic else null,
                fontFamily = if (span.isCode) FontFamily.Monospace else null,
                background = if (span.isCode) codeBgColor else Color.Unspecified,
                textDecoration = if (isLink) TextDecoration.Underline else null
            )
            if (url != null) {
                val linkAnnotation = LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(style = spanStyle),
                    linkInteractionListener = LinkInteractionListener {
                        onOpenLink(url)
                    }
                )
                withLink(linkAnnotation) {
                    withStyle(spanStyle) {
                        append(span.text)
                    }
                }
            } else {
                withStyle(spanStyle) {
                    append(span.text)
                }
            }
        }
    }
}
