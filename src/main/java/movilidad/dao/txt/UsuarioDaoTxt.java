package movilidad.dao.txt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import movilidad.dao.dto.UsuarioDTO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.modelo.Usuario;

/**
 * Implementación TXT de {@link UsuarioDAO}.
 */
public class UsuarioDaoTxt implements UsuarioDAO {

    private final String rutaArchivo;

    public UsuarioDaoTxt(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public List<UsuarioDTO> obtenerTodos() throws Exception {
        List<UsuarioDTO> lista = new ArrayList<>();

        try (Scanner read = new Scanner(new File(rutaArchivo), "UTF-8")) {
            while (read.hasNextLine()) {
                String linea = read.nextLine().trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] campos = linea.split(";", -1);
                if (campos.length < 3) continue;

                String nombre   = campos[0].trim();
                String telefono = campos[1].trim();
                String email    = campos[2].trim();

                String licencia = (campos.length > 3) ? campos[3].trim() : "";
                String patente  = (campos.length > 4) ? campos[4].trim() : "";

                lista.add(new UsuarioDTO(nombre, telefono, email, licencia, patente));
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
