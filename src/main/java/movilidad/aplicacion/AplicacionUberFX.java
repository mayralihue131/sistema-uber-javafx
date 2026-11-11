package movilidad.aplicacion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import movilidad.modelo.Servicio;
import movilidad.rutas.FabricaGraphHopper;
import movilidad.rutas.PlanificadorGraphHopper;
import movilidad.rutas.PlanificadorRutas;
import movilidad.rutas.ServicioRutasViaje;
import movilidad.ui.MapaController;
import movilidad.ui.SesionUber;
import movilidad.ui.UberController;

public class AplicacionUberFX extends Application {

    public SesionUber sesion;

    private Stage ventana;
    private UberController controlador;
    private BorderPane pantallaPrincipal;

    private PlanificadorRutas planificadorRutas;
    private ServicioRutasViaje servicioRutas;
    private boolean cerrada;
    private ResourceBundle mensajes;
    private Locale idioma;
    private ButtonType botonSi;
    private ButtonType botonNo;

    private final movilidad.rutas.UbicacionesConductores
    ubicacionesConductores =
        new movilidad.rutas.UbicacionesConductores();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ventana = stage;

        try {
            cargarIdioma();
            ventana.setTitle(texto("app.titulo"));
            sesion = new SesionUber(
                    Path.of(System.getProperty(
                            "uber.datos",
                            "datos/uber-eventos.txt")));

            mostrar("Inicio.fxml");
            ventana.setMinWidth(650);
            ventana.setMinHeight(550);
       
            ventana.show();

        } catch (Exception e) {
            Alert alerta = new Alert(
                Alert.AlertType.ERROR,
                (mensajes == null
                    ? "No se pudo abrir el proyecto: " + e.getMessage()
                    : texto("error.inicio") + " " + mensajeError(e)),
                mensajes == null ? ButtonType.OK : botonAceptar()
            );
            
            alerta.showAndWait();
        }
    }

    public synchronized PlanificadorRutas getPlanificadorRutas() {
        if (cerrada) {
            throw new IllegalStateException(
                    "La aplicación ya está cerrada.");
        }

        if (planificadorRutas == null) {
            planificadorRutas = new PlanificadorGraphHopper(
                    FabricaGraphHopper.crear());

            servicioRutas = new ServicioRutasViaje(
                    planificadorRutas);
        }

        return planificadorRutas;
    }

    public synchronized ServicioRutasViaje getServicioRutas() {
        getPlanificadorRutas();
        return servicioRutas;
    }

    public movilidad.rutas.UbicacionesConductores getUbicacionesConductores() {

        return ubicacionesConductores;
    }

    public void mostrar(String archivo) throws IOException {
        mostrar(archivo, "");
    }

    private void mostrar(String archivo, String correoEscrito) throws IOException {
        URL recurso = getClass().getResource("/" + archivo);

        if (recurso == null) {
            throw new IOException(
                    "No se encontró la pantalla: " + archivo);
        }

        FXMLLoader loader = new FXMLLoader(recurso, mensajes);

        if (archivo.equals("SolicitarViajeMapa.fxml")) {
            Parent root = loader.load();

            MapaController mapa = loader.getController();
            mapa.setAplicacion(this);

            detenerControlador();
            cambiarPantalla(archivo, root);
            return;
        }

        UberController nuevo = new UberController(this, archivo);
        loader.setController(nuevo);

        Parent root = loader.load();

        if ("Inicio.fxml".equals(archivo)) {
            @SuppressWarnings("unchecked")
            ComboBox<String> selector =
                    (ComboBox<String>) loader.getNamespace().get("selectorIdioma");
            TextField correo = (TextField) loader.getNamespace().get("campoCorreo");
            correo.setText(correoEscrito);
            configurarSelector(selector, correo);
        }

        detenerControlador();
        cambiarPantalla(archivo, root);

        controlador = nuevo;
        nuevo.preparar(root);
    }

    private void cargarIdioma() throws IOException {
        Properties configuracion = new Properties();
        try (InputStream entrada = getClass().getResourceAsStream("/application.properties")) {
            if (entrada == null) {
                throw new IOException("No se encontró application.properties");
            }
            configuracion.load(entrada);
        }

        String codigo = configuracion.getProperty("app.language", "es")
                .strip().toLowerCase(Locale.ROOT);
        if (!"es".equals(codigo) && !"en".equals(codigo)) {
            throw new IllegalArgumentException("app.language debe ser es o en");
        }
        aplicarIdioma(codigo);
    }

    private void aplicarIdioma(String codigo) {
        Locale nuevoIdioma = Locale.forLanguageTag("en".equals(codigo) ? "en-US" : "es-AR");
        ResourceBundle nuevosMensajes = ResourceBundle.getBundle(
                "mensajes", nuevoIdioma,
                ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_PROPERTIES));
        mensajes = nuevosMensajes;
        idioma = nuevoIdioma;
        botonSi = new ButtonType(texto("dialog.si"), ButtonType.YES.getButtonData());
        botonNo = new ButtonType(texto("dialog.no"), ButtonType.NO.getButtonData());
    }

    private void configurarSelector(ComboBox<String> selector, TextField correo) {
        selector.getItems().setAll("Español", "English");
        selector.getSelectionModel().select(esIngles() ? 1 : 0);
        selector.setOnAction(evento -> {
            int indice = selector.getSelectionModel().getSelectedIndex();
            if (indice < 0) {
                return;
            }

            String codigo = indice == 1 ? "en" : "es";
            String anterior = idioma.getLanguage();
            if (codigo.equals(anterior)) {
                return;
            }

            String correoEscrito = correo.getText();
            try {
                aplicarIdioma(codigo);
                mostrar("Inicio.fxml", correoEscrito);
                ventana.setTitle(texto("app.titulo"));
            } catch (IOException | RuntimeException e) {
                aplicarIdioma(anterior);
                selector.getSelectionModel().select(esIngles() ? 1 : 0);
                new Alert(Alert.AlertType.ERROR,
                        texto("error.idioma") + " " + mensajeError(e),
                        botonAceptar()).showAndWait();
            }
        });
    }

    public Locale locale() {
        return idioma;
    }

    private boolean esIngles() {
        return "en".equals(idioma.getLanguage());
    }

    public String texto(String clave) {
        return mensajes.getString(clave);
    }

    public String nombreServicio(Servicio servicio) {
        String nombre = servicio.getNombre();
        String clave = "servicio." + nombre.toLowerCase(Locale.ROOT);
        return esIngles() && mensajes.containsKey(clave) ? texto(clave) : nombre;
    }

    public String etiqueta(Enum<?> valor) {
        if (valor == null) {
            return "";
        }
        return esIngles() ? texto("valor." + valor.name()) : valor.toString();
    }

    @SafeVarargs
    public final void traducirOpciones(ComboBox<? extends Enum<?>>... opciones) {
        for (ComboBox<? extends Enum<?>> opcion : opciones) {
            if (opcion != null) {
                traducirOpcion(opcion);
            }
        }
    }

    private <T extends Enum<?>> void traducirOpcion(ComboBox<T> opcion) {
        opcion.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T valor) {
                return etiqueta(valor);
            }

            @Override
            public T fromString(String texto) {
                return opcion.getItems().stream()
                        .filter(valor -> etiqueta(valor).equals(texto))
                        .findFirst().orElse(null);
            }
        });
    }

    public ButtonType botonSi() {
        return botonSi;
    }

    public ButtonType botonNo() {
        return botonNo;
    }

    public ButtonType botonAceptar() {
        return new ButtonType(texto("dialog.aceptar"), ButtonType.OK.getButtonData());
    }

    public String mensajeError(Throwable error) {
        String mensaje = error.getMessage();
        if (!esIngles()) {
            return mensaje == null ? error.getClass().getSimpleName() : mensaje;
        }

        ResourceBundle base = ResourceBundle.getBundle("mensajes", Locale.ROOT,
                ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_PROPERTIES));
        for (String clave : base.keySet()) {
            if (clave.startsWith("error.") && base.getString(clave).equals(mensaje)) {
                return texto(clave);
            }
        }

        StackTraceElement[] origen = error.getStackTrace();
        if (mensaje != null && origen.length > 0) {
            String clase = origen[0].getClassName();
            if (clase.equals(UberController.class.getName())
                    || clase.startsWith(MapaController.class.getName())) {
                return mensaje;
            }
        }
        System.getLogger(getClass().getName()).log(
                System.Logger.Level.WARNING, "Error de la aplicación", error);
        return texto("error.accion");
    }

    private void cambiarPantalla(String archivo, Parent root) {
        boolean esIngreso = "Inicio.fxml".equals(archivo)
                || "RegistroUsuario.fxml".equals(archivo);

        Parent vista = root;

        if (esIngreso) {
            pantallaPrincipal = null;
        } else {
            if (pantallaPrincipal == null) {
                pantallaPrincipal = crearPantallaPrincipal();
            }
            pantallaPrincipal.setCenter(root);
            vista = pantallaPrincipal;
        }

        if (ventana.getScene() == null) {
            ventana.setScene(new Scene(vista, 650, 650));
        } else if (ventana.getScene().getRoot() != vista) {
            ventana.getScene().setRoot(vista);
        }
    }

    private BorderPane crearPantallaPrincipal() {
        MenuButton menu = new MenuButton(texto("vista.tu.espacio"));

        MenuItem solicitar = new MenuItem(texto("vista.solicitar.viaje"));
        solicitar.setOnAction(evento -> {
            if (controlador != null) {
                controlador.abrirSolicitud();
            }
        });

        menu.getItems().addAll(
                opcionMenu("vista.inicio", "MenuPrincipal.fxml"),
                solicitar,
                opcionMenu("vista.servicios", "ConsultarServicios.fxml"),
                opcionMenu("vista.mis.viajes", "MisViajes.fxml"),
                opcionMenu("vista.panel.conductor", "PanelConductor.fxml")
        );

        BorderPane panel = new BorderPane();
        panel.setTop(menu);
        BorderPane.setMargin(menu, new Insets(10));
        return panel;
    }

    private MenuItem opcionMenu(String clave, String archivo) {
        MenuItem opcion = new MenuItem(texto(clave));
        opcion.setOnAction(evento -> {
            try {
                mostrar(archivo);
            } catch (IOException | RuntimeException e) {
                new Alert(Alert.AlertType.ERROR, mensajeError(e), botonAceptar())
                        .showAndWait();
            }
        });
        return opcion;
    }

    private void detenerControlador() {
        if (controlador != null) {
            controlador.detener();
            controlador = null;
        }
    }

    @Override
    public synchronized void stop() {
        cerrada = true;
        detenerControlador();

        if (planificadorRutas != null) {
            planificadorRutas.close();
            planificadorRutas = null;
            servicioRutas = null;
        }
    }
}
