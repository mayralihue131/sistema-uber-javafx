package movilidad.modelo;

import java.time.LocalDateTime;

public class RegistroViaje {

    private LocalDateTime fechaHora;
    private EstadoViaje estadoViaje;

    public RegistroViaje() {
    }

    public RegistroViaje(LocalDateTime fechaHora, EstadoViaje estadoViaje) {
        this.fechaHora = fechaHora;
        this.estadoViaje = estadoViaje;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public EstadoViaje getEstadoViaje() {
        return estadoViaje;
    }

    public void setEstadoViaje(EstadoViaje estadoViaje) {
        this.estadoViaje = estadoViaje;
    }

    @Override
    public String toString() {
        return "RegistroViaje{" + "fechaHora=" + fechaHora + ", estadoViaje=" + estadoViaje + '}';
    }
}
