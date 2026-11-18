# PROMPTS.md — Fase 2 (rama `mejora-ia`)

Mejoras elegidas (mínimo 3, al menos una en ViewModel o Repository):

| # | Mejora | Capas que cambian | Commit |
|---|---|---|---|
| 1 | Filtrar por categoría | ViewModel + View | `feat(filtro): filtra lugares por categoria con chips en mapa y lista` |
| 2 | Radio de cercanía (100 / 200 / 500 m) | ViewModel + View | `feat(radio): agrega radio de cercania con circulo en el mapa` |
| 3 | Punto azul nativo | View | `feat(mapa): activa punto azul nativo solo con permiso concedido` |

## Registro de prompts

| Prompt que escribí | Qué respondió la IA | Qué cambié o descarté | Por qué |
|---|---|---|---|
| «Haz este laboratorio, pero primero haz el plan» (adjuntando la guía en PDF) | Leyó la guía, armó un plan por pasos con un commit por paso y avisó que el proyecto base no estaba en mi equipo, así que lo recreó a partir de los archivos [E] que vienen en la guía. | Revisé que los archivos [E] coincidieran con la guía (Modelos, interfaces, los 5 lugares, el ViewModel base). | La arquitectura y las firmas tenían que ser exactamente las del laboratorio para que los pasos [G] y [A] encajen. |
| «Hay partes del laboratorio desactualizadas» | Consultó Maven Central y usó versiones actuales: maps-compose 9.0.1 y play-services-location 21.4.0. Mantuvo AGP 9.4.1 y Kotlin 2.4.20 de mi laboratorio anterior, donde Kotlin viene integrado (sin el plugin `kotlin-android`). | El primer intento de leer `local.properties` con `java.util.Properties()` no compiló: en el script de Gradle, `java` es la extensión de Gradle. Se corrigió con `import java.util.Properties`. | La guía es de años anteriores. Compilar (`./gradlew assembleDebug`) después de cada paso detecta estos errores antes del commit. |
| (Prueba en el emulador) La app se quedaba en «Buscando» | Diagnosticó que `getCurrentLocation` no terminaba porque el emulador estaba actualizando Google Play Services, y agregó `withTimeoutOrNull(15 s)` dentro de `UbicacionRepositoryGoogle`. | Se incluyó en el commit del Paso 8. Al cumplirse el tiempo se cancela el token y se lanza `UbicacionNoDisponibleException`, que ya se traducía a `GPS_APAGADO` con *Reintentar*. | El tiempo límite es un detalle de *cómo* se obtiene la ubicación, por eso va en el Repository. El ViewModel y la pantalla reutilizan el estado de error que ya existía. |
| «Filtro por categoría con chips que filtre mapa y lista, respetando MVVM» | Puso `_categoria` en el ViewModel y la lista visible `lugares = combine(_todos, _categoria)`. El Composable `FiltroCategorias` solo muestra los `FilterChip` y llama a `vm.filtrar()`. | Al filtrar, si el lugar seleccionado queda oculto, se deselecciona. `masCercano` se deriva de la lista filtrada. | Filtrar es lógica: si se hiciera con `.filter {}` dentro del Composable se rompería MVVM. Con `SharingStarted.Eagerly`, el mapa, la lista y `masCercano` ven siempre la misma lista. |
| «Radio de cercanía de 100, 200 o 500 m con Circle y marcar los lugares dentro del radio» | `dentroDelRadio: StateFlow<Set<Int>>` calculado en el ViewModel con `combine(_todos, _radio)`. `Circle` en `MapaUbicacion` y la etiqueta «Dentro del radio» en la tarjeta. | El selector está deshabilitado hasta tener ubicación, y tocar el chip activo quita el radio. | Comparar distancias es lógica de negocio, igual que calcularlas (pregunta 4 de *Para pensar*). Sin ubicación no hay centro para el círculo. |
| «Punto azul nativo solo si el permiso fue concedido» | `MapProperties(isMyLocationEnabled)` y `MapUiSettings(myLocationButtonEnabled)` controlados por `puntoAzul`. El permiso se revisa en `PantallaMapa` y se vuelve a comprobar con `LifecycleResumeEffect`. | El permiso se revisa en la View, no en el ViewModel. | El ViewModel no puede recibir `Context`. Con `isMyLocationEnabled = true` sin permiso, Maps lanza `SecurityException`. Volver a comprobar al regresar cubre el caso de activar el permiso desde *Abrir ajustes*. |

## Cómo verifiqué que no se rompiera la arquitectura

- `PantallaMapa`, `MapaUbicacion`, `ListaLugares`, `FiltroCategorias` y `SelectorRadio` no usan ningún Repository ni calculan distancias.
- `MapaViewModel` solo conoce `UbicacionRepository` y `LugaresRepository` (interfaces) y no importa nada de `android.content`.
- Cada mejora compila por separado y está en su propio commit.
