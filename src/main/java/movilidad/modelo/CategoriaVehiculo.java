package movilidad.modelo;

public enum CategoriaVehiculo {
	ESTANDAR (1),
	CONFORT (2),
	PREMIUM(3);
	
	private final int codigo;

	private CategoriaVehiculo(int codigo) {
		this.codigo = codigo;
	}
	public int getCodigo() {
		return codigo;
	}
	
}
