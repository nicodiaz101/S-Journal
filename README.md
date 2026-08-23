# S Journal 📓✍️

**S Journal** es una aplicación de diario personal nativa para Android, desarrollada con filosofía **Tablet-First**, diseñada específicamente para el hardware **S Pen** de tablets Samsung Galaxy (optimizada para la **Galaxy Tab S6 Lite 2024**) y siguiendo los lineamientos de diseño de **One UI 9** (Samsung Notes).

---

## ✨ Características Principales

- **Diseño Tablet-First (Two-Pane Adaptativo):**
  - Panel izquierdo con calendario mensual interactivo, navegación táctil por swipe y tarjeta de estado.
  - Panel derecho con hoja rayada de cuaderno, botón de pantalla completa flotante y lienzo de escritura continuo.
- **Motor de Dibujo S Pen Nativo:**
  - **Palm Rejection Estricto:** Filtro de digitalizador en hardware para capturar únicamente la punta del S Pen e ignorar toques de palma o dedos.
  - **Sensibilidad a la Presión:** Variación dinámica de grosor según la fuerza aplicada con el lápiz.
  - **Botón Lateral de S Pen:** Al pulsar el botón lateral del lápiz, conmuta automáticamente a modo borrador.
  - **Herramientas de Barra Flotante:** Pluma, Resaltador translúcido, Borrador de trazos por colisión, Deshacer/Rehacer (Undo/Redo) ilimitado, paleta de colores One UI y alternador de renglones.
- **Persistencia Reactiva Local:**
  - Guardado binario ultrarrápido `.sjink` para los trazos vectoriales.
  - Base de datos local **Room (SQLite)** vinculada en tiempo real: al dibujar en una fecha, el calendario marca inmediatamente el día con un indicador azul `(●)`.
- **Modos Claro y Oscuro Fieles a Samsung Notes:**
  - Paleta exacta con negro puro OLED (`#010101`) en oscuro y contraste tonal con sombra y bordes redondeados (`24.dp`).

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 100%
- **UI Framework:** Jetpack Compose + Compose Material 3 (Adaptive)
- **Canvas S Pen:** Android Custom View + Hardware Acceleration
- **Inyección de Dependencias:** Hilt
- **Base de Datos:** Room + Flow
- **Concurrencia:** Coroutines + StateFlow
- **Build System:** Gradle Kotlin DSL (Version Catalog `libs.versions.toml`)

---

## 🗺️ Roadmap de Desarrollo

- [x] **Fase 1: Scaffolding e Infraestructura**
  - Configuración de dependencias, base de datos Room, arquitectura MVVM/DI y sistema de diseño One UI.
- [x] **Fase 2: UI Adaptativa Tablet-First**
  - Calendario mensual reactivo con swipe táctil, layout de dos paneles con colapso animado y barra de herramientas flotante.
- [x] **Fase 3: Persistencia de Datos y Motor S Pen**
  - Captura y renderizado de trazos vectoriales, Palm Rejection estricto, sensibilidad a la presión, formato binario `.sjink` y auto-guardado en Room.
- [ ] **Fase 4: Encriptación E2E y Sincronización en la Nube (Próximamente)**
  - Encriptación local AES-256 (SQLCipher + Android Keystore) y copia de seguridad en Google Drive.
