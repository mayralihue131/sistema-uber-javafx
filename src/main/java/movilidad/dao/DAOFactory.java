package movilidad.dao;

import movilidad.dao.interfaces.ServicioDAO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Interfaz Abstract Factory para la capa DAO del sistema Uber.
 */
public interface DAOFactory {

    UsuarioDAO getUsuarioDAO();

    VehiculoDAO getVehiculoDAO();

    ServicioDAO getServicioDAO();

    /**
     * Indica si esta fábrica persiste directamente las altas de usuarios en su origen
     * (por ejemplo, base de datos relacional) sin requerir el log local de eventos.
     */
    default boolean persisteUsuariosEnOrigen() {
        return false;
    }
}
