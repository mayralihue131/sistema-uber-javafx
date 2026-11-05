package movilidad.modelo;

import java.util.ArrayList;
import java.util.List;

public class Cliente {
	private List<Viaje> viajes;

	public Cliente() {
		this.viajes = new ArrayList<>();
	}
	
	public List<Viaje> getViajes() {
		return new ArrayList<>(viajes);
	}	
	public void agregarViaje(Viaje viaje) {
		viajes.add(viaje);
	}
	public boolean enViaje() {
	    for (Viaje viaje : viajes) {
	        EstadoViaje estado = viaje.estadoActual();

	        if (estado == EstadoViaje.SOLICITADO
	                || estado == EstadoViaje.ACEPTADO
	                || estado == EstadoViaje.INICIADO) {
	            return true;
	        }
	    }
	    return false;
	}
	@Override
	public String toString() {
		return "Cliente [viajes=" + viajes + "]";
	}
	
}
