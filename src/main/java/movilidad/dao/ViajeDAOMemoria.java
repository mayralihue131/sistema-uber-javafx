package movilidad.dao;

import java.util.ArrayList;
import java.util.List;
import movilidad.modelo.Viaje;

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