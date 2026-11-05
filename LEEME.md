# Sistema Uber — Movilidad Digital

Proyecto académico desarrollado con Java 21, JavaFX y Maven.

Permite registrar usuarios y conductores, consultar servicios,
solicitar viajes y gestionar su recorrido hasta la finalización
o cancelación.

## Abrir y ejecutar en Eclipse

1. Descomprimir el proyecto.
2. Ir a File → Import → Maven → Existing Maven Projects.
3. Seleccionar la carpeta que contiene pom.xml.
4. Finalizar la importación y esperar la descarga de dependencias.
5. Hacer clic derecho sobre el proyecto → Run As → Maven build…
6. En Goals escribir:

   clean javafx:run

7. Seleccionar JDK 21 en la pestaña JRE y ejecutar.

Ejecutar desde la carpeta del proyecto para encontrar los archivos
de datos y el mapa OSM.

## Cuentas de prueba

El acceso se realiza mediante correo registrado, sin contraseña.

- Cliente: paula.vazquez@email.com
- Conductora: ana.martinez@email.com
- Conductor: juan.perez@email.com

Ana tiene un Ford Focus de categoría ESTANDAR.
Juan tiene un Toyota Corolla de categoría PREMIUM.

La disponibilidad actual depende de los cambios guardados.

## Solicitar y completar un viaje

1. Ingresar como Paula.
2. Entrar en Solicitar viaje.
3. Elegir el origen y el destino haciendo clic en el mapa.
4. Seleccionar el tipo de servicio y la categoría.
5. Presionar Cotizar viaje.
6. Seleccionar una opción y presionar Solicitar viaje.
7. Cambiar a la cuenta de Ana.
8. Entrar en Panel conductor y comprobar que esté disponible.
9. Seleccionar la solicitud compatible y aceptarla.
10. Presionar Iniciar traslado y luego Finalizar viaje.
11. Guardar la calificación del cliente.
12. Cambiar a Paula, abrir el viaje desde Mis viajes y calificar
    a la conductora.

Para cambiar el vehículo activo o la categoría mínima aceptada,
el conductor debe estar fuera de servicio.

## Mapa y rutas

El mapa permite seleccionar puntos en Puerto Madryn.

Utiliza Leaflet para mostrar el mapa y GraphHopper para calcular
recorridos por calles a partir del archivo:

datos/mapas/puerto_madryn_ampliado.osm

La primera preparación del mapa puede tardar. GraphHopper guarda
una caché en:

datos/mapas/cache-auto

La vista previa muestra:

- Punto naranja: ubicación simulada de un conductor compatible.
- Ruta azul: recorrido del conductor hasta el origen.
- Punto azul: origen del viaje.
- Ruta verde: recorrido desde el origen hasta el destino.
- Punto verde: destino del viaje.

La ubicación del conductor es simulada: no se utiliza GPS real.

El conductor mostrado en la vista previa no queda asignado
automáticamente. La aceptación se realiza desde el panel del
conductor.

Actualmente, el cálculo de rutas está implementado para AUTO.

## Cotización

El precio estimado utiliza la distancia directa entre las
coordenadas y el tiempo calculado con la velocidad promedio
configurada.

La ruta dibujada por las calles es una referencia visual y
puede tener una distancia diferente de la utilizada para cotizar.

No se consideran condiciones de tránsito en tiempo real.

## Cancelaciones y vencimiento

- El cliente puede cancelar antes de la aceptación sin cargo.
- Después de la aceptación, la cancelación del cliente calcula
  un cargo con la tarifa base, los kilómetros informados y el
  tiempo transcurrido desde la aceptación.
- Si cancela el conductor, el cargo para el cliente es cero.
- Toda cancelación requiere un motivo.
- Las solicitudes vencen después de cinco minutos sin aceptación.
- Los importes son informativos: no se realizan cobros electrónicos.

## Calificaciones

Al finalizar el viaje, cliente y conductor pueden calificarse.

Las opciones son Malo, Regular, Bueno, Muy bueno y Excelente.

Cada usuario ve únicamente la evaluación que él mismo guardó.
Las calificaciones pueden actualizarse en viajes finalizados.

Los viajes cancelados no muestran la opción de calificar.

## Persistencia

Los archivos TXT de recursos contienen los datos iniciales.

Los cambios se guardan en:

datos/uber-eventos.txt

Esto permite conservar usuarios registrados, vehículos,
disponibilidad, estados de viajes, cancelaciones y calificaciones
al cerrar y volver a abrir la aplicación.

No borrar ni editar manualmente ese archivo durante el uso normal.

La aplicación está pensada para ejecutarse en un solo proceso,
alternando las cuentas desde Cambiar cuenta.

## Conexión a Internet

Actualmente se necesita Internet para descargar Leaflet y
las imágenes del mapa de OpenStreetMap.

GraphHopper calcula las rutas utilizando el archivo OSM local.
Ese archivo contiene datos de calles, no las imágenes del mapa.

La primera ejecución de Maven también necesita descargar
las dependencias del proyecto.

## Pruebas automáticas

Para ejecutar las pruebas desde Maven:

mvn test

Para ejecutar también las comprobaciones de interfaz del perfil
ui-check:

mvn -Pui-check test

En Eclipse pueden ejecutarse desde Run As → Maven build…
escribiendo test o -Pui-check test en Goals.

Se requiere JDK 21. El resultado de cada ejecución debe comprobarse
en la consola y en los informes de target/surefire-reports.

## Pruebas manuales realizadas

Se comprobaron los siguientes recorridos:

- Selección de origen y destino.
- Visualización de rutas.
- Cotización y solicitud.
- Aceptación, inicio y finalización.
- Calificaciones del cliente y del conductor.
- Cancelación del cliente antes y después de la aceptación.
- Cancelación del conductor sin cargo para el cliente.
- Visualización del motivo y del historial de cancelación.
- Conservación del historial al cerrar y volver a abrir.

También se confirmó previamente el vencimiento de solicitudes.

## Alcance

Es una aplicación académica de demostración.

No incluye autenticación real, pagos electrónicos, seguimiento
GPS, movimiento en tiempo real ni sincronización entre equipos.