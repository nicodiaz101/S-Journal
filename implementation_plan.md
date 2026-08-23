# implementation_plan.md — S Journal: Roadmap de Desarrollo

> **Última actualización:** 2026-08-23  
> **Spec técnica completa:** [`SPEC.md`](./SPEC.md)  
> **Instrucciones para agentes:** [`AGENT.md`](./AGENT.md)  

---

## Visión del Proyecto

**S Journal** es una app de diario personal nativa para Android, con filosofía **Tablet-First**, diseñada para el hardware S Pen de Samsung Galaxy Tabs y el lenguaje visual de **One UI 9**. Inspirada en Apple Journal y en la app de diario de Pixel, pero con identidad Samsung propia.

El proyecto es **exclusivamente personal**: un diario íntimo y funcional con S Pen, diseñado para tablets Samsung. La prioridad es que funcione de forma sólida primero, y luego incorporar encriptación y sync en la nube.

---

## Decisiones de Arquitectura

| Decisión                   | Elección                          | Justificación                                                              |
|----------------------------|-----------------------------------|----------------------------------------------------------------------------|
| Lenguaje                   | **Kotlin 100%**                   | Estándar moderno Android, null-safety, coroutines nativas                  |
| UI Framework               | **Jetpack Compose**               | Declarativo, composable, sin XML; ideal para layouts adaptativos           |
| Inyección de Dependencias  | **Hilt**                          | Compile-time (sin costo en runtime), estándar Google, MVVM friendly        |
| Base de datos              | **Room + SQLite**                 | ORM oficial Android, integra con Flow para reactividad                     |
| Concurrencia               | **Coroutines + Flow**             | Nativas de Kotlin, más simples que RxJava para este scope                  |
| Build System               | **Gradle Kotlin DSL**             | Type-safe, mejor IDE support, no Groovy                                    |
| Canvas S Pen               | **AndroidView nativo + MotionEvent** | Acceso a datos de presión/tilt que Compose no expone nativamente        |
| S Pen SDK (Samsung)        | **Explorar disponibilidad**       | Latencia ultra-baja si está disponible en el dispositivo target            |
| Encriptación (Fase 4)      | **SQLCipher AES-256 + Keystore**  | Estándar de industria, compatible con Room, clave protegida por hardware   |
| Cloud Backup (Fase 4)      | **Google Drive API v3**           | appDataFolder oculto, sin costo de almacenamiento visible para el usuario  |

---

## Estrategia de Ramas

```
main ─────────────────────────────────────────► [Uso personal — base estable]
│   Fases 1 + 2 + 3
│   Sin encriptación · Sin permisos de red
│   Room plain-text
│
└── feature/encryption-sync ──────────────────► [Uso personal avanzado]
        Basada en main
        + Fase 4 (SQLCipher + Keystore + Google Drive)
```

**Regla:** El código de encriptación/sync **nunca** se mergea a `main`.  
**Regla:** Mejoras de UI/S Pen en `main` **sí** se mergean periódicamente a `feature/encryption-sync`.

---

## Fases de Desarrollo

### ─────────────────────────────────────────────
### FASE 1 — Scaffolding e Infraestructura
### ─────────────────────────────────────────────

**Objetivo:** Tener un proyecto Android funcional, con toda la infraestructura lista para construir sobre ella.

**Rama:** `main`  
**Prerrequisito:** Android Studio Ladybug o superior, JDK 17+, dispositivo/emulador API 34+.

#### Tareas

- [ ] Crear proyecto Android desde CLI (`android-cli` skill) con Kotlin + Compose
- [ ] Configurar `build.gradle.kts` (app y root) con Compose BOM, Material 3
- [ ] Configurar `libs.versions.toml` (Version Catalog) para manejo de dependencias
- [ ] Agregar y configurar **Hilt**: `hilt-android`, `hilt-compiler`, plugin Gradle
- [ ] Agregar y configurar **Room**: `room-runtime`, `room-ktx`, `room-compiler`, `room-testing`
- [ ] Agregar **Navigation Compose**
- [ ] Agregar **WindowSizeClass** (`androidx.compose.material3.adaptive` o `androidx.window`)
- [ ] Crear estructura de paquetes según arquitectura definida en `SPEC.md § 4.2`
- [ ] Implementar `AppModule.kt` — módulo Hilt base
- [ ] Implementar `DatabaseModule.kt` — provee `JournalDatabase` y DAOs
- [ ] Implementar `JournalDatabase.kt` — Room database con `JournalEntry` entity
- [ ] Implementar sistema de temas One UI 9:
  - `Color.kt` — tokens de color (modo claro y oscuro)
  - `Type.kt` — escala tipográfica (Samsung One / sans-serif fallback)
  - `Shape.kt` — formas squircle para componentes
  - `Theme.kt` — `SJournalTheme` composable raíz

