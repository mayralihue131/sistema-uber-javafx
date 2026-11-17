package movilidad.dao;

import movilidad.dao.interfaces.ServicioDAO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Fábrica abstracta (patrón Abstract Factory) para la capa de acceso a datos (DAO) del sistema Uber.
 * Provee acceso a los DAOs de usuarios, vehículos y servicios según el backend configurado
 * (PostgreSQL, archivos de texto plano o memoria volatil).
 */
public interface DAOFactory {

    /**
     * Obtiene la implementación de {@link UsuarioDAO} correspondiente al origen de datos activo.
     *
     * @return instancia de UsuarioDAO
     */
    UsuarioDAO getUsuarioDAO();

    /**
     * Obtiene la implementación de {@link VehiculoDAO} correspondiente al origen de datos activo.
     *
     * @return instancia de VehiculoDAO
     */
    VehiculoDAO getVehiculoDAO();

    /**
     * Obtiene la implementación de {@link ServicioDAO} correspondiente al origen de datos activo.
     *
     * @return instancia de ServicioDAO
     */
    ServicioDAO getServicioDAO();

    /**
     * Indica si esta fábrica persiste directamente las altas de usuarios en su origen
     * (por ejemplo, base de datos relacional) sin requerir el log local de eventos.
     *
     * @return {@code true} si persiste en el origen externo, {@code false} por defecto
     */
    default boolean persisteUsuariosEnOrigen() {
        return false;
    }

    /**
     * Indica si esta fábrica persiste directamente las altas de vehículos en su origen externo.
     *
     * @return {@code true} si persiste en el origen externo
     */
    default boolean persisteVehiculosEnOrigen() {
        return persisteUsuariosEnOrigen();
    }
}
