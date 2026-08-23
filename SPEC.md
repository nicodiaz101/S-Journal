# SPEC.md — S Journal: Especificación Técnica y de Producto

> **Versión:** 1.0  
> **Fecha:** 2026-08-17  
> **Estado:** En revisión  

---

## 1. Visión General del Producto

**S Journal** es una aplicación de diario personal nativa para Android, diseñada con filosofía **Tablet-First**, optimizada exclusivamente para tablets Samsung con soporte completo del S Pen. Su propósito es ofrecer una experiencia de escritura personal íntima, fluida y segura que se sienta como una aplicación nativa del ecosistema One UI — comparable a Journal de Apple o la app de diario de Pixel, pero adaptada al hardware y lenguaje de diseño de Samsung.

El proyecto es **exclusivamente personal**, orientado a tener una experiencia de escritura de diario íntima, fluida y segura en una tablet Samsung con S Pen. La prioridad es que funcione de forma sólida primero, y luego se incorporen las funcionalidades avanzadas (encriptación y sync).

---

## 2. Plataforma Objetivo

| Atributo                   | Valor                                                              |
|----------------------------|--------------------------------------------------------------------|
| **OS**                     | Android 14+ (API 34+)                                              |
| **Dispositivo de prueba**  | **Samsung Galaxy Tab S6 Lite (2024)** — dispositivo principal      |
| **Dispositivos soportados**| Galaxy Tab S6 Lite (2024) · Tab S10 FE · Tab S10 Lite · Tab S8/S9/S10 series |
| **S Pen**                  | Requerido. S Pen pasivo (S6 Lite) o activo (serie S premium)      |
| **Factor de forma**        | Tablets (≥ 600dp de ancho) — layout principal                      |
| **Teléfonos**              | Soporte degradado (single-panel, sin S Pen canvas)                 |
| **Idioma inicial**         | Español                                                            |
| **Orientación**            | Landscape y Portrait (adaptive)                                    |

> **Nota sobre S Pen pasivo (S6 Lite, S10 FE/Lite):** Estos dispositivos incluyen un S Pen **pasivo** (sin Bluetooth, sin Air Actions). Para los propósitos de esta app (escritura y dibujo en canvas), el S Pen pasivo es completamente funcional — el digitalizador Wacom reporta correctamente presión, posición y hovering vía `MotionEvent`. Las funcionalidades que requieren S Pen activo (Air Actions, botón remoto) no forman parte del scope de S Journal.

---

## 3. Lenguaje de Diseño: One UI 9

Toda la UI debe adherir estrictamente a las guías de Samsung One UI 9, que son una evolución de One UI 8.5. Los pilares del sistema de diseño son:

### 3.1 Principios de Diseño
- **"Less clutter, more content":** La interfaz recede para que el contenido del usuario sea el protagonista.
- **Áreas de agarre cómodas:** Controles primarios en la mitad inferior de la pantalla al usar en portrait.
- **Claridad visual:** Jerarquía tipográfica marcada, uso generoso del espacio en blanco.

### 3.2 Sistema Visual

#### Tipografía
- **Familia:** Samsung One (con fallback a `sans-serif` nativo del sistema).
- **Escala:** Seguir `TextAppearance` de Material 3 adaptado a One UI:
  - `displayLarge` → Títulos de mes en calendario
  - `headlineMedium` → Fecha del día activo
  - `bodyLarge` → Cuerpo de notas de texto
  - `labelMedium` → Etiquetas, contadores de palabras
- **Tamaño base:** Respetar `sp` del sistema (accesibilidad).

#### Forma y Geometría — "Squircles"
- Usar `SquircleShape` para tarjetas y contenedores principales. One UI usa superellipse continua, no `RoundedCornerShape` estándar.
- Radio de esquina aproximado para tarjetas de entrada: `28.dp`.
- Botones: `50%` de radio (completamente redondeados).
- Iconos: formas squircle con fill/outline según estado.

#### Paleta de Colores
- Basada en Material You (Dynamic Color) con semilla del color de marca de Samsung.
- **Modo claro (default):** Fondo `#F4F0EB` (papel cálido), superficie de tarjeta `#FFFFFF`.
- **Modo oscuro:** Fondo `#1C1C1E`, superficie `#2C2C2E`.
- Acento primario: Extraído del color dinámico del sistema.
- Sin colores de borde de acento estáticos (respeta One UI).

#### Elevación y Profundidad
- **Sin sombras duras.** Usar separación tonal (color ligeramente más claro/oscuro) para indicar capas.
- Máximo 2 niveles de elevación visibles en pantalla simultáneamente.

