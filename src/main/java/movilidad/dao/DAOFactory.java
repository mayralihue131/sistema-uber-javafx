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
}
