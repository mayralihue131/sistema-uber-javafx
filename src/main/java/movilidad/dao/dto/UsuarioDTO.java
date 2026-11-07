package movilidad.dao.dto;

/**
 * DTO para transferencia de datos de Usuario entre la capa DAO y el modelo.
 * Transporta solo los campos primitivos/String, sin objetos del dominio.
 */
public class UsuarioDTO {

    private String nombre;
    private String telefono;
    private String email;
    // Conductor (opcional): vacío/null si el usuario solo es cliente
    private String licenciaConducir;
    private String patenteVehiculoActivo;

    public UsuarioDTO(String nombre, String telefono, String email,
                      String licenciaConducir, String patenteVehiculoActivo) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.licenciaConducir = licenciaConducir;
        this.patenteVehiculoActivo = patenteVehiculoActivo;
    }

    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getLicenciaConducir() { return licenciaConducir; }
    public String getPatenteVehiculoActivo() { return patenteVehiculoActivo; }
}

