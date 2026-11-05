package movilidad.modelo;

import java.util.ArrayList;
import java.util.List;

public class Vehiculo {
	private String patente;
	private String modelo;
	private int capacidadPasajeros;
	private TipoVehiculo tipoVehiculo;
	private List<TipoServicio> tipoServicio;
	private CategoriaVehiculo categoriaVehiculo;
	private Ubicacion ubicacion;
	
	public Vehiculo(String patente, String modelo, int capacidadPasajeros, TipoVehiculo tipoVehiculo,
			 CategoriaVehiculo categoriaVehiculo, Ubicacion ubicacion) {
		this.patente = patente;
		this.modelo = modelo;
		this.capacidadPasajeros = capacidadPasajeros;
		this.tipoVehiculo = tipoVehiculo;
		this.tipoServicio = new ArrayList<>();
		this.categoriaVehiculo = categoriaVehiculo;
		this.ubicacion = ubicacion;
	}
	public Vehiculo(String patente, String modelo, int capacidadPasajeros,
	        TipoVehiculo tipoVehiculo, CategoriaVehiculo categoriaVehiculo,
	        TipoServicio tipoServicio) {

	    this(patente, modelo, capacidadPasajeros, tipoVehiculo,
	            categoriaVehiculo, (Ubicacion) null);

	    agregarTipoServicio(tipoServicio);
	}
	public String getPatente() {
		return patente;
	}
	public void setPatente(String patente) {
		this.patente = patente;
	}
	public String getModelo() {
		return modelo;
	}
	public void setModelo(String modelo) {
		this.modelo = modelo;
	}
	public int getCapacidadPasajeros() {
		return capacidadPasajeros;
	}
	public void setCapacidadPasajeros(int capacidadPasajeros) {
		this.capacidadPasajeros = capacidadPasajeros;
	}
	public TipoVehiculo getTipoVehiculo() {
		return tipoVehiculo;
	}
	public void setTipoVehiculo(TipoVehiculo tipoVehiculo) {
		this.tipoVehiculo = tipoVehiculo;
	}
	public List<TipoServicio> getTipoServicio() {
		return new ArrayList<>(tipoServicio);
	}
	
	public CategoriaVehiculo getCategoriaVehiculo() {
		return categoriaVehiculo;
	}
	public void setCategoriaVehiculo(CategoriaVehiculo categoriaVehiculo) {
		this.categoriaVehiculo = categoriaVehiculo;
	}
	public Ubicacion getUbicacion() {
		return ubicacion;
	}
	public void setUbicacion(Ubicacion ubicacion) {
		this.ubicacion = ubicacion;
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((patente == null) ? 0 : patente.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vehiculo other = (Vehiculo) obj;
		if (patente == null) {
			if (other.patente != null)
				return false;
		} else if (!patente.equals(other.patente))
			return false;
		return true;
	}
		
	public void agregarTipoServicio(TipoServicio tipoServicio) {
	    if (tipoServicio == null) {
	        throw new IllegalArgumentException(
	                "El tipo de servicio no puede ser null");
	    }

	    if (!this.tipoServicio.contains(tipoServicio)) {
	        this.tipoServicio.add(tipoServicio);
	    }
	}
	
	@Override
	public String toString() {
		return "Vehiculo [patente=" + patente + ", modelo=" + modelo + ", capacidadPasajeros=" + capacidadPasajeros
				+ ", tipoServicio=" + tipoServicio + ", categoriaVehiculo=" + categoriaVehiculo + "]";
	}
}
