package movilidad.dao;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import movilidad.dao.bd.BdDAOFactory;
import movilidad.dao.memoria.MemoriaDAOFactory;
import movilidad.dao.txt.TxtDAOFactory;

/**
 * Carga y mantiene la única instancia de {@link DAOFactory} para toda la aplicación.
 *
 * <p>Lee el archivo {@code src/main/resources/config.properties} en busca de la clave
 * {@code origen.datos}:
 * <ul>
 *   <li>{@code BD}      → usa {@link BdDAOFactory} (PostgreSQL vía JDBC).</li>
 *   <li>{@code MEMORIA} → usa {@link MemoriaDAOFactory} (colecciones en RAM).</li>
 *   <li>{@code TXT}     → usa {@link TxtDAOFactory} (archivos planos separados por {@code ;}).</li>
 * </ul>
 */
public class DAOLoader {

    private static final DAOFactory instanciaFabrica;

    static {
        DAOFactory fabrica;
        try {
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
                prop.load(input);
            }

            String tipoOrigen = prop.getProperty("origen.datos", "TXT").toUpperCase();

            if ("BD".equals(tipoOrigen)) {
                fabrica = new BdDAOFactory();
            } else if ("MEMORIA".equals(tipoOrigen)) {
                fabrica = new MemoriaDAOFactory();
            } else {
                String archivoUsuarios  = prop.getProperty("usuario",  "src/main/resources/usuarios_test.txt");
                String archivoVehiculos = prop.getProperty("vehiculo", "src/main/resources/vehiculos_test.txt");
                String archivoServicios = prop.getProperty("servicio", "src/main/resources/servicios_test.txt");

                fabrica = new TxtDAOFactory(archivoUsuarios, archivoVehiculos, archivoServicios);
            }
        } catch (Exception e) {
            fabrica = new TxtDAOFactory(
                "src/main/resources/usuarios_test.txt",
                "src/main/resources/vehiculos_test.txt",
                "src/main/resources/servicios_test.txt"
            );
        }
        instanciaFabrica = fabrica;
    }

    public static DAOFactory getFactory() {
        return instanciaFabrica;
    }
}
