package movilidad.rutas;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.GHUtility;

/**
 * Fábrica que inicializa y configura el motor de ruteo vial {@link GraphHopper}.
 *
 * <p>Carga la cartografía local OpenStreetMap (archivo {@code .osm}), configura los perfiles
 * de vehículo para automóvil ({@code car}) y motocicleta ({@code motorcycle}), y genera o
 * recupera la caché de ruteo en disco.
 */
public final class FabricaGraphHopper {

    private static Properties parametrosMapa;

    private FabricaGraphHopper() {
    }

    public static synchronized String parametroMapa(String clave) {
        if (parametrosMapa == null) {
            String ciudad = cargar("/config/mapas.properties")
                    .getProperty("ciudad", "Puerto Madryn").trim().toLowerCase(Locale.ROOT);
            String perfil = switch (ciudad) {
                case "puerto madryn", "madryn" -> "/config/madryn.properties";
                case "trelew" -> "/config/trelew.properties";
                default -> throw new IllegalStateException(
                        "En mapas.properties elegí ciudad=Puerto Madryn o ciudad=Trelew.");
            };
            parametrosMapa = cargar(perfil);
        }
        return parametrosMapa.getProperty(clave, "");
    }

    public static PuntoRuta centroMapa() {
        double[] centro = coordenadas("centro", requerirParametro("centro"), 2);
        return new PuntoRuta(centro[0], centro[1]);
    }

    public static String urlMapa(URL html, String idioma) {
        PuntoRuta centro = centroMapa();
        String nombre = URLEncoder.encode(requerirParametro("nombre"), StandardCharsets.UTF_8);
        int zoom = Integer.parseInt(requerirParametro("zoom"));
        return html.toExternalForm() + "#idioma=" + idioma + "&nombre=" + nombre
                + "&latitud=" + centro.latitud() + "&longitud=" + centro.longitud()
                + "&zoom=" + zoom;
    }

    public static boolean puntoAdmitido(double latitud, double longitud) {
        double[] zona = limites("limites");
        return latitud >= zona[0] && latitud <= zona[2]
                && longitud >= zona[1] && longitud <= zona[3];
    }

    static double[] zonaConductores() {
        String clave = parametroMapa("conductores").isEmpty() ? "limites" : "conductores";
        return limites(clave);
    }

    private static double[] limites(String clave) {
        double[] zona = coordenadas(clave, requerirParametro(clave), 4);
        if (zona[0] >= zona[2] || zona[1] >= zona[3]) {
            throw new IllegalArgumentException("Límites inválidos en el archivo de la ciudad: " + clave);
        }
        return zona;
    }

    private static double[] coordenadas(String clave, String valor, int cantidad) {
        String[] partes = valor.split(",", -1);
        if (partes.length != cantidad) {
            throw new IllegalArgumentException("Coordenadas inválidas en el archivo de la ciudad: " + clave);
        }
        double[] numeros = new double[cantidad];
        for (int i = 0; i < cantidad; i++) {
            numeros[i] = Double.parseDouble(partes[i].trim());
            if (!Double.isFinite(numeros[i])) {
                throw new IllegalArgumentException("Coordenadas inválidas en el archivo de la ciudad: " + clave);
            }
        }
        return numeros;
    }

    private static String requerirParametro(String clave) {
        String valor = parametroMapa(clave);
        if (valor.isEmpty()) {
            throw new IllegalStateException("Falta '" + clave + "' para la ciudad activa en el archivo de la ciudad");
        }
        return valor;
    }

    private static Properties cargar(String ruta) {
        try (InputStream entrada = FabricaGraphHopper.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró " + ruta);
            }
            Properties propiedades = new Properties();
            propiedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return propiedades;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ruta, e);
        }
    }

    /**
     * Construye e inicializa una instancia lista para calcular rutas de GraphHopper.
     *
     * @return instancia de {@link GraphHopper} cargada
     * @throws IllegalStateException si no se encuentra el archivo cartográfico .osm
     */
    public static GraphHopper crear() {
        Path archivo = Path.of(requerirParametro("osm"));

        if (!Files.isRegularFile(archivo)) {
            throw new IllegalStateException(
                "No se encontró el mapa de " + requerirParametro("nombre") + ": "
                    + archivo.toAbsolutePath()
            );
        }

        GraphHopper motor = new GraphHopper();

        try {
            motor.setOSMFile(archivo.toString());

            motor.setGraphHopperLocation(requerirParametro("cache"));

            motor.setEncodedValuesString(
                "car_access, car_average_speed, track_type, road_access, road_class, surface"
            );

            Profile auto = new Profile("car")
                .setCustomModel(
                    GHUtility.loadCustomModelFromJar("car.json")
                );

            Profile moto = new Profile("motorcycle")
                .setCustomModel(
                    GHUtility.loadCustomModelFromJar("motorcycle.json")
                );

            motor.setProfiles(auto, moto);

            motor.getCHPreparationHandler()
                .setCHProfiles(new CHProfile("car"), new CHProfile("motorcycle"));

            motor.importOrLoad();

            return motor;

        } catch (RuntimeException e) {
            motor.close();
            throw e;
        }
    }
}
