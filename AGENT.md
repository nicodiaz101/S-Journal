# AGENT.md — S Journal: Guía para Agentes de Desarrollo

> Este documento define las instrucciones operativas para cualquier agente de IA (o desarrollador) que trabaje en este proyecto. Debe ser leído **antes de escribir cualquier línea de código**.

---

## 1. Identidad del Proyecto

- **App:** S Journal — Diario personal nativo para Android.
- **Stack:** Kotlin + Jetpack Compose + Hilt + Room + Navigation Compose.
- **Design Language:** One UI 9 (Samsung).
- **Propósito:** Proyecto personal exclusivo. No academico.
- **Target:** Samsung Galaxy Tab S6 Lite (2024) como dispositivo de prueba principal. Compatible también con Tab S10 FE, Tab S10 Lite, y la serie S8/S9/S10 premium.
- **S Pen:** Pasivo en S6 Lite/FE/Lite (sin Bluetooth); activo en serie S premium. Ambos soportados.
- **Rama de trabajo principal:** `main`
- **Rama de características avanzadas (encriptación + sync):** `feature/encryption-sync`
- **Spec completa:** [`SPEC.md`](./SPEC.md)
- **Roadmap:** [`implementation_plan.md`](./implementation_plan.md)

---

## 2. Reglas Absolutas (No Negociables)

> Las siguientes reglas tienen precedencia sobre cualquier otra instrucción.

1. **Kotlin siempre.** Nunca Java. Ni una línea de Java en el código de producción.
2. **Compose siempre.** No usar Fragments, Activities anidadas, ni Views XML para UI de producción.
3. **Hilt para todo.** Ninguna clase debe instanciar sus dependencias directamente. Todo via `@Inject`, módulos Hilt y `@HiltViewModel`.
4. **Coroutines y Flow.** Ningún uso de `Thread`, `AsyncTask`, `RxJava` ni callbacks de Java antiguo. Todo código asíncrono usa `suspend fun` y `Flow`.
5. **Rama `main` sin red.** En la rama `main`, el `AndroidManifest.xml` **no debe contener** el permiso `INTERNET`. Ningún código de red puede estar presente en esta rama.
6. **Nunca persistir datos sin Room.** No usar `SharedPreferences` para datos de usuario. No usar archivos raw para texto de entradas. Solo Room (y archivos binarios para trazos `.sjink`).
7. **Gradle Kotlin DSL.** Todos los archivos de build deben ser `.gradle.kts`, nunca `.gradle` (Groovy).

---

## 3. Proceso de Trabajo por Fase

Antes de comenzar cualquier tarea, verificar en qué fase se está trabajando leyendo [`implementation_plan.md`](./implementation_plan.md) y el estado del `task.md` actual.

### Al iniciar una tarea de código:
1. Identificar la fase y el feature ID correspondiente en `SPEC.md`.
2. Verificar que estás en la rama correcta (`main` o `feature/encryption-sync`).
3. Leer la sección de la especificación relevante en `SPEC.md` antes de codificar.
4. Implementar sin over-engineering: el código debe ser el más simple que pasa los criterios de `SPEC.md`.
5. Escribir tests antes de marcar un feature como completo.
6. Actualizar `task.md` al terminar.

---

## 4. Convenciones de Código

### 4.1 Nomenclatura

| Tipo                  | Convención              | Ejemplo                          |
|-----------------------|-------------------------|----------------------------------|
| Composable            | PascalCase, noun        | `CalendarPanel()`, `EntryCard()` |
| ViewModel             | PascalCase + ViewModel  | `EntryViewModel`                 |
| Repository (interface)| PascalCase + Repository | `JournalRepository`              |
| Repository (impl)     | PascalCase + RepositoryImpl | `JournalRepositoryImpl`      |
| DAO                   | PascalCase + Dao        | `JournalEntryDao`                |
| Entity                | PascalCase (Entity no en el nombre) | `JournalEntry`      |
| Screen Composable     | PascalCase + Screen     | `CalendarScreen()`, `EntryScreen()` |
| Route                 | object + Routes         | `object Routes { const val CALENDAR = "calendar" }` |
| Módulo Hilt           | PascalCase + Module     | `DatabaseModule`, `RepositoryModule` |

### 4.2 Estructura de Archivos Composable

