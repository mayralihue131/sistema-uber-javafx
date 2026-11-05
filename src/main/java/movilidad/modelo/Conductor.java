package movilidad.modelo;
import java.util.ArrayList;
import java.util.List;

public class Conductor{
	private String licenciaConducir;
	private EstadoConductor estadoConductor;
	private List<Vehiculo> vehiculos;
	private CategoriaVehiculo categoriaVehiculoActivo;
	private Vehiculo vehiculoActivo;
	private List<Viaje> viajes;
	
	public Conductor(String licenciaConducir, EstadoConductor estadoConductor, 
			CategoriaVehiculo categoriaVehiculoActivo, Vehiculo vehiculoActivo) {
	
		this.licenciaConducir = licenciaConducir;
		this.estadoConductor = estadoConductor;
		this.vehiculoActivo = vehiculoActivo;
		this.categoriaVehiculoActivo = categoriaVehiculoActivo;
		
		this.viajes = new ArrayList<>();
		this.vehiculos = new ArrayList<>();
		
		this.vehiculos.add(vehiculoActivo);

	}
	public String getLicenciaConducir() {
		return licenciaConducir;
	}
	public void setLicenciaConducir(String licenciaConducir) {
		this.licenciaConducir = licenciaConducir;
	}
	public EstadoConductor getEstadoConductor() {
		return estadoConductor;
	}
	public void setEstadoConductor(EstadoConductor estadoConductor) {
		this.estadoConductor = estadoConductor;
	}
	//agregado
	public void setEstado(EstadoConductor estado) {
	    setEstadoConductor(estado);
	}
	public List<Vehiculo> getVehiculos() {
		return new ArrayList<>(vehiculos);
	}
	
	public CategoriaVehiculo getCategoriaVehiculoActivo() {
		return categoriaVehiculoActivo;
	}
	public void setCategoriaVehiculoActivo(CategoriaVehiculo categoriaVehiculoActivo) {
		this.categoriaVehiculoActivo = categoriaVehiculoActivo;
	}
	public Vehiculo getVehiculoActivo() {
		return vehiculoActivo;
	}
	public void setVehiculoActivo(Vehiculo vehiculoActivo) {
		if(!vehiculos.contains(vehiculoActivo)){
			 throw new IllegalArgumentException("El vehículo debe pertenecer al conductor");
		}
		this.vehiculoActivo = vehiculoActivo;
	}
	public List<Viaje> getViajes() {
		return new ArrayList<>(viajes);
	}
	//metodo implementado
	public void  agregarVehiculo(Vehiculo vehiculo) {
		vehiculos.add(vehiculo);
	}
	//metodo implementado
	public void agregarViaje(Viaje viaje) {
		viajes.add(viaje);
	}
	@Override
	public String toString() {
		return "Conductor [licenciaConducir=" + licenciaConducir + ", estadoConductor=" + estadoConductor
				+ ", vehiculos=" + vehiculos + ", categoriaVehiculoActivo=" + categoriaVehiculoActivo
				+ ", vehiculoActivo=" + vehiculoActivo + ", viajes=" + viajes + "]";
	}
	
}
