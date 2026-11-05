package movilidad.dao;

import java.util.ArrayList;
import java.util.List;
import movilidad.modelo.Usuario;

public class UsuarioDAOMemoria implements UsuarioDAO {

    private List<Usuario> usuarios = new ArrayList<>();

    @Override
    public void guardar(Usuario usuario) {
        usuarios.add(usuario);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        for (Usuario usuario : usuarios) {
        	if (usuario.getEmail().equalsIgnoreCase(email))  {
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