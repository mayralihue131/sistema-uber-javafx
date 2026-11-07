package movilidad.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import movilidad.dao.DAOFactory;
import movilidad.dao.dto.ServicioDTO;
import movilidad.dao.dto.VehiculoDTO;
import movilidad.dao.interfaces.ServicioDAO;
import movilidad.dao.interfaces.UsuarioDAO;
import movilidad.dao.interfaces.VehiculoDAO;
import movilidad.dao.interfaces.ViajeDAO;

/**
 * Fábrica DAO para pruebas unitarias y entornos en memoria (sin conexión externa).
 */
public class MemoriaDAOFactory implements DAOFactory {

    private final UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();
    private final ViajeDAO viajeDAO = new ViajeDAOMemoria();

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    @Override
    public VehiculoDAO getVehiculoDAO() {
        return new VehiculoDAO() {
            @Override
            public List<VehiculoDTO> obtenerTodos() {
                return new ArrayList<>();
            }
        };
    }

    @Override
    public ServicioDAO getServicioDAO() {
        return new ServicioDAO() {
            @Override
            public List<ServicioDTO> obtenerTodos() {
                return new ArrayList<>();
            }
        };
    }

    public ViajeDAO getViajeDAO() {
        return viajeDAO;
    }
}

