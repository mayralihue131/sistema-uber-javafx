package movilidad.aplicacion;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

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

    private PlanificadorRutas planificadorRutas;
    private ServicioRutasViaje servicioRutas;
    private boolean cerrada;

    private final movilidad.rutas.UbicacionesConductores ubicacionesConductores = new movilidad.rutas.UbicacionesConductores();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ventana = stage;
        ventana.setTitle("Sistema Uber");

        try {
            sesion = new SesionUber(
                    Path.of(System.getProperty(
                            "uber.datos",
                            "datos/uber-eventos.txt")));

            mostrar("Inicio.fxml");

            ventana.setMinWidth(930);
            ventana.setMinHeight(680);
            ventana.show();

        } catch (Exception e) {
            Alert alerta = new Alert(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir el proyecto: " + e.getMessage());

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
        URL recurso = getClass().getResource("/" + archivo);

        if (recurso == null) {
            throw new IOException(
                    "No se encontró la pantalla: " + archivo);
        }

        FXMLLoader loader = new FXMLLoader(recurso);

        if (archivo.equals("SolicitarViajeMapa.fxml")) {
            Parent root = loader.load();

            MapaController mapa = loader.getController();
            mapa.setAplicacion(this);

            detenerControlador();
            cambiarPantalla(root);
            return;
        }

        UberController nuevo = new UberController(this, archivo);
        loader.setController(nuevo);

        Parent root = loader.load();

        detenerControlador();
        cambiarPantalla(root);

        controlador = nuevo;
        nuevo.preparar(root);
    }

    private void cambiarPantalla(Parent root) {
        if (ventana.getScene() == null) {
            ventana.setScene(new Scene(root));
        } else {
            ventana.getScene().setRoot(root);
        }
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