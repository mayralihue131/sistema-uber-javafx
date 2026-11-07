package movilidad.dao.interfaces;

import java.util.List;
import movilidad.dao.dto.ServicioDTO;

public interface ServicioDAO {

    List<ServicioDTO> obtenerTodos() throws Exception;
}

