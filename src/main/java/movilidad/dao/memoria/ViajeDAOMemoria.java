package movilidad.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import movilidad.dao.interfaces.ViajeDAO;
import movilidad.modelo.Viaje;

/**
 * Implementación en memoria de {@link ViajeDAO}.
 * Mantiene la lista de viajes en RAM durante la sesión activa.
 */
public class ViajeDAOMemoria implements ViajeDAO {

    private final List<Viaje> viajes;

    public ViajeDAOMemoria() {
        this.viajes = new ArrayList<>();
    }

    @Override
    public void guardar(Viaje viaje) {
        viajes.add(viaje);
    }

    @Override
    public List<Viaje> listarTodos() {
        return new ArrayList<>(viajes);
    }
}

