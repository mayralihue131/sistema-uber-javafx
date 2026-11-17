package movilidad.rutas;

import java.util.Objects;

import movilidad.modelo.TipoVehiculo;

/**
 * Servicio de dominio que orquesta el cálculo combinado de los dos tramos viales de un viaje:
 * el tramo de búsqueda del chofer hacia el origen y el tramo de transporte hacia el destino.
 */
public final class ServicioRutasViaje {

    private final PlanificadorRutas planificador;

    /**
     * Construye el servicio inyectando el planificador de rutas.
     *
     * @param planificador implementación concreta de {@link PlanificadorRutas}
     */
    public ServicioRutasViaje(PlanificadorRutas planificador) {
        this.planificador = Objects.requireNonNull(planificador);
    }

    /**
     * Calcula ambos tramos viales para el tipo de vehículo especificado.
     *
     * @param conductor    posición actual del conductor
     * @param origen       punto de recogida del cliente
     * @param destino      punto de llegada final
     * @param tipoVehiculo perfil del vehículo (AUTO o MOTO)
     * @return instancia de {@link RutasViaje} conteniendo ambas polilíneas y métricas
     */
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