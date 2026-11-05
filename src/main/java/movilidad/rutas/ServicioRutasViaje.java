package movilidad.rutas;

import java.util.Objects;

import movilidad.modelo.TipoVehiculo;

public final class ServicioRutasViaje {

    private final PlanificadorRutas planificador;

    public ServicioRutasViaje(PlanificadorRutas planificador) {
        this.planificador = Objects.requireNonNull(planificador);
    }

    public RutasViaje calcular(
            PuntoRuta conductor,
            PuntoRuta origen,
            PuntoRuta destino,
            TipoVehiculo tipoVehiculo) {

        RutaCalculada hastaOrigen = planificador.calcular(
            conductor, origen, tipoVehiculo
        );

        RutaCalculada hastaDestino = planificador.calcular(
            origen, destino, tipoVehiculo
        );

        return new RutasViaje(hastaOrigen, hastaDestino);
    }
}