# Documentación de Japan Travel Tools

Bienvenido a la documentación oficial del proyecto **Japan Travel Tools**. Toda la documentación técnica, especificaciones SDD, análisis arquitectónico, planificación de tareas TDD y la guía de usuario se mantienen en español y actualizadas en cada iteración del repositorio.

---

## Estructura de la Documentación

### 1. Especificaciones Funcionales (SDD — `/docs/specs/`)
Documentos de especificación que definen el **qué** antes de diseñar o escribir cualquier línea de código:
- [**00 — Especificación Core: Idioma, Tema, Persistencia y Offline-First**](specs/00-core-configuration-spec.md)
- [**01 — Especificación: Relojes de Viaje y Ubicación (Hora Local y País Origen)**](specs/01-world-clock-spec.md)
- [**02 — Especificación: Conversor de Divisa Offline-First (EUR <-> JPY)**](specs/02-currency-converter-spec.md)
- [**03 — Especificación: Pasaporte/DNI a Mano, Repositorio de Documentos y Guías de Viaje**](specs/03-documents-and-id-vault-spec.md)
- [**04 — Especificación: Planificador de Viaje (Fotos, Google Maps, Calendario y Horarios)**](specs/04-trip-planner-spec.md)
- [**05 — Especificación: Planificador de Transporte (Vuelos, Trenes/Shinkansen, Metros y Autobuses)**](specs/05-flight-planner-spec.md)
- [**06 — Especificación: Asistente IA con Gemini y Acceso Directo a Traducción Simultánea**](specs/06-gemini-ai-and-translator-spec.md)
- [**07 — Especificación: Previsión Meteorológica con Open-Meteo para Planificación de Usuario e IA**](specs/07-weather-open-meteo-spec.md)
- [**08 — Especificación: Tickets de Compra, Tax-Free y Control de Gastos del Viaje con Escaneo Inteligente**](specs/08-shopping-receipts-tax-free-spec.md)
- [**08B — Guía de Esquema JSON Todo en Uno (`japan-travel-plan.v1`) para Generación con IA y Portabilidad**](specs/08-travel-plan-json-schema-guide.md)
- [**09 — Especificación: Importación de Billetes Apple Wallet (`.pkpass`) y Visor QR/Aztec Offline**](specs/09-apple-wallet-pkpass-import-spec.md)
- [**10 — Especificación: Sistema de Actualizaciones Automáticas vía GitHub Releases y Firma Criptográfica de Producción**](specs/10-github-releases-auto-updater-spec.md)
- [**11 — Especificación: Botón de Emergencia, Directorio Consular y Asistencia Médica Offline**](specs/11-emergency-consular-assistance-spec.md)
- [**12 — Especificación: Alarmas y Notificaciones de Transporte en Segundo Plano con AlarmManager**](specs/12-transport-alarms-spec.md)
- [**13 — Especificación: Optimizador Geodésico y Reordenación Inteligente de Visitas del Día**](specs/13-day-itinerary-optimizer-spec.md)
- [**14 — Especificación: Exportación de Itinerarios y Guías a Documento PDF Compartible 100% Offline**](specs/14-itinerary-pdf-export-spec.md)

### 2. Análisis Técnico (`/docs/analysis/`)
Documentos que transforman las especificaciones en decisiones de arquitectura, modelos de datos y contratos verificables:
- [**Arquitectura Android, Stack Tecnológico y Estrategia TDD**](analysis/architecture-and-tech-stack.md)
- [**04 — Análisis Técnico: Mapa Integrado de Visitas del Día, Etiquetas Anticolisión y Detalle con Guías y Billetes**](analysis/04-day-itinerary-map-and-linked-details-analysis.md)
- [**08 — Análisis Técnico: Módulo de Tickets de Compra, Tax-Free y Control de Gastos (SPEC-08)**](analysis/08-shopping-receipts-tax-free-analysis.md)
- [**09 — Análisis Técnico: Importación de Billetes Apple Wallet (`.pkpass`) y Renderizado QR/Aztec**](analysis/09-apple-wallet-pkpass-import-analysis.md)
- [**10 — Análisis Técnico: Importación Unificada de Plan de Viaje, Guías y Billetes en un Solo JSON**](analysis/10-unified-travel-plan-guides-and-documents-analysis.md)
- [**11 — Análisis Técnico: Renderizado Nativo de Markdown en Guías de Viaje y Vista Previa en Editor**](analysis/11-travel-guides-markdown-rendering-analysis.md)
- [**12 — Análisis Técnico: Sistema de Actualizaciones Automáticas vía GitHub Releases y Firma de Producción**](analysis/12-github-releases-auto-updater-analysis.md)
- [**13 — Análisis Técnico: Emergencias, Directorio Consular y Asistencia Médica Offline**](analysis/13-emergency-consular-assistance-analysis.md)
- [**14 — Análisis Técnico: Motor de Alarmas y Notificaciones de Transporte Offline con AlarmManager**](analysis/14-transport-alarms-analysis.md)
- [**15 — Análisis Técnico: Optimizador Geodésico de Rutas del Día**](analysis/15-day-itinerary-optimizer-analysis.md)
- [**16 — Análisis Técnico: Generador Nativo de PDF para Itinerarios y Guías**](analysis/16-itinerary-pdf-export-analysis.md)

### 3. Tareas e Implementación (`/docs/tasks/`)
Desglose secuencial de tareas atómicas listas para ser abordadas mediante ciclos **Red -> Green -> Refactor**:
- [**Backlog Maestro y Plan de Tareas (SDD + TDD)**](tasks/backlog.md)
- [**Tareas: Mapa Integrado de Visitas del Día y Etiquetas Anticolisión**](tasks/tasks-day-itinerary-map.md)
- [**Tareas: Importación de Billetes Apple Wallet (`.pkpass`)**](tasks/tasks-apple-wallet-pkpass-import.md)
- [**Tareas: Importación Unificada de Plan, Guías y Billetes en un Solo JSON**](tasks/tasks-unified-travel-plan-import.md)
- [**Tareas: Renderizado Nativo de Markdown en Guías de Viaje y Vista Previa**](tasks/tasks-travel-guides-markdown-rendering.md)
- [**Tareas: Actualizador Automático vía GitHub Releases y Firma Criptográfica**](tasks/tasks-github-releases-auto-updater.md)

### 4. Guías Operativas y de Usuario
- [**Guía de Usuario Completa**](user-guide.md)
- [**Flujo de Trabajo: SDD, TDD, Gitflow, Conventional Commits y Releases**](development-workflow.md)