#### Animaciones y Transiciones
- `spring` physics para transiciones de elementos de UI (stiffness media, damping moderado).
- Transiciones de pantalla: slide horizontal suave (< 300ms).
- Microinteracciones: feedback háptico sutil al seleccionar fecha en calendario o al activar herramienta S Pen.

---

## 4. Arquitectura de la Aplicación

### 4.1 Stack Tecnológico

| Capa              | Tecnología                                                    |
|-------------------|---------------------------------------------------------------|
| Lenguaje          | **Kotlin** (100%)                                             |
| UI                | **Jetpack Compose** (Compose BOM más reciente estable)        |
| Navegación        | **Navigation Compose**                                        |
| DI                | **Hilt** (compile-time, no reflection)                        |
| Base de datos     | **Room** (SQLite) → SQLCipher en rama de encriptación         |
| Concurrencia      | **Kotlin Coroutines + Flow**                                  |
| Build system      | **Gradle con Kotlin DSL** (`.gradle.kts`)                     |
| S Pen API         | **Samsung S Pen SDK** + Android `MotionEvent` (pressure/tilt) |
| Canvas            | **Compose Canvas** + `android.graphics.Canvas` nativo         |
| Seguridad (fase 4)| **Android Keystore** + **SQLCipher AES-256**                  |
| Cloud (fase 4)    | **Google Drive API v3** (REST + OAuth2 silent auth)           |

### 4.2 Patrón Arquitectónico

**MVVM + Clean Architecture (simplificada)**

```
app/
├── di/                    # Módulos Hilt (AppModule, DatabaseModule, etc.)
├── data/
│   ├── local/
│   │   ├── dao/           # DAOs de Room
│   │   ├── entity/        # Entidades de Room
│   │   └── database/      # JournalDatabase.kt
│   ├── repository/        # Implementaciones de repositorio
│   └── model/             # Data classes del dominio
├── domain/
│   └── repository/        # Interfaces de repositorio (contratos)
├── ui/
│   ├── theme/             # Color, Type, Shape, Theme (One UI tokens)
│   ├── components/        # Composables reutilizables
│   ├── calendar/          # Feature: Navegador de tiempo
│   ├── entry/             # Feature: Entrada de diario
│   │   ├── text/          # Editor de texto
│   │   └── canvas/        # Canvas S Pen
│   └── navigation/        # NavGraph, Routes
└── util/                  # Extensiones, helpers
```

### 4.3 Modelo de Datos

#### Entidad: `JournalEntry`
```kotlin
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val id: String,          // UUID v4
    val date: LocalDate,                 // Fecha de la entrada
    val createdAt: Instant,              // Timestamp de creación
    val updatedAt: Instant,              // Timestamp de última edición
    val type: EntryType,                 // TEXT | CANVAS | MIXED
    val title: String?,                  // Título opcional
    val textContent: String?,            // Contenido en texto plano (nullable)
    val wordCount: Int,                  // Conteo de palabras (texto)
    val strokeDataPath: String?,         // Ruta al archivo .sjink de trazos S Pen
    val previewThumbnailPath: String?,   // Miniatura PNG para el calendario
    val tags: List<String>,              // Etiquetas (serializado como JSON)
    val isFavorite: Boolean,             // Marcado como favorito
    val mood: Int?                       // Mood indicator 1-5 (opcional, fase futura)
)

enum class EntryType { TEXT, CANVAS, MIXED }
```

#### Almacenamiento de Trazos S Pen (`.sjink`)
Los trazos del S Pen se almacenan en formato binario eficiente, **NO** como imágenes PNG, para permitir re-renderizado vectorial, edición no destructiva y exportación futura.

Cada `StrokePoint` contiene:
```
x: Float, y: Float           // Posición normalizada (0.0 - 1.0)
pressure: Float               // 0.0 - 1.0 (desde MotionEvent)
tiltX: Float, tiltY: Float   // Inclinación del S Pen
timestamp: Long               // Para animación replay futura
```

---

## 5. Funcionalidades por Fase

### Fase 1 — Scaffolding e Infraestructura

| ID    | Funcionalidad                                      | Prioridad |
|-------|---------------------------------------------------|-----------|
| F1-01 | Proyecto Android creado y buildable                | P0        |
| F1-02 | Hilt configurado y funcional                       | P0        |
| F1-03 | Room configurado con entidades base                | P0        |
| F1-04 | Navigation Compose con grafo básico                | P0        |
| F1-05 | Sistema de temas One UI 9 (Color, Type, Shape)     | P0        |
| F1-06 | Estructura de paquetes según arquitectura          | P0        |

---

### Fase 2 — UI Adaptativa Tablet-First

