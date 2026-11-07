package movilidad.dao.dto;

/**
 * DTO para transferencia de datos de Servicio entre la capa DAO y el modelo.
 * Los enums se representan como String para independencia de la capa de datos.
 */
public class ServicioDTO {

    private String nombre;
    private double tarifaBase;
    private double precioKm;
    private double precioMinuto;
    private String tipoVehiculo;      // "AUTO" o "MOTO"
    private String categoriaVehiculo; // "ESTANDAR", "CONFORT", "PREMIUM"
    private String tipoServicio;      // "PASAJEROS" o "ENVIOS"

    public ServicioDTO(String nombre, double tarifaBase, double precioKm,
                       double precioMinuto, String tipoVehiculo,
                       String categoriaVehiculo, String tipoServicio) {
        this.nombre = nombre;
        this.tarifaBase = tarifaBase;
        this.precioKm = precioKm;
        this.precioMinuto = precioMinuto;
        this.tipoVehiculo = tipoVehiculo;
        this.categoriaVehiculo = categoriaVehiculo;
        this.tipoServicio = tipoServicio;
    }

    public String getNombre() { return nombre; }
    public double getTarifaBase() { return tarifaBase; }
    public double getPrecioKm() { return precioKm; }
    public double getPrecioMinuto() { return precioMinuto; }
    public String getTipoVehiculo() { return tipoVehiculo; }
    public String getCategoriaVehiculo() { return categoriaVehiculo; }
    public String getTipoServicio() { return tipoServicio; }
}

