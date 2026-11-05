package movilidad.ui;

import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import netscape.javascript.JSObject;

import movilidad.aplicacion.AplicacionUberFX;
import movilidad.modelo.CategoriaVehiculo;
import movilidad.modelo.Servicio;
import movilidad.modelo.TipoServicio;
import movilidad.modelo.Ubicacion;
import movilidad.modelo.Viaje;
import movilidad.rutas.PuntoRuta;
import movilidad.rutas.RutaCalculada;

public class MapaController {

    @FXML
    private WebView mapaWeb;

    @FXML
    private TextField origenTexto;

    @FXML
    private TextField destinoTexto;

    @FXML
    private ComboBox<TipoServicio> tipoServicio;

    @FXML
    private ComboBox<CategoriaVehiculo> categoria;

    @FXML
    private ListView<Servicio> opciones;

    @FXML
    private Button btnSolicitar;

    private AplicacionUberFX aplicacion;

    private Ubicacion origenCotizado;
    private Ubicacion destinoCotizado;

    private long versionRuta;
    private boolean pantallaCerrada;

    private Task<ResultadoRutas> tareaRutas;

    private final PuenteMapa puente = new PuenteMapa();

    @FXML
    private void initialize() {
        tipoServicio.getItems().setAll(TipoServicio.values());
        categoria.getItems().setAll(CategoriaVehiculo.values());

        tipoServicio.setValue(TipoServicio.PASAJEROS);
        categoria.setValue(CategoriaVehiculo.ESTANDAR);

        if (btnSolicitar != null) {
            btnSolicitar.setDisable(true);
        }

        opciones.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, anterior, actual) -> {
                if (btnSolicitar != null) {
                    btnSolicitar.setDisable(
                        actual == null
                            || origenCotizado == null
                            || destinoCotizado == null
                    );
                }

                if (actual != null) {
                    dibujarRutaSeleccionada();
                }
            });

        origenTexto.textProperty().addListener(
            (observable, anterior, actual) -> limpiarCotizacion()
        );

        destinoTexto.textProperty().addListener(
            (observable, anterior, actual) -> limpiarCotizacion()
        );

        tipoServicio.valueProperty().addListener(
            (observable, anterior, actual) -> limpiarCotizacion()
        );

        categoria.valueProperty().addListener(
            (observable, anterior, actual) -> limpiarCotizacion()
        );

        mapaWeb.sceneProperty().addListener(
            (observable, anterior, actual) -> {
                if (anterior != null && actual == null) {
                    cerrarPantalla();
                }
            }
        );

        WebEngine motor = mapaWeb.getEngine();

        motor.getLoadWorker().stateProperty().addListener(
            (observable, anterior, actual) -> {
                if (actual == Worker.State.SUCCEEDED
                        && !pantallaCerrada) {
                    try {
                        conectarClics(motor);
                        dibujarRutaSeleccionada();
                    } catch (Exception e) {
                        avisar(
                            "No se pudieron conectar los clics: "
                                + e.getMessage()
                        );
                    }
                }
            }
        );

        URL archivo = getClass().getResource("/ui/map/mapa.html");

        if (archivo == null) {
            throw new IllegalStateException(
                "No se encontró ui/map/mapa.html"
            );
        }

        motor.load(archivo.toExternalForm());
    }

    public void setAplicacion(AplicacionUberFX aplicacion) {
        this.aplicacion = aplicacion;
    }

    private void conectarClics(WebEngine motor) {
        JSObject ventana = (JSObject) motor.executeScript("window");
        ventana.setMember("javaBridge", puente);

        motor.executeScript("""
            if (typeof mapa !== "undefined") {
                var puntoOrigen = null;
                var puntoDestino = null;
                var eligiendoOrigen = true;

                aviso.textContent =
                    "Hacé clic para elegir el origen.";

                mapa.on("click", function (evento) {
                    var lat = evento.latlng.lat;
                    var lon = evento.latlng.lng;

                    if (lat < -42.832 || lat > -42.704 ||
                        lon < -65.126 || lon > -65.005) {
                        aviso.textContent =
                            "Elegí un punto dentro de Puerto Madryn.";
                        return;
                    }

                    if (eligiendoOrigen) {
                        if (puntoOrigen) {
                            mapa.removeLayer(puntoOrigen);
                        }

                        if (puntoDestino) {
                            mapa.removeLayer(puntoDestino);
                            puntoDestino = null;
                        }

                        if (typeof window.limpiarRutas === "function") {
                            window.limpiarRutas();
                        }

                        puntoOrigen = L.circleMarker([lat, lon], {
                            radius: 9,
                            color: "#ffffff",
                            weight: 3,
                            fillColor: "#2864D4",
                            fillOpacity: 1
                        }).addTo(mapa);

                        puntoOrigen.bindTooltip("Origen");

                        window.javaBridge.seleccionar(
                            "ORIGEN", lat, lon
                        );

                        eligiendoOrigen = false;

                        aviso.textContent =
                            "Origen elegido. Ahora elegí el destino.";
                    } else {
                        puntoDestino = L.circleMarker([lat, lon], {
                            radius: 9,
                            color: "#ffffff",
                            weight: 3,
                            fillColor: "#22c55e",
                            fillOpacity: 1
                        }).addTo(mapa);

                        puntoDestino.bindTooltip("Destino");

                        window.javaBridge.seleccionar(
                            "DESTINO", lat, lon
                        );

                        eligiendoOrigen = true;

                        aviso.textContent =
                            "Recorrido elegido. Tocá Cotizar viaje.";
                    }
                });
            }
            """);
    }

    private void cancelarCalculoRuta() {
        versionRuta++;

        if (tareaRutas != null) {
            tareaRutas.cancel();
            tareaRutas = null;
        }
    }

    private void cerrarPantalla() {
        pantallaCerrada = true;
        cancelarCalculoRuta();
    }

    private void limpiarCotizacion() {
        cancelarCalculoRuta();

        origenCotizado = null;
        destinoCotizado = null;

        opciones.getItems().clear();

        if (btnSolicitar != null) {
            btnSolicitar.setDisable(true);
        }

        limpiarRutaVisible();
    }

    private void limpiarRutaVisible() {
        WebEngine motor = mapaWeb.getEngine();

        if (motor.getLoadWorker().getState()
                == Worker.State.SUCCEEDED) {

            motor.executeScript("""
                if (typeof window.limpiarRutas === "function") {
                    window.limpiarRutas();
                }
                """);
        }
    }

    private void mostrarAvisoMapa(String mensaje) {
        JSObject avisoMapa = (JSObject)
            mapaWeb.getEngine().executeScript(
                "document.getElementById('aviso')"
            );

        avisoMapa.setMember("textContent", mensaje);
    }

    private Ubicacion leerPunto(TextField campo) {
        String texto = campo.getText();

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                "Primero elegí origen y destino en el mapa."
            );
        }

        String[] partes = texto.split(",");

        if (partes.length != 2) {
            throw new IllegalArgumentException(
                "Primero elegí origen y destino en el mapa."
            );
        }

        double lat = Double.parseDouble(partes[0].trim());
        double lon = Double.parseDouble(partes[1].trim());

        if (!puntoValido(lat, lon)) {
            throw new IllegalArgumentException(
                "Elegí puntos dentro de Puerto Madryn."
            );
        }

        return new Ubicacion(lat, lon);
    }

    private boolean puntoValido(double lat, double lon) {
        return Double.isFinite(lat)
            && Double.isFinite(lon)
            && lat >= -42.832
            && lat <= -42.704
            && lon >= -65.126
            && lon <= -65.005;
    }

    private void exigirAplicacion() {
        if (aplicacion == null) {
            throw new IllegalStateException(
                "Falta conectar setAplicacion en AplicacionUberFX."
            );
        }
    }

    @FXML
    private void cotizarViaje() {
        try {
            exigirAplicacion();
            limpiarCotizacion();

            Ubicacion origen = leerPunto(origenTexto);
            Ubicacion destino = leerPunto(destinoTexto);

            double km = origen.calcularDistancia(destino);

            if (km <= 0) {
                throw new IllegalArgumentException(
                    "Elegí un origen y un destino diferentes."
                );
            }

            if (tipoServicio.getValue() == null
                    || categoria.getValue() == null) {
                throw new IllegalArgumentException(
                    "Elegí tipo de servicio y categoría."
                );
            }

            var sistema = aplicacion.sesion.sistema;

            var servicios = sistema.consultarServicios(
                tipoServicio.getValue(),
                categoria.getValue()
            );

            opciones.setCellFactory(lista -> new ListCell<Servicio>() {

                @Override
                protected void updateItem(
                        Servicio servicio, boolean vacia) {

                    super.updateItem(servicio, vacia);
                    setGraphic(null);

                    if (vacia || servicio == null) {
                        setText(null);
                        return;
                    }

                    double minutos = sistema.estimarTiempo(
                        servicio, origen, destino
                    );

                    double precio = servicio.calcularCosto(
                        km, minutos
                    );

                    setText(String.format(
                        Locale.forLanguageTag("es-AR"),
                        "%s · %s · %.2f km · %.1f min "
                            + "· $%,.2f estimados",
                        servicio.getNombre(),
                        servicio.getTipoVehiculo(),
                        km,
                        minutos,
                        precio
                    ));

                    setStyle(
                        "-fx-text-fill: white;"
                            + "-fx-padding: 14;"
                    );
                }
            });

            origenCotizado = origen;
            destinoCotizado = destino;

            opciones.getItems().setAll(servicios);

            if (servicios.isEmpty()) {
                avisar(
                    "No hay servicios para esa combinación. "
                        + "Probá otra categoría."
                );
            } else {
                opciones.getSelectionModel().selectFirst();
            }

        } catch (Exception e) {
            avisar(e.getMessage());
        }
    }

    private void dibujarRutaSeleccionada() {
        if (mapaWeb.getEngine().getLoadWorker().getState()
                != Worker.State.SUCCEEDED) {
            return;
        }

        Servicio servicio = opciones.getSelectionModel()
            .getSelectedItem();

        if (servicio == null
                || origenCotizado == null
                || destinoCotizado == null
                || pantallaCerrada) {
            return;
        }

        cancelarCalculoRuta();
        final long version = versionRuta;

        try {
            exigirAplicacion();
            limpiarRutaVisible();

            PuntoRuta origen = new PuntoRuta(
                origenCotizado.getLatitud(),
                origenCotizado.getLongitud()
            );

            PuntoRuta destino = new PuntoRuta(
                destinoCotizado.getLatitud(),
                destinoCotizado.getLongitud()
            );

            var tipoVehiculo = servicio.getTipoVehiculo();

            var sistema = aplicacion.sesion.sistema;
            var cliente = sistema.buscarUsuarioPorMail(
                aplicacion.sesion.correo
            );

            if (cliente == null) {
                throw new IllegalStateException(
                    "No se encontró el usuario de la sesión."
                );
            }

            // Este objeto se usa solo para consultar compatibilidad.
            // No se registra ni se solicita el viaje.
            Viaje consulta = new Viaje(
                origenCotizado,
                destinoCotizado,
                cliente,
                servicio
            );

            var candidatos =
                sistema.buscarConductoresDisponibles(consulta)
                    .stream()
                    .map(usuario -> java.util.Map.entry(
                        usuario.getEmail(),
                        usuario.getNombre()
                    ))
                    .toList();

            mostrarAvisoMapa(
                "Calculando rutas… La primera preparación "
                    + "del mapa puede tardar."
            );

            Task<ResultadoRutas> tarea = new Task<>() {

                @Override
                protected ResultadoRutas call() {
                    var planificador =
                        aplicacion.getPlanificadorRutas();

                    if (isCancelled()) {
                        return null;
                    }

                    PuntoRuta origenEnCalle =
                        planificador.ajustarACalle(
                            origen, tipoVehiculo
                        ).orElseThrow(() ->
                            new IllegalArgumentException(
                                "El origen está lejos de una calle "
                                    + "habilitada. Elegí otro punto."
                            )
                        );

                    PuntoRuta destinoEnCalle =
                        planificador.ajustarACalle(
                            destino, tipoVehiculo
                        ).orElseThrow(() ->
                            new IllegalArgumentException(
                                "El destino está lejos de una calle "
                                    + "habilitada. Elegí otro punto."
                            )
                        );

                    if (isCancelled()) {
                        return null;
                    }

                    RutaCalculada rutaViaje =
                        planificador.calcular(
                            origenEnCalle,
                            destinoEnCalle,
                            tipoVehiculo
                        );

                    RutaCalculada rutaConductor = null;
                    String nombreConductor = null;

                    double distanciaMenor =
                        Double.POSITIVE_INFINITY;

                    var ubicaciones =
                        aplicacion.getUbicacionesConductores();

                    Ubicacion ubicacionOrigen = new Ubicacion(
                        origenEnCalle.latitud(),
                        origenEnCalle.longitud()
                    );

                    for (var candidato : candidatos) {
                        if (isCancelled()) {
                            return null;
                        }

                        PuntoRuta posicion;

                        try {
                            posicion = ubicaciones.obtener(
                                candidato.getKey(),
                                tipoVehiculo,
                                planificador
                            );
                        } catch (IllegalStateException e) {
                            continue;
                        }

                        double distancia = new Ubicacion(
                            posicion.latitud(),
                            posicion.longitud()
                        ).calcularDistancia(ubicacionOrigen);

                        if (distancia >= distanciaMenor) {
                            continue;
                        }

                        try {
                            RutaCalculada recogida =
                                planificador.calcular(
                                    posicion,
                                    origenEnCalle,
                                    tipoVehiculo
                                );

                            rutaConductor = recogida;
                            nombreConductor =
                                candidato.getValue();

                            distanciaMenor = distancia;

                        } catch (IllegalArgumentException e) {
                            // Si no hay conexión vial,
                            // se prueba otro candidato.
                        }
                    }

                    return new ResultadoRutas(
                        rutaViaje,
                        rutaConductor,
                        nombreConductor
                    );
                }
            };

            tarea.setOnSucceeded(evento -> {
                if (!resultadoVigente(version)) {
                    return;
                }

                ResultadoRutas resultado = tarea.getValue();

                if (resultado == null) {
                    return;
                }

                try {
                    String puntosViaje = convertirPuntos(
                        resultado.viaje()
                    );

                    String puntosConductor =
                        resultado.recogida() == null
                            ? "[]"
                            : convertirPuntos(
                                resultado.recogida()
                            );

                    mapaWeb.getEngine().executeScript(
                        "window.mostrarRutas("
                            + puntosConductor + ","
                            + puntosViaje + ");"
                    );

                    if (resultado.conductor() == null) {
                        mostrarAvisoMapa(
                            "Ruta del viaje calculada. "
                                + "Sin conductor compatible con "
                                + "ruta disponible para la "
                                + "vista previa."
                        );
                    } else {
                        mostrarAvisoMapa(
                            "Vista previa: "
                                + resultado.conductor()
                                + " · Ubicación simulada. "
                                + "Azul: hasta el origen. "
                                + "Verde: hasta el destino. "
                                + "Aceptación pendiente."
                        );
                    }

                } catch (Exception e) {
                    avisar(
                        "No se pudieron dibujar las rutas: "
                            + e.getMessage()
                    );
                }
            });

            tarea.setOnFailed(evento -> {
                if (!resultadoVigente(version)) {
                    return;
                }

                mostrarAvisoMapa(
                    "No se pudieron calcular las rutas."
                );

                avisar(
                    "La cotización está disponible, pero no se "
                        + "pudieron mostrar las rutas: "
                        + tarea.getException().getMessage()
                );
            });

            tareaRutas = tarea;

            Thread hilo = new Thread(tarea, "calculo-rutas");
            hilo.setDaemon(true);
            hilo.start();

        } catch (Exception e) {
            avisar(
                "No se pudo preparar la vista previa: "
                    + e.getMessage()
            );
        }
    }

    private record ResultadoRutas(
            RutaCalculada viaje,
            RutaCalculada recogida,
            String conductor) {
    }

    private boolean resultadoVigente(long version) {
        return !pantallaCerrada
            && version == versionRuta
            && mapaWeb.getScene() != null;
    }

    private String convertirPuntos(RutaCalculada ruta) {
        StringBuilder resultado = new StringBuilder("[");

        for (PuntoRuta punto : ruta.puntos()) {
            if (resultado.length() > 1) {
                resultado.append(",");
            }

            resultado.append("[")
                .append(punto.latitud())
                .append(",")
                .append(punto.longitud())
                .append("]");
        }

        return resultado.append("]").toString();
    }

    @FXML
    private void solicitarViaje() {
        try {
            exigirAplicacion();

            Servicio servicio = opciones.getSelectionModel()
                .getSelectedItem();

            if (servicio == null
                    || origenCotizado == null
                    || destinoCotizado == null) {
                throw new IllegalArgumentException(
                    "Primero cotizá y seleccioná un servicio."
                );
            }

            var sesion = aplicacion.sesion;
            UUID id = UUID.randomUUID();

            sesion.ejecutar(
                "SOLICITAR",
                sesion.correo,
                id.toString(),
                servicio.getNombre(),
                Double.toString(origenCotizado.getLatitud()),
                Double.toString(origenCotizado.getLongitud()),
                Double.toString(destinoCotizado.getLatitud()),
                Double.toString(destinoCotizado.getLongitud())
            );

            sesion.viajeSeleccionado = id;
            aplicacion.mostrar("DetalleViaje.fxml");

            cerrarPantalla();

        } catch (Exception e) {
            avisar(e.getMessage());
        }
    }

    @FXML
    private void volverMenu() {
        try {
            exigirAplicacion();
            aplicacion.mostrar("MenuPrincipal.fxml");

            cerrarPantalla();

        } catch (Exception e) {
            avisar(e.getMessage());
        }
    }

    private void avisar(String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);

        alerta.setTitle("Sistema Uber");
        alerta.setHeaderText(null);

        alerta.setContentText(
            texto == null
                ? "No se pudo completar la acción."
                : texto
        );

        alerta.showAndWait();
    }

    public class PuenteMapa {

        public void seleccionar(String tipo, double lat, double lon) {
            if (!puntoValido(lat, lon)) {
                return;
            }

            Platform.runLater(() -> {
                if (pantallaCerrada) {
                    return;
                }

                String coordenadas = String.format(
                    Locale.US, "%.5f, %.5f", lat, lon
                );

                if ("ORIGEN".equals(tipo)) {
                    origenTexto.setText(coordenadas);
                    destinoTexto.clear();

                } else if ("DESTINO".equals(tipo)) {
                    destinoTexto.setText(coordenadas);
                }
            });
        }
    }
}