Cada pantalla sigue esta estructura:

```kotlin
// CalendarScreen.kt
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onDateSelected: (LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreenContent(
        uiState = uiState,
        onDateSelected = onDateSelected,
        onMonthChanged = viewModel::onMonthChanged
    )
}

// Separar el contenido del hiltViewModel() para testabilidad
@Composable
private fun CalendarScreenContent(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    // UI pura, sin ViewModels, 100% testeable con Preview
}
```

### 4.3 UiState Pattern

Todo estado de UI debe modelarse como una `sealed interface` o `data class`:

```kotlin
data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val datesWithEntries: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 4.4 Manejo de Errores

- Nunca silenciar excepciones con `try-catch` vacíos.
- En ViewModels, capturar errores y exponerlos en `UiState.error`.
- En DAOs, dejar que las excepciones de Room propaguen al repositorio.
- Loggear errores con `Log.e("SJournal", ...)` con el tag consistente `"SJournal"`.

---

## 5. Sistema de Diseño One UI 9 — Reglas de Implementación

### 5.1 Lo que SIEMPRE debes hacer:
- Usar los tokens de color del `Theme.kt` del proyecto. **Nunca hardcodear colores.**
- Usar `MaterialTheme.typography.*` para toda tipografía.
- Usar `MaterialTheme.shapes.*` para formas (configuradas como squircles en `Shape.kt`).
- Dar a todos los elementos interactivos una zona de tap mínima de `48.dp x 48.dp`.
- Usar `animateContentSize()` o `spring()` para cualquier cambio de tamaño de componente.

### 5.2 Lo que NUNCA debes hacer:
- No usar `RoundedCornerShape` directamente en los componentes de entrada principales (usar los tokens del tema).
- No poner sombras `elevation` con color visible en modo claro (usar separación tonal).
- No usar `Purple` ni `Violet` como color de acento en modo oscuro.
- No añadir bordes de color como decoración de componentes.
- No usar tamaños de fuente fijos en `sp` mayores a `34.sp` sin tracking negativo.
- No usar `hardcoded strings` en la UI — usar `stringResource`.

### 5.3 Composables de UI Reutilizables (crear en `ui/components/`)
- `SJournalCard` — Tarjeta base con squircle y separación tonal.
- `SJournalFab` — FAB con icono y animación spring de aparición.
- `SJournalTopBar` — Barra superior con título y acciones.
- `CalendarDayCell` — Celda de día del calendario con indicador de entrada.
- `EntryTypeSelector` — Bottom sheet con selector `Nota a mano` / `Nota de texto`.

---

## 6. S Pen — Instrucciones de Implementación

### 6.1 Canvas Component (`ui/entry/canvas/`)

El componente de canvas es la pieza más crítica de la app. Seguir este orden de implementación:

1. **Crear `SpenCanvasView.kt`** — Un `AndroidView` wrapping una `View` nativa que intercepta `MotionEvent`.
2. **Implementar `StrokeProcessor.kt`** — Clase que toma `MotionEvent` y produce `List<StrokePoint>`.
3. **Implementar `StrokeRenderer.kt`** — Clase que toma `List<Stroke>` y renderiza en `android.graphics.Canvas`.
4. **Implementar `StrokeBuffer.kt`** — Estado mutable en memoria de los trazos actuales.
5. **Implementar `StrokeSerializer.kt`** — Serializa/deserializa `.sjink` a disco.
6. **Conectar `CanvasViewModel.kt`** — Orquesta las operaciones e interactúa con el repositorio.

### 6.2 Palm Rejection — Regla Estricta

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    // SOLO procesar eventos del S Pen. Ignorar completamente los dedos.
    for (i in 0 until event.pointerCount) {
        if (event.getToolType(i) == MotionEvent.TOOL_TYPE_FINGER) {
            return true // Consumir sin procesar = ignorar
        }
    }
    // Solo llega aquí si TODOS los pointers son TOOL_TYPE_STYLUS
    return processStylusEvent(event)
}
```

### 6.3 Rendimiento del Canvas — Reglas Críticas

