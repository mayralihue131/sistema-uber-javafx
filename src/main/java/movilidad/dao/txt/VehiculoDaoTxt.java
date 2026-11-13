package movilidad.dao.txt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Implementación TXT de {@link VehiculoDAO}.
 */
public class VehiculoDaoTxt implements VehiculoDAO {

    private final String rutaArchivo;

    public VehiculoDaoTxt(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    @Override
    public List<VehiculoDTO> obtenerTodos() throws Exception {
        List<VehiculoDTO> lista = new ArrayList<>();

        try (Scanner read = new Scanner(new File(rutaArchivo), "UTF-8")) {
            while (read.hasNextLine()) {
                String linea = read.nextLine().trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] campos = linea.split(";", -1);
                if (campos.length < 5) continue;

                String patente              = campos[0].trim();
                String modelo               = campos[1].trim();
                int    capacidad            = Integer.parseInt(campos[2].trim());
                String tipoVehiculo         = campos[3].trim();
                String categoriaVehiculo    = campos[4].trim();

                List<String> tiposServicio = new ArrayList<>();
                for (int i = 5; i < campos.length; i++) {
                    String ts = campos[i].trim();
                    if (!ts.isEmpty()) tiposServicio.add(ts);
                }

                lista.add(new VehiculoDTO(patente, modelo, capacidad,
                        tipoVehiculo, categoriaVehiculo, tiposServicio));
            }
        }
        return lista;
    }

    @Override
    public void guardar(VehiculoDTO vehiculo, String emailConductor, String licenciaConducir) {
        if (vehiculo == null) return;
        try (java.io.FileWriter fw = new java.io.FileWriter(rutaArchivo, true);
             java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
            String servicios = (vehiculo.getTiposServicio() == null) ? "" : String.join(";", vehiculo.getTiposServicio());
            pw.println(vehiculo.getPatente() + ";" + vehiculo.getModelo() + ";" + vehiculo.getCapacidadPasajeros()
                    + ";" + vehiculo.getTipoVehiculo() + ";" + vehiculo.getCategoriaVehiculo()
                    + (servicios.isEmpty() ? "" : ";" + servicios));
        } catch (Exception e) {
            System.err.println("Advertencia al guardar vehículo en TXT: " + e.getMessage());
        }
    }
}
