package movilidad.dao.interfaces;

import java.util.List;
import movilidad.dao.dto.ServicioDTO;

/**
 * Interfaz DAO para la lectura y recuperación del catálogo de servicios de movilidad.
 */
public interface ServicioDAO {

    /**
     * Recupera todos los servicios disponibles desde el origen de datos activo.
     *
     * @return lista de objetos de transferencia {@link ServicioDTO}
     * @throws Exception si ocurre un error durante el acceso a datos
     */
    List<ServicioDTO> obtenerTodos() throws Exception;
}

