package movilidad.aplicacion;

import movilidad.ui.UIEngine;
import movilidad.ui.UILoader;

public class Aplicacion {

    public static void main(String[] args) {
        UIEngine interfaz = UILoader.createUIEngine();
        interfaz.start();
    }
}