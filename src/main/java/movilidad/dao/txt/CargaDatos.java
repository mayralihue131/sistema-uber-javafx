package movilidad.dao.txt;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import movilidad.dao.dto.UsuarioDTO;
import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.dto.ServicioDTO;

/**
 * Fachada de carga de datos para el sistema Uber desde archivos de texto.
 */
public class CargaDatos {

    // ── Métodos clásicos (compatibilidad hacia atrás) ─────────────────────────

    public static List<List<String>> cargarUsuarios(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    public static List<List<String>> cargarVehiculos(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    public static List<List<String>> cargarServicios(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    private static List<List<String>> leerArchivo(String fileName)
            throws FileNotFoundException {

        List<List<String>> datos = new ArrayList<>();

        try (Scanner archivo = new Scanner(new File(fileName), "UTF-8")) {
            while (archivo.hasNextLine()) {
                String linea = archivo.nextLine().trim();

                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                List<String> campos = new ArrayList<>();

                try (Scanner lectorCampos = new Scanner(linea)) {
                    lectorCampos.useDelimiter(";");

                    while (lectorCampos.hasNext()) {
                        campos.add(lectorCampos.next().trim());
                    }
                }

                datos.add(campos);
            }
        }

        return datos;
    }

    // ── Métodos DAO tipados ───────────────────────────────────────────────────

    public static List<UsuarioDTO> obtenerUsuariosDTO(String fileName) throws Exception {
        return new UsuarioDaoTxt(fileName).obtenerTodos();
    }

    public static List<VehiculoDTO> obtenerVehiculosDTO(String fileName) throws Exception {
        return new VehiculoDaoTxt(fileName).obtenerTodos();
    }

    public static List<ServicioDTO> obtenerServiciosDTO(String fileName) throws Exception {
        return new ServicioDaoTxt(fileName).obtenerTodos();
    }
}

