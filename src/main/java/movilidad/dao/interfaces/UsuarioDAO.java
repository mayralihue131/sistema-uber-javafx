package movilidad.dao.interfaces;

import java.util.List;
import movilidad.modelo.Usuario;

/**
 * Interfaz DAO que define las operaciones de persistencia y consulta de cuentas de usuario.
 */
public interface UsuarioDAO {

    /**
     * Persiste un usuario en el origen de datos activo.
     *
     * @param usuario entidad a almacenar
     */
    void guardar(Usuario usuario);

    /**
     * Busca un usuario por su dirección de correo electrónico única.
     *
     * @param email correo a buscar
     * @return entidad {@link Usuario} encontrada o {@code null}
     */
    Usuario buscarPorEmail(String email);

    /**
     * Lista todos los usuarios registrados en el origen de datos.
     *
     * @return colección de usuarios
     */
    List<Usuario> listarTodos();
}

