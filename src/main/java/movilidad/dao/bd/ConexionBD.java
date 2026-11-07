package movilidad.dao.bd;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton de conexión JDBC para el sistema Uber.
 *
 * <p>Lee el archivo {@code src/main/resources/jdbc.properties} con las claves:
 * <ul>
 *   <li>{@code driver}  – nombre completo del driver JDBC (ej. {@code org.postgresql.Driver})</li>
 *   <li>{@code url}     – URL de conexión (ej. {@code jdbc:postgresql://host:5432/uber})</li>
 *   <li>{@code usr}     – usuario de la base de datos</li>
 *   <li>{@code pwd}     – contraseña</li>
 *   <li>{@code schema}  – esquema a usar (opcional, por defecto {@code public})</li>
 * </ul>
 *
 * <p>La conexión se cierra automáticamente al finalizar la JVM mediante un
 * {@link Runtime#addShutdownHook(Thread) shutdown hook}.
 */
public class ConexionBD {

    private static Connection con = null;

    /**
     * Devuelve la conexión abierta. La crea la primera vez (Singleton lazy).
     *
     * @return conexión JDBC activa
     * @throws RuntimeException si no se puede establecer la conexión
     */
    public static Connection obtenerConexion() {
        try {
            if (con == null) {
                Runtime.getRuntime().addShutdownHook(new ShutdownHook());

                Properties prop = new Properties();
                try (FileInputStream input = new FileInputStream("src/main/resources/jdbc.properties")) {
                    prop.load(input);
                }

                String driver = prop.getProperty("driver");
                String url    = prop.getProperty("url");
                String usr    = prop.getProperty("usr");
                String pwd    = prop.getProperty("pwd");
                String schema = prop.getProperty("schema", "public");

                Class.forName(driver);
                con = DriverManager.getConnection(url, usr, pwd);

                // Establecer esquema en PostgreSQL
                try (Statement st = con.createStatement()) {
                    st.execute("SET search_path TO '" + schema + "'");
                }
            }
            return con;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error al crear la conexión con la base de datos", ex);
        }
    }

    /** Cierra la conexión al terminar la JVM. */
    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

