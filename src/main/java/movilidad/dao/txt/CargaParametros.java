package movilidad.dao.txt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class CargaParametros {

    private static double velocidadPromedioKmH = 40.0;
    private static String archivoUsuarios;
    private static String archivoVehiculos;
    private static String archivoServicios;

    public static void parametros() throws IOException {
        Properties prop = new Properties();

        try (InputStream input = CargaParametros.class
                .getResourceAsStream("/config.properties")) {

            if (input == null) {
                throw new IOException("No se encontró config.properties");
            }

            prop.load(input);
        }

        velocidadPromedioKmH = Double.parseDouble(
                prop.getProperty("velocidadPromedioKmH", "40"));

        if (!Double.isFinite(velocidadPromedioKmH)
                || velocidadPromedioKmH <= 0) {
            throw new IOException(
                    "La velocidad promedio debe ser positiva");
        }

        archivoUsuarios = prop.getProperty("usuario");
        archivoVehiculos = prop.getProperty("vehiculo");
        archivoServicios = prop.getProperty("servicio");

        if (archivoUsuarios == null || archivoUsuarios.isBlank()
                || archivoVehiculos == null || archivoVehiculos.isBlank()
                || archivoServicios == null || archivoServicios.isBlank()) {
            throw new IOException(
                    "Faltan las claves usuario, vehiculo o servicio "
                    + "en config.properties");
        }
    }

    public static String ui() {
        Properties prop = new Properties();

        try (InputStream input = CargaParametros.class
                .getResourceAsStream("/application.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "No se encontró application.properties");
            }

            prop.load(input);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer application.properties", e);
        }

        return prop.getProperty("app.ui", "javafx")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public static double getVelocidadPromedioKmH() {
        return velocidadPromedioKmH;
    }

    public static String getArchivoUsuarios() {
        return archivoUsuarios;
    }

    public static String getArchivoVehiculos() {
        return archivoVehiculos;
    }

    public static String getArchivoServicios() {
        return archivoServicios;
    }
}

