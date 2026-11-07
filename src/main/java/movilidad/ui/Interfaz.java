package movilidad.ui;

import movilidad.aplicacion.AplicacionConsultas;

public class Interfaz implements UIEngine {

    @Override
    public void start() {
        AplicacionConsultas.main(new String[0]);
    }
}