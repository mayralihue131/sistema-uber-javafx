package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import movilidad.dao.dto.ServicioDTO;
import movilidad.dao.interfaces.ServicioDAO;

/**
 * Implementación JDBC de {@link ServicioDAO}.
 */
public class ServicioDaoBd implements ServicioDAO {

    @Override
    public List<ServicioDTO> obtenerTodos() throws Exception {
        List<ServicioDTO> lista = new ArrayList<>();

        String sql = "SELECT nombre, tarifa_base, precio_km, precio_minuto, " +
                     "tipo_vehiculo, categoria_vehiculo, tipo_servicio FROM servicios";

        Connection con = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new ServicioDTO(
                        rs.getString("nombre").trim(),
                        rs.getDouble("tarifa_base"),
                        rs.getDouble("precio_km"),
                        rs.getDouble("precio_minuto"),
                        rs.getString("tipo_vehiculo").trim(),
                        rs.getString("categoria_vehiculo").trim(),
                        rs.getString("tipo_servicio").trim()
                ));
            }
        }
        return lista;
    }
}
