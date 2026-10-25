# Japan Travel Tools 🇯🇵✈️

Aplicación Android **offline-first** con herramientas esenciales para viajar a Japón, desarrollada en **Kotlin** y **Jetpack Compose** siguiendo metodologías **SDD (Spec-Driven Development)** y **TDD (Test-Driven Development)** para agentes de Inteligencia Artificial.

> **Estado actual del proyecto**: `v0.0.1-beta.1` (Fase 1: Especificación SDD, Análisis, Plan de Tareas y Configuración de Repositorio).

---

## Características Principales

### Configuración Global y Base (Core)
- **Multiidioma**: Español e Inglés. Selecciona automáticamente el idioma del sistema (con **Inglés** como idioma por defecto si el sistema usa otro idioma) y permite cambiarlo manualmente dentro de la app.
- **Tema Personalizable**: Modo Claro, Modo Oscuro o Automático según el sistema (opción por defecto).
- **100% Persistente y Offline-First**: Todas las preferencias, cachés de datos externos y documentos del viajero se persisten localmente en el dispositivo para funcionar sin conexión a internet en cualquier momento del viaje.

### Herramientas Incluidas en la Especificación (v0.0.1)
1. **Relojes Duales (Hora Local / Japón / País de Origen) [En Especificación - Beta]**
   - Visualización simultánea de la hora actual según ubicación GPS/red, hora de Japón (`Asia/Tokyo`) y hora del país/zona horaria de origen seleccionada por el usuario.
   - Indicador de diferencia horaria y tramo del día (mañana/tarde/noche) para evitar llamadas a deshoras.
2. **Conversor de Divisa Offline-First (`EUR <-> JPY` por defecto) [En Especificación - Beta]**
   - Conversión bidireccional instantánea entre Euro (`EUR`), Yen Japonés (`JPY`) y otras divisas soportadas.
   - Descarga y actualización de tasas de cambio desde fuente open-source (**Frankfurter API** - datos del Banco Central Europeo).
   - Caché local persistente con fecha de última actualización y tasa semilla inicial para funcionar 100% offline desde el primer arranque.
3. **Bóveda de Identificación (Pasaporte y DNI a mano) [En Especificación - Beta]**
   - Acceso rápido offline a las fotografías de tu Pasaporte y DNI/documento de identidad (anverso/reverso) para mostrar en controles, hoteles o compras *Tax-Free*.
   - Almacenamiento privado interno de la aplicación con protección opcional mediante **Biometría / PIN del dispositivo**.
4. **Repositorio de Documentos y Guías de Viaje [En Especificación - Beta]**
   - Gestión y visualización offline de PDFs e imágenes de billetes de vuelo, reservas de hotel, JR Pass, seguros de viaje y entradas.
   - **Gestor y Creador de Guías de Viaje**: Permite tanto importar guías externas (PDF con marcador de página, Markdown, mapas en imagen) como crear y editar guías propias por ciudad/región con texto enriquecido y fotos adjuntas offline.
5. **Planificador de Viaje con Fotos, Google Maps, Calendario y Horarios [En Especificación - Beta]**
   - Calendario de viaje por días y línea temporal de horarios (`Timeline` en zona horaria `Asia/Tokyo`) con detección de solapamientos.
   - Galería de fotos offline asociada a cada parada o actividad del itinerario.
   - **Integración dual con Google Maps**: Mapa interactivo embebido (`Maps Compose`) con marcadores numerados de la ruta del día + almacenamiento offline de coordenadas/direcciones en japonés y lanzamiento directo de rutas en la app de Google Maps.
   - Sincronización/exportación opcional al Calendario del sistema / Google Calendar y vinculación directa con PDFs de reservas y guías de viaje.

---

## Metodología de Desarrollo (SDD + TDD + Gitflow)

Este proyecto se rige por las directrices definidas en [`AGENTS.md`](AGENTS.md):

1. **Especificación (`/docs/specs/`)** -> **Análisis (`/docs/analysis/`)** -> **Tareas (`/docs/tasks/`)** -> **Implementación (TDD)**.
2. **Gitflow**:
   - `main`: Releases estables y betas publicadas con su correspondiente APK en GitHub Releases.
   - `develop`: Rama de integración continua de desarrollo.
   - `feature/*`, `release/*`, `hotfix/*`: Ramas de trabajo.
3. **Conventional Commits v1.0.0**: Historial semántico estandarizado (`feat`, `fix`, `docs`, `test`, `refactor`, `chore`).
4. **Política de Versiones y Releases**:
   - Comenzamos en la versión `0.0.1`.
   - Toda herramienta no implementada al 100%, probada y aprobada mantiene la versión en estado **Beta** (`-beta.X`).
   - Cada publicación en `main` genera su tag y una **GitHub Release** con el archivo `.apk` funcional adjunto.

---

## Estructura de Documentación (`/docs`)

Toda la documentación técnica y funcional se encuentra en español dentro del directorio [`/docs`](docs/):

- **[Índice General de Documentación](docs/index.md)**
- **Especificaciones (SDD)**:
  - [00 - Visión General, Configuración, Idioma, Tema y Offline-First](docs/specs/00-core-configuration-spec.md)
  - [01 - Herramienta: Relojes y Ubicación (Hora Local y País Origen)](docs/specs/01-world-clock-spec.md)
  - [02 - Herramienta: Conversor de Divisa Offline-First (EUR <-> JPY)](docs/specs/02-currency-converter-spec.md)
  - [03 - Herramienta: Pasaporte/DNI, Repositorio de Documentos y Guías de Viaje](docs/specs/03-documents-and-id-vault-spec.md)
  - [04 - Herramienta: Planificador de Viaje (Fotos, Google Maps, Calendario y Horarios)](docs/specs/04-trip-planner-spec.md)
- **Análisis Técnico**:
  - [Arquitectura, Stack Tecnológico y Estrategia TDD](docs/analysis/architecture-and-tech-stack.md)
- **Planificación de Tareas**:
  - [Backlog y Desglose de Tareas SDD + TDD](docs/tasks/backlog.md)
- **Guías**:
  - [Guía de Usuario Completa](docs/user-guide.md)
  - [Flujo de Trabajo: Gitflow, Conventional Commits y Releases](docs/development-workflow.md)

---

## Próximos Pasos (TODO Inmediato)
- [ ] **Buscar un template moderno y útil para la app** (`TASK-004` en [`docs/tasks/backlog.md`](docs/tasks/backlog.md)): Evaluar y elegir una plantilla de UI/arquitectura en Jetpack Compose + Material 3 moderna, limpia y orientada a utilidades de viaje antes de iniciar el scaffolding del código Android.

---

## Historial de Cambios

Consulta el archivo [`CHANGELOG.md`](CHANGELOG.md) para ver el detalle de cada versión.