| ID    | Funcionalidad                                               | Prioridad |
|-------|-------------------------------------------------------------|-----------|
| F2-01 | Detección de `WindowSizeClass` (Expanded/Compact)           | P0        |
| F2-02 | Layout dual-panel para tablets (≥ 600dp)                    | P0        |
| F2-03 | Layout single-panel para teléfonos                          | P1        |
| F2-04 | **Panel izq. — Header:** mensaje de estado + menú `⋮`       | P0        |
| F2-05 | **Panel izq. — Calendario** mensual interactivo             | P0        |
| F2-06 | Indicadores visuales (●) en días con entradas               | P0        |
| F2-07 | Navegación entre meses (swipe horizontal + flechas ◄►)     | P0        |
| F2-08 | **Panel izq. — Botón `+ ADD NOW`** (píldora al fondo)       | P0        |
| F2-09 | **Panel der. — Botón fullscreen** (expandir entrada) arriba a la derecha | P0 |
| F2-10 | **Panel der. — Canvas con renglones** visible por defecto                 | P0 |
| F2-11 | **Panel der. — Toolbar S Pen flotante** (píldora inferior)                | P0 |
| F2-12 | Mensaje "No posts today" en header del panel izquierdo                    | P0 |
| F2-13 | Animación spring al seleccionar fecha en calendario                       | P1 |

> **Sin TopBar de ancho completo.** Cada panel gestiona sus propios controles. No existe una barra de navegación que abarque los dos paneles simultáneamente.

#### Layout Dual-Panel — Basado en Wireframe

```
┌────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────┐  ┌──────────────────────────────────────┐ │
│  │ No posts today   ⋮  │  │                       [⛶ Fullscreen]│ │
│  │                     │  │  ∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿  │ │
│  │ Lu Ma Mi Ju Vi Sá Do│  │  ∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿   │ │
│  │  ○  ○  ○  ○  ○  ○ ○│  │  ∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿   │ │
│  │  ○  ○  ○  ○  ○  ○ ○│  │                                      │ │
│  │  ○  ○  ○  ○  ○  ○ ○│  │  (Canvas S Pen con renglones)        │ │
│  │  ○  ○  ○ [○] ○  ○ ○│  │                                      │ │
│  │  ○  ○  ○  ○  ○  ○ ○│  │  ∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿   │ │
│  │                     │  │                                      │ │
│  │  ( + ADD NOW )      │  │ ╭─ 🖊 ✏ ⬛ 💧 ▭ ≡ ··· ─────────╮  │ │
│  └─────────────────────┘  │ ╰── Toolbar S Pen flotante ─────────╯  │ │
│                            └──────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘

[○] = día seleccionado · ● = día con entrada · ( ) = botón píldora
∿∿∿ = renglones del canvas · [⛶] = botón de pantalla completa
Toolbar pill: lápiz · rotulador · borrador · color · formas · renglones · más
```

---

### Fase 3 — Persistencia y S Pen

| ID    | Funcionalidad                                          | Prioridad |
|-------|-------------------------------------------------------|-----------|
| F3-01 | CRUD completo de `JournalEntry` via Room               | P0        |
| F3-02 | Repositorio con Flow para actualización reactiva       | P0        |
| F3-03 | Editor de texto enriquecido (básico: bold, italic)     | P1        |
| F3-04 | Contador de palabras en tiempo real                    | P2        |
| F3-05 | **Canvas S Pen con baja latencia**                     | P0        |
| F3-06 | Palm rejection (via `MotionEvent.TOOL_TYPE_FINGER`)    | P0        |
| F3-07 | Sensibilidad a la presión del trazo                    | P0        |
| F3-08 | Detección del S Pen hovering (preview del cursor)      | P1        |
| F3-09 | Barra flotante S Pen (lápiz, borrador, color, grosor)  | P0        |
| F3-10 | Deshacer/Rehacer trazos (stack de comandos)            | P0        |
| F3-11 | Fondo de canvas con líneas (estilo hoja de carpeta)    | P1        |
| F3-12 | Selector de tipo de entrada al crear (texto / canvas)  | P0        |
| F3-13 | Persistencia de trazos en archivo `.sjink`             | P0        |
| F3-14 | Generación de thumbnail PNG para preview en calendario | P1        |
| F3-15 | Etiquetado de entradas                                 | P2        |
| F3-16 | Marcado de favoritos                                   | P2        |
| F3-17 | Búsqueda en texto plano de entradas                    | P2        |

#### Pipeline del Canvas S Pen

```
MotionEvent (S Pen hardware)
    → StrokeProcessor    (filtra ruido, interpola puntos intermedios)
    → StrokeBuffer       (acumula en memoria, renderiza en tiempo real)
    → Canvas render      (drawPath con ancho variable según presión)
    → StrokeSerializer   (persiste .sjink al guardar / auto-save)
```

