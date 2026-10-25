# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html) y [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

## [0.0.1-beta.2] - 2026-10-25

### Added
- **Exportación e Importación de Planes de Viaje y Transporte en JSON Legible (`SPEC-04 RF-04.6` & `SPEC-05 RF-05.4`)**:
  - `TravelPlanJsonCodec` y `PortableTravelPlanBundle` (`schemaVersion = "japan-travel-plan.v1"`) con formato JSON indentado (pretty-printed) 100% legible por humanos para preparar itinerarios, calendarios, vuelos, trenes Shinkansen, metros y autobuses desde aplicaciones externas e importarlos mediante **Fusión (`Merge`)** o **Reemplazo (`Replace`)**.
  - `JsonPortabilityModalSheet` integrada en el Planificador de Viaje, Planificador de Transporte y pestaña de IA para exportar/compartir JSON o pegar/importar planes externos en tiempo real.
- **Creación y Mejora de Planes de Viaje con IA Gemini (`SPEC-06 RF-06.2`)**:
  - `AiPlanBuilderEngine` y `SharedTravelPlanStore` para que la IA integrada pueda crear nuevos planes de viaje y optimizar los planes existentes en la app (resolución automática de solapamientos horarios y adición de Día 3 en Osaka con paradas + conexión en Shinkansen Nozomi Kioto ➔ Shin-Osaka).

## [0.0.1-beta.1] - 2026-10-25

### Added
- Inicialización del repositorio orientado a Agentes de IA (`AGENTS.md`, `.gitignore`, `README.md`, `CHANGELOG.md`).
- Configuración de ramas **Gitflow** (`main` y `develop`).
- Documentación completa de **Fase 1: Especificación (SDD)** en `/docs/specs/` (`SPEC-00` a `SPEC-06`), **Fase 2: Análisis Técnico** y **Fase 3: Plan de Tareas (TDD)**.
- Implementación funcional completa de **`v0.0.1-beta.1`** con **TDD (100% tests unitarios en verde)** e interfaz inspirada en **JetBrains KotlinConf App** con paleta japonesa (*Washi*, *Sumi*, *Akabeni Torii*, *Ai-iro*, *Matcha*) e icono adaptativo Torii/Sol Naciente:
  - **Core (`SPEC-00`)**: Persistencia en `DefaultUserPreferencesRepository`, cambio reactivo de idioma (`ES`/`EN`/Sistema con fallback a `EN`) y tema (`Claro`/`Oscuro`/`Sistema`), píldora de conexión en vivo (`Wi-Fi`, `Pocket Wi-Fi`, `SIM`, `eSIM`, `Roaming`, `Offline`) con acceso al panel nativo de Internet/SIM de Android, sincronización multi-dispositivo con Cuenta de Google, apartado **"Acerca de"** con versión `0.0.1-beta.1` y listado de librerías Open Source de terceros, y diseño adaptativo (`NavigationBar` en **Pixel 10/9 Pro XL** y `NavigationRail` + doble panel en **TCL NXTPAPER 11 Plus**).
  - **Relojes y Ubicación (`SPEC-01`)**: `CalculateClockComparisonUseCase` y `OfflineTimeZoneResolver` con hora en Japón (`Asia/Tokyo`), hora local GPS y selector persistente de país de origen con diferencia horaria exacta (DST).
  - **Conversor de Divisas Offline-First (`SPEC-02`)**: `ConvertCurrencyUseCase`, tasas semilla offline preinstaladas (`EUR <-> JPY`), sincronización en vivo con **Frankfurter API (BCE)** y botones rápidos de billetes/monedas de Japón (`¥100` a `¥10,000`).
  - **Pasaporte/DNI, Documentos y Guías de Viaje (`SPEC-03`)**: Bóveda rápida de Pasaporte y DNI con interruptor de bloqueo biométrico/PIN, repositorio buscable de documentos por categoría y guías de viaje offline (*Tokio*, *Kioto*, *Osaka*).
  - **Planificador de Viaje (`SPEC-04`)**: Línea temporal de horarios (`JST`) con detección de solapamientos, fotos offline por parada, mapa de ruta con marcadores numerados, apertura directa en **Google Maps** y exportación a **Google Calendar / Calendario del sistema**.
  - **Planificador de Transporte Multimodal (`SPEC-05`)**: Gestión de Vuelos (facturación, embarque, puerta, salida, llegada), Shinkansen/Trenes (andén, coche, asiento, maletas grandes, JR Pass), Metro (código de estación, vagón recomendado, número de salida `Exit`) y Autobuses (dársena), con cálculo automático de escalas y transbordos entre husos horarios.
  - **Asistente IA con Gemini y Traducción Simultánea (`SPEC-06`)**: Botón de acceso directo global en cabecera, traductor cara a cara giratorio **180°** con Romaji y asistente Gemini con conversión automática de precios de cartas en `¥` a `EUR`.
- Sistema de despliegue **ADB Debug para dispositivos autorizados** (`./gradlew installAuthorizedDebug`, `scripts/deploy-authorized-devices.sh` y `authorized-devices.local.conf` ignorado en Git).
