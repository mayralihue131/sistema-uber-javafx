package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Implementación JDBC de {@link VehiculoDAO} para la base de datos PostgreSQL de la cátedra.
 *
 * <p>Esquema real consultado en el servidor:
 * <ul>
 *   <li>Tabla {@code vehiculo} (patente, modelo, capacidad_pasajeros, tipo_vehiculo, categoria_vehiculo, tipo_servicios)</li>
 * </ul>
 */
public class VehiculoDaoBd implements VehiculoDAO {

    @Override
    public List<VehiculoDTO> obtenerTodos() throws Exception {
        List<VehiculoDTO> lista = new ArrayList<>();

        String sql = "SELECT patente, modelo, capacidad_pasajeros, " +
                     "tipo_vehiculo, categoria_vehiculo, tipo_servicios FROM vehiculo ORDER BY patente";

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

                // Decodificación de tipo_servicios (máscara de bits: 1=PASAJEROS, 2=ENVIOS, 3=AMBOS)
                int servBits = rs.getInt("tipo_servicios");
                List<String> tiposServicio = new ArrayList<>();
                if ((servBits & 1) != 0) {
                    tiposServicio.add("PASAJEROS");
                }
                if ((servBits & 2) != 0) {
                    tiposServicio.add("ENVIOS");
                }
                if (tiposServicio.isEmpty()) {
                    tiposServicio.add("PASAJEROS");
                }

                lista.add(new VehiculoDTO(
                        rs.getString("patente").trim(),
                        rs.getString("modelo").trim(),
                        rs.getInt("capacidad_pasajeros"),
                        tipoVehiculo,
                        categoriaVehiculo,
                        tiposServicio
                ));
            }
        }
        return lista;
    }
}
