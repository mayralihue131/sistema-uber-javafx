package movilidad.ui;

import javafx.application.Application;
import movilidad.aplicacion.AplicacionUberFX;

/**
 * Adaptador de interfaz que arranca la aplicación visual en modo gráfico (JavaFX).
 */
public class JavaFXUserInterface implements UIEngine {

    @Override
    public void start() {
        Application.launch(AplicacionUberFX.class);
    }
}