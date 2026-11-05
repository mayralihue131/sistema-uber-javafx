package movilidad.dao;

import java.util.List;
import movilidad.modelo.Usuario;

public interface UsuarioDAO {

    void guardar(Usuario usuario);

    Usuario buscarPorEmail(String email);

    List<Usuario> listarTodos();
}