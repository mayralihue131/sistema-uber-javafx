package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Implementación JDBC de {@link VehiculoDAO}.
 */
public class VehiculoDaoBd implements VehiculoDAO {

    @Override
    public List<VehiculoDTO> obtenerTodos() throws Exception {
        List<VehiculoDTO> lista = new ArrayList<>();

        String sql = "SELECT patente, modelo, capacidad_pasajeros, " +
                     "tipo_vehiculo, categoria_vehiculo, tipos_servicio FROM vehiculos";

        Connection con = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String tiposRaw = rs.getString("tipos_servicio");
                List<String> tiposServicio = new ArrayList<>();
                if (tiposRaw != null && !tiposRaw.isBlank()) {
                    tiposServicio = Arrays.stream(tiposRaw.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }

                lista.add(new VehiculoDTO(
                        rs.getString("patente").trim(),
                        rs.getString("modelo").trim(),
                        rs.getInt("capacidad_pasajeros"),
                        rs.getString("tipo_vehiculo").trim(),
                        rs.getString("categoria_vehiculo").trim(),
                        tiposServicio
                ));
            }
        }
        return lista;
    }
}
