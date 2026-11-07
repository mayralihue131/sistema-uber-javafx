package movilidad.dao.interfaces;

import java.util.List;
import movilidad.dao.dto.VehiculoDTO;

public interface VehiculoDAO {

    List<VehiculoDTO> obtenerTodos() throws Exception;
}

