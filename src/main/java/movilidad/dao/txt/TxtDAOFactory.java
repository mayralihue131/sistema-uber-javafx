package movilidad.dao.txt;

import movilidad.dao.DAOFactory;
import movilidad.dao.interfaces.ServicioDAO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.interfaces.VehiculoDAO;

/**
 * Fábrica DAO que provee implementaciones basadas en archivos de texto plano (TXT).
 */
public class TxtDAOFactory implements DAOFactory {

    private final String rutaUsuarios;
    private final String rutaVehiculos;
    private final String rutaServicios;

    public TxtDAOFactory(String rutaUsuarios, String rutaVehiculos, String rutaServicios) {
        this.rutaUsuarios  = rutaUsuarios;
        this.rutaVehiculos = rutaVehiculos;
        this.rutaServicios = rutaServicios;
    }

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return new UsuarioDaoTxt(rutaUsuarios);
    }

    @Override
    public VehiculoDAO getVehiculoDAO() {
        return new VehiculoDaoTxt(rutaVehiculos);
    }

    @Override
    public ServicioDAO getServicioDAO() {
        return new ServicioDaoTxt(rutaServicios);
    }
}
