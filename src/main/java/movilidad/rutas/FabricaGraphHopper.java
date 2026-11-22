package movilidad.rutas;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
 * recupera la caché de ruteo en disco según la ciudad seleccionada.
 */
public final class FabricaGraphHopper {

    private static final List<String> CIUDADES_DISPONIBLES = List.of("Puerto Madryn", "Trelew");

    private static String ciudadActiva;
    private static Properties parametrosMapa;

    private FabricaGraphHopper() {
    }

    /**
     * Retorna la lista inmutable de nombres de ciudades disponibles para su selección,
     * leída dinámicamente de {@code /config/mapa.properties} o con fallback a las predeterminadas.
     */
    public static List<String> getCiudadesDisponibles() {
        Properties config = cargarOpcional("/config/mapa.properties");
        if (config != null) {
            String lista = config.getProperty("ciudades");
            if (lista != null && !lista.isBlank()) {
                List<String> items = new java.util.ArrayList<>();
                for (String c : lista.split(",")) {
                    String limpia = c.trim();
                    if (!limpia.isEmpty()) {
                        items.add(limpia);
                    }
                }
                if (!items.isEmpty()) {
                    return List.copyOf(items);
                }
            }
        }
        return List.of("Puerto Madryn", "Trelew");
    }

    /**
     * Retorna el nombre canónico de la ciudad actualmente activa.
     */
    public static synchronized String getCiudadActiva() {
        asegurarCargado();
        return ciudadActiva;
    }

    /**
     * Establece dinámicamente la ciudad activa, recargando sus parámetros cartográficos.
     *
     * @param ciudad nombre de la ciudad (por ej. "Puerto Madryn" o "Trelew")
     */
    public static synchronized void setCiudadActiva(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            throw new IllegalArgumentException("El nombre de la ciudad no puede ser vacío.");
        }
        String normalizada = ciudad.trim();
        String perfil = resolverPerfil(normalizada);
        parametrosMapa = cargar(perfil);
        ciudadActiva = parametrosMapa.getProperty("nombre", normalizada);
    }

    /**
     * Reinicia el estado interno en memoria para volver a leer desde archivo de configuración.
     */
    public static synchronized void reiniciar() {
        ciudadActiva = null;
        parametrosMapa = null;
    }

    public static synchronized String parametroMapa(String clave) {
        asegurarCargado();
        return parametrosMapa.getProperty(clave, "");
    }

    private static void asegurarCargado() {
        if (parametrosMapa == null) {
            String ciudad = (ciudadActiva != null && !ciudadActiva.isBlank())
                    ? ciudadActiva
                    : resolverCiudadInicial();
            setCiudadActiva(ciudad);
        }
    }

    /**
     * Determina la ciudad inicial leyendo {@code /config/mapa.properties}
     * (con valor por defecto "Puerto Madryn").
     */
    private static String resolverCiudadInicial() {
        Properties config = cargarOpcional("/config/mapa.properties");
        if (config != null && config.getProperty("ciudad") != null && !config.getProperty("ciudad").isBlank()) {
            return config.getProperty("ciudad").trim();
        }
        return "Puerto Madryn";
    }

    private static String resolverPerfil(String ciudad) {
        String normalizada = ciudad.trim().toLowerCase(Locale.ROOT);
        String claveProp = normalizada.replace(" ", "_");

        // 1. Probar mapeo explícito en mapa.properties (ej. perfil.puerto_madryn=/config/madryn.properties)
        Properties config = cargarOpcional("/config/mapa.properties");
        if (config != null) {
            String perfilMapeado = config.getProperty("perfil." + claveProp);
            if (perfilMapeado == null || perfilMapeado.isBlank()) {
                perfilMapeado = config.getProperty("perfil." + normalizada);
            }
            if (perfilMapeado != null && !perfilMapeado.isBlank()) {
                return perfilMapeado.trim();
            }
        }

        // 2. Mapeos tradicionales conocidos
        if (normalizada.equals("puerto madryn") || normalizada.equals("madryn")) {
            return "/config/madryn.properties";
        }
        if (normalizada.equals("trelew")) {
            return "/config/trelew.properties";
        }

        // 3. Probar si existe directamente /config/<slug>.properties
        String rutaDirecta = "/config/" + claveProp + ".properties";
        try (InputStream in = FabricaGraphHopper.class.getResourceAsStream(rutaDirecta)) {
            if (in != null) {
                return rutaDirecta;
            }
        } catch (Exception ignored) {
        }

        throw new IllegalStateException(
                "Ciudad no reconocida: '" + ciudad + "'. Verificá que exista en src/main/resources/config/ "
                        + "o esté configurada en mapa.properties. Opciones: " + getCiudadesDisponibles());
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

    public static double[] zonaConductores() {
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

    private static Properties cargarOpcional(String ruta) {
        try (InputStream entrada = FabricaGraphHopper.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                return null;
            }
            Properties propiedades = new Properties();
            propiedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return propiedades;
        } catch (Exception e) {
            return null;
        }
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
