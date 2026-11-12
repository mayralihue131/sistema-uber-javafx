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

- Cliente: `paula.vazquez@email.com`
- Cliente: `carlos.rodriguez@email.com`
- Conductora: `ana.martinez@email.com` (Ford Focus — `AUTO`, categoría `ESTANDAR`, Pasajeros y Envíos)
- Conductor: `juan.perez@email.com` (Toyota Corolla — `AUTO`, categoría `PREMIUM`, Pasajeros)
- Conductor: `diego.sanchez@email.com` (Chevrolet Cruze — `AUTO`, categoría `CONFORT`, Pasajeros y Envíos)
- Conductor: `martin.moto@email.com` (Honda Wave 110 — `MOTO`, categoría `ESTANDAR`, Pasajeros)

La disponibilidad actual depende de los cambios guardados.

*(Nota: Si el origen de datos está configurado en `BD`, las cuentas disponibles serán las registradas en la tabla `usuario` de PostgreSQL más cualquier usuario nuevo que registres desde la pantalla **Crear una cuenta**).*

## Configuración del sistema

Los archivos de configuración se encuentran en `src/main/resources/`:

### 1. Interfaz e Idioma (`application.properties`)
```properties
app.ui=javafx
app.language=es
app.country=AR
```
- `app.ui`: `javafx` (interfaz gráfica) o `cli` (consola).
- `app.language`: idioma inicial (`es` para Español o `en` para Inglés). Además, desde la pantalla de **Inicio** podés cambiar el idioma en tiempo real con el selector **Idioma**.

### 2. Origen de datos y parámetros (`config.properties`)
```properties
origen.datos=BD
velocidadPromedioKmH=40
```
- `origen.datos`: define dónde obtiene y guarda la información la aplicación:
  - `BD`: se conecta a la base de datos PostgreSQL configurada en `jdbc.properties`.
  - `TXT`: carga los datos iniciales desde `usuarios_test.txt`, `vehiculos_test.txt` y `servicios_test.txt`.
  - `MEMORIA`: utiliza colecciones en memoria RAM sin lectura externa (ideal para pruebas).

### 3. Conexión a Base de Datos (`jdbc.properties`)
```properties
driver=org.postgresql.Driver
url=jdbc:postgresql://pgs.fi.mdn.unp.edu.ar:30000/bd1
schema=movilidad_digital
usr=estudiante
pwd=bd1estudiante
```
- `url`: servidor, puerto y nombre de la base de datos PostgreSQL.
- `schema`: esquema donde residen las tablas del sistema (`movilidad_digital`).
- `usr` y `pwd`: credenciales de acceso a PostgreSQL.

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

datos/mapas/cache-auto-moto

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

El cálculo de rutas por calles está implementado tanto para **AUTO**
como para **MOTO**.

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

## Ejemplos de resultados según opciones del usuario

### 1. Cotización de un mismo recorrido (5 km · 7,5 min a 40 km/h)
Fórmula: `Tarifa Base + (km × Precio/km) + (min × Precio/min)`.

| Tipo / Categoría elegida | Servicio mostrado | Tarifa Base | Precio/km | Precio/min | Total estimado |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `PASAJEROS` + `ESTANDAR` | **Mototaxi** (`MOTO`) | \$1.000 | \$100 | \$50 | **\$1.875,00** |
| `PASAJEROS` + `ESTANDAR` | **Estandar** (`AUTO`) | \$2.000 | \$200 | \$100 | **\$3.750,00** |
| `PASAJEROS` + `CONFORT` | **Confort** (`AUTO`) | \$2.500 | \$250 | \$125 | **\$4.687,50** |
| `PASAJEROS` + `PREMIUM` | **Premium** (`AUTO`) | \$3.000 | \$300 | \$150 | **\$5.625,00** |
| `ENVIOS` + `ESTANDAR` | **Envios** (`AUTO`) | \$1.500 | \$150 | \$75 | **\$2.812,50** |
| `ENVIOS` + `PREMIUM` | *(Sin resultados)* | — | — | — | **Mensaje de aviso** |

