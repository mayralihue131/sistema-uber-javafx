package movilidad.ui;

import movilidad.aplicacion.AplicacionConsultas;

/**
 * Adaptador de interfaz que inicia la aplicación en modo consola (CLI).
 */
public class Interfaz implements UIEngine {

    @Override
    public void start() {
        AplicacionConsultas.main(new String[0]);
    }
}