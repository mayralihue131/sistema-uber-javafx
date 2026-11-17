package movilidad.dao.interfaces;

import java.util.List;
import movilidad.modelo.Viaje;

/**
 * Interfaz DAO que define la persistencia y consulta del ciclo de vida de los viajes.
 */
public interface ViajeDAO {

    /**
     * Almacena o actualiza el estado de un viaje en el origen de datos.
     *
     * @param viaje entidad del viaje a guardar
     */
    void guardar(Viaje viaje);

    /**
     * Recupera todos los viajes registrados.
     *
     * @return lista de viajes
     */
    List<Viaje> listarTodos();
}