### 2. Solicitudes visibles para el conductor según su categoría mínima
Se exige: `Categoría Mínima Aceptada <= Categoría del Servicio <= Categoría del Vehículo Activo`.

- **Juan Pérez (`AUTO` `PREMIUM`) con mínima `ESTANDAR`:** recibe solicitudes `ESTANDAR`, `CONFORT` y `PREMIUM` de pasajeros.
- **Juan Pérez con mínima `PREMIUM`:** solo ve solicitudes `PREMIUM`; las demás dejan de mostrarse.
- **Ana Martínez (`AUTO` `ESTANDAR`):** recibe `ESTANDAR` (pasajeros) y `ENVIOS`. Si intenta fijar mínima `CONFORT`, el sistema lo impide.
- **Martín Soto (`MOTO` `ESTANDAR`):** solo ve solicitudes de `Mototaxi` (`MOTO`).

### 3. Cancelación según momento y usuario (ejemplo servicio Estandar)
- **Cliente cancela antes de ser aceptado (`SOLICITADO`):** cargo = **\$0,00**.
- **Cliente cancela después de aceptar (`ACEPTADO`)** con 2 km y 3 min transcurridos: cargo = **\$2.700,00** (`2000 + 2×200 + 3×100`).
- **Conductor cancela (`ACEPTADO` o `INICIADO`):** cargo al cliente = **\$0,00** y el conductor regresa a `DISPONIBLE`.

## Persistencia y Base de Datos

El sistema utiliza el patrón **DAO** configurado mediante `origen.datos` en `src/main/resources/config.properties`:

### Modo Base de Datos (`origen.datos=BD`)
- Se conecta al servidor **PostgreSQL** especificado en `src/main/resources/jdbc.properties` (esquema `movilidad_digital` en `pgs.fi.mdn.unp.edu.ar:30000/bd1`) manteniendo una conexión persistente durante la sesión.
- **Carga inicial desde PostgreSQL:**
  - Tabla `usuario` junto con `conductor_vehiculo` (usuarios, licencias de conducir y patente del vehículo activo).
  - Tabla `vehiculo` (patente, modelo, capacidad, tipo, categoría y máscara de bits `tipo_servicios`: `1` = Pasajeros, `2` = Envíos, `3` = Ambos).
  - Tabla `servicio` (tarifas base, por kilómetro y por minuto).
- **Alta de nuevos usuarios:** Al registrarse desde la pantalla **Crear una cuenta**, el usuario se inserta directamente en la tabla `usuario` (`INSERT INTO usuario (email, nombre, telefono)`) y queda disponible en la base de datos. Si el correo ya existe en la tabla, el sistema informa el error sin duplicar registros.
- **Tolerancia a fallos (Fallback automático):** Si al iniciar en modo `BD` no hay conexión de red o el servidor PostgreSQL no responde, la aplicación advierte por consola y carga automáticamente los archivos `.txt` locales para no interrumpir la ejecución.

### Modo Archivos de Texto (`origen.datos=TXT`) y Log Local
- Carga los datos iniciales desde los archivos `.txt` en `src/main/resources/` (`usuarios_test.txt`, `vehiculos_test.txt`, `servicios_test.txt`).
- Las acciones operativas durante la sesión (altas en modo TXT, vehículos agregados, cambios de estado, solicitudes, viajes, cancelaciones y calificaciones) se guardan en:

  `datos/uber-eventos.txt`

  Esto permite conservar el historial y los cambios al cerrar y volver a abrir la aplicación. No borrar ni editar manualmente ese archivo durante el uso normal.

### Modo Memoria (`origen.datos=MEMORIA`)
- Mantiene los datos únicamente en memoria RAM durante la ejecución actual (utilizado principalmente por las pruebas unitarias automatizadas).

La aplicación está pensada para ejecutarse en un solo proceso, alternando las cuentas desde **Cambiar cuenta**.

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