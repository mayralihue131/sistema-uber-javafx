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
