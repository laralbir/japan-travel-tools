# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html) y [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

### Added
- Inicialización del repositorio orientado a Agentes de IA (`AGENTS.md`, `.gitignore`, `README.md`, `CHANGELOG.md`).
- Configuración de ramas **Gitflow** (`main` y `develop`).
- Documentación completa de **Fase 1: Especificación (SDD)** en `/docs/specs/`:
  - `00-core-configuration-spec.md`: Requisitos de internacionalización (ES/EN con fallback a EN), sistema de diseño inspirado en **JetBrains KotlinConf App** con paleta de colores tradicionales de Japón (*Washi*, *Sumi*, *Akabeni*, *Ai-iro*) en tema claro/oscuro/sistema, iconografía vectorial moderna, generación de **Adaptive Icon** (con capa monocromática) y **Splash Screen** nativa, indicador en vivo de tipo de conexión y cambio rápido (**Wi-Fi / SIM / eSIM / Offline**), **sincronización multi-dispositivo con Cuenta de Google** (móvil <-> tablet manteniendo copia 100% offline), persistencia, arquitectura offline-first, apartado **"Acerca de"** (versión y librerías open source de terceros) y optimización adaptativa para **Pixel 10 Pro XL**, **Pixel 9 Pro XL** y tablet **TCL NXTPAPER 11 Plus**.
  - `01-world-clock-spec.md`: Especificación de relojes duales/múltiples con detección de ubicación y país de origen.
  - `02-currency-converter-spec.md`: Especificación de conversor de divisas (`EUR <-> JPY` por defecto) con **Frankfurter API** y caché offline persistente.
  - `03-documents-and-id-vault-spec.md`: Especificación de bóveda de Pasaporte/DNI (con bloqueo biométrico opcional), repositorio offline de documentos de viaje (vuelos, hoteles, etc.) y gestor/creador de **Guías de Viaje** (importadas en PDF/Markdown/Imagen y nativas con fotos).
  - `04-trip-planner-spec.md`: Especificación del **Planificador de Viaje** por días y horarios (`Timeline`), integrado con fotos offline por parada, **Google Maps** (mapa interactivo embebido + coordenadas/enlaces/rutas en la app de Google Maps) y sincronización con Calendario.
  - `05-flight-planner-spec.md`: Especificación del **Planificador de Transporte Multimodal (Vuelos, Trenes/Shinkansen, Metros y Autobuses)** con horas de facturación, embarque, salida, llegada, andenes, vagones, salidas de estación (`Exit`), cobertura JR Pass/Suica, cálculo automático de duración entre husos horarios, asistente de escalas/transbordos y vínculo directo con tarjetas de embarque y billetes QR offline.
  - `06-gemini-ai-and-translator-spec.md`: Especificación del **Asistente IA con Google Gemini** (Cloud + Gemini Nano On-Device en Pixel 9/10 Pro XL con caché offline) y **Acceso Directo a Traducción Simultánea** por voz, texto y cámara (`ES/EN <-> Japonés`).
- Documentación de **Fase 2: Análisis Técnico** en `/docs/analysis/architecture-and-tech-stack.md`.
- Documentación de **Fase 3: Plan de Tareas (TDD)** en `/docs/tasks/backlog.md`, incluyendo en el TODO la búsqueda y selección de un template moderno y útil para la app antes del scaffolding.
- Guía de usuario en español (`docs/user-guide.md`) y guía de flujo de trabajo Gitflow/Releases/Issues (`docs/development-workflow.md`), incluyendo el protocolo obligatorio de revisión, análisis de procedencia y respuesta a **GitHub Issues**.