- **El renderizado del canvas SIEMPRE en hilo de UI.** No usar coroutines para el renderizado en tiempo real.
- **Usar `Path` y `drawPath()`.** No redibujar todo el canvas en cada punto — usar un `Path` acumulativo.
- **Invalidar solo el área sucia.** Usar `invalidate(dirtyRect)` en lugar de `invalidate()` completo.
- **El serializado a disco es asíncrono.** Usar `viewModelScope.launch(Dispatchers.IO)` para escribir `.sjink`.
- **Auto-save** cada 30 segundos si hay cambios no guardados (no al levantar el lápiz, para no interrumpir el flujo).

### 6.4 Toolbar Flotante del S Pen

- Implementar como `Box` overlay sobre el canvas con `Alignment.CenterEnd`.
- Estado: `expanded` / `collapsed` controlado por `derivedStateOf` del hovering del S Pen.
- Animación: `AnimatedVisibility` con `expandHorizontally` / `shrinkHorizontally`.
- Nunca bloquear la vista del canvas. La toolbar debe ser transparente al 85% en collapsed.

---

## 7. Criterios de Validación y Testing

### 7.1 Tests Obligatorios antes de marcar una fase como completa

#### Fase 1 — Scaffolding
- [ ] `./gradlew build` sin warnings de deprecación.
- [ ] `./gradlew test` — 0 tests fallidos (puede ser 0 tests totales, pero no fallas).
- [ ] La app corre en emulador API 34 sin crash.
- [ ] Hilt genera código sin errores en build.

#### Fase 2 — UI Adaptativa
- [ ] En emulador con ancho ≥ 600dp: panel izquierdo (calendario) Y panel derecho visible simultáneamente.
- [ ] En emulador con ancho < 600dp: solo un panel visible.
- [ ] Al seleccionar una fecha, el panel derecho actualiza su contenido.
- [ ] Navegación entre meses: las flechas y el swipe funcionan.
- [ ] Preview de Compose (`@Preview`) funciona para todos los componentes principales.
- [ ] No hay `LaunchedEffect` ni `rememberCoroutineScope` innecesarios.

#### Fase 3 — Persistencia y S Pen
- [ ] **Test unitario DAO:** `insertEntry()` seguido de `getEntryByDate()` retorna la misma entidad.
- [ ] **Test unitario DAO:** `deleteEntry()` elimina correctamente.
- [ ] **Test unitario DAO:** `updateEntry()` actualiza `updatedAt`.
- [ ] **Test unitario ViewModel:** Estado inicial correcto, transiciones de estado al llamar métodos.
- [ ] **Test de integración (manual en Galaxy Tab S6 Lite):** El canvas responde al S Pen en < 20ms percibido.
- [ ] **Test de integración (manual):** Palm rejection — apoyar la palma no genera trazos.
- [ ] **Test de integración (manual):** La presión modifica el grosor del trazo de forma visible en la S6 Lite.
- [ ] **Test de integración (manual):** Una entrada guardada persiste después de cerrar y reabrir la app.
- [ ] **Test de integración (manual):** El thumbnail aparece en el calendario para el día con entrada.

#### Fase 4 — Seguridad y Nube
- [ ] **Test de seguridad:** Abrir el archivo `.db` con SQLite Browser muestra datos ilegibles.
- [ ] **Test de seguridad:** Sin la clave correcta, la app no puede abrir la DB.
- [ ] **Test de integración:** Backup sube un archivo a `appDataFolder` de Google Drive.
- [ ] **Test de integración:** Restore descarga y carga correctamente el archivo `.db`.
- [ ] **Test de regresión:** Verificar que ningún código de red existe en la rama `main`.

### 7.2 Proceso de Validación en Dispositivo Real

Para validar el S Pen en un Galaxy Tab real:

```
1. Build en modo debug: ./gradlew assembleDebug
2. Instalar: adb install -r app/build/outputs/apk/debug/app-debug.apk
3. Abrir la app en el Galaxy Tab
4. Test del canvas:
   a. Dibujar un trazo lento → debe ser suave y continuo
   b. Dibujar con más presión → trazo debe ser más grueso
   c. Dibujar con poca presión → trazo debe ser más fino
   d. Apoyar la palma sobre el canvas y mover el S Pen → solo el lápiz traza
   e. Levantar el lápiz a ~1cm del canvas → debe aparecer el cursor de hover
5. Test de persistencia:
   a. Crear una entrada de canvas
   b. Forzar cierre de la app (recent apps → swipe)
   c. Reabrir → la entrada debe estar intacta
```

