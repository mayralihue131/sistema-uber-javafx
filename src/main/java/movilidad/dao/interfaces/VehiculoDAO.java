package movilidad.dao.interfaces;

import java.util.List;
import movilidad.dao.dto.VehiculoDTO;

/**
 * Interfaz DAO para la lectura y registro de vehículos en el sistema de persistencia.
 */
public interface VehiculoDAO {

    /**
     * Recupera todos los vehículos almacenados.
     *
     * @return lista de objetos {@link VehiculoDTO}
     * @throws Exception si ocurre un error de lectura en el origen
     */
    List<VehiculoDTO> obtenerTodos() throws Exception;

    /**
     * Persiste un vehículo y lo vincula con el conductor y su licencia.
     *
     * @param vehiculo         datos de transferencia del vehículo
     * @param emailConductor   correo del chofer propietario
     * @param licenciaConducir número de licencia habilitante
     */
    void guardar(VehiculoDTO vehiculo, String emailConductor, String licenciaConducir);

    /**
     * Persiste un vehículo sin asignación inmediata de conductor.
     *
     * @param vehiculo datos de transferencia del vehículo
     */
    default void guardar(VehiculoDTO vehiculo) {
        guardar(vehiculo, null, null);
    }
}

