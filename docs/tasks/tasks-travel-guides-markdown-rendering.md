# Tareas: Renderizado Nativo de Markdown en Guías de Viaje y Vista Previa en Editor

- **ID**: `TASKS-TRAVEL-GUIDES-MARKDOWN`
- **Especificación**: [`docs/specs/03-documents-and-id-vault-spec.md`](../specs/03-documents-and-id-vault-spec.md) (`RF-03.6`)
- **Análisis Técnico**: [`docs/analysis/11-travel-guides-markdown-rendering-analysis.md`](../analysis/11-travel-guides-markdown-rendering-analysis.md) (`ANALYSIS-11`)
- **Ciclo TDD**: Red ➔ Green ➔ Refactor

---

## 1. Tareas de Testing Unitario (RED)
- [ ] `T-11.1`: Ampliar `MarkdownTextParserTest.kt` con pruebas unitarias para encabezados `H1`–`H6`, separadores horizontales (`---`, `***`, `___`) y bloques de cita (`> ` simples y multilínea).
- [ ] `T-11.2`: Añadir pruebas unitarias en `MarkdownTextParserTest.kt` para listas con sublistas indentadas (`BulletItem.level`, `NumberedItem.level`) y casillas de verificación (`- [ ]`, `- [x]`, `- [X]`).
- [ ] `T-11.3`: Añadir pruebas unitarias en `MarkdownTextParserTest.kt` para tablas Markdown (`| Cabecera | ... |` con fila divisoria `| --- | :---: | ---: |` y filas de celdas).
- [ ] `T-11.4`: Añadir pruebas unitarias en `MarkdownTextParserTest.kt` para formato inline extendido (`***negrita cursiva***`, `__negrita__`, enlaces `[texto](url)`) y para `MarkdownTextParser.extractPlainTextPreview(...)` asegurando ausencia total de sintaxis Markdown cruda en extractos.

---

## 2. Tareas de Dominio y Renderizador Compose (GREEN)
- [ ] `T-11.5`: Extender `MarkdownBlock`, `InlineSpan` y `MarkdownTextParser.kt` implementando `TaskListItem`, `Blockquote`, `HorizontalRule`, `Table`, niveles de indentación, enlaces `[texto](url)`, negrita+cursiva y `extractPlainTextPreview(...)`.
- [ ] `T-11.6`: Actualizar `MarkdownTextRenderer.kt` (`@Composable fun MarkdownText`) con escala tipográfica parametrizable (`baseFontSize`, `baseLineHeight`) y renderizado Material 3 para `H1`–`H6`, sublistas indentadas, casillas de verificación, citas con barra lateral, separadores horizontales, tablas con scroll horizontal y enlaces interactivos seguros (`LinkAnnotation.Url`).

---

## 3. Tareas de Integración UI en Guías y Mapa (GREEN)
- [ ] `T-11.7`: Integrar `MarkdownText` en `FullGuideReaderDialog` (`MainScreen.kt`) sustituyendo el `Text` plano para renderizar las guías enriquecidas tanto desde la pestaña de Guías como desde el Mapa del Día.
- [ ] `T-11.8`: Aplicar `MarkdownTextParser.extractPlainTextPreview(...)` en las tarjetas del listado de Guías (`MainScreen.kt`) y en las tarjetas de guías vinculadas del Mapa del Día (`StopDetailMapModalSheet.kt`).
- [ ] `T-11.9`: Añadir pestañas conmutables `✍️ Editar` / `👁️ Vista previa` en `GuideEditorModalSheet` (`MainScreen.kt`) para previsualizar el Markdown en vivo antes de guardar.

---

## 4. Refactorización, Verificación y Documentación (REFACTOR)
- [ ] `T-11.10`: Ejecutar `./gradlew testDebugUnitTest` verificando el 100% de los tests unitarios en verde y sincronizar `docs/index.md`, `docs/user-guide.md`, `README.md` y `CHANGELOG.md` al cerrar la implementación.
