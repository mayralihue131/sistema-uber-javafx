package movilidad.dao.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import movilidad.dao.dto.UsuarioDTO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.modelo.Usuario;

/**
 * Implementación JDBC de {@link UsuarioDAO} para la base de datos PostgreSQL de la cátedra.
 *
 * <p>Esquema real consultado en el servidor:
 * <ul>
 *   <li>Tabla {@code usuario} (email, nombre, telefono, licencia_conducir)</li>
 *   <li>Tabla {@code conductor_vehiculo} (email, patente)</li>
 * </ul>
 */
public class UsuarioDaoBd implements UsuarioDAO {

    public List<UsuarioDTO> obtenerTodos() throws Exception {
        List<UsuarioDTO> lista = new ArrayList<>();

        String sql = """
                SELECT u.nombre, u.telefono, u.email, u.licencia_conducir, MIN(cv.patente) AS patente_activo
                FROM usuario u
                LEFT JOIN conductor_vehiculo cv ON cv.email = u.email
                GROUP BY u.nombre, u.telefono, u.email, u.licencia_conducir
                ORDER BY u.nombre
                """;

        Connection con = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new UsuarioDTO(
                        rs.getString("nombre"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("licencia_conducir"),
                        rs.getString("patente_activo")
                ));
            }
        }
        return lista;
    }

    private final List<Usuario> usuariosMemoria = new ArrayList<>();

    @Override
    public void guardar(Usuario usuario) {
        usuariosMemoria.add(usuario);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        for (Usuario u : usuariosMemoria) {
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuariosMemoria);
    }
}
