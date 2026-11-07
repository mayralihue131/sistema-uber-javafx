package movilidad.dao.txt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import movilidad.dao.dto.ServicioDTO;
import movilidad.dao.interfaces.ServicioDAO;

/**
 * Implementación TXT de {@link ServicioDAO}.
 */
public class ServicioDaoTxt implements ServicioDAO {

    private final String rutaArchivo;

    public ServicioDaoTxt(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    @Override
    public List<ServicioDTO> obtenerTodos() throws Exception {
        List<ServicioDTO> lista = new ArrayList<>();

        try (Scanner read = new Scanner(new File(rutaArchivo), "UTF-8")) {
            while (read.hasNextLine()) {
                String linea = read.nextLine().trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] campos = linea.split(";", -1);
                if (campos.length < 7) continue;

                String nombre            = campos[0].trim();
                double tarifaBase        = Double.parseDouble(campos[1].trim());
                double precioKm          = Double.parseDouble(campos[2].trim());
                double precioMinuto      = Double.parseDouble(campos[3].trim());
                String tipoVehiculo      = campos[4].trim();
                String categoriaVehiculo = campos[5].trim();
                String tipoServicio      = campos[6].trim();

                lista.add(new ServicioDTO(nombre, tarifaBase, precioKm, precioMinuto,
                        tipoVehiculo, categoriaVehiculo, tipoServicio));
            }
        }
        return lista;
    }
}
