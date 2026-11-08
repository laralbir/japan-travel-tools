# SPEC-11: Botón de Emergencia, Directorio Consular y Asistencia Médica Offline

- **ID**: `SPEC-11`
- **Estado**: `Aprobado para Análisis e Implementación`
- **Herramienta**: Herramienta 11 — Emergencias y Asistencia Consular
- **Referencias Cruzadas**:
  - [`SPEC-00`](00-core-configuration-spec.md) (Configuración Base, Persistencia Offline-First, i18n EN/ES y Material Design 3)
  - [`SPEC-03`](03-documents-and-id-vault-spec.md) (Bóveda de Documentos, Pólizas de Seguro Médico y Bloqueo Biométrico)
  - [`SPEC-06`](06-gemini-ai-and-translator-spec.md) (Motor de Traducción Offline y Locución TTS en Japonés)
  - [`ANALYSIS-13`](../analysis/13-emergency-consular-assistance-analysis.md) (Diseño Arquitectónico y Modelos)

---

## 1. Contexto, Motivación y Objetivo

Viajar por Japón es sumamente seguro, pero ante una emergencia médica repentina, un accidente, un desastre natural (terremoto, tifón) o la pérdida/robo del pasaporte o cartera, la barrera idiomática y el desconocimiento de los protocolos locales pueden generar una situación de indefensión crítica para el viajero.

En situaciones de estrés extremo o sin conexión a internet (modo avión, zonas rurales, fallo de eSIM o Pocket Wi-Fi), el viajero necesita acceder en **menos de dos toques** a:
1. Los **números de emergencia locales de Japón** con marcación telefónica directa.
2. La **Embajada y red de Consulados** de su país con dirección física, horarios, teléfonos de emergencia consular 24 horas y enlace cartográfico offline.
3. Su **Ficha Médica y Póliza de Seguro de Viaje** (alergias, medicación habitual, tipo de sangre, contacto SOS de familiares y número de asistencia médica en el extranjero).
4. **Tarjetas de Auxilio Inmediato en Japonés** con texto en kanji/kana grande, transcripción fonética en romaji, traducción en español y locución de voz (TTS) para mostrar o hacer sonar ante personal sanitario, policías (*kōban*) o transeúntes.

### Objetivo
Implementar una herramienta **100% Offline-First de Emergencias y Asistencia Consular** integrada en la navegación de **Japan Travel Tools**, accesible de forma prominente en la cabecera y el panel de herramientas, que centralice números de socorro, asistencia consular oficial, ficha médica personal y frases críticas de auxilio con audio.

---

## 2. Requisitos Funcionales (RF)

### RF-11.1: Números de Emergencia Oficiales de Japón y Marcación Rápida
1. **Directorio Semilla Preinstalado (100% Offline)**:
   - `110`: Policía (*Keisatsu* - robos, accidentes de tráfico, altercados).
   - `119`: Bomberos y Ambulancias (*Shōbō / Kyūkyū* - incendios, emergencias médicas graves).
   - `118`: Guardia Costera (*Kaijō Hoan-chō* - accidentes marítimos, ferry, emergencias en costa).
   - `0570-000-911` / `03-5285-8088`: Japan Visitor Hotline (JNTO - asistencia turística y soporte médico multilingüe 24/7 en inglés y español).
   - `03-3501-0110`: Línea de Asistencia en Inglés de la Policía Metropolitana de Tokio (*Tokyo Metropolitan Police English Helpline*).
   - `#7119`: Servicio de Consulta Telefónica de Emergencias Médicas no críticas (orientación sobre si acudir a urgencias o llamar a una ambulancia).
2. **Acción de Marcación Directa**:
   - Cada tarjeta de emergencia debe incluir un botón de marcación directa que lance el marcador telefónico del sistema mediante `Intent(Intent.ACTION_DIAL, Uri.parse("tel:..."))`, sin requerir permisos invasivos de llamada directa (`CALL_PHONE`).

### RF-11.2: Directorio Consular y Diplomático Oficial
1. **Catálogo Integrado de Misiones Diplomáticas en Japón**:
   - **España**:
     - Embajada de España en Tokio (Roppongi, Minato-ku): Teléfono general, teléfono de **Emergencia Consular 24h** (exclusivo para emergencias graves de ciudadanos españoles en Japón), dirección postal oficial y coordenadas GPS.
     - Consulado General en Tokio y Consulados Honorarios en Osaka, Fukuoka, Nagoya y Takayama.
   - **Países Hispanohablantes Adicionales**:
     - Embajadas y Consulados de México, Colombia, Argentina, Chile y Perú en Tokio/Osaka, permitiendo al viajero seleccionar su país de origen desde Ajustes o en la propia pantalla.
2. **Acciones Rápidas por Sede Consular**:
   - Botón `📞 Llamada de Emergencia Consular`: Lanza `Intent.ACTION_DIAL` con el teléfono de guardia 24 horas.
   - Botón `🗺️ Cómo Llegar`: Abre las coordenadas de la sede en Google Maps / mapa offline mediante `geo:lat,lng?q=...`.
   - Botón `📋 Copiar Dirección en Japonés`: Copia al portapapeles la dirección oficial en kanji/japonés para mostrar directamente a un taxista.

