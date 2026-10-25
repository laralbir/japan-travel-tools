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
- [**05 — Especificación: Planificador de Vuelos (Facturación, Embarque, Salida, Escalas y Llegada)**](specs/05-flight-planner-spec.md)

### 2. Análisis Técnico (`/docs/analysis/`)
Documentos que transforman las especificaciones en decisiones de arquitectura, modelos de datos y contratos verificables:
- [**Arquitectura Android, Stack Tecnológico y Estrategia TDD**](analysis/architecture-and-tech-stack.md)

### 3. Tareas e Implementación (`/docs/tasks/`)
Desglose secuencial de tareas atómicas listas para ser abordadas mediante ciclos **Red -> Green -> Refactor**:
- [**Backlog y Plan de Tareas (v0.0.1-beta.X -> v0.0.1)**](tasks/backlog.md)

### 4. Guías Operativas y de Usuario
- [**Guía de Usuario Completa**](user-guide.md)
- [**Flujo de Trabajo: SDD, TDD, Gitflow, Conventional Commits y Releases**](development-workflow.md)
