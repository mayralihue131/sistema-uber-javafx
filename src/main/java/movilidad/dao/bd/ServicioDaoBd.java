package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import movilidad.dao.dto.ServicioDTO;
import movilidad.dao.interfaces.ServicioDAO;

/**
 * Implementación JDBC de {@link ServicioDAO} para la base de datos PostgreSQL de la cátedra.
 *
 * <p>Esquema real consultado en el servidor:
 * <ul>
 *   <li>Tabla {@code servicio} (nombre, tarifa_base, precio_km, precio_minuto, tipo_vehiculo, categoria_vehiculo, tipo_servicio)</li>
 * </ul>
 */
public class ServicioDaoBd implements ServicioDAO {

    @Override
    public List<ServicioDTO> obtenerTodos() throws Exception {
        List<ServicioDTO> lista = new ArrayList<>();

        String sql = "SELECT nombre, tarifa_base, precio_km, precio_minuto, " +
                     "tipo_vehiculo, categoria_vehiculo, tipo_servicio FROM servicio ORDER BY nombre";

        Connection con = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int tipoVehiculoInt = rs.getInt("tipo_vehiculo");
                String tipoVehiculo = (tipoVehiculoInt == 1) ? "MOTO" : "AUTO";

                int catInt = rs.getInt("categoria_vehiculo");
                String categoriaVehiculo = switch (catInt) {
                    case 1 -> "CONFORT";
                    case 2 -> "PREMIUM";
                    default -> "ESTANDAR";
                };

                int tipoServInt = rs.getInt("tipo_servicio");
                String tipoServicio = (tipoServInt == 1) ? "ENVIOS" : "PASAJEROS";

                lista.add(new ServicioDTO(
                        rs.getString("nombre").trim(),
                        rs.getDouble("tarifa_base"),
                        rs.getDouble("precio_km"),
                        rs.getDouble("precio_minuto"),
                        tipoVehiculo,
                        categoriaVehiculo,
                        tipoServicio
                ));
            }
        }
        return lista;
    }
}