#### Toolbar Flotante S Pen
- **Posición:** Derecha del canvas, verticalmente centrada.
- **Comportamiento:** Colapsable; se expande al hovering del S Pen; se colapsa 2 segundos después de retirar el lápiz.
- **Herramientas:** Lápiz, Rotulador, Borrador puntual, Borrador de trazo completo, Selector de color (paleta One UI), Limpiar lienzo (con confirmación).

---

### Fase 4 — Seguridad y Nube (Rama `feature/encryption-sync`)

| ID    | Funcionalidad                                              | Prioridad |
|-------|------------------------------------------------------------|-----------|
| F4-01 | Onboarding con generación de frase BIP39 (12 palabras)     | P0        |
| F4-02 | Migración de Room a SQLCipher (AES-256)                    | P0        |
| F4-03 | Gestión de clave maestra via Android Keystore              | P0        |
| F4-04 | Autenticación biométrica (huella / Face ID)                | P0        |
| F4-05 | Exportación manual del archivo `.db` cifrado               | P1        |
| F4-06 | Autenticación OAuth2 silenciosa con Google Drive           | P0        |
| F4-07 | Backup del `.db` cifrado a Google Drive (appDataFolder)    | P0        |
| F4-08 | Restore del `.db` cifrado desde Google Drive               | P0        |
| F4-09 | Indicador de última sincronización                         | P1        |
| F4-10 | Backup automático programado (WorkManager)                 | P1        |

---

## 6. Integración con S Pen — Detalles de Hardware

### APIs Relevantes
- `MotionEvent.TOOL_TYPE_STYLUS` — identificar input del S Pen
- `MotionEvent.TOOL_TYPE_FINGER` — identificar dedos (para palm rejection)
- `MotionEvent.getPressure(pointerIndex)` — presión 0.0..1.0
- `MotionEvent.getAxisValue(MotionEvent.AXIS_TILT)` — inclinación
- `MotionEvent.getAxisValue(MotionEvent.AXIS_ORIENTATION)` — orientación rotacional
- `View.setMotionEventSplittingEnabled(false)` — crucial para canvas multitouch correcto
- **Samsung SPen SDK** (`SpenEventLibrary`): explorar disponibilidad para latencia ultra-baja

### Palm Rejection
```kotlin
fun MotionEvent.isStylusInput(): Boolean =
    getToolType(actionIndex) == MotionEvent.TOOL_TYPE_STYLUS

fun MotionEvent.isPalmToIgnore(): Boolean =
    getToolType(actionIndex) == MotionEvent.TOOL_TYPE_FINGER
```

---

## 7. Requisitos No Funcionales

| Categoría          | Requisito                                                                     |
|--------------------|-------------------------------------------------------------------------------|
| **Rendimiento**    | Canvas S Pen: latencia < 20ms (percibida como instantánea)                   |
| **Rendimiento**    | 60fps en animaciones de UI en Galaxy Tab S6 Lite 2024                                  |
| **Persistencia**   | Datos jamás perdidos ante cierre forzado (auto-save en canvas)               |
| **Accesibilidad**  | Respetar tamaño de fuente del sistema (`sp`), contraste WCAG AA               |
| **Compatibilidad** | API 34+ (Android 14). Sin comprometer compatibilidad sin justificación        |
| **Privacidad**     | Sin analytics, sin telemetría, sin permisos de red en rama `main`             |
| **Offline-first**  | App 100% funcional sin internet (sync es opcional)                            |

---

## 8. Estrategia de Ramas

```
main                          
│   Sin encriptación
│   Room plain-text DB
│   Sin permisos de red
│
**Rama `feature/encryption-sync` (Uso Personal Avanzado):**
- Basada en main
- + SQLCipher + Keystore + Google Drive
```

**Reglas de merge:**
- Código de encriptación/sync **nunca** sube a `main`.
- Mejoras de UI/UX/S Pen en `main` **sí** se mergean periódicamente a `feature/encryption-sync`.

---

## 9. Definición de "Done" por Fase

| Fase | Criterio                                                                         |
|------|----------------------------------------------------------------------------------|
| 1    | App buildea y corre en emulador y en la Galaxy Tab S6 Lite, DI funcional, tema OK |
| 2    | Dual-panel en tablet, single-panel en teléfono, calendario navega entre meses    |
| 3    | Crear/guardar/recuperar entrada de texto Y de canvas con S Pen (Galaxy Tab S6 Lite) |
| 4    | DB ilegible en texto plano; backup/restore funcional con cuenta Google           |

---

## 10. Backlog Futuro (Out of Scope v1)

- Exportación a PDF de entradas
- Widget de inicio rápido (One UI Widget)
- Soporte Samsung DeX
- Modo "escribir sin distracciones" (pantalla completa)
- Mood tracker visual en el calendario
- Multi-idioma (EN/ES)
- Sincronización selectiva por rango de fechas
