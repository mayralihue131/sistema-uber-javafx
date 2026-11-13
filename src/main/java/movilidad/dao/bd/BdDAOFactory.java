package movilidad.dao.bd;

import movilidad.dao.DAOFactory;
import movilidad.dao.interfaces.ServicioDAO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Fábrica DAO que provee implementaciones basadas en base de datos relacional (JDBC).
 */
public class BdDAOFactory implements DAOFactory {

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return new UsuarioDaoBd();
    }

    @Override
    public VehiculoDAO getVehiculoDAO() {
        return new VehiculoDaoBd();
    }

    @Override
    public ServicioDAO getServicioDAO() {
        return new ServicioDaoBd();
    }

    @Override
    public boolean persisteUsuariosEnOrigen() {
        return true;
    }

    @Override
    public boolean persisteVehiculosEnOrigen() {
        return true;
    }
}
