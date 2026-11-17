package movilidad.aplicacion;

import movilidad.ui.UIEngine;
import movilidad.ui.UILoader;

/**
 * Punto de entrada principal polimórfico de la aplicación.
 *
 * <p>Lee la configuración de {@code application.properties} (clave {@code app.ui}) a través
 * de {@link UILoader} para instanciar e iniciar dinámicamente la interfaz gráfica JavaFX
 * o la interfaz de consola de comandos (CLI).
 */
public class Aplicacion {

    /**
     * Método main estándar que arranca la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        UIEngine interfaz = UILoader.createUIEngine();
        interfaz.start();
    }
}