#### Criterio de Completitud (Fase 1 "Done")
- `./gradlew build` sin errores ni warnings de deprecación
- App abre en emulador API 34 con un scaffold básico (TopBar, body vacío)
- App abre en la **Galaxy Tab S6 Lite (2024)** via ADB sin crash
- Hilt genera código en compile-time sin errores
- El tema oscuro/claro respeta la configuración del sistema

---

### ─────────────────────────────────────────────
### FASE 2 — UI Adaptativa Tablet-First
### ─────────────────────────────────────────────

**Objetivo:** Implementar el layout dual-panel con el calendario y el contenedor de entrada. La UI debe verse correcta en tablet (≥ 600dp) y degradar a single-panel en teléfono.

**Rama:** `main`

#### Tareas

**Layout Adaptativo**
- [ ] Implementar detección de `WindowSizeClass` en `MainActivity.kt`
- [ ] Crear `AdaptiveLayout.kt` — composable que decide entre `TwoPaneLayout` y `SinglePaneLayout`
- [ ] Implementar `TwoPaneLayout.kt` — layout tablet (panel izquierdo fijo + panel derecho expandido)
- [ ] Implementar `SinglePaneLayout.kt` — layout teléfono (un panel, navegación con back stack)

**Panel Izquierdo — Header + Calendario + ADD NOW**
- [ ] Implementar `PanelHeader.kt` — header del panel izquierdo con:
  - Mensaje de estado dinámico (p. ej. "No posts today" / nombre del día seleccionado)
  - Menú `⋮` (opciones de la entrada: eliminar, compartir, etc.)
- [ ] Implementar `CalendarViewModel.kt` — estado del mes y día seleccionado
- [ ] Implementar `MonthCalendar.kt` — grilla de 7 columnas × N filas
- [ ] Implementar `CalendarDayCell.kt` — celda con número del día, indicador (●), estado selected/today
- [ ] Implementar navegación entre meses (flechas ◄/►)
- [ ] Implementar swipe horizontal para cambio de mes (`HorizontalPager`)
- [ ] Implementar `AddNowButton.kt` — botón píldora `+ ADD NOW` al fondo del panel izquierdo
- [ ] Conectar `AddNowButton` con la acción de crear entrada para el día actual

**Panel Derecho — Canvas + Fullscreen + Toolbar S Pen**
- [ ] Implementar `EntryPanelScreen.kt` — pantalla del panel derecho, siempre visible
- [ ] Implementar `FullscreenToggleButton.kt` — botón flotante en `Alignment.TopEnd`; alterna entre dual-panel y pantalla completa
- [ ] Implementar `RuledCanvasBackground.kt` — dibuja líneas horizontales de fondo con `Canvas` (Compose)
- [ ] Integrar `RuledCanvasBackground` como fondo del panel derecho (visible incluso sin entrada)
- [ ] Implementar `SpenToolbarPill.kt` — píldora flotante inferior con herramientas S Pen:
  - Tipos de trazo: lápiz, rotulador, pincel
  - Borrador (puntual y de trazo completo)
  - Selector de color (paleta One UI)
  - Formas geométricas
  - Toggle hoja rayada (on/off)
  - Botón `···` para más opciones
- [ ] El canvas S Pen se superpone sobre `RuledCanvasBackground` (implementado en Fase 3)

**Componentes Base Compartidos**
- [ ] Implementar `SJournalCard.kt` — contenedor base con squircle + separación tonal

#### Criterio de Completitud (Fase 2 "Done")
- Dual-panel visible en tablet; single-panel en teléfono (verificar en emulador)
- Calendario navega entre meses; al tocar una fecha el `PanelHeader` actualiza el mensaje
- El `AddNowButton` es visible al fondo del panel izquierdo
- El panel derecho muestra los renglones de fondo
- La `SpenToolbarPill` aparece como píldora flotante al fondo del panel derecho
- El `FullscreenToggleButton` aparece en la esquina superior derecha y expande el panel
- No hay `TopAppBar` que abarque ambos paneles
- No hay ningún color hardcodeado en ningún Composable

---

### ─────────────────────────────────────────────
### FASE 3 — Persistencia de Datos y S Pen
### ─────────────────────────────────────────────

