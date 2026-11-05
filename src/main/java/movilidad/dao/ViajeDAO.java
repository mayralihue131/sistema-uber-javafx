package movilidad.dao;

import java.util.List;
import movilidad.modelo.Viaje;

public interface ViajeDAO {

    void guardar(Viaje viaje);

    List<Viaje> listarTodos();
}