### RF-11.3: Ficha Médica Personal y Póliza de Seguro SOS
1. **Almacenamiento Local y Privado**:
   - Los datos se almacenan exclusivamente en el almacenamiento interno privado (`Context.filesDir`), protegidos por la configuración de privacidad de la app.
2. **Campos de la Ficha Médica**:
   - Nombre y apellidos del titular.
   - Grupo sanguíneo (A+, A-, B+, B-, AB+, AB-, O+, O-, Desconocido).
   - Alergias graves y alimentarias (ej. penicilina, frutos secos, marisco, etc.) con traducción automática o sugerida en japonés.
   - Condiciones médicas preexistentes y medicación habitual.
   - Contacto de emergencia familiar (nombre, parentesco, teléfono con prefijo internacional).
3. **Póliza de Seguro de Viaje**:
   - Nombre de la compañía aseguradora (IATI, Mondo, Chapka, Mapfre, Allianz, etc.).
   - Número de póliza.
   - Teléfono de asistencia médica 24 horas para llamadas a cobro revertido / internacionales.
   - Documento PDF o fotografía del certificado de póliza vinculado directamente a la Bóveda de Documentos (`SPEC-03`).

### RF-11.4: Tarjetas de Auxilio Crítico en Japonés con Audio (TTS)
1. **Catálogo de Situaciones de Emergencia**:
   - **Médicas**:
     - *"¡Por favor, llame a una ambulancia!"* (`救急車を呼んでください` / *Kyūkyūsha o yonde kudasai*).
     - *"Tengo dolor muy fuerte en el pecho / me cuesta respirar"* (`胸が激しく痛みます / 息が苦しいです`).
     - *"Soy diabético / necesito insulina"* (`私は糖尿病です / インスリンが必要です`).
     - *"Tengo una reacción alérgica grave"* (`重いアレルギー反応が出ています`).
   - **Policiales / Seguridad**:
     - *"¡Por favor, llame a la policía!"* (`警察を呼んでください` / *Keisatsu o yonde kudasai*).
     - *"He perdido mi pasaporte y mi cartera"* (`パスポートと財布を失くしました` / *Pasupōto to saifu o nakushimashita*).
     - *"¿Dónde está el kōban (puesto de policía) más cercano?"* (`一番近い交番はどこですか` / *Ichiban chikai kōban wa doko desu ka*).
   - **Desastres Naturales**:
     - *"¿Dónde está la zona de evacuación por terremoto / tsunami?"* (`避難所はどこですか` / *Hinanjo wa doko desu ka*).
2. **Interfaz de Auxilio**:
   - Tipografía en gran tamaño (`DisplaySmall` / `HeadlineMedium`) con fondo de alto contraste para mostrar a otra persona a 1 metro de distancia.
   - Botón de audio `🔊 Escuchar` que utiliza el motor TTS (`OfflineTranslatorEngine` / `TextToSpeechHelper`) para locutar la frase en japonés con pronunciación nativa clara.

---

## 3. Reglas de Negocio y Offline-First

1. **Cero Dependencia de Red (RN-11.1)**: Todos los números de emergencia, direcciones consulares y frases de auxilio con TTS están embebidos localmente en el binario de la aplicación.
2. **Acceso Inmediato en Caso de Crisis (RN-11.2)**: La sección de emergencias nunca debe requerir inicio de sesión, biometría obligatoria (a menos que el usuario haya bloqueado explícitamente toda la app) ni pantallas de espera.
3. **Marcación Segura (RN-11.3)**: El uso de `Intent.ACTION_DIAL` garantiza que el usuario siempre vea el número en el marcador nativo antes de emitir la llamada, evitando llamadas accidentales.

---

## 4. Diseño de Datos

```kotlin
data class EmergencyContactNumber(
    val id: String,
    val nameEn: String,
    val nameEs: String,
    val japaneseName: String,
    val phoneNumber: String,
    val descriptionEn: String,
    val descriptionEs: String,
    val iconName: String,
    val isPrimaryEmergency: Boolean = false
)

data class ConsularMissionItem(
    val id: String,
    val countryCode: String, // "ES", "MX", "CO", "AR", etc.
    val countryName: String,
    val city: String,
    val addressJapanese: String,
    val addressRomaji: String,
    val emergencyPhone24h: String,
    val standardPhone: String,
    val latitude: Double,
    val longitude: Double,
    val websiteUrl: String
)

data class MedicalEmergencyProfile(
    val fullName: String = "",
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val conditions: String = "",
    val medications: String = "",
    val insuranceCompany: String = "",
    val insurancePolicyNumber: String = "",
    val insuranceEmergencyPhone: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelation: String = ""
)

data class EmergencyPhraseItem(
    val id: String,
    val category: EmergencyCategory, // MEDICAL, POLICE, DISASTER, ALLERGY
    val spanishText: String,
    val englishText: String,
    val japaneseKanji: String,
    val romaji: String
)
```

---

## 5. Criterios de Aceptación (DoD)

1. Funcionalidad 100% verificada en modo avión estricto.
2. Suite unitaria con tests para repositorios de datos de emergencia, serialización JSON de la ficha médica y catálogo consular.
3. Compatibilidad completa con temas Claro (*Washi*) y Oscuro (*Sumi*).
4. Textos bilingües en `strings.xml` (EN por defecto) y `values-es/strings.xml`.
5. Salto de línea final estándar POSIX (`\n`) en todos los archivos.