**Objetivo:** Implementar el corazón de la app — el CRUD de entradas y el canvas S Pen de alta fidelidad. Esta fase es la más compleja y el punto estrella del TPO.

**Rama:** `main`

#### 3A — Capa de Datos (Room + Repositorio)

- [ ] Definir `JournalEntry` entity completa (ver `SPEC.md § 4.3`)
- [ ] Implementar `JournalEntryDao.kt`:
  - `insertEntry(entry: JournalEntry)`
  - `updateEntry(entry: JournalEntry)`
  - `deleteEntry(entry: JournalEntry)`
  - `getEntryByDate(date: LocalDate): Flow<JournalEntry?>`
  - `getEntriesInMonth(year: Int, month: Int): Flow<List<JournalEntry>>`
  - `getAllEntries(): Flow<List<JournalEntry>>`
- [ ] Definir `JournalRepository` interface (dominio)
- [ ] Implementar `JournalRepositoryImpl` (data)
- [ ] Conectar en `RepositoryModule.kt` con Hilt

#### 3B — Editor de Texto

- [ ] Implementar `TextEntryScreen.kt` + `TextEntryViewModel.kt`
- [ ] Implementar editor con `BasicTextField` (Compose) con hint text estilo One UI
- [ ] Contador de palabras en tiempo real (debounced, cada 500ms)
- [ ] Auto-save al salir de la pantalla (no al levantar el teclado)
- [ ] Formato básico: **negrita**, *cursiva* (toolbar mínima)

#### 3C — Canvas S Pen (Componente Crítico)

Implementar en el siguiente orden estricto:

- [ ] **`StrokePoint.kt`** — Data class con x, y, pressure, tiltX, tiltY, timestamp
- [ ] **`Stroke.kt`** — Data class con `List<StrokePoint>` + color + width + toolType
- [ ] **`StrokeProcessor.kt`** — Convierte `MotionEvent` → `StrokePoint` con interpolación
- [ ] **`StrokeBuffer.kt`** — Estado mutable de trazos en memoria (undo/redo stack)
- [ ] **`StrokeRenderer.kt`** — Renderiza `List<Stroke>` en `android.graphics.Canvas` con `Path`
- [ ] **`StrokeSerializer.kt`** — Serializa/deserializa `.sjink` (formato binario propio con `DataOutputStream`)
- [ ] **`SpenCanvasView.kt`** — `AndroidView` que intercepta `MotionEvent`, aplica palm rejection
- [ ] **`SpenCanvasComposable.kt`** — Wrapper Compose de `SpenCanvasView`
- [ ] **`SpenToolbar.kt`** — Toolbar flotante (lápiz, borrador, color, grosor)
- [ ] **`CanvasEntryScreen.kt`** — Pantalla completa con canvas + toolbar
- [ ] **`CanvasEntryViewModel.kt`** — Orquesta canvas state, guardado, undo/redo

**Palm rejection (ver `AGENT.md § 6.2`)**  
**Pipeline de renderizado (ver `SPEC.md § 5, Fase 3`)**

#### 3D — Integración y Preview en Calendario

- [ ] Al guardar una entrada de canvas, generar thumbnail PNG (128x128dp) con `Canvas.drawBitmap`
- [ ] Almacenar thumbnail como archivo en `filesDir` y su path en `JournalEntry.previewThumbnailPath`
- [ ] Mostrar thumbnail en `CalendarDayCell` para días con entrada de canvas
- [ ] Para entradas de texto, mostrar las primeras ~20 palabras como preview en el panel derecho

#### Criterio de Completitud (Fase 3 "Done")
- Crear una entrada de texto → cerrar app → reabrir → texto intacto ✓
- Crear una entrada de canvas con S Pen → cerrar app → reabrir → trazos intactos ✓
- Palm rejection activo en la Galaxy Tab S6 Lite: apoyar palma no genera trazos ✓
- Presión afecta grosor del trazo visiblemente en la Galaxy Tab S6 Lite ✓
- Thumbnail del canvas aparece en el calendario ✓
- Tests unitarios de DAO y ViewModel pasando ✓

**→ Al completar Fase 3: `main` tiene la app funcional al 100% en su forma base.**

---

### ─────────────────────────────────────────────
### FASE 4 — Encriptación E2E y Sincronización
### ─────────────────────────────────────────────

> **Rama:** `feature/encryption-sync` (branching desde `main` completa)  
> **Prioridad:** Baja. Esta fase es post-TPO, para uso personal.

#### 4A — Encriptación Local

