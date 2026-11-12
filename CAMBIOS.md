# Cambios respecto del ZIP enviado

## Interfaz
- Se reemplazó AplicacionUberFX por un lanzador que mantiene una sesión compartida.
- Nuevo movilidad.ui.UberController: navegación y acciones de todas las pantallas.
- Los nueve FXML se reorganizaron con estilo común en uber.css, tarjetas, menú lateral, estados y selección visible.
- Confirmar abre el detalle, con progreso y acciones según la cuenta y el estado.
- Lugares de demostración y modo de coordenadas; no se agregó servicio de mapas.

## Datos y lógica
- Nuevo movilidad.ui.SesionUber: registro persistente de acciones y recuperación de usuarios, vehículos y viajes; guardado a archivo temporal y reemplazo antes de publicar el nuevo estado en memoria.
- AplicacionConsultas.cargarDatos es público para reutilizar la carga existente.
- CargaParametros incorpora lectura de velocidadPromedioKmH para estimaciones de la lógica.
- MovilidadDigital y Viaje: compatibilidad entre tipo de vehículo, tipo de servicio, categoría mínima y categoría del vehículo; cancelación también en viaje iniciado; cargo con tiempo transcurrido desde aceptación y kilómetros informados; validación de valores no finitos; corrección del orden de calificaciones al finalizar.
- El vencimiento de solicitudes se revisa cada dos segundos con Timeline, y al abrir pantallas; los vencimientos se guardan. Al reiniciar se revisan las solicitudes recuperadas.

## Pruebas y construcción
- Se conservaron las pruebas originales.
- Nuevas SesionUberTest e InterfazFlowTest.
- Perfil Maven ui-check opcional para pruebas JavaFX headless; la ejecución normal sigue siendo clean javafx:run.
- Nuevo artifactId sistema-uber-conectado para importarlo junto al proyecto anterior.

No se incluyeron archivos compilados ni datos de las pruebas en el ZIP.

---

# Integración del Mapa Interactivo y Simulación de Movilidad en Detalle del Viaje

## 1. Cambios implementados en la vista y el controlador

- Integración de WebView en DetalleViaje.fxml:
  Se sustituyó la visualización de la ruta en texto plano por un contenedor interactivo WebView (fx:id="mapaWeb") embebido directamente en la pantalla de seguimiento del viaje.

- Conexión con el motor Leaflet (UberController.java):
  Se enlazó el WebView con mapa.html, cargando los estilos y capas cartográficas de OpenStreetMap de forma sincronizada con el idioma activo de la aplicación.

- Optimización concurrente del cálculo de rutas:
  El cálculo de geometrías de las calles mediante GraphHopper (PlanificadorRutas) se desacopló del hilo de la interfaz de usuario (JavaFX Application Thread), ejecutándose dentro de tareas concurrentes en segundo plano (Task<T> daemon).

- Caché de estado y viaje:
  Para no recalcular rutas innecesariamente en cada ciclo del temporizador de actualización periódica (Timeline cada 2 segundos), se implementó un mecanismo de caché que solo dispara el recálculo cuando cambian el identificador del viaje o su estado de negocio.

- Control del botón de inicio de traslado:
  Se bloqueó el botón "Iniciar traslado" (setDisable(true)) mientras el conductor está en camino al punto de recogida. El botón se habilita de forma reactiva únicamente cuando el vehículo arriba al punto de origen.

## 2. Implementación de la movilidad y simulación en tiempo real

Para otorgar movimiento continuo y visualmente fluido al vehículo sin depender de hardware GPS real ni saturar el procesador, se combinó JavaFX con el API de Leaflet mediante las siguientes técnicas:

- Discretización de coordenadas en Leaflet:
  A partir de la polilínea calculada por GraphHopper (List<PuntoRuta>), Java serializa los puntos como arreglos JavaScript de pares [latitud, longitud].

- Interpolación temporal por pasos (setInterval):
  En mapa.html se crearon temporizadores ligeros que avanzan el marcador circular del vehículo (L.circleMarker) a lo largo del arreglo de puntos:
  1. En estado ACEPTADO: el vehículo recorre la ruta de recogida hasta el punto de origen (pickup).
  2. En estado INICIADO: el vehículo se traslada a lo largo de la ruta de viaje hasta el punto de destino final.
  El tamaño del paso (pasoViaje / pasoCond) se calcula dinámicamente en función de la cantidad total de puntos para que la animación tenga una duración uniforme y natural (entre 5 y 9 segundos).

- Marcadores semánticos en el mapa:
  Se agregaron marcadores distintivos con tooltip para el punto de Origen (azul), Destino (rojo) y Conductor/Vehículo (naranja). Al finalizar el recorrido, el marcador del conductor queda posicionado de forma fija sobre el destino.

- Puente bidireccional JavaScript - Java (JSObject / javaBridge):
  Se implementó una clase interna PuenteDetalle inyectada en el objeto window del navegador web. Esto permite sincronizar los eventos visuales del mapa con las reglas de negocio del sistema:
  1. onConductorLlegadoAlOrigen(): se dispara cuando la animación del conductor alcanza el origen. En Java, cambia el estado del botón a habilitado y actualiza los avisos textuales informando que el traslado puede comenzar.
  2. onViajeLlegadoAlDestino(): se dispara en el instante exacto en que el vehículo llega al destino final. En Java, ejecuta de manera automática la acción de finalizar viaje (s().ejecutar("FINALIZAR", ...)), actualizando la máquina de estados, persistiendo el cambio y habilitando el panel de calificación del servicio.

