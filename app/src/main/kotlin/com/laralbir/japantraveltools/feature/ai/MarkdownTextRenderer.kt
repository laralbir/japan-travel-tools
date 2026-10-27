package com.laralbir.japantraveltools.feature.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = remember(markdown) { MarkdownTextParser.parseBlocks(markdown) }
    val codeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val annotated = remember(block.text, codeBg) {
                        buildAnnotatedText(MarkdownTextParser.parseInline(block.text), codeBg)
                    }
                    val (fontSize, fontWeight, color) = when (block.level) {
                        1 -> Triple(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.primary)
                        2 -> Triple(16.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface)
                        else -> Triple(14.5.sp, FontWeight.SemiBold, MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = annotated,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    val annotated = remember(block.text, codeBg) {
                        buildAnnotatedText(MarkdownTextParser.parseInline(block.text), codeBg)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    val annotated = remember(block.text, codeBg) {
                        buildAnnotatedText(MarkdownTextParser.parseInline(block.text), codeBg)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, end = 6.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, codeBg) {
                        buildAnnotatedText(MarkdownTextParser.parseInline(block.text), codeBg)
                    }
                    Text(
                        text = annotated,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedText(spans: List<InlineSpan>, codeBgColor: Color): AnnotatedString {
    return buildAnnotatedString {
        for (span in spans) {
            val spanStyle = SpanStyle(
                fontWeight = if (span.isBold) FontWeight.Bold else null,
                fontStyle = if (span.isItalic) FontStyle.Italic else null,
                fontFamily = if (span.isCode) FontFamily.Monospace else null,
                background = if (span.isCode) codeBgColor else Color.Unspecified
            )
            withStyle(spanStyle) {
                append(span.text)
            }
        }
    }
}