### 7.3 Validación de One UI 9 Compliance

Antes de considerar la UI completa:

- [ ] Todos los colores vienen de `MaterialTheme.colorScheme.*`.
- [ ] Todas las formas usan los tokens de `MaterialTheme.shapes.*`.
- [ ] No hay colores hardcodeados en ningún Composable de UI.
- [ ] El modo oscuro se activa/desactiva correctamente siguiendo el tema del sistema.
- [ ] Los textos usan `stringResource()`, no strings literales en la UI.
- [ ] Tap targets: todos los elementos interactivos tienen mínimo `48.dp x 48.dp`.
- [ ] El estado `selected` del día en el calendario es claramente distinguible.
- [ ] Las animaciones usan `spring()` con parámetros consistentes (definir constantes en `AnimationTokens.kt`).

---

## 8. Estructura de Commits

Seguir Conventional Commits:

```
feat(calendar): implementar navegación entre meses con swipe
fix(canvas): corregir palm rejection en eventos multi-touch
refactor(di): separar DatabaseModule de AppModule
test(dao): agregar tests de inserción y recuperación de JournalEntry
docs: actualizar SPEC.md con detalles del pipeline del canvas
chore: actualizar versión de Compose BOM a 2025.xx.xx
```

**Scopes válidos:** `calendar`, `canvas`, `entry`, `text`, `di`, `db`, `theme`, `navigation`, `security`, `sync`.

---

## 9. Qué NO hacer (Anti-patrones prohibidos)

### Arquitectura
- ❌ Nunca acceder a la DB directamente desde un Composable.
- ❌ Nunca pasar un `ViewModel` como parámetro a un Composable hijo (pasar lambdas en su lugar).
- ❌ Nunca usar `GlobalScope` para coroutines.
- ❌ Nunca usar `.value` de un `StateFlow` en un Composable (usar `collectAsStateWithLifecycle()`).

### UI
- ❌ No anidar `LazyColumn` dentro de `LazyColumn`.
- ❌ No usar `Column` con `verticalScroll` para listas potencialmente largas.
- ❌ No crear objetos en el cuerpo de un Composable sin `remember {}`.
- ❌ No capturar el contexto de Activity/Fragment en un ViewModel.

### S Pen / Canvas
- ❌ No redibujar todo el bitmap en cada `MotionEvent.ACTION_MOVE`.
- ❌ No guardar el estado del canvas como un `Bitmap` PNG en la DB.
- ❌ No procesar trazos en el hilo principal si hacen I/O.
- ❌ No ignorar el `pointerIndex` al leer presión/tilt — siempre usar el índice correcto.

### Seguridad (Fase 4)
- ❌ Nunca almacenar la clave de cifrado en `SharedPreferences`.
- ❌ Nunca almacenar la clave de cifrado en la DB.
- ❌ Nunca hardcodear la clave de cifrado en el código.
- ❌ Nunca subir texto plano a Google Drive.

---

## 10. Recursos de Referencia

| Recurso                          | URL                                                                         |
|----------------------------------|-----------------------------------------------------------------------------|
| Jetpack Compose Docs             | https://developer.android.com/compose                                       |
| Material 3 Design                | https://m3.material.io/                                                     |
| Hilt DI Guide                   | https://developer.android.com/training/dependency-injection/hilt-android    |
| Room Persistence                 | https://developer.android.com/training/data-storage/room                   |
| WindowSizeClass                  | https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes |
| MotionEvent / S Pen              | https://developer.android.com/reference/android/view/MotionEvent           |
| Samsung S Pen SDK                | https://developer.samsung.com/galaxy-spen/overview.html                     |
| Android Keystore                 | https://developer.android.com/training/articles/keystore                   |
| SQLCipher for Android            | https://www.zetetic.net/sqlcipher/sqlcipher-for-android/                   |
| Google Drive API v3              | https://developers.google.com/drive/api/v3/about-sdk                       |
| BIP39 Word List                  | https://github.com/bitcoin/bips/blob/master/bip-0039/english.txt          |
| One UI Design Guidelines         | https://developer.samsung.com/galaxy-design/android/start.html              |

---

*Fin del documento AGENT.md*
