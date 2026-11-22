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

    /**
     * Carga y parsea la lista de registros de usuarios desde un archivo de texto delimitado por punto y coma.
     *
     * @param fileName ruta al archivo de texto
     * @return matriz de strings con los campos leídos
     * @throws FileNotFoundException si el archivo no existe
     */
    public static List<List<String>> cargarUsuarios(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    /**
     * Carga y parsea la lista de registros de vehículos desde un archivo de texto.
     *
     * @param fileName ruta al archivo de texto
     * @return matriz de strings con los campos leídos
     * @throws FileNotFoundException si el archivo no existe
     */
    public static List<List<String>> cargarVehiculos(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    /**
     * Carga y parsea la lista de registros de servicios desde un archivo de texto.
     *
     * @param fileName ruta al archivo de texto
     * @return matriz de strings con los campos leídos
     * @throws FileNotFoundException si el archivo no existe
     */
    public static List<List<String>> cargarServicios(String fileName)
            throws FileNotFoundException {
        return leerArchivo(fileName);
    }

    private static List<List<String>> leerArchivo(String fileName)
            throws FileNotFoundException {

        List<List<String>> datos = new ArrayList<>();

        File archivoFisico = new File(fileName);
        if (!archivoFisico.exists()) {
            if (fileName.contains("src/main/resources/") && !fileName.contains("src/main/resources/datos/")) {
                File alternativo = new File(fileName.replace("src/main/resources/", "src/main/resources/datos/"));
                if (alternativo.exists()) {
                    archivoFisico = alternativo;
                }
            } else if (!fileName.contains("datos/")) {
                File alternativo = new File("src/main/resources/datos/" + archivoFisico.getName());
                if (alternativo.exists()) {
                    archivoFisico = alternativo;
                }
            }
        }

        try (Scanner archivo = new Scanner(archivoFisico, "UTF-8")) {
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

