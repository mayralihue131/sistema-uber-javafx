package movilidad.dao.interfaces;

import java.util.List;
import movilidad.dao.dto.VehiculoDTO;

public interface VehiculoDAO {

    List<VehiculoDTO> obtenerTodos() throws Exception;

    void guardar(VehiculoDTO vehiculo, String emailConductor, String licenciaConducir);

    default void guardar(VehiculoDTO vehiculo) {
        guardar(vehiculo, null, null);
    }
}