- [ ] Agregar dependencias: `sqlcipher-android`, `androidx.sqlite:sqlite-ktx`
- [ ] Crear `SecurityManager.kt` — genera y gestiona la clave maestra en Android Keystore
- [ ] Crear `BIP39Generator.kt` — genera 12 palabras seed, deriva clave AES-256 con PBKDF2
- [ ] Implementar `OnboardingScreen.kt` — flujo de primera vez con seed phrase display
- [ ] Migrar `JournalDatabase` a SQLCipher (cambiar `SupportFactory`)
- [ ] Implementar `LockScreen.kt` — autenticación biométrica con `BiometricPrompt`
- [ ] Test de seguridad: verificar DB ilegible con SQLite Browser

#### 4B — Google Drive Sync

- [ ] Configurar Google Cloud Project + OAuth2 credentials
- [ ] Agregar Google Sign-In SDK (auth silenciosa, no UI intrusiva)
- [ ] Implementar `DriveRepository.kt`:
  - `uploadDatabase(file: File): Result<Unit>`
  - `downloadDatabase(): Result<File>`
  - `getLastSyncTimestamp(): Result<Instant?>`
- [ ] Implementar `SyncViewModel.kt` — lógica de conflicto (timestamp más reciente gana)
- [ ] Implementar `SyncSettingsScreen.kt` — estado de sincronización, botón manual
- [ ] Implementar sync automático con `WorkManager` (periodicidad: cada 24h en WiFi)
- [ ] Indicador de última sincronización en la UI

#### Criterio de Completitud (Fase 4 "Done")
- Abrir `.db` con SQLite Browser → datos ilegibles ✓
- Backup funcional: archivo aparece en `appDataFolder` de Google Drive ✓
- Restore funcional: desinstalar app, reinstalar, hacer restore → datos recuperados ✓
- La app no pide login de Google en cada apertura ✓

---

## Resumen Visual del Roadmap

```
FASE 1 ──► FASE 2 ──► FASE 3 ──► [TPO UADE ENTREGADO]
  │                                        │
  │                                        └──► FASE 4
  │                                              (branch: feature/encryption-sync)
  │
Scaffolding  UI Adaptativa  Persistencia     Encriptación
  + DI          + Calendario  + S Pen         + Google Drive
  + Temas       + Layout TP   (Core Feature)  (Uso Personal)
```

---

## Plan de Testing

### Tests Automatizados

```
androidTest/
├── dao/
│   └── JournalEntryDaoTest.kt     # CRUD completo, queries por fecha/mes
└── (instrumented tests si S Pen tests son necesarios)

test/
├── viewmodel/
│   ├── CalendarViewModelTest.kt   # Navegación de meses, selección de fecha
│   ├── TextEntryViewModelTest.kt  # Guardado, word count, auto-save
│   └── CanvasEntryViewModelTest.kt # Undo/redo, serialización
└── util/
    └── StrokeProcessorTest.kt     # Procesamiento de MotionEvent
```

### Tests Manuales en Galaxy Tab

| Test                          | Cómo verificar                                      | Fase |
|-------------------------------|-----------------------------------------------------|------|
| Dual panel                    | Rotar tablet, verificar layout                      | 2    |
| Latencia del canvas           | Percepción subjetiva: trazo "pega" al lápiz        | 3    |
| Palm rejection                | Apoyar palma + trazar → solo trazos del lápiz      | 3    |
| Sensibilidad a la presión     | Trazo suave → fino; trazo fuerte → grueso          | 3    |
| Hovering del S Pen            | Acercar lápiz → cursor aparece en canvas           | 3    |
| Persistencia ante force close | Force close → reabrir → datos intactos             | 3    |
| DB ilegible                   | `adb shell` + `hexdump` del `.db` → binario cifrado| 4    |

---

## Dependencias Clave (Version Catalog)

```toml
# libs.versions.toml

[versions]
kotlin = "2.0.x"
compose-bom = "2025.xx.xx"          # Usar la más reciente estable al iniciar
hilt = "2.51.x"
room = "2.7.x"
navigation = "2.8.x"
window = "1.4.x"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.x" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Window / Adaptive
window = { group = "androidx.window", name = "window", version.ref = "window" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = "1.8.x" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test" }

[plugins]
android-application = { id = "com.android.application", version = "8.7.x" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.x-1.0.x" }
```

> ⚠️ **Nota:** Actualizar las versiones `x` a los últimos estables al momento de crear el proyecto. Siempre consultar [Maven Central](https://search.maven.org) y [Google Maven](https://maven.google.com).
