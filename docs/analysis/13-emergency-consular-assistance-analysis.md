# ANALYSIS-13: Diseño Técnico y Arquitectura de Emergencias y Asistencia Consular Offline

- **ID**: `ANALYSIS-13`
- **Especificación**: [`SPEC-11`](../specs/11-emergency-consular-assistance-spec.md)
- **Capa**: Clean Architecture (Domain, Data, UI) + MVVM/MVI
- **Paquete Kotlin**: `com.laralbir.japantraveltools.feature.emergency`

---

## 1. Arquitectura y Módulos de Clean Architecture

El módulo de emergencias y asistencia consular sigue estrictamente la arquitectura desacoplada del proyecto:

```
feature.emergency/
├── domain/
│   ├── model/
│   │   ├── EmergencyModels.kt          # EmergencyContactNumber, ConsularMissionItem, MedicalProfile
│   │   └── EmergencyPhraseItem.kt      # Tarjetas de auxilio con Kanji, Romaji y TTS
│   ├── repository/
│   │   ├── EmergencyCatalogRepository.kt # Números oficiales y consulados semilla
│   │   └── MedicalProfileRepository.kt   # Persistencia privada de datos médicos del viajero
│   └── usecase/
│       ├── GetEmergencyContactsUseCase.kt
│       ├── GetConsularMissionsUseCase.kt
│       └── ManageMedicalProfileUseCase.kt
├── data/
│   ├── catalog/
│   │   ├── SeedEmergencyContacts.kt     # 110, 119, 118, JNTO Helpline, Policía en inglés
│   │   └── SeedConsularMissions.kt      # España (Tokio, Osaka, etc.) y países hispanohablantes
│   ├── local/
│   │   └── LocalMedicalProfileStore.kt  # Serialización JSON cifrada/privada en filesDir
│   └── repository/
│       ├── EmergencyCatalogRepositoryImpl.kt
│       └── MedicalProfileRepositoryImpl.kt
└── ui/
    ├── EmergencyScreen.kt               # Pantalla principal con pestañas: Números, Consulados, Médico, Frases
    ├── components/
    │   ├── EmergencyCallCard.kt         # Tarjeta con botón Intent.ACTION_DIAL
    │   ├── ConsularMissionCard.kt       # Dirección, teléfono 24h y botón a Google Maps
    │   ├── MedicalProfileSection.kt     # Ficha médica editable con grupo sanguíneo y alergias
    │   └── EmergencyPhraseAudioCard.kt  # Tarjeta de auxilio con tipografía grande y botón TTS
    └── EmergencyViewModel.kt
```

---

## 2. Contratos e Interfaces de Dominio

```kotlin
interface EmergencyCatalogRepository {
    fun getEmergencyNumbers(): List<EmergencyContactNumber>
    fun getConsularMissions(countryCode: String? = null): List<ConsularMissionItem>
    fun getEmergencyPhrases(category: EmergencyCategory? = null): List<EmergencyPhraseItem>
}

interface MedicalProfileRepository {
    suspend fun getMedicalProfile(): MedicalEmergencyProfile
    suspend fun updateMedicalProfile(profile: MedicalEmergencyProfile)
}
```

---

## 3. Estrategia de Testing (TDD)

1. **`EmergencyCatalogRepositoryTest`**:
   - Verificar que los números críticos (`110`, `119`, `118`, `0570-000-911`) estén presentes en la lista preinstalada.
   - Validar filtrado por país para misiones consulares (España `ES`, México `MX`, etc.).
   - Verificar integridad de textos japoneses y pronunciaciones romaji en las frases de auxilio.
2. **`MedicalProfileRepositoryTest`**:
   - Probar serialización y deserialización JSON en `filesDir`.
   - Probar actualización atómica de campos médicos y póliza de seguro.
   - Probar valores por defecto cuando el archivo aún no existe.
3. **`EmergencyViewModelTest`**:
   - Probar emisión reactiva de estados (UiState) al filtrar por categorías de frases o cambiar de país.
