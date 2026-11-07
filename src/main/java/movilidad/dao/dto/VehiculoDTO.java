package movilidad.dao.dto;

import java.util.List;

/**
 * DTO para transferencia de datos de Vehiculo entre la capa DAO y el modelo.
 * Los enums se representan como String para independencia de la capa de datos.
 */
public class VehiculoDTO {

    private String patente;
    private String modelo;
    private int capacidadPasajeros;
    private String tipoVehiculo;        // "AUTO" o "MOTO"
    private String categoriaVehiculo;   // "ESTANDAR", "CONFORT", "PREMIUM"
    private List<String> tiposServicio; // ["PASAJEROS", "ENVIOS"]

    public VehiculoDTO(String patente, String modelo, int capacidadPasajeros,
                       String tipoVehiculo, String categoriaVehiculo,
                       List<String> tiposServicio) {
        this.patente = patente;
        this.modelo = modelo;
        this.capacidadPasajeros = capacidadPasajeros;
        this.tipoVehiculo = tipoVehiculo;
        this.categoriaVehiculo = categoriaVehiculo;
        this.tiposServicio = tiposServicio;
    }

    public String getPatente() { return patente; }
    public String getModelo() { return modelo; }
    public int getCapacidadPasajeros() { return capacidadPasajeros; }
    public String getTipoVehiculo() { return tipoVehiculo; }
    public String getCategoriaVehiculo() { return categoriaVehiculo; }
    public List<String> getTiposServicio() { return tiposServicio; }
}

