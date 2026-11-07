package movilidad.ui;

import javafx.application.Application;
import movilidad.aplicacion.AplicacionUberFX;

public class JavaFXUserInterface implements UIEngine {

    @Override
    public void start() {
        Application.launch(AplicacionUberFX.class);
    }
}