package movilidad.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import movilidad.aplicacion.AplicacionUberFX;
import movilidad.modelo.CalificacionViaje;
import movilidad.modelo.EstadoConductor;
import movilidad.modelo.Servicio;
import movilidad.modelo.Viaje;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(
    named = "uber.ui.test",
    matches = "true"
)
class InterfazFlowTest {

    private AplicacionUberFX app;

    private Parent root(Stage stage) {
        Parent raiz = stage.getScene().getRoot();
        raiz.applyCss();
        raiz.layout();
        return raiz;
    }

    private void clic(Stage stage, String texto) {
        Button boton = (Button) root(stage)
            .lookupAll(".button")
            .stream()
            .filter(n -> n instanceof Button b
                && b.getText().equals(texto))
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Falta el botón: " + texto)
            );

        assertFalse(
            boton.isDisabled(),
            "El botón está deshabilitado: " + texto
        );

        boton.fire();
    }

    private void texto(
        Stage stage,
        String id,
        String valor
    ) {
        TextField campo = nodo(stage, id);
        campo.setText(valor);
    }

    @SuppressWarnings("unchecked")
    private <T> T nodo(Stage stage, String id) {
        Object encontrado = root(stage).lookup("#" + id);

        assertNotNull(
            encontrado,
            "No se encontró el control: " + id
        );

        return (T) encontrado;
    }

    private void captura(
        Stage stage,
        String nombre
    ) throws Exception {
        javafx.scene.image.WritableImage imagen =
            root(stage).snapshot(null, null);

        Class<?> swing = Class.forName(
            "javafx.embed.swing.SwingFXUtils"
        );

        java.awt.image.BufferedImage buffer =
            (java.awt.image.BufferedImage) swing
                .getMethod(
                    "fromFXImage",
                    javafx.scene.image.Image.class,
                    java.awt.image.BufferedImage.class
                )
                .invoke(null, imagen, null);

        Files.createDirectories(Path.of("target"));

        javax.imageio.ImageIO.write(
            buffer,
            "png",
            Path.of("target", nombre + ".png").toFile()
        );
    }

    @Test
    void clienteYConductorCompletanFlujoEnPantallas()
        throws Exception {

        String datosAnteriores =
            System.getProperty("uber.datos");

        Path archivoTemporal = Files
            .createTempDirectory("uber-ui-")
            .resolve("eventos.txt");

        System.setProperty(
            "uber.datos",
            archivoTemporal.toString()
        );

        try {
            CountDownLatch inicio = new CountDownLatch(1);

            try {
                Platform.startup(inicio::countDown);
            } catch (IllegalStateException e) {
                // JavaFX ya estaba iniciado.
                Platform.runLater(inicio::countDown);
            }

            assertTrue(
                inicio.await(15, TimeUnit.SECONDS),
                "JavaFX no pudo iniciarse"
            );

            CountDownLatch fin = new CountDownLatch(1);
            AtomicReference<Throwable> error =
                new AtomicReference<>();

            Platform.runLater(() -> {
                Stage stage = null;

                try {
                    stage = new Stage();

                    app = new AplicacionUberFX();
                    app.start(stage);

                    // Ingresar como cliente.
                    texto(
                        stage,
                        "campoCorreo",
                        "paula.vazquez@email.com"
                    );
                    clic(stage, "Continuar");

                    // Abrir la pantalla nueva del mapa.
                    clic(stage, "Solicitar un viaje");

                    // Se cargan coordenadas directamente para
                    // probar el formulario sin depender de clics
                    // sobre las imágenes del mapa.
                    texto(
                        stage,
                        "origenTexto",
                        "-42.78683, -65.07048"
                    );
                    texto(
                        stage,
                        "destinoTexto",
                        "-42.78513, -65.04503"
                    );

                    clic(stage, "Cotizar viaje");

                    ListView<Servicio> opciones =
                        nodo(stage, "opciones");

                    assertFalse(
                        opciones.getItems().isEmpty(),
                        "Deben aparecer servicios disponibles"
                    );

                    opciones.getSelectionModel().select(0);
                    clic(stage, "Solicitar viaje");

                    Label estado = nodo(stage, "estadoViaje");
                    assertEquals(
                        "SOLICITADO",
                        estado.getText()
                    );

                    // Cambiar a la conductora.
                    clic(stage, "Cambiar cuenta");
                    texto(
                        stage,
                        "campoCorreo",
                        "ana.martinez@email.com"
                    );
                    clic(stage, "Continuar");
                    clic(stage, "Ir al panel");

                    ListView<Viaje> solicitudes =
                        nodo(stage, "solicitudes");

                    assertFalse(
                        solicitudes.getItems().isEmpty(),
                        "Ana debe ver la solicitud compatible"
                    );

                    solicitudes
                        .getSelectionModel()
                        .select(0);

                    clic(
                        stage,
                        "Aceptar solicitud seleccionada"
                    );

                    estado = nodo(stage, "estadoViaje");
                    assertEquals(
                        "ACEPTADO",
                        estado.getText()
                    );

                    // Iniciar y finalizar.
                    clic(stage, "Iniciar traslado");

                    estado = nodo(stage, "estadoViaje");
                    assertEquals(
                        "INICIADO",
                        estado.getText()
                    );

                    clic(stage, "Finalizar viaje");

                    estado = nodo(stage, "estadoViaje");
                    assertEquals(
                        "FINALIZADO",
                        estado.getText()
                    );

                    // La conductora califica al cliente.
                    ComboBox<CalificacionViaje> calificacion =
                        nodo(stage, "calificacion");

                    calificacion.setValue(
                        CalificacionViaje.EXCELENTE
                    );
                    clic(stage, "Guardar mi calificación");

                    // El cliente abre su viaje y califica.
                    clic(stage, "Cambiar cuenta");
                    texto(
                        stage,
                        "campoCorreo",
                        "paula.vazquez@email.com"
                    );
                    clic(stage, "Continuar");
                    clic(stage, "Ver mis viajes");

                    ListView<Viaje> viajes =
                        nodo(stage, "listaViajes");

                    assertEquals(
                        1,
                        viajes.getItems().size()
                    );

                    viajes.getSelectionModel().select(0);

                    clic(
                        stage,
                        "Ver detalle del seleccionado"
                    );

                    calificacion = nodo(
                        stage,
                        "calificacion"
                    );
                    calificacion.setValue(
                        CalificacionViaje.BUENO
                    );
                    clic(stage, "Guardar mi calificación");

                    Label evaluacion = nodo(
                        stage,
                        "miCalificacion"
                    );

                    assertTrue(
                        evaluacion.getText().contains("BUENO"),
                        "Debe mostrarse la evaluación guardada"
                    );

                    // Cargar las vistas y guardar capturas.
                    String[] pantallas = {
                        "MenuPrincipal.fxml",
                        "SolicitarViajeMapa.fxml",
                        "ConsultarServicios.fxml",
                        "MisViajes.fxml",
                        "PanelConductor.fxml",
                        "RegistroConductor.fxml",
                        "DetalleViaje.fxml",
                        "RegistroUsuario.fxml",
                        "Inicio.fxml"
                    };

                    for (String archivo : pantallas) {
                        app.mostrar(archivo);

                        captura(
                            stage,
                            archivo.replace(".fxml", "")
                        );
                    }

                    app.mostrar("MenuPrincipal.fxml");
                    captura(stage, "vista-menu");

                    // Registrar un usuario nuevo.
                    app.mostrar("Inicio.fxml");
                    clic(stage, "Crear una cuenta");

                    texto(stage, "nombre", "Usuario UI");
                    texto(stage, "telefono", "123456");
                    texto(
                        stage,
                        "correoRegistro",
                        "ui@example.com"
                    );

                    clic(stage, "Crear cuenta y entrar");

                    assertEquals(
                        "ui@example.com",
                        app.sesion.correo
                    );

                    // Habilitar conductor y agregar vehículo.
                    clic(
                        stage,
                        "Habilitarme / agregar vehículo"
                    );

                    texto(stage, "licencia", "B1");
                    texto(stage, "patente", "UI123AB");
                    texto(
                        stage,
                        "modelo",
                        "Auto de prueba"
                    );
                    texto(stage, "capacidad", "4");

                    clic(stage, "Guardar vehículo");
                    clic(stage, "Ponerme disponible");

                    assertEquals(
                        EstadoConductor.DISPONIBLE,
                        app.sesion
                            .usuario()
                            .getConductor()
                            .getEstadoConductor()
                    );

                } catch (Throwable t) {
                    error.set(t);

                } finally {
                    try {
                        if (app != null) {
                            app.stop();
                        }
                    } catch (Throwable t) {
                        error.compareAndSet(null, t);

                    } finally {
                        if (stage != null) {
                            stage.close();
                        }

                        fin.countDown();
                    }
                }
            });

            assertTrue(
                fin.await(90, TimeUnit.SECONDS),
                "El flujo de interfaz no terminó"
            );

            if (error.get() != null) {
                throw new AssertionError(
                    "Falló el recorrido de pantallas",
                    error.get()
                );
            }

        } finally {
            if (datosAnteriores == null) {
                System.clearProperty("uber.datos");
            } else {
                System.setProperty(
                    "uber.datos",
                    datosAnteriores
                );
            }
        }
    }
}