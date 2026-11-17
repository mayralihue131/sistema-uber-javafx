package movilidad.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.modelo.Usuario;

/**
 * Implementación en memoria de {@link UsuarioDAO}.
 * Almacena los usuarios en colecciones en memoria RAM, ideal para pruebas unitarias aisladas.
 */
public class UsuarioDAOMemoria implements UsuarioDAO {

    private List<Usuario> usuarios = new ArrayList<>();

    @Override
    public void guardar(Usuario usuario) {
        usuarios.add(usuario);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        for (Usuario usuario : usuarios) {
            if (usuario.getEmail().equalsIgnoreCase(email)) {
                return usuario;
            }
        }
        return null;
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuarios);
    }
}

